package com.topcoder.nasa.job;

import gov.nasa.pds.entities.EntityInfo;
import gov.nasa.pds.entities.MapImage;
import gov.nasa.pds.entities.Page;
import gov.nasa.pds.entities.PagedResults;
import gov.nasa.pds.entities.SearchCriteria;
import gov.nasa.pds.services.DataSetProcessingException;
import gov.nasa.pds.services.DataSetService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.topcoder.nasa.file.S3FileUploader;
import com.topcoder.nasa.image.FileSystemImagePreparer;
import com.topcoder.nasa.image.ImageFetcherTask;
import com.topcoder.nasa.job.binary.AggregateExeTask;
import com.topcoder.nasa.job.binary.ExeTaskCompletedListener;
import com.topcoder.nasa.job.binary.ExeTask_gdal_translate;
import com.topcoder.nasa.job.binary.ExeTask_gdalbuildvrt;
import com.topcoder.nasa.job.hadoop.HadoopJobCompletedListener;
import com.topcoder.nasa.job.hadoop.HadoopRunningJobMonitor;
import com.topcoder.nasa.job.hadoop.HadoopWorkflow;
import com.topcoder.nasa.job.http.ImageFetcher;
import com.topcoder.nasa.rest.GenerateResource;

/**
 * The job workflow. We could use a state machine and/or workflow engine and/or some other stuff, but this workflow is
 * simple enough for now. Here's the lowdown:
 * <p/>
 * <ul>
 * <li>When the (singleton) instance is fully constracted, the {@link #init()} method is called to associate itself with
 * the {@link HadoopRunningJobMonitor}</li>
 * <li>When a request to {@link GenerateResource} comes in, we query ODE and ask it to tell us about all the images that
 * match the {@link OdeSearchCriteria} the client passed in</li>
 * <li>The ODE request takes a long time and may require pagination so we do this asynchronously - this is the job of
 * the {@link OdeServiceTask}</li>
 * <li>As ODE pages come back, we schedule the images that they refer to to be fetched into the image cache using the
 * {@link ImageFetcherTask}</li>
 * <li>Once all the images have been fetched into the image cache for a job, we use the {@link FileSystemImagePreparer}
 * to prepare the filesystem to start the Hadoop job</li>
 * <li>Then, we start the Hadoop job</li>
 * <li>When a Hadoop job completes, one of {@link #onHadoopJobSuccessful(LmmpJob)} /
 * {@link #onHadoopJobFailure(LmmpJob, String)} is called</li>
 * <li>If the job fails, we stop doing anything more</li>
 * <li>Else, we launch the {@link #exeTask}, which is actually an {@link AggregateExeTask} of all the executables we
 * need to run for this workflow.</li>
 * <li>Once the {@link AggregateExeTask} tells us it has completed, we upload the file to S3</li>
 * <li>Then we're done</li>
 * </ul>
 * 
 *
 */
@Component
public class LmmpJobWorkflow implements HadoopJobCompletedListener, ExeTaskCompletedListener {
    private static final Logger LOG = LoggerFactory.getLogger(LmmpJobWorkflow.class);

    private static final int MAP_IMAGES_PER_PAGE = 100;

    /** Allows {@link #startFor(LmmpJob, SearchCriteria)} to return immediately. */
    private static final ExecutorService WORKFLOW_EXECUTION_THREADPOOL = Executors.newFixedThreadPool(1);

    @Autowired
    private LmmpJobRepository lmmpJobRepository;

    @Autowired
    private HadoopRunningJobMonitor hadoopRunningJobMonitor;

    @Autowired
    private S3FileUploader fileUploader;

    @Autowired
    private FileSystemImagePreparer fileSystemImagePreparer;

    @Autowired
    private HadoopWorkflow hadoopWorkflow;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private ImageFetcher imageFetcher;

    private AggregateExeTask exeTask;

    // =========================================================================

    /**
     * Associate ourselves with the {@link HadoopRunningJobMonitor} and creates the {@link #exeTask} .
     */
    @PostConstruct
    public void init() {
        LOG.info("Creating AggregateExeTask");
        exeTask = new AggregateExeTask();

        exeTask.addExecutableJobProcessor(new ExeTask_gdalbuildvrt());
        exeTask.addExecutableJobProcessor(new ExeTask_gdal_translate());

        exeTask.setRunCompleteListener(this);

        LOG.info("Setting myself as the HadoopRunningJobMonitor callback!");
        hadoopRunningJobMonitor.setHadoopJobCompletedListener(this);
    }

    // =========================================================================

    /**
     * Start the workflow for a new job.
     * 
     * @param job
     *            the job to start the workflow for
     * @param searchCriteria
     *            the search criteria we should pass to PDS for image retrieval
     * @throws IllegalStateException
     *             if an {@link LmmpJob} is already currently running
     */
    public void startFor(final LmmpJob job, final SearchCriteria searchCriteria) {
        List<LmmpJob> currentlyRunningJobs = lmmpJobRepository.findRunningJobs();
        if (!currentlyRunningJobs.isEmpty()) {
            throw new IllegalStateException("Cannot run more than one job. Currently running: " + currentlyRunningJobs);
        }

        WORKFLOW_EXECUTION_THREADPOOL.submit(new Runnable() {
            public void run() {
                try {
                    doStartFor(job, searchCriteria);
                } catch (DataSetProcessingException e) {
                    LOG.error("Exception thrown while processing job id {}", job.getUuid(), e);

                    job.failed(e.getMessage());
                    lmmpJobRepository.update(job);
                }
            }
        });
    }

    /**
     * Synchronously executes the first "part" of an LMMP job workflow: the {@link DataSetService} querying and the
     * Hadoop scheduling.
     * 
     * @param job
     *            the job to start processing
     * @param searchCriteria
     *            the search criteria of the map images of interest
     * @throws DataSetProcessingException
     *             if something went wrong finding the map images of interest
     */
    private void doStartFor(final LmmpJob job, SearchCriteria searchCriteria) throws DataSetProcessingException {
        LOG.info("Computing which images to use for job id {}", job.getUuid());
        List<String> allUrls = computeImagePaths(searchCriteria);

        if (allUrls.isEmpty()) {
            job.failed("No images for SearchCriteria");
            lmmpJobRepository.update(job);
            return;
        }

        List<File> allFiles = imageFetcher.fetchAll(allUrls);

        while (imageFetcher.isFetching()) {
            try {
                LOG.info("Waiting for images to download...");
                Thread.sleep(1000);// completely arbitrary sleep time
            } catch (InterruptedException e) {
                // swallow
            }
        }

        LOG.info("Computing requisite images for job {}; starting to copy them...", job.getUuid());
        fileSystemImagePreparer.prepare(allFiles);

        LOG.info("Images copied! Starting Hadoop job...");
        hadoopWorkflow.executeFor(job);

        lmmpJobRepository.update(job);
    }

    /**
     * Uses our {@link #dataSetService} to find work out which {@link MapImage}s are of interest to the given
     * {@link SearchCriteria}, and subsequently asks the {@link DataSetService} to fetch all the paths to those images.
     * The image paths are the URLs we then need to download.
     * 
     * @param searchCriteria
     *            the criteria that each MapImage must meet
     * @return
     * @throws DataSetProcessingException
     *             if something went wrong fetching the data
     */
    private List<String> computeImagePaths(SearchCriteria searchCriteria) throws DataSetProcessingException {
        int nextPage = 1;

        List<String> allUrls = new ArrayList<String>();

        while (true) {
            LOG.info("Fetching page {} of MapImage EntityInfos for SearchCrtieria {}", nextPage,
                    searchCriteria.toJSONString());
            Page page = new Page(nextPage, MAP_IMAGES_PER_PAGE);

            // get this page of identifiers (of MapImage entities) of interest
            PagedResults<EntityInfo> results = dataSetService.searchMapImagesByCriteria(searchCriteria, page);

            List<String> paths = dataSetService.getMapImagePaths(results.getResults());

            for (String path : paths) {
                allUrls.add(path);
            }

            if (results.getResults().size() != MAP_IMAGES_PER_PAGE) {
                break;
            }

            ++nextPage;
        }

        return allUrls;
    }

    // =========================================================================

    /**
     * {@link AggregateExeTask} completed - we are done - let's upload
     */
    @Override
    public void onTaskCompleted(LmmpJob lmmpJob) {
        LOG.info("Job UUID {} executables are completed -- uploading file!", lmmpJob.getUuid());

        fileUploader.upload(lmmpJob);

        LOG.info("Job UUID {} is uploaded and completed!", lmmpJob.getUuid());

        // _assume_ all is good
        lmmpJob.completed();
        lmmpJobRepository.update(lmmpJob);
    }

    // =========================================================================

    /**
     * Hadoop job was successful, let's start the {@link #exeTask}
     */
    @Override
    public void onHadoopJobSuccessful(LmmpJob job) {
        // let's kick off the exe tasks
        LOG.info("Hadoop job for uuid {} was successful; kicking off the binaries", job.getUuid());

        job.markAsRunningExecutables();
        lmmpJobRepository.update(job);

        // go
        exeTask.runTaskFor(job);
    }

    /**
     * Sad face :( Hadoop job failed.
     */
    @Override
    public void onHadoopJobFailure(LmmpJob job, String failReason) {
        LOG.info("Hadoop job for uuid {} failed; updating in DB", job.getUuid());
        job.failed(failReason);

        lmmpJobRepository.update(job);
    }

    // =========================================================================

}

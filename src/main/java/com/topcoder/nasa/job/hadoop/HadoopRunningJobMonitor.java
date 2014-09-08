package com.topcoder.nasa.job.hadoop;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.RunningJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.topcoder.nasa.image.FileSystemImagePreparer;
import com.topcoder.nasa.job.LmmpJob;
import com.topcoder.nasa.job.LmmpJobRepository;

/**
 * Monitor class that is responsible for checking when Hadoop jobs have completed successfully or failed.
 * <p/>
 * Note that this class does NOT maintain an "operating context" (i.e. thread). Instead, it expects a third party to
 * call it periodically to start a monitoring iteration. I.e. a timer.
 * 
 */
@Component
public class HadoopRunningJobMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(HadoopRunningJobMonitor.class);

    @Autowired
    private LmmpJobRepository lmmpJobRepository;

    @Autowired
    private JobClient jobClient;

    @Autowired
    private FileSystemImagePreparer fileSystemImagePreparer;

    private HadoopJobCompletedListener hadoopJobCompletedListener;

    // =============================================================================

    public void monitor() {
        LOG.debug("Job monitor kicking off...");

        try {
            doMonitor();
        } catch (IOException e) {
            throw new IllegalStateException("Exception in Job Monitor iteration", e);
        }

        LOG.debug("Job monitor done");
    }

    // =============================================================================

    private void doMonitor() throws IOException {
        // find the jobs in the DB that are running in Hadoop
        List<LmmpJob> runningJobs = lmmpJobRepository.findRunningHadoopJobs();

        // nothing to do? Move on
        if (runningJobs.isEmpty()) {
            LOG.debug("No running jobs!");
            return;
        }

        // TEMPORARY GUARD: at the moment we can only support ONE Hadoop job running at a time
        if (runningJobs.size() > 1) {
            throw new IllegalStateException(
                    "Multiple Hadoop jobs are running; right now we only support a single job: " + runningJobs.size());
        }

        // for each job CURRENTLY RUNNING IN HADOOP...
        for (LmmpJob job : runningJobs) {
            LOG.debug("Asking Hadoop for details of hadoop job id {}", job.getHadoopJobId());

            // ... get its Hadoop reference
            RunningJob rj = jobClient.getJob(JobID.forName(job.getHadoopJobId()));

            if (rj == null) {
                throw new IllegalStateException("Hadoop did not know about hadoop job id " + job.getHadoopJobId()
                        + " - DB may be out of sync");
            }

            // check if completed
            boolean completed = rj.isComplete();

            // if this job is not yet completed, move on
            if (!completed) {
                LOG.debug("Lmmp Job {} did not yet complete in Hadoop", job.getUuid());
                continue;
            }

            // this job completed - but was it successful?
            boolean successful = rj.isSuccessful();

            if (successful) {
                LOG.info("Lmmp Job {} completed successfully in Hadoop", job.getUuid());

                hadoopJobCompletedListener.onHadoopJobSuccessful(job);
            } else {
                String failInfo = rj.getFailureInfo();

                LOG.info("Lmp Job {} completed BUT FAILED because: {}", job.getUuid(), failInfo);

                hadoopJobCompletedListener.onHadoopJobFailure(job, failInfo);
            }

            // TEMPORARY GUARD: when the Hadoop job is complete, clear-down the pic directory
            if (runningJobs.size() == 1) {
                fileSystemImagePreparer.cleanPicDirectory();
            }
        }
    }

    // =============================================================================

    public void setHadoopJobCompletedListener(HadoopJobCompletedListener hadoopJobCompletedListener) {
        this.hadoopJobCompletedListener = hadoopJobCompletedListener;
    }
}

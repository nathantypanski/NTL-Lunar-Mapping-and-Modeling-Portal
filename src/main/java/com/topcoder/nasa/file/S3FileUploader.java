package com.topcoder.nasa.file;

import java.io.File;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.MediaType;

import org.jclouds.ContextBuilder;
import org.jclouds.aws.s3.blobstore.AWSS3BlobStoreContext;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.options.PutOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.topcoder.nasa.job.LmmpJob;

/**
 * Responsible for uploading successful LmmpJobs' outputs (i.e. "mosaic.png") to S3.
 */
public class S3FileUploader extends AbstractS3FileManager {
    private static final Logger LOG = LoggerFactory.getLogger(S3FileUploader.class);

    private BlobStoreContext blobStoreContext;

    // =========================================================================

    public void upload(LmmpJob job) {
        LOG.info("Starting upload of job UUID {} to S3", job.getUuid());

        try {
            doUpload(job);
            LOG.info("Completed upload of job UUID {} to S3", job.getUuid());
        } catch (Exception e) {
            LOG.error("Exception while uploading job UUID {} to S3", e);
        }
    }

    private void doUpload(LmmpJob job) {
        BlobStore blobStore = blobStoreContext.getBlobStore();

        File jobFile = job.getJobCompletedFile();
        String s3Key = S3FileConstants.computeKey(job);

        Blob blob = blobStore //
                .blobBuilder(s3Key) //
                .payload(Files.asByteSource(jobFile)) //
                .contentType(MediaType.APPLICATION_OCTET_STREAM) //
                .build();

        blob.getMetadata().getContentMetadata().setContentLength(jobFile.length());

        blobStore.putBlob(bucketName, blob, PutOptions.Builder.multipart());
    }

    // =========================================================================

    @PostConstruct
    public void setupBlobStoreContext() {
        LOG.info("Initializing S3FileUploader with the provided credentials");

        this.blobStoreContext = ContextBuilder.newBuilder("aws-s3")//
                .credentials(identity, credential) //
                .buildView(AWSS3BlobStoreContext.class);
    }

    @PreDestroy
    public void teardownBlobStoreContext() throws Exception {
        LOG.info("Gracefully tearing down S3FileUploader");

        blobStoreContext.close();
    }
}

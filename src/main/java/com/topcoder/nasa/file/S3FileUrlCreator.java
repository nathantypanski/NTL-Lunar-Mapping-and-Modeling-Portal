package com.topcoder.nasa.file;

import java.net.URL;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.topcoder.nasa.job.LmmpJob;

/**
 * Responsible for producing URLs that allow (time limited) access to a resource in S3.
 */
public class S3FileUrlCreator extends AbstractS3FileManager implements InitializingBean {
    private static final Logger LOG = LoggerFactory.getLogger(S3FileUrlCreator.class);

    private AmazonS3Client s3Client;

    public String generateUrlFor(LmmpJob job) {
        Date now = new Date();
        Date fiveMinsFromNow = new Date(now.getTime() + 300000);

        String key = S3FileConstants.computeKey(job);

        URL url = s3Client.generatePresignedUrl(bucketName, key, fiveMinsFromNow);

        return url.toExternalForm();
    }

    // =========================================================================

    @Override
    public void afterPropertiesSet() throws Exception {
        AWSCredentials credentials = new BasicAWSCredentials(identity, credential);
        s3Client = new AmazonS3Client(credentials);

        LOG.info("Created native S3 Client...");
    }
}

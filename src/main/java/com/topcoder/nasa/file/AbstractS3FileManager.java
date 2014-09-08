package com.topcoder.nasa.file;

import org.springframework.beans.factory.annotation.Required;

/**
 * Base class that all S3-related file classes should extend. Simply encapsulates salient S3 data.
 * This might make configuration in a DI framework more straightforward.
 *
 */
public class AbstractS3FileManager {
    /** The S3 bucket to use */
    protected String bucketName;

    /** The identity (access key id) */
    protected String identity;

    /** The credential (access secret) */
    protected String credential;

    // =========================================================================

    @Required
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Required
    public void setIdentity(String identity) {
        this.identity = identity;
    }

    @Required
    public void setCredential(String credential) {
        this.credential = credential;
    }

}

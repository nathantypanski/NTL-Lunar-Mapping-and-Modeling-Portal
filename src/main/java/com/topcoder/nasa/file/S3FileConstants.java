package com.topcoder.nasa.file;

import com.topcoder.nasa.job.LmmpJob;

/**
 * When we upload a file to its ultimate destination, we prepend the {@link #S3_NAME_PREFIX} to the
 * UUID.
 *
 */
public class S3FileConstants {
    static final String S3_NAME_PREFIX = "lmmp-rest-";

    static String computeKey(LmmpJob job) {
        return S3_NAME_PREFIX + job.getUuid() + "." + job.getFileType();
    }
}

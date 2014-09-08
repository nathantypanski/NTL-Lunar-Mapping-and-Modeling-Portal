package com.topcoder.nasa.job.hadoop;

import com.topcoder.nasa.job.LmmpJob;

/**
 * When a Hadoop job completes successfully or fails miserably, the implementor of this interface is
 * told.
 *
 */
public interface HadoopJobCompletedListener {
    /**
     * Job succeeded. Yes! This is good.
     */
    void onHadoopJobSuccessful(LmmpJob job);

    /**
     * Job failed. Something went wrong.
     */
    void onHadoopJobFailure(LmmpJob job, String failReason);
}

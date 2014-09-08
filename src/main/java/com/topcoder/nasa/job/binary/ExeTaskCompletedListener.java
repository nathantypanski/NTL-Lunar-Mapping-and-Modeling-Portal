package com.topcoder.nasa.job.binary;

import com.topcoder.nasa.job.LmmpJob;

/**
 * When an ExeTask completes for a given {@link LmmpJob}, {@link #onTaskCompleted(LmmpJob)} is
 * called.
 *
 */
public interface ExeTaskCompletedListener {
    void onTaskCompleted(LmmpJob lmmpJob);
}

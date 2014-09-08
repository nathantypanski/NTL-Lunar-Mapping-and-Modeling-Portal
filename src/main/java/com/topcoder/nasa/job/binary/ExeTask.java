package com.topcoder.nasa.job.binary;

import com.topcoder.nasa.job.LmmpJob;

/**
 * An "Executable Task". As part of the mosaic file creation, we run a Hadoop task which produces
 * some output(s). Then, we need to run a bunch of binary/executables on those outputs.
 * <p/>
 * Implementations of this class encapsulate the running of one of those binaries.
 *
 */
public interface ExeTask {

    /**
     * Run this EXEcutable TASK, for the the given LmmpJob
     */
    Process runTaskFor(LmmpJob lmmpJob);

    /**
     * When this ExeTask has finished, callback to this guy
     */
    void setRunCompleteListener(ExeTaskCompletedListener runCompleteListener);

}
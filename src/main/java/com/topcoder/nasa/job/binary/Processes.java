package com.topcoder.nasa.job.binary;

/**
 * Noddy helper class.
 *
 */
public class Processes {
    public static boolean hasFinished(Process p) {
        try {
            p.exitValue();
        } catch (IllegalThreadStateException e) {
            return false;
        }

        return true;
    }
}

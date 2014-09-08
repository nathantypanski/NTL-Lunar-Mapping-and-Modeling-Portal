package com.topcoder.nasa.job;

import java.util.List;

import com.topcoder.nasa.job.LmmpJob.Status;

/**
 * <a href="http://en.wikipedia.org/wiki/Domain-driven_design">DDD style repository</a> that acts as
 * the gateway to system's universe of {@link LmmpJob}s.
 */
public interface LmmpJobRepository {
    /**
     * Adds a new Job to the repository.
     * 
     * @param job
     *            the new Job to add
     */
    public void add(LmmpJob job);

    /**
     * Updates an existing Job in the repository. Will update <b>all</b> properties - not just those
     * that are set in the instance provided.
     * 
     * @param job
     *            the Job to update
     */
    public void update(LmmpJob job);

    /**
     * Loads a single Job by UUID
     * 
     * @param uuid
     * @return a Job if found; null otherwise
     */
    public LmmpJob load(String uuid);

    /**
     * Finds all the jobs that have a persisted status of {@link Status#RUNNING_HADOOP}
     * 
     * @return the running jobs or an empty list if none
     */
    public List<LmmpJob> findRunningHadoopJobs();

    /**
     * Finds all the jobs have have a persisted status of either {@link Status#RUNNING_PDS_API},
     * {@link Status#RUNNING_HADOOP} or {@link Status#RUNNING_EXECUTABLES}.
     * 
     * @return the running jobs or an empty list if none
     */
    List<LmmpJob> findRunningJobs();
}

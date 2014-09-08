package com.topcoder.nasa.job.hadoop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.topcoder.nasa.job.LmmpJob;

/**
 * The Hadoop part of the workflow does two things:
 * <ol>
 * <li>Prepares the HDFS Environment - see {@link HadoopEnvironmentPreparer}</li>
 * <li>Spawns the job using the JobClient - see {@link HadoopJobRunner}</li>
 * </ol>
 *
 */
@Component
public class HadoopWorkflow {
    private static final Logger LOG = LoggerFactory.getLogger(HadoopWorkflow.class);

    @Autowired
    private HadoopEnvironmentPreparer hadoopEnvironmentPreparer;

    @Autowired
    private HadoopJobRunner hadoopJobRunner;

    public void executeFor(LmmpJob job) {
        boolean cleanedUp = hadoopEnvironmentPreparer.go();
        
        if (!cleanedUp) {
            job.failed("Could not connect to HDFS");
            return;
        }

        String hadoopJobId = hadoopJobRunner.executeFor(job);

        job.setHadoopJobId(hadoopJobId);
    }

}

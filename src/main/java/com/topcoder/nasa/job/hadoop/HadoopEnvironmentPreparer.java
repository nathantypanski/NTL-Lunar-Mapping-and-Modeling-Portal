package com.topcoder.nasa.job.hadoop;

import java.io.IOException;
import java.net.ConnectException;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Prepares the HDFS environment for this run (in exactly the same way the start.sh script used to.)
 * <p/>
 * See {@link #doPrepareEnvironment()}.
 */
@Component
public class HadoopEnvironmentPreparer {
    private static final Logger LOG = LoggerFactory.getLogger(HadoopEnvironmentPreparer.class);

    @Autowired
    private FileSystem fileSystem;

    public boolean go() {
        LOG.info("Preparing Hadoop HDFS environment for new job...");
        try {
            doPrepareEnvironment();
            LOG.info("Done preparing Hadoop HDFS environment!");
            return true;
        } catch (ConnectException ce) {
            LOG.error("Unable to connect to HDFS - cannot clean up environment");
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("Exception while cleaning up Hadoop environment", e);
        }
    }
    // TODO - change hardcoded paths
    private void doPrepareEnvironment() throws IOException {
        LOG.info("Deleting /output recursively from HDFS");
        fileSystem.delete(new Path("/output"), true);

        LOG.info("Deleting /distcache recursively from HDFS");
        fileSystem.delete(new Path("/distcache"), true);

        LOG.info("Creating /distcache in HDFS");
        fileSystem.mkdirs(new Path("/distcache"));

        LOG.info("Putting CustomerPartitioner.jar into HDFS");
        fileSystem.copyFromLocalFile(new Path(
                "/home/hadoop/demo/CustomPartitioner/CustomPartitioner.jar"),
                new Path("/distcache"));

        LOG.info("Deleting /url recursively from HDFS");
        fileSystem.delete(new Path("/url"), true);

        LOG.info("Creating /url in HDFS");
        fileSystem.mkdirs(new Path("/url"));

        LOG.info("Putting part-000000 into HDFS");
        fileSystem.copyFromLocalFile(new Path("/home/hadoop/demo/part-000000"), new Path("/url"));
    }
}

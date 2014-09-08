package com.topcoder.nasa.job;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a "job" that is created when a generation request is submitted successfully.
 *
 */
public class LmmpJob {
    private static final Logger LOG = LoggerFactory.getLogger(LmmpJob.class);

    public enum Status {
        RUNNING_PDS_API("running"), RUNNING_HADOOP("running"), RUNNING_EXECUTABLES("running"), FAILED, COMPLETED, KILLED;

        private String displayName;

        Status() {
            this.displayName = name().toLowerCase();
        }

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /** The final file name to check for job completion */
    private static final String FINAL_PATH = "/tmp/{uuid}_final";
    private static final String MOSAIC_FILE = FINAL_PATH + "/mosaic.";
    private static final String VRT_FILE = FINAL_PATH + "/mosaic.vrt";

    /** The default output format to use - geotiff. */
    private static final String DEFAULT_OUTPUT_FORMAT = "gtiff";

    /** The UUID of this Job */
    private String uuid;

    /** This Job's current status */
    private Status status;

    /** If this job failed, here is where we store why */
    private String failInfo;

    /** The Hadoop Job ID for this job */
    private String hadoopJobId;

    /** The output format to pass to gdal_translate for this job. */
    private String outputFormat;

    // =========================================================================

    /**
     * Constructor for creating a <b>new</b> Job that automatically assigns a (random) UUID and sets
     * the job in RUNNING status.
     */
    public LmmpJob() {
        this.uuid = UUID.randomUUID().toString();
        this.status = Status.RUNNING_PDS_API;

        LOG.info("Created new Job with uuid {}", uuid);
    }

    /**
     * Constructor for creating an <b>existing</b> Job and populating all the fields.
     * 
     * @param uuid
     *            this Job's UUID
     * @param statusStr
     *            string representation of this Job's {@link Status}
     * @param failInfo
     */
    public LmmpJob(String uuid, String statusStr, String hadoopJobId, String failInfo,
            String outputFormat) {
        this.status = Status.valueOf(statusStr);
        this.uuid = uuid;
        this.hadoopJobId = hadoopJobId;
        this.failInfo = failInfo;
        this.outputFormat = outputFormat;

        LOG.debug("Loaded Job with uuid {}, status {} and hadoopJobId {}", uuid, status,
                hadoopJobId);
    }

    // =========================================================================

    public String getUuid() {
        return uuid;
    }

    public Status getStatus() {
        return status;
    }

    public void markAsRunningExecutables() {
        status = Status.RUNNING_EXECUTABLES;
    }

    public void failed(String failInfo) {
        this.failInfo = failInfo;
        status = Status.FAILED;
    }

    public String getFailInfo() {
        return failInfo;
    }

    public void completed() {
        status = Status.COMPLETED;
    }

    public void killed() {
        status = Status.KILLED;
    }

    public File getFinalPath() {
        String fileName = FINAL_PATH.replace("{uuid}", uuid);

        return new File(fileName);
    }

    public File getJobVrtFile() {
        String fileName = VRT_FILE.replace("{uuid}", uuid);

        return new File(fileName);
    }

    public File getJobCompletedFile() {
        String fileName = MOSAIC_FILE.replace("{uuid}", uuid);

        fileName = fileName + outputFormat;

        return new File(fileName);
    }

    public String getHadoopJobId() {
        return hadoopJobId;
    }

    public void setHadoopJobId(String hadoopJobId) {
        this.hadoopJobId = hadoopJobId;
        status = Status.RUNNING_HADOOP;
    }

    public String getOutputFormat() {
        if (outputFormat == null) {
            outputFormat = DEFAULT_OUTPUT_FORMAT;
        }

        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    /**
     * Map the {@link #outputFormat} to its file extension.
     */
    public String getFileType() {
        String outputFormat = getOutputFormat();

        if (outputFormat.toLowerCase().equals("gtiff")) {
            return "tiff";
        }

        return outputFormat;
    }

    // =========================================================================

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof LmmpJob)) {
            return false;
        }

        LmmpJob that = (LmmpJob) obj;

        return this.uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    // =========================================================================

    @Override
    public String toString() {
        return "LmmpJob [uuid=" + uuid + ", status=" + status + "]";
    }

}

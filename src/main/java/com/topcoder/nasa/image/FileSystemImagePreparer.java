package com.topcoder.nasa.image;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prepares the local file system for the Hadoop job.
 * <p/>
 * This is a drop-in replacement for a legacy bash script and, as such, does not consider any of the intricacies of
 * multiple jobs being submitted at the same time or while one job is already running.
 *
 */
public class FileSystemImagePreparer {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemImagePreparer.class);

    /** Where the images that will passed to Hadoop reside on the local fs. */
    private File picDirectory;

    /** The "part-000000" file. */
    private File partFile;

    // =============================================================================================

    /** Empties the {@link #picDirectory} and creates the "part-000000" file. */
    public void prepare(Collection<File> imageFiles) {
        cleanPicDirectory();

        createPartFile(imageFiles);
    }

    /**
     * Creates the "part" file that will be sent to Hadoop. This file simply contains the <b>names</b> of the image
     * files with their parent path (i.e. directory) stripped.
     * 
     * @param imageFiles
     *            the image files to pull the names from.
     */
    private void createPartFile(Collection<File> imageFiles) {
        try {
            FileWriter writer = new FileWriter(partFile);

            for (File imageFile : imageFiles) {
                String fileName = imageFile.getName().toUpperCase();

                LOG.info("Writing {} to {}", fileName, partFile);

                writer.write(fileName);
                writer.write('\n');
            }

            writer.close();
        } catch (IOException e) {
            throw new IllegalStateException("Exception creating part file", e);
        }
    }

    /**
     * Cleans the {@link #picDirectory} - empties it.
     */
    public void cleanPicDirectory() {
        try {
            LOG.info("Clearing down picDirectory " + picDirectory);
            FileUtils.cleanDirectory(picDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Exception while cleaning picDirectory: " + picDirectory);
        }
    }

    // =============================================================================================

    public void setPicDirectory(File picDirectory) {
        this.picDirectory = picDirectory;
    }

    public void setPartFile(File partFile) {
        this.partFile = partFile;
    }
}

package com.topcoder.nasa.job.binary;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.topcoder.nasa.job.LmmpJob;

/**
 * ExeTask implementation responsible for calling "gdalbuildvrt".
 */
public class ExeTask_gdalbuildvrt extends AbstractExeTask {
    private static final Logger LOG = LoggerFactory.getLogger(ExeTask_gdalbuildvrt.class);

    public ExeTask_gdalbuildvrt() {
        super("/usr/bin/gdalbuildvrt");
    }

    protected List<String> getArgsFor(LmmpJob lmmpJob) {
        List<String> args = new ArrayList<String>();
        args.add(lmmpJob.getFinalPath() + "/mosaic.vrt");

        // NOTE that wildcards don't work when spawning a process from java,
        // so we need to find all the tifs in the directory and add them to the
        // command's args
        File[] files = lmmpJob.getFinalPath().listFiles();

        if (files == null) {
            throw new IllegalStateException("finalPath does not exist?");
        }

        for (File file : files) {
            if (file.getAbsolutePath().toLowerCase().endsWith(".tif")) {
                args.add(file.getAbsolutePath());
            }
        }

        return args;
    }
}

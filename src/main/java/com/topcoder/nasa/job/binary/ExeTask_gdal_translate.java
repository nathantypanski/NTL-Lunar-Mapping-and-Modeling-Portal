package com.topcoder.nasa.job.binary;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.topcoder.nasa.job.LmmpJob;

/**
 * ExeTask implementation responsible for calling "gdal_translate".
 */
public class ExeTask_gdal_translate extends AbstractExeTask {
    private static final Logger LOG = LoggerFactory.getLogger(ExeTask_gdal_translate.class);

    public ExeTask_gdal_translate() {
        super("/usr/bin/gdal_translate");
    }

    protected List<String> getArgsFor(LmmpJob lmmpJob) {
        return Arrays.asList("-of", lmmpJob.getOutputFormat(), //
                lmmpJob.getJobVrtFile().getAbsolutePath(), //
                lmmpJob.getJobCompletedFile().getAbsolutePath());
    }
}

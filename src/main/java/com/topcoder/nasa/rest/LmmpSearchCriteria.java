package com.topcoder.nasa.rest;

import gov.nasa.pds.entities.SearchCriteria;

/**
 * Lmmp specialization of the PDS SearchCriteria which adds the output format as a field.
 *
 */
public class LmmpSearchCriteria extends SearchCriteria {
    private String outputFormat;

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
}

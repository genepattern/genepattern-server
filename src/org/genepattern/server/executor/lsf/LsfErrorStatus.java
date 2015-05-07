/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.lsf;

import org.genepattern.drm.DrmJobState;

/**
 * Data object for representing errorMessage and exitCode from an LSF job.
 * 
 * @author pcarr
 */
public class LsfErrorStatus {
    private DrmJobState jobState;
    private String errorMessage=null;
    private int exitCode=0;
    private Integer maxThreads=null;
    private Integer maxProcesses=null;
        
    public LsfErrorStatus(final DrmJobState jobState, final int exitCode, final String errorMessage) {
        this.jobState=jobState;
        this.exitCode=exitCode;
        this.errorMessage=errorMessage;
    }
    protected void setMaxThreads(Integer maxThreads) {
        this.maxThreads=maxThreads;
    }
    protected void setMaxProcesses(Integer maxProcesses) {
        this.maxProcesses=maxProcesses;
    }

    public int getExitCode() {
        return exitCode;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
    public DrmJobState getJobState() {
        return jobState;
    }
    public Integer getMaxThreads() {
        return maxThreads;
    }
    public Integer getMaxProcesses() {
        return maxProcesses;
    }
}

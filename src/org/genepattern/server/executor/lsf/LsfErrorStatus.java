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
        
    public LsfErrorStatus(final DrmJobState jobState, final int exitCode, final String errorMessage) {
        this.jobState=jobState;
        this.exitCode=exitCode;
        this.errorMessage=errorMessage;
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
}

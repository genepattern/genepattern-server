package org.genepattern.server.executor.lsf;

/**
 * Data object for representing errorMessage and exitCode from an LSF job.
 * 
 * @author pcarr
 */
public class LsfErrorStatus {
    private String errorMessage=null;
    private int exitCode=0;
        
    public LsfErrorStatus(final int exitCode, final String errorMessage) {
        this.exitCode=exitCode;
        this.errorMessage=errorMessage;
    }

    public int getExitCode() {
        return exitCode;
    }
    public String getErrorMessage() {
        return errorMessage;
    }
}

package org.genepattern.server.executor.drm;

/**
 * Indicate the status of an external job running on a specific instance of a JobRunner.
 * 
 * TODO: optionally allow for progress indicator to be included, e.g. percentComplete.
 * 
 * @author pcarr
 *
 */
public class DrmJobStatus {
    private final String drmJobId;
    private final JobState jobState;
    private final String jobStatusMessage;
    private final Integer exitCode;
    
    public DrmJobStatus(final String drmJobId, final JobState jobState) {
        this(drmJobId, jobState, null);
    }
    public DrmJobStatus(final String drmJobId, final JobState jobState, final Integer exitCode) {
        this(drmJobId, jobState, jobState.toString(), exitCode);
    }
    public DrmJobStatus(final String drmJobId, final JobState jobState, final String jobStatusMessage, final Integer exitCode) {
        this.drmJobId=drmJobId;
        this.jobState=jobState;
        this.jobStatusMessage=jobStatusMessage;
        this.exitCode=exitCode;
    }
    
    /**
     * Get the external job id.
     * @return
     */
    public String getDrmJobId() {
        return drmJobId;
    }

    /**
     * Get the current status of the job.
     * @return
     */
    public JobState getJobState() {
        return jobState;
    }

    /**
     * For completed jobs, get the exit code.
     * @return
     */
    public Integer getExitCode() {
        return exitCode;
    }

    /**
     * Get an optional status message providing details about the current status of the job.
     * @return
     */
    public String getJobStatusMessage() {
        return jobStatusMessage;
    }
    
}

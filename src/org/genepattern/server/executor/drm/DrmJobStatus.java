package org.genepattern.server.executor.drm;


public class DrmJobStatus {
    public final String drmJobId;
    public final JobState jobState;
    public final Integer exitCode;
    
    public DrmJobStatus(final String drmJobId, final JobState jobState) {
        this(drmJobId, jobState, null);
    }
    public DrmJobStatus(final String drmJobId, final JobState jobState, final Integer exitCode) {
        this.drmJobId=drmJobId;
        this.jobState=jobState;
        this.exitCode=exitCode;
    }
    
    public String getDrmJobId() {
        return drmJobId;
    }
    
    public JobState getJobState() {
        return jobState;
    }
    
    public Integer getExitCode() {
        return exitCode;
    }
    
}

package org.genepattern.drm;

import java.io.File;

/**
 * A record which maps a GenePattern job (gpJobNo) to a job in the external queuing system (extJobId).
 * Also provides access to the working directory for the job.
 * 
 * @author pcarr
 *
 */
public class DrmJobRecord {
    private final Integer gpJobNo;
    private final String extJobId;    
    private final File workingDir;    
    private final File stdinFile;
    private final File stdoutFile;
    private final File stderrFile;
    private final File logFile;

    /**
     * Get the GenePattern job number.
     * @return
     */
    public Integer getGpJobNo() {
        return gpJobNo;
    }

    /**
     * Get the external job id, used to identify the job by the queuing system.
     * For example the LSF job number.
     * @return
     */
    public String getExtJobId() {
        return extJobId;
    }

    /**
     * Get the working directory for the job, can be null if not set.
     * @return
     */
    public File getWorkingDir() {
        return workingDir;
    }

    /**
     * Get the optional stdin stream for the job, can be null of not set.
     * If it's a relative path, it's relative to the workingDir for the job.
     * @return
     */
    public File getStdinFile() {
        return stdinFile;
    }

    /**
     * Get the stdout stream from the job, can be null if ...
     * a) the job has not yet completed, or
     * b) the job has no stdout.
     * If it's a relative path, it's relative to the workingDir for the job.
     * @return
     */
    public File getStdoutFile() {
        return stdoutFile;
    }
    
    /**
     * Get the stderr stream from the job, can be null if ...
     * a) the job has not yet completed, or
     * b) the job has not stderr.
     * If it's a relative path, it's relative to the workingDir for the job.
     * 
     * @return
     */
    public File getStderrFile() {
        return stderrFile;
    }

    /**
     * Get the optional log file for the job.
     * Can be null of there is no log file.
     * If it's a relative path, it's relative to the workingDir for the job.
     * @return
     */
    public File getLogFile() {
        return logFile;
    }

    private DrmJobRecord(final Builder builder) {
        this.gpJobNo=builder.gpJobNo;
        this.extJobId=builder.extJobId;
        this.workingDir=builder.workingDir;
        this.stdinFile=builder.stdinFile;
        this.stdoutFile=builder.stdoutFile;
        this.stderrFile=builder.stderrFile;
        this.logFile=builder.logFile;
    }

    public static final class Builder {
        private final Integer gpJobNo;
        private String extJobId="";
        private File workingDir=null;
        private File stdinFile=null;
        private File stdoutFile=null;
        private File stderrFile=null;
        private File logFile=null;

        public Builder(final Integer gpJobNo) {
            this.gpJobNo=gpJobNo;
        }
        
        public Builder(final String extJobId, final DrmJobSubmission in) {
            this.extJobId=extJobId;
            this.gpJobNo=in.getGpJobNo();
            this.workingDir=in.getWorkingDir();
            this.stdinFile=in.getStdinFile();
            this.stdoutFile=in.getStdoutFile();
            this.stderrFile=in.getStderrFile();
            this.logFile=in.getLogFile();
        }
        
        public Builder(final DrmJobRecord in) {
            this.gpJobNo=in.gpJobNo;
            this.extJobId=in.extJobId;
            this.workingDir=in.workingDir;
            this.stdinFile=in.stdinFile;
            this.stdoutFile=in.stdoutFile;
            this.stderrFile=in.stderrFile;
            this.logFile=in.logFile;
        }
        
        public Builder extJobId(final String extJobId) {
            this.extJobId=extJobId;
            return this;
        }
        
        public Builder workingDir(final File workingDir) {
            this.workingDir=workingDir;
            return this;
        }

        public Builder stdinFile(final File stdinFile) {
            this.stdinFile=stdinFile;
            return this;
        }

        public Builder stdoutFile(final File stdoutFile) {
            this.stdoutFile=stdoutFile;
            return this;
        }

        public Builder stderrFile(final File stderrFile) {
            this.stderrFile=stderrFile;
            return this;
        }

        public Builder logFile(final File logFile) {
            this.logFile=logFile;
            return this;
        }

        public DrmJobRecord build() {
            return new DrmJobRecord(this);
        }
    }
    
}
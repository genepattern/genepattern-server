package org.genepattern.server.executor.drm.dao;

import java.io.File;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;

/**
 * An record (for the the DB or in a runtime cache) used by the DrmLookup class for recording the status of an
 * external job.
 * 
 * @author pcarr
 *
 */
@Entity
@Table(name="job_runner_job",
       uniqueConstraints=@UniqueConstraint(columnNames={"jr_classname", "jr_name", "ext_job_id"}))
public class JobRunnerJob {
    
    //this is a foreign key to the analysis_job table
    @Id
    @Column(name="gp_job_no")
    private Integer gpJobNo;

    @Column(name="jr_classname", nullable=false)
    private String jobRunnerClassname;

    @Column(name="jr_name", nullable=false)
    private String jobRunnerName;

    @Column(name="ext_job_id", nullable=true)
    private String extJobId;
    
    @Column(name="exit_code", nullable=true)
    private Integer exitCode;
    
    @Column(name="terminating_signal", nullable=true)
    private String terminatingSignal;
    
    @Column(name="job_state", nullable=true)
    private String jobState;

    @Column(name="status_message", nullable=true)
    private String statusMessage;
    
    @Column(name="status_date", nullable=false)
    private Date statusDate;

    @Column(name="working_dir", nullable=false)
    private String workingDir;
    
    @Column(name="stdout_file", nullable=true)
    private String stdoutFile;

    @Column(name="stderr_file", nullable=true)
    private String stderrFile;
    
    @Column(name="stdin_file", nullable=true)
    private String stdinFile;
    
    @Column(name="log_file", nullable=true)
    private String logFile;

    private JobRunnerJob(final Builder builder) {
        this(builder, new Date());
    }
    private JobRunnerJob(final Builder builder, final Date statusDate) {
        this.gpJobNo=builder.gpJobNo;
        this.jobRunnerClassname=builder.jobRunnerClassname;
        this.jobRunnerName=builder.jobRunnerName;
        this.extJobId=builder.extJobId;
        this.exitCode=builder.exitCode;
        this.terminatingSignal=builder.terminatingSignal;
        this.jobState=builder.jobState;
        this.statusMessage=builder.statusMessage;
        this.workingDir=builder.workingDir;
        this.stdoutFile=builder.stdoutFile;
        this.stderrFile=builder.stderrFile;
        this.stdinFile=builder.stdinFile;
        this.logFile=builder.logFile;
        
        this.statusDate=statusDate;
    }

    public static final class Builder {
        private final Integer gpJobNo;
        private final String jobRunnerClassname;
        private String jobRunnerName;
        private String extJobId="";
        private Integer exitCode=null;
        private String terminatingSignal;
        private String jobState;
        private String statusMessage;
        private final String workingDir;
        private String stdoutFile;
        private String stderrFile;
        private String stdinFile;
        private String logFile;
        
        public Builder(final String jobRunnerClassname, final DrmJobSubmission in) {
            this.gpJobNo=in.getGpJobNo();
            this.jobRunnerClassname=jobRunnerClassname;
            this.workingDir=in.getWorkingDir().getAbsolutePath();
            this.stdoutFile=fileAsString(in.getStdoutFile());
            this.stderrFile=fileAsString(in.getStderrFile());
            this.stdinFile=fileAsString(in.getStdinFile());
            this.logFile=fileAsString(in.getLogFile());
        }
        
        /**
         * Convert the given File (which might be null) into a String.
         * @param in
         * @return null if the in file is null, otherwise file#path.
         */
        private static final String fileAsString(final File in) {
            if (in==null) {
                return null;
            }
            return in.getPath();
        }
        
        public Builder(final String jobRunnerClassname, final File workingDir, final Integer gpJobNo) {
            this.gpJobNo=gpJobNo;
            this.jobRunnerClassname=jobRunnerClassname;
            this.workingDir=workingDir.getAbsolutePath();
        }
        
        public Builder(final JobRunnerJob in) {
            this.gpJobNo=in.gpJobNo;
            this.jobRunnerClassname=in.jobRunnerClassname;
            this.jobRunnerName=in.jobRunnerName;
            this.extJobId=in.extJobId;
            this.exitCode=in.exitCode;
            this.terminatingSignal=in.terminatingSignal;
            this.jobState=in.jobState;
            this.statusMessage=in.statusMessage;
            this.workingDir=in.workingDir;
            this.stdoutFile=in.stdoutFile;
            this.stderrFile=in.stderrFile;
            this.stdinFile=in.stdinFile;
            this.logFile=in.logFile;
        }
        
        public Builder drmJobStatus(final DrmJobStatus updated) {
            this.extJobId=updated.getDrmJobId();
            this.exitCode=updated.getExitCode();
            this.terminatingSignal=updated.getTerminatingSignal();
            this.jobState=updated.getJobState().name();
            this.statusMessage=updated.getJobStatusMessage();
            return this;
        }

        public Builder jobRunnerName(final String jobRunnerName) {
            this.jobRunnerName=jobRunnerName;
            return this;
        }
        
        public Builder extJobId(final String extJobId) {
            this.extJobId=extJobId;
            return this;
        }
        
        public Builder exitCode(final Integer exitCode) {
            this.exitCode=exitCode;
            return this;
        }
        
        public Builder terminatingSignal(final String terminatingSignal) {
            this.terminatingSignal=terminatingSignal;
            return this;
        }
        
        public Builder jobState(final DrmJobState jobState) {
            this.jobState=jobState.name();
            return this;
        }
        
        public Builder statusMessage(final String statusMessage) {
            this.statusMessage=statusMessage;
            return this;
        }
        
        public Builder stdoutFile(final String stdoutFile) {
            this.stdoutFile=stdoutFile;
            return this;
        }

        public Builder stderrFile(final String stderrFile) {
            this.stderrFile=stderrFile;
            return this;
        }

        public Builder stdinFile(final String stdinFile) {
            this.stdinFile=stdinFile;
            return this;
        }

        public Builder logFile(final String logFile) {
            this.logFile=logFile;
            return this;
        }

        public JobRunnerJob build() {
            return new JobRunnerJob(this);
        }
    }
    
    public Integer getGpJobNo() {
        return gpJobNo;
    }

    public String getJobRunnerClassname() {
        return jobRunnerClassname;
    }

    public String getJobRunnerName() {
        return jobRunnerName;
    }

    public String getExtJobId() {
        return extJobId;
    }
    
    public Integer getExitCode() {
        return exitCode;
    }
    
    public String getTerminatingSignal() {
        return terminatingSignal;
    }

    public String getJobState() {
        return jobState;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Date getStatusDate() {
        return statusDate;
    }

    public String getWorkingDir() {
        return workingDir;
    }
    
    public String getStdinFile() {
        return stdinFile;
    }
    
    public String getStdoutFile() {
        return stdoutFile;
    }
    
    public String getStderrFile() {
        return stderrFile;
    }
    
    public String getLogFile() {
        return logFile;
    }

    //private no-arg constructor for hibernate
    private JobRunnerJob() {
    }
    
    //private setters for hibernate, making this class 'kind-of' immutable 
    private void setGpJobNo(final Integer gpJobNo) {
        this.gpJobNo = gpJobNo;
    }

    private void setJobRunnerClassname(final String jobRunnerClassname) {
        this.jobRunnerClassname = jobRunnerClassname;
    }

    private void setJobRunnerName(final String jobRunnerName) {
        this.jobRunnerName = jobRunnerName;
    }

    private void setExtJobId(final String extJobId) {
        this.extJobId = extJobId;
    }
    
    private void setExitCode(final Integer exitCode) {
        this.exitCode = exitCode;
    }
    
    private void setTerminatingSignal(final String terminatingSignal) {
        this.terminatingSignal = terminatingSignal;
    }

    private void setJobState(final String jobState) {
        this.jobState = jobState;
    }

    private void setStatusMessage(final String extJobStatusMessage) {
        this.statusMessage = extJobStatusMessage;
    }

    private void setStatusDate(final Date timestamp) {
        this.statusDate = timestamp;
    }

    private void setWorkingDir(final String workingDir) {
        this.workingDir = workingDir;
    }
    
    private void setStdinFile(final String stdinFile) {
        this.stdinFile = stdinFile;
    }

    private void setStdoutFile(final String stdoutFile) {
        this.stdoutFile = stdoutFile;
    }
    
    private void setLogFile(final String logFile) {
        this.logFile = logFile;
    }
}
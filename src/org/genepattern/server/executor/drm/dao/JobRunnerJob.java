package org.genepattern.server.executor.drm.dao;

import java.io.File;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.genepattern.drm.CpuTime;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.Memory;
import org.hibernate.validator.Size;

/**
 * A record (for the the DB or in a runtime cache) used by the DrmLookup class for recording the status of an
 * external job.
 * 
 * @author pcarr
 *
 */
@Entity
@Table(name="job_runner_job",
       uniqueConstraints=@UniqueConstraint(columnNames={"jr_classname", "jr_name"}))
public class JobRunnerJob {
     private static final Logger log = Logger.getLogger(JobRunnerJob.class);
     
     /** DB max length of the status_message column, e.g. varchar2(2000) in Oracle. */
     public static final int STATUS_MESSAGE_LENGTH=2000;
   
    /**
     * Truncate the string so that it is no longer than MAX characters.
     * @param in
     * @param MAX
     * @return
     */
    public static String truncate(String in, int MAX) {
        if (in==null) {
            return in;
        }
        if (in.length() <= MAX) {
            return in;
        }
        if (MAX<0) {
            log.error("expecting value >0 for MAX="+MAX);
        }
        return in.substring(0, MAX);
    }
    
    //this is a foreign key to the analysis_job table
    @Id
    @Column(name="gp_job_no")
    private Integer gpJobNo;
    
    @Column(name="lsid", nullable=false, length=255)
    private String lsid;

    @Column(name="jr_classname", nullable=false, length=511)
    private String jobRunnerClassname;

    @Column(name="jr_name", nullable=false, length=255)
    private String jobRunnerName;

    @Column(name="ext_job_id", nullable=true, length=255)
    private String extJobId;
    
    @Column(name="queue_id", nullable=true, length=511)
    private String queueId;

    /**
     * The date that the status was recorded into the database.
     */
    @Column(name="status_date", nullable=false)
    private Date statusDate;

    /**
     * The time that the job was submitted to the remote queue.
     * Can be null, when the job has not yet been submitted.
     * This is not the same as the date the job was added to the GenePattern queue.
     */
    @Column(name="submit_time", nullable=true)
    private Date submitTime;

    /**
     * The time that the job started on the remote queue.
     * Can be null, when the job has not yet started.
     */
    @Column(name="start_time", nullable=true)
    private Date startTime;

    /**
     * The time that the job completed on the remote queue.
     * Can be null, when the job has not yet completed.
     */
    @Column(name="end_time", nullable=true)
    private Date endTime;
    
    @Column(name="job_state", nullable=true, length=255)
    private String jobState;

    @Column(name="status_message", nullable=true, length=STATUS_MESSAGE_LENGTH)
    @Size(max=STATUS_MESSAGE_LENGTH)
    private String statusMessage;
    
    @Column(name="exit_code", nullable=true, length=255)
    private Integer exitCode;
    
    @Column(name="terminating_signal", nullable=true, length=255)
    private String terminatingSignal;
    
    /**
     * The amount of cpu time used for the job, in number of milliseconds.
     */
    @Column(name="cpu_time", nullable=true)
    private Long cpuTime=null;
    
    /**
     * The maximum memory usage for the job, in number of bytes.
     */
    @Column(name="max_mem", nullable=true)
    private Long maxMem=null;
    
    /**
     * The maximum swap space usage for the job, in number of bytes.
     */
    @Column(name="max_swap", nullable=true)
    private Long maxSwap;
    
    /**
     * The maximum number of processes for the job, for example, as reported in the .lsf.out log file.
     */
    @Column(name="max_processes", nullable=true)
    private Integer maxProcesses;
    
    /**
     * The maximum number of threads used by the job, for example, as reported in the .lsf.out log file.
     */
    @Column(name="max_threads", nullable=true)
    private Integer maxThreads;
    
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
        this.lsid=builder.lsid;
        this.jobRunnerClassname=builder.jobRunnerClassname;
        this.jobRunnerName=builder.jobRunnerName;
        this.extJobId=builder.extJobId;
        this.queueId=builder.queueId;
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
        this.submitTime=builder.submitTime;
        this.startTime=builder.startTime;
        this.endTime=builder.endTime;
        
        this.cpuTime=builder.cpuTime;
        this.maxMem=builder.maxMem;
        this.maxSwap=builder.maxSwap;
        this.maxProcesses=builder.maxProcesses;
        this.maxThreads=builder.maxThreads;
    }

    public static final class Builder {
        private Integer gpJobNo;
        private String lsid="";
        private String jobRunnerClassname;
        private String jobRunnerName;
        private String extJobId="";
        private String queueId=null; // null means 'not set'
        private Integer exitCode=null;
        private String terminatingSignal;
        private String jobState;
        private String statusMessage;
        private String workingDir;
        private String stdoutFile;
        private String stderrFile;
        private String stdinFile;
        private String logFile;
        
        private Date submitTime=null;
        private Date startTime=null;
        private Date endTime=null;
        
        private Long cpuTime=null;
        private Long maxMem=null;
        private Long maxSwap=null;
        private Integer maxThreads=null;
        private Integer maxProcesses=null;
        
        public Builder(final String jobRunnerClassname, final DrmJobSubmission in) {
            this.gpJobNo=in.getGpJobNo();
            this.lsid=in.getJobContext().getLsid();
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
        
        public Builder() {
        }
        
        public Builder(final String jobRunnerClassname, final File workingDir, final Integer gpJobNo) {
            this.gpJobNo=gpJobNo;
            this.jobRunnerClassname=jobRunnerClassname;
            this.workingDir=workingDir.getAbsolutePath();
        }
        
        public Builder(final JobRunnerJob in) {
            this.gpJobNo=in.gpJobNo;
            this.lsid=in.lsid;
            this.jobRunnerClassname=in.jobRunnerClassname;
            this.jobRunnerName=in.jobRunnerName;
            this.extJobId=in.extJobId;
            this.queueId=in.queueId;
            this.jobState=in.jobState;
            this.statusMessage=in.statusMessage;
            this.exitCode=in.exitCode;
            this.terminatingSignal=in.terminatingSignal;
            this.workingDir=in.workingDir;
            this.stdinFile=in.stdinFile;
            this.stdoutFile=in.stdoutFile;
            this.logFile=in.logFile;
            this.stderrFile=in.stderrFile;
            this.submitTime=in.submitTime;
            this.startTime=in.startTime;
            this.endTime=in.endTime;
            this.cpuTime=in.cpuTime;
            this.maxMem=in.maxMem;
            this.maxSwap=in.maxSwap;
            this.maxProcesses=in.maxProcesses;
            this.maxThreads=in.maxThreads;
        }
        
        public Builder drmJobStatus(final DrmJobStatus updated) {
            this.extJobId=updated.getDrmJobId();
            this.queueId=updated.getQueueId();
            this.jobState=updated.getJobState().name();
            this.statusMessage=updated.getJobStatusMessage();
            this.exitCode=updated.getExitCode();
            this.terminatingSignal=updated.getTerminatingSignal();
            this.submitTime=updated.getSubmitTime();
            this.startTime=updated.getStartTime();
            this.endTime=updated.getEndTime();
            if (updated.getCpuTime() == null) {
                this.cpuTime=null;
            }
            else {
                this.cpuTime=updated.getCpuTime().asMillis();
            }
            if (updated.getMemory() == null) {
                this.maxMem=null;
            }
            else {
                this.maxMem = updated.getMemory().getNumBytes();
            }
            if (updated.getMaxSwap() == null) {
                this.maxSwap=null;
            }
            else {
                this.maxSwap=updated.getMaxSwap().getNumBytes();
            }
            this.maxProcesses=updated.getMaxProcesses();
            this.maxThreads=updated.getMaxThreads();
            return this;
        }
        
        public Builder gpJobNo(int gpJobNo) {
            this.gpJobNo=gpJobNo;
            return this;
        }
        
        public Builder lsid(final String lsid) {
            this.lsid=lsid;
            return this;
        }
        
        public Builder jobRunnerClassname(String jobRunnerClassname) {
            this.jobRunnerClassname=jobRunnerClassname;
            return this;
        }
        
        public Builder workingDir(String workingDir) {
            this.workingDir=workingDir;
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
        
        public Builder queueId(final String queueId) {
            this.queueId=queueId;
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
        
        public Builder statusMessage(final String statusMessageIn) {
            this.statusMessage=truncate(statusMessageIn, STATUS_MESSAGE_LENGTH);
            if (log.isDebugEnabled() && statusMessageIn.length()>STATUS_MESSAGE_LENGTH) {
                log.warn("truncating statusMessage because it is greater than max DB length="+STATUS_MESSAGE_LENGTH);
            }
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
        
        public Builder submitTime(final Date submitTime) {
            this.submitTime=submitTime;
            return this;
        }
        
        public Builder startTime(final Date startTime) {
            this.startTime=startTime;
            return this;
        }
        
        public Builder endTime(final Date endTime) {
            this.endTime=endTime;
            return this;
        }
        
        public Builder cpuTime(final CpuTime cpuTime) {
            this.cpuTime=cpuTime.asMillis();
            return this;
        }
        
        public Builder maxMemory(final Memory maxMemory) {
            this.maxMem=maxMemory.getNumBytes();
            return this;
        }
        
        public Builder maxSwap(final Memory maxSwap) {
            this.maxSwap=maxSwap.getNumBytes();
            return this;
        }
        
        public Builder maxProcesses(final int maxProcesses) {
            this.maxProcesses=maxProcesses;
            return this;
        }
        
        public Builder maxThreads(final int maxThreads) {
            this.maxThreads=maxThreads;
            return this;
        }

        public JobRunnerJob build() {
            return new JobRunnerJob(this);
        }
    }
    
    public Integer getGpJobNo() {
        return gpJobNo;
    }
    
    public String getLsid() {
        return lsid;
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
    
    public String getQueueId() {
        return queueId;
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

    public Date getSubmitTime() {
        return submitTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public Long getCpuTime() {
        return cpuTime;
    }

    public Long getMaxMemory() {
        return maxMem;
    }
    
    public Long getMaxSwap() {
        return maxSwap;
    }

    public Integer getMaxProcesses() {
        return maxProcesses;
    }

    public Integer getMaxThreads() {
        return maxThreads;
    }

    //private no-arg constructor for hibernate
    private JobRunnerJob() {
    }
    
    //private setters for hibernate, making this class 'kind-of' immutable 
    private void setGpJobNo(final Integer gpJobNo) {
        this.gpJobNo = gpJobNo;
    }
    
    private void setLsid(final String lsid) {
        this.lsid = lsid;
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
    
    private void setQueueId(final String queueId) {
        this.queueId=queueId;
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

    private void setSubmitTime(Date submitTime) {
        this.submitTime = submitTime;
    }

    private void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    private void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    private void setCpuTime(Long cpuTime) {
        this.cpuTime = cpuTime;
    }

    private void setMaxMem(Long maxMem) {
        this.maxMem = maxMem;
    }

    private void setMaxProcesses(Integer maxProcesses) {
        this.maxProcesses = maxProcesses;
    }

    private void setMaxThreads(Integer maxThreads) {
        this.maxThreads = maxThreads;
    }
}
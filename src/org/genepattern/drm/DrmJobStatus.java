package org.genepattern.drm;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;


import com.google.common.collect.ImmutableMap;

/**
 * Indicate the status of an external job running on a specific instance of a JobRunner.
 * 
 * @author pcarr
 *
 */
public class DrmJobStatus {
     private static final Logger log = Logger.getLogger(DrmJobStatus.class);
   
    @Override
    public String toString() {
        return this.toString;
    }
    
    private final String drmJobId;
    private final String queueId;
    private final DrmJobState jobState;
    private final Date submitTime;
    private final Date startTime;
    private final Date endTime;
    private final CpuTime cpuTime;
    private final Memory memory;
    private final Memory maxSwap;
    private final Integer maxProcesses;
    private final Integer maxThreads;
    private final String jobStatusMessage;
    private final Integer exitCode;
    private final String terminatingSignal;
    private final ImmutableMap<String,String> resourceUsage;
    
    private final String toString;
    
    private DrmJobStatus(final Builder builder) {
        this.drmJobId=builder.drmJobId;
        this.queueId=builder.queueId;
        this.jobState=builder.jobState;
        this.submitTime=builder.submitTime;
        this.startTime=builder.startTime;
        this.endTime=builder.endTime;
        this.jobStatusMessage=builder.jobStatusMessage;
        this.exitCode=builder.exitCode;
        this.terminatingSignal=builder.terminatingSignal;
        this.resourceUsage=builder.resourceUsage;
        this.cpuTime=builder.cpuTime;
        this.memory=builder.memory;
        this.maxSwap=builder.maxSwap;
        this.maxProcesses=builder.maxProcesses;
        this.maxThreads=builder.maxThreads;
        
        //for debugging
        StringBuffer buf=new StringBuffer();
        buf.append("drmJobId="); buf.append(drmJobId);
        buf.append(", queueId="); buf.append(queueId);
        buf.append(", jobState="); buf.append(jobState); 
        buf.append(", exitCode="); buf.append(exitCode);
        if (log.isDebugEnabled()) {
            buf.append("\n    submitTime="); buf.append(submitTime);
            buf.append(", startTime="); buf.append(startTime);
            buf.append(", endTime="); buf.append(endTime);
            buf.append("\n    jobStatusMessage="); buf.append(jobStatusMessage);
            buf.append(", terminatingSignal="); buf.append(terminatingSignal);
            buf.append("\ncpuTime="+cpuTime);
            buf.append("\nmemory="+memory);
            buf.append("\nmaxSwap="+maxSwap);
            buf.append("\nmaxProcesses="+maxProcesses);
            buf.append("\nmaxThreads="+maxThreads);
            buf.append("\nresourceUsage="); buf.append(resourceUsage);
        }
        this.toString=buf.toString();
    }
    
    /**
     * Get the external job id.
     * @return
     */
    public String getDrmJobId() {
        return drmJobId;
    }
    
    /**
     * Get the queue to which the job was submitted, by default return the empty string.
     */
    public String getQueueId() {
        return queueId;
    }

    /**
     * Get the current status of the job.
     * @return
     */
    public DrmJobState getJobState() {
        return jobState;
    }

    /**
     * Get the time that the job was added to the queue, e.g. for LSF the time that the bsub command was issued.
     * @return
     */
    public Date getSubmitTime() {
        if (submitTime==null) {
            return null;
        }
        return new Date(submitTime.getTime());
    }
    
    /**
     * Get the time that the job started on the queue, can be null if the job hasn't started yet.
     * @return
     */
    public Date getStartTime() {
        if (startTime==null) {
            return null;
        }
        return new Date(startTime.getTime());
    }

    /**
     * Get the time that the job completed, can be null of the job hasn't finished.
     * @return
     */
    public Date getEndTime() {
        if (endTime==null) {
            return null;
        }
        return new Date(endTime.getTime());
    }

    /**
     * Get the amount of cpu time used by the job, can be null if this is not known.
     * @return
     */
    public CpuTime getCpuTime() {
        return cpuTime;
    }
    
    /**
     * Get the amount of memory used by the job, can be null if this is not known.
     */
    public Memory getMemory() {
        return memory;
    }
    
    /**
     * Get the amount of swap memory used by the job, can be null if this is not known.
     * @return
     */
    public Memory getMaxSwap() {
        return maxSwap;
    }
    
    public Integer getMaxProcesses() {
        return maxProcesses;
    }
    
    public Integer getMaxThreads() {
        return maxThreads;
    }

    /**
     * For completed jobs, get the exit code, can be null of the job is not finished or if for some other reason the exit code is not available.
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
    
    /**
     * Optionally, for completed jobs, get the terminating signal.
     * @return the terminating signal or null if the job has no terminating signal
     */
    public String getTerminatingSignal() {
        return terminatingSignal;
    }
    
    /**
     * Get the resource usage data, for example 'CPU time' and 'Max memory' are reported by LSF jobs.
     * If this isn't available the map may be empty or null.
     * 
     * @deprecated - use one of the existing options such as memory, maxThreads to indicate resource usage.
     * 
     * @return the jobs resource usage data if available in the form of an ImmutableMap. You should not
     *     make any changes to this value.
     */
    public Map<String,String> getResourceUsage() {
        return resourceUsage;
    }
    
    /**
     * Helper class so that we can ensure that each instance of the DrmJobStatus class is immutable.
     * @author pcarr
     *
     */
    public static class Builder {
        private String drmJobId;
        private String queueId=null;
        private DrmJobState jobState;
        private Date submitTime=null;
        private Date startTime=null;
        private Date endTime=null;
        private CpuTime cpuTime=new CpuTime();
        private Memory memory=null;
        private Memory maxSwap=null;
        private Integer maxProcesses=null;
        private Integer maxThreads=null;
        private String jobStatusMessage="";
        private Integer exitCode=null;
        private String terminatingSignal="";
        private ImmutableMap<String,String> resourceUsage=null;

        public Builder() {
        }
        
        //copy constructor
        public Builder(DrmJobStatus in) {
            this.drmJobId=in.drmJobId;
            this.queueId=in.queueId;
            this.jobState=in.jobState;
            this.submitTime=in.submitTime;
            this.startTime=in.startTime;
            this.endTime=in.endTime;
            this.cpuTime=in.cpuTime;
            this.memory=in.memory;
            this.maxSwap=in.maxSwap;
            this.maxProcesses=in.maxProcesses;
            this.maxThreads=in.maxThreads;
            this.jobStatusMessage=in.jobStatusMessage;
            this.exitCode=in.exitCode;
            this.terminatingSignal=in.terminatingSignal;
            this.resourceUsage=in.resourceUsage;
        }
        
        public Builder(final String drmJobId, final DrmJobState jobState) {
            this.drmJobId=drmJobId;
            this.jobState=jobState;
        }
        
        public Builder extJobId(final String extJobId) {
            this.drmJobId=extJobId;
            return this;
        }
        
        public Builder queueId(final String queueId) {
            this.queueId=queueId;
            return this;
        }
        
        public Builder jobState(final DrmJobState jobState) {
            this.jobState=jobState;
            return this;
        }
        
        public Builder submitTime(final Date submitTime) {
            if (submitTime==null) {
                this.submitTime=null;
            }
            else {
                this.submitTime=new Date(submitTime.getTime());
            }
            return this;
        }
        
        public Builder startTime(final Date startTime) {
            if (startTime==null) {
                this.startTime=null;
            }
            else {
                this.startTime=new Date(startTime.getTime());
            }
            return this;
        }
        
        public Builder endTime(final Date endTime) {
            if (endTime==null) {
                this.endTime=null;
            }
            else {
                this.endTime=new Date(endTime.getTime());
            }
            return this;
        }
        
        public Builder cpuTime(final CpuTime cpuTime) {
            this.cpuTime=cpuTime;
            return this;
        }

        public Builder memory(final long sizeInBytes) {
            this.memory=Memory.fromSizeInBytes(sizeInBytes);
            return this;
        }

        public Builder memory(final String memSpec) {
            this.memory=Memory.fromString(memSpec);
            return this;
        }
        
        public Builder memory(final Memory memory) {
            this.memory=memory;
            return this;
        }
        
        public Builder maxSwap(final long sizeInBytes) {
            this.maxSwap=Memory.fromSizeInBytes(sizeInBytes);
            return this;
        }
        
        public Builder maxSwap(final String memSpec) {
            this.maxSwap=Memory.fromString(memSpec);
            return this;
        }
        
        public Builder maxSwap(final Memory maxSwap) {
            this.maxSwap=maxSwap;
            return this;
        } 
        
        public Builder maxProcesses(final Integer maxProcesses) {
            this.maxProcesses=maxProcesses;
            return this;
        }
        
        public Builder maxThreads(final Integer maxThreads) {
            this.maxThreads=maxThreads;
            return this;
        }
        
        public Builder jobStatusMessage(final String jobStatusMessage) {
            this.jobStatusMessage=jobStatusMessage;
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
        
        public Builder resourceUsage(final Map<String,String> resourceUsageIn) {
            if (resourceUsageIn==null) {
                this.resourceUsage=new ImmutableMap.Builder<String,String>().build();
            }
            else {
                this.resourceUsage=new ImmutableMap.Builder<String,String>().putAll(resourceUsageIn).build();
            }
            return this;
        }
        
        public DrmJobStatus build() {
            return new DrmJobStatus(this);
        }
    }
    
}

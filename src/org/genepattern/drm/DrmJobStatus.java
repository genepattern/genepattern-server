package org.genepattern.drm;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;


import com.google.common.collect.ImmutableMap;

/**
 * Indicate the status of an external job running on a specific instance of a JobRunner.
 * 
 * @author pcarr
 *
 */
public class DrmJobStatus {
    
    /**
     * Representation of an amount of time.
     * @author pcarr
     *
     */
    public static class Duration {
        private final long time;
        private final TimeUnit timeUnit;
        
        public Duration() {
            this(0, TimeUnit.MILLISECONDS);
        }
        public Duration(long time, TimeUnit timeUnit) {
            this.time=time;
            this.timeUnit=timeUnit;
        }
        
        public long getTime() {
            return time;
        }
        public TimeUnit getTimeUnit() {
            return timeUnit;
        }
    }
    
    private final String drmJobId;
    private final DrmJobState jobState;
    private final Date submitTime;
    private final Date startTime;
    private final Date endTime;
    private final Duration cpuTime;
    private final String jobStatusMessage;
    private final Integer exitCode;
    private final String terminatingSignal;
    private final ImmutableMap<String,String> resourceUsage;
    
    private DrmJobStatus(final Builder builder) {
        this.drmJobId=builder.drmJobId;
        this.jobState=builder.jobState;
        this.submitTime=builder.submitTime;
        this.startTime=builder.startTime;
        this.endTime=builder.endTime;
        this.jobStatusMessage=builder.jobStatusMessage;
        this.exitCode=builder.exitCode;
        this.terminatingSignal=builder.terminatingSignal;
        this.resourceUsage=builder.resourceUsage;
        this.cpuTime=builder.cpuTime;
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
    public DrmJobState getJobState() {
        return jobState;
    }

    /**
     * Get the time that the job was added to the queue, e.g. for LSF the time that the bsub command was issued.
     * @return
     */
    public Date getSubmitTime() {
        return submitTime;
    }
    
    /**
     * Get the time that the job started on the queue, can be null if the job hasn't started yet.
     * @return
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Get the time that the job completed, can be null of the job hasn't finished.
     * @return
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * Get the amount of cpu time used by the job.
     * @return
     */
    public Duration getCpuTime() {
        return cpuTime;
    }

    /**
     * For completed jobs, get the exit code.
     * @return null if the job has not completed or if for some other reason the exit code is not available.
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
     * @return the terminating signal or null if the job has not terminating signal
     */
    public String getTerminatingSignal() {
        return terminatingSignal;
    }
    
    /**
     * Get the resource usage data, for example 'CPU time' and 'Max memory' are reported by LSF jobs.
     * If this isn't available the map may be empty or null.
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
        private final String drmJobId;
        private final DrmJobState jobState;
        private Date submitTime=null;
        private Date startTime=null;
        private Date endTime=null;
        private Duration cpuTime=new Duration();
        private String jobStatusMessage="";
        private Integer exitCode=null;
        private String terminatingSignal="";
        private ImmutableMap<String,String> resourceUsage=null;
        
        public Builder(final String drmJobId, final DrmJobState jobState) {
            this.drmJobId=drmJobId;
            this.jobState=jobState;
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
        
        public Builder cpuTime(final Duration cpuTime) {
            this.cpuTime=cpuTime;
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

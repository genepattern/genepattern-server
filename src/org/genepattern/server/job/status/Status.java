/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.drm.CpuTime;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.drm.Walltime;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.genepattern.server.webapp.rest.api.v1.job.GpLink;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobInfoUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Representation of the status of a GenePattern job, used to generate the 'status.json' representation
 * from the REST API.
 * 
 * Example JSON format
 * <pre>
   {  
     "extJobId":"8937799",
     "isPending":false,
     "isFinished":true,
     "hasError":false,
     "executionLogLocation":"http://127.0.0.1:8080/gp/jobResults/1/gp_execution_log.txt",
     "stderrLocation":"http://127.0.0.1:8080/gp/jobResults/1/stderr.txt",
     "statusDate":"2014-06-04T13:20:10-04:00",
     "statusFlag":"DONE",
     "statusMessage":"Completed on 2014-06-04T13:20:10-04:00"
   }
   
   REST API keys for Usage stats 
     'cpuTimeMillis', 'cpuTime',   
     'maxMemoryBytes', 'maxMemory', 
     'maxSwapBytes', 'maxSwap'
     'maxProcesses', 
     'maxThreads'

 * </pre>
 * 
 * @author pcarr
 *
 */
public class Status {
    private static final Logger log = Logger.getLogger(Status.class);

    private Integer gpJobNo=null;
    private Date dateSubmittedToGp=null;
    private Date dateCompletedInGp=null;
    private String extJobId=null;
    private String executionLogLocation=null;
    private String stderrLocation=null;
    private boolean hasError=false;
    private boolean isFinished=false;
    private boolean isPending=false; 
    private DrmJobState jobState=null;
    private String statusFlag="";
    private String statusMessage="";
    private Date statusDate=new Date();
    private Date submitTime=null; // date the job was submitted to the external queue
    private Date startTime=null;  // date the job started in the external queue
    private Date endTime=null;    // date the job ending in the external queue
    private Integer exitCode=null;
    
    // Requested resources
    private List<StatusEntry> resourceRequirements=null;

    // Usage stats
    private List<StatusEntry> usageStats=null;
    private CpuTime cpuTime=null;
    private Memory maxMemory=null;
    private Memory maxSwap=null;
    private Integer maxProcesses=null;
    private Integer maxThreads=null;

    private String queueId = "";
    private List<GpLink> links=null;

    private void addLink(GpLink link) {
        if (links==null) {
            links=new ArrayList<GpLink>();
        }
        links.add(link);
    }
    
    public Integer getGpJobNo() {
        return gpJobNo;
    }
    
    public String getExtJobId() {
        return extJobId;
    }
    
    public String getExecutionLogLocation() {
        return executionLogLocation;
    }
    
    public String getStderrLocation() {
        return stderrLocation;
    }

    public boolean isHasError() {
        return hasError;
    }

    public boolean getIsFinished() {
        return isFinished;
    }

    public boolean getIsPending() {
        return isPending;
    }
    
    public boolean getIsRunning() {
        return !isFinished && !isPending;
    }
    
    public String getStatusFlag() {
        return statusFlag;
    }
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public Date getStatusDate() {
        return statusDate;
    }
    
    public Date getDateSubmittedToGp() {
        return dateSubmittedToGp;
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
    
    public Date getDateCompletedInGp() {
        return dateCompletedInGp;
    }
    
    public Integer getExitCode() {
        return exitCode;
    }
    
    public String getQueueId() {
        if (queueId == null) return "";
        else return queueId;
    }
    
    public DrmJobState getJobState() {
        return jobState;
    }
    
    public List<GpLink> getLinks() {
        return Collections.unmodifiableList(links);
    }

    /**
     * An entry in the job event log.
     * @author pcarr
     *
     */
    public static class JobEvent {
        private String event;
        private String isoFormat;
        
        public JobEvent() {
        }
        public JobEvent(String event, Date date) {
            this.event=event;
            this.isoFormat=DateUtil.toIso8601(date);
        }
        
        public String getEvent() {
            return event;
        }
        
        public JSONObject toJsonObj() throws JSONException {
            final JSONObject eventObj=new JSONObject();
            eventObj.put("event", event);
            eventObj.put("time", isoFormat);
            return eventObj;
        }
        
        public static JSONArray toJsonObj(final List<JobEvent> eventLog) throws JSONException {
            final JSONArray eventsObj=new JSONArray();
            for(final JobEvent jobEvent : eventLog) {
                eventsObj.put(jobEvent.toJsonObj());
            }
            return eventsObj;
        }
    }
    
    public List<JobEvent> getJobEvents() {
        List<JobEvent> eventLog = new ArrayList<JobEvent>();
        eventLog.add(new JobEvent("Added to GenePattern", dateSubmittedToGp));
        eventLog.add(new JobEvent("Submitted to queue", submitTime));
        eventLog.add(new JobEvent("Started running", startTime));
        eventLog.add(new JobEvent("Finished running", endTime));
        eventLog.add(new JobEvent("Completed in GenePattern", dateCompletedInGp));
        return Collections.unmodifiableList(eventLog);
    }

    /**
     * Get the list of resource requirements for display on the Job Details page.
     * These are resource requests (as opposed to resource usage), such as the 
     * job.memory or the job.walltime.
     */
    public List<StatusEntry> getResourceRequirements() {
        return resourceRequirements;
    }
    
    public List<StatusEntry> getUsageStats() {
        return usageStats;
    }

    /**
     * A key:value property associated with a job status object, such as
     * the list of resourceRequirements or job usageStats.
     * 
     * @author pcarr
     */
    public static class StatusEntry {
        private final String key;  // e.g. 'job.memory'
        private final String keyName; // e.g. Memory
        private final String value; // e.g. '16 Gb'
        private final String displayValue; // optional, defaults to '<key>=<value>'
        
        public StatusEntry(final String key, final String value) {
            this(key, key, value, value);
        }
        public StatusEntry(final String key, final String value, final String displayValue) {
            this(key, key, value, displayValue);
        }
        public StatusEntry(final String key, final String keyName, final String value, final String displayValue) {
            this.key=key;
            this.keyName=keyName;
            this.value=value;
            this.displayValue=displayValue;
        }
        
        public String getKey() {
            return key;
        }
        
        public String getKeyName() {
            return keyName;
        }
        
        public String getValue() {
            return value;
        }
        
        public String getDisplayValue() {
            return displayValue;
        }
        
        public JSONObject toJsonObj() throws JSONException {
            final JSONObject eventObj=new JSONObject();
            eventObj.put("key", key);
            eventObj.put("keyName", keyName);
            eventObj.put("value", value);
            eventObj.put("displayValue", displayValue);
            return eventObj;
        } 
    }
    
    /**
     * Get the string formatted JSON representation.
     * @return
     * @throws JSONException
     */
    public String getJson() throws JSONException {
        return toJsonObj().toString(2);
    }

    public JSONObject toJsonObj() throws JSONException {
        //init jobStatus
        final JSONObject jobStatus = new JSONObject();
        jobStatus.put("isFinished", isFinished);
        jobStatus.put("hasError", hasError);
        jobStatus.put("isPending", isPending);
        if (executionLogLocation != null) {
            jobStatus.put("executionLogLocation", executionLogLocation);
        } 
        if (stderrLocation != null) {
            jobStatus.put("stderrLocation", stderrLocation);
        }
        if (statusFlag != null) {
            jobStatus.put("statusFlag", statusFlag);
        }
        if (statusMessage != null) {
            jobStatus.put("statusMessage", statusMessage);
        }
        if (statusDate != null) {
            jobStatus.put("statusDate", DateUtil.toIso8601(statusDate));
        }
        if (dateSubmittedToGp != null) {
            jobStatus.put("addedToGp", DateUtil.toIso8601(dateSubmittedToGp));
        }
        if (submitTime != null) {
            jobStatus.put("submitTime", DateUtil.toIso8601(submitTime));
        }
        if (startTime != null) {
            jobStatus.put("startTime", DateUtil.toIso8601(startTime));
        }
        if (endTime != null) {
            jobStatus.put("endTime", DateUtil.toIso8601(endTime));
        }
        if (dateCompletedInGp != null) {
            jobStatus.put("completedInGp", DateUtil.toIso8601(dateCompletedInGp));
        }
        
        if (cpuTime != null) {
            jobStatus.put("cpuTimeMillis",    cpuTime.asMillis());
            jobStatus.put("cpuTime", cpuTime.getDisplayValue());
        }
        if (maxMemory != null) {
            jobStatus.put("maxMemoryBytes", maxMemory.getNumBytes());
            jobStatus.put("maxMemory", maxMemory.format());
        }
        if (maxSwap != null) {
            jobStatus.put("maxSwapBytes", maxSwap.getNumBytes());
            jobStatus.put("maxSwap", maxSwap.format());
        }
        if (maxProcesses != null) {
            jobStatus.put("maxProcesses", maxProcesses);
        }
        if (maxThreads != null) {
            jobStatus.put("maxThreads", maxThreads);
        }
        if (queueId != null) {
            jobStatus.put("queueId", queueId);
        }
        if (extJobId != null) {
            jobStatus.put("extJobId", extJobId);
        }
        
        if (links != null) {
            JSONArray linksArr=new JSONArray();
            for(GpLink link : links) {
                linksArr.put( link.toJson() );
            }
            jobStatus.put("links", linksArr);
        }

        List<JobEvent> jobEvents=getJobEvents();
        JSONArray eventLog=JobEvent.toJsonObj(jobEvents);
        jobStatus.put("eventLog", eventLog);

        if (resourceRequirements != null) {
            final JSONArray arr=new JSONArray();
            for(final StatusEntry r : resourceRequirements) {
                arr.put(r.toJsonObj());
            }
            jobStatus.put("resourceRequirements", arr);
        }
        return jobStatus;
    }
    
//    /**
//     * Example output as json string using Jackson library.
//     * @return
//     * @throws Exception
//     */
//    public String toJson() throws Exception {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
//        mapper.setSerializationInclusion(Inclusion.NON_NULL);
//        
//        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
//        mapper.setAnnotationIntrospector(introspector);
//        
//        return mapper.writeValueAsString(this);
//    }
    
    /**
     * Construct a new job status instance from the given JobInfo, executionLogLocation, 
     * and jobStatusRecord. 
     * 
     * @author pcarr
     *
     */
    public static class Builder {
        private Integer gpJobNo;
        private Date dateSubmittedToGp=null;
        private Date dateCompletedInGp=null;
        private String extJobId;
        private JobInfo jobInfo=null;
        private String executionLogLocation=null;
        private String stderrLocation=null;
        private JobRunnerJob jobStatusRecord=null;
        private String jobHref;
        private List<StatusEntry> resourceRequirements=null;
        private List<StatusEntry> usageStats=null;
        
        public Builder gpJobNo(final Integer gpJobNo) {
            this.gpJobNo=gpJobNo;
            return this;
        }
        
        public Builder extJobId(final String extJobId) {
            this.extJobId=extJobId;
            return this;
        }
        
        public Builder dateSubmittedToGp(final Date dateSubmittedToGp) {
            this.dateSubmittedToGp=dateSubmittedToGp;
            return this;
        }
        
        public Builder dateCompletedInGp(final Date dateCompletedInGp) {
            this.dateCompletedInGp=dateCompletedInGp;
            return this;
        }

        /**
         * initialize status from the given JobInfo.
         * @param jobInfo
         * @return
         */
        public Builder jobInfo(final JobInfo jobInfo) {
            this.jobInfo=jobInfo;
            return this;
        }
        /**
         * Set the execution log location, as an href, e.g.
         *     http://127.0.0.1:8080/gp/jobResults/1/gp_execution_log.txt
         *     
         * @param executionLogLocation
         * @return
         */
        public Builder executionLogLocation(String executionLogLocation) {
            this.executionLogLocation=executionLogLocation;
            return this;
        }
        
        public Builder stderrLocation(String stderrLocation) {
            this.stderrLocation=stderrLocation;
            return this;
        }
        
        /**
         * Add status details from the newer JobRunner API.
         * This record is loaded from the 'job_runner_job' table.
         * The values in the jobStatusRecord take precedence over the values in the jobInfo,
         * so that we can include more detailed job status information such as 'Pending in LSF queue'.
         * 
         * @param jrj
         * @return
         */
        public Builder jobStatusRecord(final JobRunnerJob jrj) {
            this.jobStatusRecord=jrj;
            return this;
        }

        /**
         * Add href to the parent job, so that links can be generated.
         * @param jobHref
         * @return
         */
        public Builder jobHref(final String jobHref) {
            this.jobHref=jobHref;
            return this;
        }
        
        /**
         * Add 'resource request' to status
         * @return
         */
        public Builder addResourceRequirement(final String key, final String value) {
            return addResourceRequirement(key, value, value);
        }
        
        public Builder addResourceRequirement(final String key, final String value, final String displayValue) {
            return addResourceRequirement(new StatusEntry(key, value, displayValue));
        }

        public Builder addResourceRequirement(final StatusEntry entry) {
            if (resourceRequirements==null) {
                resourceRequirements=new ArrayList<StatusEntry>();
            }
            resourceRequirements.add(entry);
            return this;
        }

        public Builder addUsageStat(final String key, final String value) {
            return addUsageStat(key, key, value, value);
        }

        public Builder addUsageStat(final String key, final String keyName, final String value, final String displayValue) {
            return addUsageStat(new StatusEntry(key, keyName, value, displayValue));
        }

        public Builder addUsageStat(final StatusEntry entry) {
            if (usageStats==null) {
                usageStats=new ArrayList<StatusEntry>();
            }
            usageStats.add(entry);
            return this;
        }

        public Status build() {
            Status status = new Status();
            status.gpJobNo=gpJobNo;
            status.extJobId=extJobId;
            status.dateSubmittedToGp=dateSubmittedToGp;
            status.dateCompletedInGp=dateCompletedInGp;
            
            // step 1, initialize from optional JobInfo arg
            DrmJobState jobState = initFromJobInfo(status);
            if (executionLogLocation != null) {
                status.executionLogLocation=executionLogLocation;
            }
            if (stderrLocation != null) {
                status.stderrLocation=stderrLocation;
            }
            // step 2, initialize from optional jobStatusRecord arg
            jobState = initFromJobStatusRecord(status, jobState);

            if (jobState != null) {
                status.statusFlag=jobState.name();
                status.jobState=jobState;
                //special-case, when statusMessage is not set, use the status flag
                if (Strings.isNullOrEmpty(status.statusMessage)) {
                    status.statusMessage=jobState.getDescription();
                }
            }
            if (jobHref != null) {
                status.addLink( 
                        new GpLink.Builder().href( jobHref + "/status.json" )
                                .addRel( Rel.self )
                                .addRel( Rel.gp_status )
                            .build() 
                );
                status.addLink( new GpLink.Builder().href( jobHref ).addRel( Rel.gp_job ).build() );
            }
            
            if (jobStatusRecord != null) {
                status.gpJobNo=jobStatusRecord.getGpJobNo();
                status.submitTime=jobStatusRecord.getSubmitTime();
                status.startTime=jobStatusRecord.getStartTime();
                status.endTime=jobStatusRecord.getEndTime();
                status.exitCode=jobStatusRecord.getExitCode();
                status.queueId=jobStatusRecord.getQueueId();


                if (jobStatusRecord.getExitCode() != null && jobStatusRecord.getExitCode() != 0) {
                    status.hasError = true;
                }
                
                //initialize resource requirements
                if (jobStatusRecord.getRequestedMemory() != null) {
                    this.addResourceRequirement(JobRunner.PROP_MEMORY, Memory.fromSizeInBytes(jobStatusRecord.getRequestedMemory()).getDisplayValue());
                }
                if (jobStatusRecord.getRequestedCpuCount() != null) {
                    this.addResourceRequirement(JobRunner.PROP_CPU_COUNT, ""+jobStatusRecord.getRequestedCpuCount());
                }
                if (jobStatusRecord.getRequestedNodeCount() != null) {
                    this.addResourceRequirement(JobRunner.PROP_NODE_COUNT, ""+jobStatusRecord.getRequestedNodeCount());
                }
                final Walltime requestedWalltime=initWalltime(jobStatusRecord);
                if (requestedWalltime != null) {
                    this.addResourceRequirement(JobRunner.PROP_WALLTIME, requestedWalltime.toString());
                }
                if (jobStatusRecord.getRequestedQueue() != null) {
                    this.addResourceRequirement(JobRunner.PROP_QUEUE, jobStatusRecord.getRequestedQueue());
                }

                // initialize usage stats ...
                //   CPU Usage     ('cpuTimeMillis', 'cpuTime')
                if (jobStatusRecord.getCpuTime() != null) {
                    final CpuTime cpuTime= new CpuTime(jobStatusRecord.getCpuTime());
                    status.cpuTime=cpuTime;
                    this.addUsageStat("cpuTime", "CPU usage", ""+cpuTime.asMillis(), cpuTime.getDisplayValue());
                }
                //   Max memory    ('maxMemoryBytes', 'maxMemory')
                if (jobStatusRecord.getMaxMemory() != null) {
                    final Memory maxMemory=Memory.fromSizeInBytes(jobStatusRecord.getMaxMemory());
                    status.maxMemory=maxMemory;
                    this.addUsageStat("maxMemory", "Max memory", ""+maxMemory.format(), maxMemory.getDisplayValue());
                }
                else {
                    // for debugging (not set)
                    this.addUsageStat("maxMemory", "Max memory", "0", "");
                }
                //   Max swap      ('maxSwapBytes', 'maxSwap')
                if (jobStatusRecord.getMaxSwap() != null) {
                    final Memory maxSwap=Memory.fromSizeInBytes(jobStatusRecord.getMaxSwap());
                    status.maxSwap=maxSwap;
                    this.addUsageStat("maxSwap", "Max swap", maxSwap.format(), maxSwap.getDisplayValue());
                }
                else {
                    // for debugging
                    this.addUsageStat("maxSwap", "Max swap", "", "");
                }
                //   Max processes ('maxProcesses')
                status.maxProcesses=jobStatusRecord.getMaxProcesses();
                if (jobStatusRecord.getMaxProcesses() != null) {
                    this.addUsageStat("maxProcesses", "Max processes", ""+jobStatusRecord.getMaxProcesses(), ""+jobStatusRecord.getMaxProcesses());
                }
                else {
                    // for debugging
                    this.addUsageStat("maxProcesses", "Max processes", "", "");
                }
                //   Max threads   ('maxThreads')
                status.maxThreads=jobStatusRecord.getMaxThreads();
                if (jobStatusRecord.getMaxThreads() != null) {
                    this.addUsageStat("maxThreads", "Max threads", ""+jobStatusRecord.getMaxThreads(), ""+jobStatusRecord.getMaxThreads());
                }
                else {
                    // for debugging
                    this.addUsageStat("maxThreads", "Max threads", "", "");
                }
            }
            
            // when jobInfo != null, only set the isFinished flag after the 
            // output files have been recorded to the DB
            initIsFinished(status);
            
            if (this.resourceRequirements == null || this.resourceRequirements.size()==0) {
                status.resourceRequirements=Collections.emptyList();
            }
            else {
                status.resourceRequirements=ImmutableList.copyOf(this.resourceRequirements);
            }
            
            if (this.usageStats == null || this.usageStats.size()==0) {
                status.usageStats=Collections.emptyList();
            }
            else {
                status.usageStats=ImmutableList.copyOf(this.usageStats);
            }

            return status;
        }
        
        protected Walltime initWalltime(JobRunnerJob jrj) {
            String wtSpec=jrj.getRequestedWalltime();
            if (wtSpec == null) {
                log.debug("wtSpec is null");
                return null;
            }
            try {
                Walltime wt=Walltime.fromString(wtSpec);
                return wt;
            }
            catch (Throwable t) {
                log.error("Invalid wtSpec="+wtSpec, t);
                return null;
            }
        }
        
        private boolean initIsFinished(Status status) {
            if (jobInfo != null) {
                return status.isFinished = JobInfoUtil.isFinished(jobInfo);
            }
            return status.isFinished;
        }

        private DrmJobState initFromJobInfo(Status status) {
            if (jobInfo==null) {
                // no-op
                return null;
            }
            
            status.gpJobNo=jobInfo.getJobNumber();
            status.dateSubmittedToGp=jobInfo.getDateSubmitted();
            status.dateCompletedInGp=jobInfo.getDateCompleted();
            status.isFinished=JobInfoUtil.isFinished(jobInfo);
            status.hasError=JobInfoUtil.hasError(jobInfo);
            status.isPending=JobInfoUtil.isPending(jobInfo);

            DrmJobState jobState=null;
            if (status.isPending) {
                jobState=DrmJobState.GP_PENDING;
                status.statusDate=jobInfo.getDateSubmitted();
            }
            else if (status.isFinished) {
                jobState=DrmJobState.GP_FINISHED;
                status.statusDate=jobInfo.getDateCompleted();
            }
            else {
                jobState=DrmJobState.GP_PROCESSING;
                status.statusDate=jobInfo.getDateSubmitted();
            }
            return jobState;
        }

        private DrmJobState initFromJobStatusRecord(Status status, DrmJobState jobState) {
            if (jobStatusRecord == null) {
                // no-op
                return jobState;
            }
            String jobStateStr=jobStatusRecord.getJobState();
            if (!Strings.isNullOrEmpty(jobStateStr)) {
                try {
                    jobState=DrmJobState.valueOf(jobStateStr);
                    if (jobState.is(DrmJobState.IS_QUEUED)) {
                        //we are still pending regardless of what the GP DB says!
                        status.isPending=true;
                    }
                    else if (jobState.is(DrmJobState.TERMINATED)) {
                        status.isFinished=true;
                    }
                }
                catch (Throwable t) {
                    log.error("Error initializing DrmJobState enum from job_runner_job.job_state column="+jobStateStr, t);
                }
            }
            if (!Strings.isNullOrEmpty(jobStatusRecord.getStatusMessage())) {
                status.statusMessage=jobStatusRecord.getStatusMessage();
            }
            status.statusDate=jobStatusRecord.getStatusDate();
            status.extJobId=jobStatusRecord.getExtJobId();
            return jobState;
        }
    }
}
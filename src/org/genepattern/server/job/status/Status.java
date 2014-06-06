package org.genepattern.server.job.status;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobState;
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

 * </pre>
 * 
 * @author pcarr
 *
 */
public class Status {
    private static final Logger log = Logger.getLogger(Status.class);

    private String executionLogLocation=null;
    private String stderrLocation=null;
    private boolean hasError=false;
    private boolean isFinished=false;
    private boolean isPending=false; 
    private DrmJobState jobState=null;
    private String statusFlag="";
    private String statusMessage="";
    private String extJobId=null;
    private Date statusDate=new Date();
    private List<GpLink> links=null;
    
    private void addLink(GpLink link) {
        if (links==null) {
            links=new ArrayList<GpLink>();
        }
        links.add(link);
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
    
    public String getStatusFlag() {
        return statusFlag;
    }
    public String getStatusMessage() {
        return statusMessage;
    }
    
    public Date getStatusDate() {
        return statusDate;
    }
    
    public DrmJobState getJobState() {
        return jobState;
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
        private JobInfo jobInfo=null;
        private String executionLogLocation=null;
        private String stderrLocation=null;
        private JobRunnerJob jobStatusRecord=null;
        private String jobHref;

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
        
        public Status build() {
            Status status = new Status();
            
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
            return status;
        }

        private DrmJobState initFromJobInfo(Status status) {
            if (jobInfo==null) {
                // no-op
                return null;
            }
            
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
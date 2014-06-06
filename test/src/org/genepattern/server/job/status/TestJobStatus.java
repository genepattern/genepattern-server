package org.genepattern.server.job.status;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.genepattern.drm.DrmJobState;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.genepattern.server.webapp.rest.api.v1.job.GpLink;
import org.genepattern.webservice.JobInfo;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * junit tests for the getting the job status.
 * @author pcarr
 *
 */
public class TestJobStatus {

    private static final int gpJobNo = 1;
    private static final String jobHref = "http://127.0.0.1:8080/gp/jobResults/"+gpJobNo;
    private static final String executionLogLocation = "http://127.0.0.1:8080/gp/jobResults/"+gpJobNo+"/gp_execution_log.txt";
    private static final String stderrLocation = "http://127.0.0.1:8080/gp/jobResults/"+gpJobNo+"/stderr.txt";

    private JobInfo jobInfo;
    JobRunnerJob jobStatusRecord;
    private Date dateSubmitted; // added to GP
    private Date dateQueued; // bsub command
    private Date dateStarted; // lsf status from PENDING -> RUNNING
    private Date dateCompleted; // lsf status from RUNNING -> FINISHED
    private String tzOffsetStr;
    private Date statusDate;
    
    @Before
    public void setUp() {
        jobInfo=mock(JobInfo.class);

        DateTime dt = new DateTime("2014-06-01T08:55:10.23");
        tzOffsetStr=DateTimeFormat.forPattern("ZZ").print(dt);
        dateSubmitted = dt.toDate();
        dateQueued=dt.plusMinutes(3).plusSeconds(15).toDate();
        dateStarted=dt.plusDays(1).plusHours(-5).plusMinutes(23).toDate();
        dateCompleted=dt.plusDays(3).plusHours(4).plusMinutes(25).toDate();
        when(jobInfo.getDateSubmitted()).thenReturn(dateSubmitted);
        when(jobInfo.getDateCompleted()).thenReturn(null);
        
        jobStatusRecord=mock(JobRunnerJob.class);
        statusDate=new Date();
        when(jobStatusRecord.getGpJobNo()).thenReturn(gpJobNo);
        when(jobStatusRecord.getStatusDate()).thenReturn(statusDate);
        when(jobStatusRecord.getExtJobId()).thenReturn("8937799");
        when(jobStatusRecord.getStatusMessage()).thenReturn(null);
    }
    
    /**
     * test case for getting the status.json for a job. For example,
     * <pre>
       "status": {
         "executionLogLocation": "http://gpdev.broadinstitute.org:80/gp/jobResults/65305/gp_execution_log.txt",
         "hasError": true,
         "isFinished": true,
         "isPending": false,
         "stderrLocation": "http://gpdev.broadinstitute.org:80/gp/jobResults/65305/stderr.txt",
         "jobState": "UNDETERMINED",
         "statusMessage": "Status unknown"
         "statusDate": "<the last time the status was checked, in ISO 8601 format>"
       }
       </pre>
       
       "status": {
           "isPending": false,
           "isFinished": true,
           "hasError": false,
           "executionLogLocation": "http://gpdev.broadinstitute.org:80/gp/jobResults/65349/gp_execution_log.txt"
  }
     */
    @Test
    public void getStatus_default() throws Exception {
        Status status= new Status.Builder().build();
        final JSONObject statusObj = status.toJsonObj();
        
        statusObj.getBoolean("hasError");
        statusObj.getBoolean("isFinished");
        statusObj.getBoolean("isPending");
        statusObj.getString("statusFlag"); 
        statusObj.getString("statusDate");
        statusObj.getString("statusMessage");
        Assert.assertFalse("expect no 'executionLogLocation'", statusObj.has("executionLogLocation"));
        Assert.assertFalse("expect no 'stderrLocation'", statusObj.has("stderrLocation"));
    }
    
    @Test
    public void builderNullArgs() throws JSONException {
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(null) //ignore null jobStatusRecord
            .build();
        final JSONObject statusObj = status.toJsonObj();
        statusObj.getBoolean("hasError");
        statusObj.getBoolean("isFinished");
        statusObj.getBoolean("isPending");
        statusObj.getString("statusFlag"); 
        statusObj.getString("statusDate");
        statusObj.getString("statusMessage");
        Assert.assertFalse("expect no 'executionLogLocation'", statusObj.has("executionLogLocation"));
        Assert.assertFalse("expect no 'stderrLocation'", statusObj.has("stderrLocation"));
    }
    
    /**
     * fix for GP-5198
     * @throws JSONException
     */
    @Test
    public void nullJobStateEnumValue() throws JSONException {
        when(jobStatusRecord.getJobState()).thenReturn(null);
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobStatusRecord)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        Assert.assertEquals("Expecting empty string", 
                "",
                statusObj.getString("statusFlag"));
    }
    
    @Test
    public void emptyJobStateEnumValue() throws JSONException {
        when(jobStatusRecord.getJobState()).thenReturn("");
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobStatusRecord)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        Assert.assertEquals("Expecting empty string", 
                "",
                statusObj.getString("statusFlag"));
    }

    @Test
    public void bogusJobStateEnumValue() throws JSONException {
        when(jobStatusRecord.getJobState()).thenReturn("PROCESSING");  // not a valid DrmJobState value
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobStatusRecord)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        Assert.assertEquals("Expecting empty string", 
                "",
                statusObj.getString("statusFlag"));
    }
    
    @Test
    public void nullStatusFlagNonNullStatusMessage() throws JSONException {
        when(jobStatusRecord.getStatusMessage()).thenReturn("Custom status message");  // not a valid DrmJobState value
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobStatusRecord)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        Assert.assertEquals( 
                "Custom status message",
                statusObj.getString("statusMessage"));
    }

    @Test
    public void nullStatusMessageNonNullStatusFlag() throws JSONException {
        when(jobStatusRecord.getJobState()).thenReturn(DrmJobState.QUEUED.name());  // not a valid DrmJobState value
        when(jobStatusRecord.getStatusMessage()).thenReturn("");  // not a valid DrmJobState value
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobStatusRecord)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        Assert.assertEquals( 
                "The job is queued or being scheduled and executed",
                statusObj.getString("statusMessage"));
    }
    
    @Test
    public void nullJobStatusRecord() throws JSONException {
        when(jobInfo.getStatus()).thenReturn(JobStatus.PENDING);
        Status status=new Status.Builder()
            .jobHref(jobHref)
            .jobInfo(jobInfo)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        Assert.assertEquals( 
                "Pending in the GenePattern queue, it has not been submitted to an external queuing system",
                statusObj.getString("statusMessage"));
        
    }


    @Test
    public void pendingInGp() throws Exception {
        //get job status when there is no entry in the job_runner_job table
        when(jobInfo.getStatus()).thenReturn(JobStatus.PENDING);
        
        Status status=new Status.Builder()
            .jobHref(jobHref)
            .jobInfo(jobInfo)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        Assert.assertEquals("isPending", true, statusObj.getBoolean("isPending"));
        Assert.assertEquals("isFinished", false, statusObj.getBoolean("isFinished"));
        Assert.assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        Assert.assertEquals("jobState", "GP_PENDING", statusObj.getString("statusFlag"));
        Assert.assertEquals("statusMessage", 
                "Pending in the GenePattern queue, it has not been submitted to an external queuing system", 
                statusObj.getString("statusMessage"));
        Assert.assertEquals("statusDate", 
                "2014-06-01T08:55:10"+tzOffsetStr, statusObj.getString("statusDate"));

        Assert.assertFalse("Expecting executionLogLocation to not be set", statusObj.has("executionLogLocation"));
        
        //expecting links
        Assert.assertTrue("Expecting a 'links' array", statusObj.has("links"));
        JSONArray linksArray = statusObj.getJSONArray("links");
        List<JSONObject> selfLinks = GpLink.findLinks(Rel.self.name(), linksArray);
        Assert.assertEquals("Expecting 1 'self' link", 1, selfLinks.size());
        Assert.assertTrue( "self", matchesRel("self", selfLinks.get(0).getString("rel") ));
        Assert.assertTrue( "gp_status", matchesRel(Rel.gp_status.name(), selfLinks.get(0).getString("rel") ));
    }
    
    /**
     * Does the given 'rel' match on of the space-separated list of zero or more 'rels'?
     * 
     * Note: should replace this with a regular expression.
     * @param rel
     * @param rels
     * @return
     */
    private boolean matchesRel(String rel, String rels) {
        rel=rel.toLowerCase();
        rels=rels.toLowerCase();
        if (rel.equals(rels)) {
            return true;
        }
        else if (rels.startsWith(rel+" ")) {
            return true;
        }
        else if (rels.endsWith(" "+rel)) {
            return true;
        }
        else if (rels.contains(" "+rel+" ")) {
            return true;
        }
        return false;
    }
    
    @Test
    public void processingInGp() throws Exception {
        //get job status when there is no entry in the job_runner_job table
        when(jobInfo.getStatus()).thenReturn(JobStatus.PROCESSING);
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
        .build();
        
        //final JSONObject statusObj = new JSONObject( status.toJson() );
        final JSONObject statusObj = status.toJsonObj();
        
        Assert.assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        Assert.assertEquals("isFinished", false, statusObj.getBoolean("isFinished"));
        Assert.assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        Assert.assertEquals("statusFlag", "GP_PROCESSING", statusObj.getString("statusFlag"));
        Assert.assertEquals("statusMessage", 
                "Submitted from GenePattern to the external queuing system", 
                statusObj.getString("statusMessage"));
        Assert.assertEquals("statusDate", 
                "2014-06-01T08:55:10"+tzOffsetStr, statusObj.getString("statusDate"));
        
        Assert.assertFalse("Expecting executionLogLocation to not be set", statusObj.has("executionLogLocation"));
    }

    @Test
    public void finishedInGp() throws Exception {
        Date dateCompleted=new Date();
        //get job status when there is no entry in the job_runner_job table
        when(jobInfo.getStatus()).thenReturn(JobStatus.FINISHED);
        when(jobInfo.getDateCompleted()).thenReturn(dateCompleted);
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .executionLogLocation(executionLogLocation)
        .build();
        
        //final JSONObject statusObj = new JSONObject( status.toJson() );
        final JSONObject statusObj = status.toJsonObj();
        
        Assert.assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        Assert.assertEquals("isFinished", true, statusObj.getBoolean("isFinished"));
        Assert.assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        Assert.assertEquals("statusFlag", "GP_FINISHED", statusObj.getString("statusFlag"));
        Assert.assertEquals("statusMessage", 
                "Completed and status is recorded in the GenePattern database", 
                statusObj.getString("statusMessage"));
        Assert.assertEquals("statusDate", 
                DateUtil.toIso8601(dateCompleted), statusObj.getString("statusDate"));
        
        Assert.assertEquals( "executionLogLocation",
                executionLogLocation,
                statusObj.getString("executionLogLocation"));
    }

    @Test
    public void finishedInGpWithError() throws Exception {
        Date dateCompleted=new Date();
        //get job status when there is no entry in the job_runner_job table
        when(jobInfo.getStatus()).thenReturn(JobStatus.ERROR);
        when(jobInfo.getDateCompleted()).thenReturn(dateCompleted);
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .stderrLocation(stderrLocation)
            .executionLogLocation(executionLogLocation)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        Assert.assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        Assert.assertEquals("isFinished", true, statusObj.getBoolean("isFinished"));
        Assert.assertEquals("hasError", true, statusObj.getBoolean("hasError"));
        Assert.assertEquals("statusFlag", "GP_FINISHED", statusObj.getString("statusFlag"));
        Assert.assertEquals("statusMessage", 
                "Completed and status is recorded in the GenePattern database", 
                statusObj.getString("statusMessage"));
        Assert.assertEquals("statusDate", 
                DateUtil.toIso8601(dateCompleted), statusObj.getString("statusDate"));
        
        Assert.assertEquals( "executionLogLocation",
                executionLogLocation,
                statusObj.getString("executionLogLocation"));
        Assert.assertEquals( "stderrLocation",
                stderrLocation,
                statusObj.getString("stderrLocation"));
    }
    
    @Test
    public void pendingInLsf() throws Exception {
        when(jobStatusRecord.getJobState()).thenReturn(DrmJobState.QUEUED.name());
        when(jobStatusRecord.getStatusMessage()).thenReturn("Added to queue on "+DateUtil.toIso8601(dateQueued));
        when(jobStatusRecord.getExtJobId()).thenReturn("");
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .jobStatusRecord(jobStatusRecord)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        Assert.assertEquals("isPending", true, statusObj.getBoolean("isPending"));
        Assert.assertEquals("isFinished", false, statusObj.getBoolean("isFinished"));
        Assert.assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        Assert.assertEquals("statusFlag", "QUEUED", statusObj.getString("statusFlag"));
        Assert.assertEquals("statusMessage", 
                "Added to queue on "+DateUtil.toIso8601(dateQueued),
                statusObj.getString("statusMessage"));
        Assert.assertEquals("statusDate", 
                DateUtil.toIso8601(statusDate), statusObj.getString("statusDate"));

        Assert.assertFalse("Expecting executionLogLocation to not be set", statusObj.has("executionLogLocation"));
        Assert.assertEquals("extJobId", "", statusObj.getString("extJobId"));
    }

    @Test
    public void runningInLsf() throws Exception { 
        when(jobStatusRecord.getJobState()).thenReturn(DrmJobState.RUNNING.name());
        when(jobStatusRecord.getStatusMessage()).thenReturn("Started on "+DateUtil.toIso8601(dateStarted));
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .jobStatusRecord(jobStatusRecord)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        Assert.assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        Assert.assertEquals("isFinished", false, statusObj.getBoolean("isFinished"));
        Assert.assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        Assert.assertEquals("statusFlag", "RUNNING", statusObj.getString("statusFlag"));
        Assert.assertEquals("statusMessage", 
                "Started on "+DateUtil.toIso8601(dateStarted),
                statusObj.getString("statusMessage"));
        Assert.assertEquals("statusDate", 
                DateUtil.toIso8601(statusDate), statusObj.getString("statusDate"));

        Assert.assertFalse("Expecting executionLogLocation to not be set", statusObj.has("executionLogLocation"));
        Assert.assertEquals("extJobId", "8937799", statusObj.getString("extJobId"));
    }

    @Test
    public void finishedInLsf() throws Exception { 
        when(jobStatusRecord.getJobState()).thenReturn(DrmJobState.DONE.name());
        when(jobStatusRecord.getStatusMessage()).thenReturn("Completed on "+DateUtil.toIso8601(dateCompleted));
        when(jobStatusRecord.getStatusDate()).thenReturn(dateCompleted);
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .executionLogLocation(executionLogLocation)
            .jobStatusRecord(jobStatusRecord)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        Assert.assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        Assert.assertEquals("isFinished", true, statusObj.getBoolean("isFinished"));
        Assert.assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        Assert.assertEquals("statusFlag", "DONE", statusObj.getString("statusFlag"));
        Assert.assertEquals("statusMessage", 
                "Completed on "+DateUtil.toIso8601(dateCompleted),
                statusObj.getString("statusMessage"));
        Assert.assertEquals("statusDate", 
                DateUtil.toIso8601(dateCompleted), statusObj.getString("statusDate"));

        Assert.assertEquals( "executionLogLocation",
                executionLogLocation,
                statusObj.getString("executionLogLocation"));
    }

    @Test
    public void finishedInLsfWithError() throws Exception { 
        when(jobInfo.getStatus()).thenReturn(JobStatus.ERROR);
        when(jobStatusRecord.getJobState()).thenReturn(DrmJobState.FAILED.name());
        when(jobStatusRecord.getStatusMessage()).thenReturn("Failed on "+DateUtil.toIso8601(dateCompleted));
        when(jobStatusRecord.getStatusDate()).thenReturn(dateCompleted);
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .executionLogLocation(executionLogLocation)
            .jobStatusRecord(jobStatusRecord)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        Assert.assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        Assert.assertEquals("isFinished", true, statusObj.getBoolean("isFinished"));
        Assert.assertEquals("hasError", true, statusObj.getBoolean("hasError"));
        Assert.assertEquals("statusFlag", "FAILED", statusObj.getString("statusFlag"));
        Assert.assertEquals("statusMessage", 
                "Failed on "+DateUtil.toIso8601(dateCompleted),
                statusObj.getString("statusMessage"));
        Assert.assertEquals("statusDate", 
                DateUtil.toIso8601(dateCompleted), statusObj.getString("statusDate"));

        Assert.assertEquals( "executionLogLocation",
                executionLogLocation,
                statusObj.getString("executionLogLocation"));
    }

}

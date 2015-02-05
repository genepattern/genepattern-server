package org.genepattern.server.job.status;

import static org.genepattern.server.webapp.rest.api.v1.DateUtil.HOUR;
import static org.genepattern.server.webapp.rest.api.v1.DateUtil.MIN;
import static org.genepattern.server.webapp.rest.api.v1.DateUtil.SEC;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.drm.Walltime;
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
    JobRunnerJob jobRunnerJob;
    private Date dateAddedToGp; // added to GP
    private Date dateSubmittedToQueue; // bsub command
    private Date dateStartedInQueue; // lsf status from PENDING -> RUNNING
    private Date dateFinishedInQueue; // lsf status from RUNNING -> FINISHED
    private Date dateCompletedInGp;

//    //include usage stats
//    Date addedToGp=new DateTime("2014-07-17T10:55:23").toDate();
//    Date submitTime=new DateTime("2014-07-17T11:55:23").toDate();
//    Date startTime=new DateTime("2014-07-18T01:55:23").toDate();
//    Date endTime=new DateTime("2014-07-18T13:55:23").toDate();
//    Date completedInGp=new DateTime("2014-07-18T14:00:00").toDate();

    private String tzOffsetStr;
    private Date statusDate;
    private String queueId="genepattern_long";
    
    @Before
    public void setUp() { 
        jobInfo=mock(JobInfo.class);

        DateTime dt = new DateTime("2014-06-01T08:55:10.23");
        tzOffsetStr=DateTimeFormat.forPattern("ZZ").print(dt);
        dateAddedToGp = dt.toDate();
        dateSubmittedToQueue=dt.plusMinutes(3).plusSeconds(15).toDate();
        dateStartedInQueue=dt.plusDays(1).plusHours(-5).plusMinutes(23).toDate();
        dateFinishedInQueue=dt.plusDays(3).plusHours(4).plusMinutes(25).toDate();
        dateCompletedInGp=dt.plusMinutes(5).toDate();
        when(jobInfo.getDateSubmitted()).thenReturn(dateAddedToGp);
        when(jobInfo.getDateCompleted()).thenReturn(null);
        
        jobRunnerJob=mock(JobRunnerJob.class);
        statusDate=new Date();
        when(jobRunnerJob.getGpJobNo()).thenReturn(gpJobNo);
        when(jobRunnerJob.getStatusDate()).thenReturn(statusDate);
        when(jobRunnerJob.getExtJobId()).thenReturn("8937799");
        when(jobRunnerJob.getStatusMessage()).thenReturn(null);
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
        when(jobRunnerJob.getJobState()).thenReturn(null);
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobRunnerJob)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        assertEquals("Expecting empty string", 
                "",
                statusObj.getString("statusFlag"));
    }
    
    @Test
    public void emptyJobStateEnumValue() throws JSONException {
        when(jobRunnerJob.getJobState()).thenReturn("");
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobRunnerJob)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        assertEquals("Expecting empty string", 
                "",
                statusObj.getString("statusFlag"));
    }

    @Test
    public void bogusJobStateEnumValue() throws JSONException {
        when(jobRunnerJob.getJobState()).thenReturn("PROCESSING");  // not a valid DrmJobState value
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobRunnerJob)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        assertEquals("Expecting empty string", 
                "",
                statusObj.getString("statusFlag"));
    }
    
    @Test
    public void nullStatusFlagNonNullStatusMessage() throws JSONException {
        when(jobRunnerJob.getStatusMessage()).thenReturn("Custom status message");  // not a valid DrmJobState value
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobRunnerJob)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        assertEquals( 
                "Custom status message",
                statusObj.getString("statusMessage"));
    }

    @Test
    public void nullStatusMessageNonNullStatusFlag() throws JSONException {
        when(jobRunnerJob.getJobState()).thenReturn(DrmJobState.QUEUED.name());  // not a valid DrmJobState value
        when(jobRunnerJob.getStatusMessage()).thenReturn("");  // not a valid DrmJobState value
        Status status= new Status.Builder()
            .jobInfo(null)  //ignore null jobInfo
            .jobStatusRecord(jobRunnerJob)
            .build();
        final JSONObject statusObj = status.toJsonObj();
        assertEquals( 
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
        assertEquals( 
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
        
        assertEquals("isPending", true, statusObj.getBoolean("isPending"));
        assertEquals("isFinished", false, statusObj.getBoolean("isFinished"));
        assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        assertEquals("jobState", "GP_PENDING", statusObj.getString("statusFlag"));
        assertEquals("statusMessage", 
                "Pending in the GenePattern queue, it has not been submitted to an external queuing system", 
                statusObj.getString("statusMessage"));
        assertEquals("statusDate", 
                "2014-06-01T08:55:10"+tzOffsetStr, statusObj.getString("statusDate"));

        Assert.assertFalse("Expecting executionLogLocation to not be set", statusObj.has("executionLogLocation"));
        
        //expecting links
        Assert.assertTrue("Expecting a 'links' array", statusObj.has("links"));
        JSONArray linksArray = statusObj.getJSONArray("links");
        List<JSONObject> selfLinks = GpLink.findLinks(Rel.self.name(), linksArray);
        assertEquals("Expecting 1 'self' link", 1, selfLinks.size());
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
        
        assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        assertEquals("isFinished", false, statusObj.getBoolean("isFinished"));
        assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        assertEquals("statusFlag", "GP_PROCESSING", statusObj.getString("statusFlag"));
        assertEquals("statusMessage", 
                "Submitted from GenePattern to the external queuing system", 
                statusObj.getString("statusMessage"));
        assertEquals("statusDate", 
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
        
        assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        assertEquals("isFinished", true, statusObj.getBoolean("isFinished"));
        assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        assertEquals("statusFlag", "GP_FINISHED", statusObj.getString("statusFlag"));
        assertEquals("statusMessage", 
                "Completed and status is recorded in the GenePattern database", 
                statusObj.getString("statusMessage"));
        assertEquals("statusDate", 
                DateUtil.toIso8601(dateCompleted), statusObj.getString("statusDate"));
        
        assertEquals( "executionLogLocation",
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
        
        assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        assertEquals("isFinished", true, statusObj.getBoolean("isFinished"));
        assertEquals("hasError", true, statusObj.getBoolean("hasError"));
        assertEquals("statusFlag", "GP_FINISHED", statusObj.getString("statusFlag"));
        assertEquals("statusMessage", 
                "Completed and status is recorded in the GenePattern database", 
                statusObj.getString("statusMessage"));
        assertEquals("statusDate", 
                DateUtil.toIso8601(dateCompleted), statusObj.getString("statusDate"));
        
        assertEquals( "executionLogLocation",
                executionLogLocation,
                statusObj.getString("executionLogLocation"));
        assertEquals( "stderrLocation",
                stderrLocation,
                statusObj.getString("stderrLocation"));
    }
    
    @Test
    public void pendingInLsf() throws Exception {
        when(jobRunnerJob.getJobState()).thenReturn(DrmJobState.QUEUED.name());
        when(jobRunnerJob.getStatusMessage()).thenReturn("Added to queue on "+DateUtil.toIso8601(dateSubmittedToQueue));
        when(jobRunnerJob.getExtJobId()).thenReturn("");
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .jobStatusRecord(jobRunnerJob)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        assertEquals("isPending", true, statusObj.getBoolean("isPending"));
        assertEquals("isFinished", false, statusObj.getBoolean("isFinished"));
        assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        assertEquals("statusFlag", "QUEUED", statusObj.getString("statusFlag"));
        assertEquals("statusMessage", 
                "Added to queue on "+DateUtil.toIso8601(dateSubmittedToQueue),
                statusObj.getString("statusMessage"));
        assertEquals("statusDate", 
                DateUtil.toIso8601(statusDate), statusObj.getString("statusDate"));

        Assert.assertFalse("Expecting executionLogLocation to not be set", statusObj.has("executionLogLocation"));
        assertEquals("extJobId", "", statusObj.getString("extJobId"));
    }

    @Test
    public void runningInLsf() throws Exception { 
        when(jobRunnerJob.getJobState()).thenReturn(DrmJobState.RUNNING.name());
        when(jobRunnerJob.getStatusMessage()).thenReturn("Started on "+DateUtil.toIso8601(dateStartedInQueue));
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .jobStatusRecord(jobRunnerJob)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        assertEquals("isFinished", false, statusObj.getBoolean("isFinished"));
        assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        assertEquals("statusFlag", "RUNNING", statusObj.getString("statusFlag"));
        assertEquals("statusMessage", 
                "Started on "+DateUtil.toIso8601(dateStartedInQueue),
                statusObj.getString("statusMessage"));
        assertEquals("statusDate", 
                DateUtil.toIso8601(statusDate), statusObj.getString("statusDate"));

        Assert.assertFalse("Expecting executionLogLocation to not be set", statusObj.has("executionLogLocation"));
        assertEquals("extJobId", "8937799", statusObj.getString("extJobId"));
    }

    @Test
    public void finishedInLsf_fullStatusCheck() throws Exception { 
        when(jobRunnerJob.getJobState()).thenReturn(DrmJobState.DONE.name());
        when(jobRunnerJob.getStatusMessage()).thenReturn("Completed on "+DateUtil.toIso8601(dateFinishedInQueue));
        when(jobRunnerJob.getStatusDate()).thenReturn(dateFinishedInQueue);
        
        //include usage stats
        Date addedToGp=new DateTime("2014-07-17T10:55:23").toDate();
        Date submitTime=new DateTime("2014-07-17T11:55:23").toDate();
        Date startTime=new DateTime("2014-07-18T01:55:23").toDate();
        Date endTime=new DateTime("2014-07-18T13:55:23").toDate();
        Date completedInGp=new DateTime("2014-07-18T14:00:00").toDate();
        
        // 4 hours 37 minutes 20.12 seconds
        Long cpuTimeMillis =
                (4 * HOUR) + (37 * MIN) + (20 * SEC) + 120;
        
        Memory maxMemory=Memory.fromString("21266 MB");
        Memory maxSwap=Memory.fromString("21341 MB");
        Integer maxProcesses=2;
        Integer maxThreads=4;
        
        when(jobInfo.getDateSubmitted()).thenReturn(addedToGp);
        when(jobInfo.getDateCompleted()).thenReturn(completedInGp);
        when(jobInfo.getStatus()).thenReturn(JobStatus.FINISHED);

        when(jobRunnerJob.getSubmitTime()).thenReturn(submitTime);
        when(jobRunnerJob.getStartTime()).thenReturn(startTime);
        when(jobRunnerJob.getEndTime()).thenReturn(endTime);
        when(jobRunnerJob.getCpuTime()).thenReturn(cpuTimeMillis);
        when(jobRunnerJob.getMaxMemory()).thenReturn(maxMemory.getNumBytes());
        when(jobRunnerJob.getMaxSwap()).thenReturn(maxSwap.getNumBytes());
        when(jobRunnerJob.getMaxProcesses()).thenReturn(maxProcesses);
        when(jobRunnerJob.getMaxThreads()).thenReturn(maxThreads);
        when(jobRunnerJob.getQueueId()).thenReturn(queueId);
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .executionLogLocation(executionLogLocation)
            .jobStatusRecord(jobRunnerJob)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        assertEquals("isFinished", true, statusObj.getBoolean("isFinished"));
        assertEquals("hasError", false, statusObj.getBoolean("hasError"));
        assertEquals("statusFlag", "DONE", statusObj.getString("statusFlag"));
        assertEquals("statusMessage", 
                "Completed on "+DateUtil.toIso8601(dateFinishedInQueue),
                statusObj.getString("statusMessage"));
        assertEquals("statusDate", 
                DateUtil.toIso8601(dateFinishedInQueue), statusObj.getString("statusDate"));

        assertEquals( "executionLogLocation",
                executionLogLocation,
                statusObj.getString("executionLogLocation"));
        
        assertEquals("addedToGp",     "2014-07-17T10:55:23"+tzOffsetStr, statusObj.getString("addedToGp"));
        assertEquals("submitTime",    "2014-07-17T11:55:23"+tzOffsetStr, statusObj.getString("submitTime"));
        assertEquals("startTime",     "2014-07-18T01:55:23"+tzOffsetStr, statusObj.getString("startTime"));
        assertEquals("endTime",       "2014-07-18T13:55:23"+tzOffsetStr, statusObj.getString("endTime"));
        assertEquals("completedInGp", "2014-07-18T14:00:00"+tzOffsetStr, statusObj.getString("completedInGp"));
        
        assertEquals("cpuTimeMillis", cpuTimeMillis, (Long) statusObj.getLong("cpuTimeMillis"));
        assertEquals("cpuTime", "4 hours, 37 minutes, 20 seconds and 120 milliseconds", statusObj.getString("cpuTime"));
        
        assertEquals("maxMemoryBytes", maxMemory.getNumBytes(), statusObj.getLong("maxMemoryBytes")); 
        assertEquals("maxMemory", "20.8 GB", statusObj.getString("maxMemory"));

        assertEquals("maxSwapBytes", maxSwap.getNumBytes(), statusObj.getLong("maxSwapBytes")); 
        assertEquals("maxSwap", "20.8 GB", statusObj.getString("maxSwap"));
        assertEquals("maxProcesses", maxProcesses, (Integer) statusObj.getInt("maxProcesses"));
        assertEquals("maxThreads", maxThreads, (Integer) statusObj.getInt("maxThreads"));
        assertEquals("queueId", queueId, statusObj.getString("queueId"));
        
        assertNotNull("has eventLog", statusObj.getJSONArray("eventLog"));
        assertEquals("eventLog[0].event", "Added to GenePattern", 
                statusObj.getJSONArray("eventLog").getJSONObject(0).get("event"));
        assertEquals("eventLog[0].time", "2014-07-17T10:55:23"+tzOffsetStr, 
                statusObj.getJSONArray("eventLog").getJSONObject(0).get("time"));
        assertEquals("eventLog[1].event", "Submitted to queue", 
                statusObj.getJSONArray("eventLog").getJSONObject(1).get("event"));
        assertEquals("eventLog[1].time", "2014-07-17T11:55:23"+tzOffsetStr, 
                statusObj.getJSONArray("eventLog").getJSONObject(1).get("time"));
        assertEquals("eventLog[2].event", "Started running", 
                statusObj.getJSONArray("eventLog").getJSONObject(2).get("event"));
        assertEquals("eventLog[2].time", "2014-07-18T01:55:23"+tzOffsetStr, 
                statusObj.getJSONArray("eventLog").getJSONObject(2).get("time"));
        assertEquals("eventLog[3].event", "Finished running", 
                statusObj.getJSONArray("eventLog").getJSONObject(3).get("event"));
        assertEquals("eventLog[3].time", "2014-07-18T13:55:23"+tzOffsetStr, 
                statusObj.getJSONArray("eventLog").getJSONObject(3).get("time"));
        assertEquals("eventLog[4].event", "Completed in GenePattern", 
                statusObj.getJSONArray("eventLog").getJSONObject(4).get("event"));
        assertEquals("eventLog[4].time", "2014-07-18T14:00:00"+tzOffsetStr, 
                statusObj.getJSONArray("eventLog").getJSONObject(4).get("time"));
    }
    
    @Test
    public void submitTimeNotSet() throws JSONException {
        when(jobRunnerJob.getSubmitTime()).thenReturn(null);
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        assertFalse("expecting no submitTime", statusObj.has("submitTime"));
    }

    @Test
    public void startTimeNotSet() throws JSONException {
        when(jobRunnerJob.getStartTime()).thenReturn(null);
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        assertFalse("expecting no startTime", statusObj.has("startTime"));
    }

    @Test
    public void endTimeNotSet() throws JSONException {
        when(jobRunnerJob.getEndTime()).thenReturn(null);
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        assertFalse("expecting no endTime", statusObj.has("endTime"));
    }

    @Test
    public void cpuTimeNotSet() throws JSONException {
        when(jobRunnerJob.getCpuTime()).thenReturn(null);
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        assertFalse("expecting no cpuTime", statusObj.has("cpuTime"));
    }
    
    @Test
    public void maxMemoryNotSet() throws JSONException {
        when(jobRunnerJob.getMaxMemory()).thenReturn(null);
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        assertFalse("expecting no maxMemory", statusObj.has("maxMemory"));
        assertFalse("expecting no maxMemoryBytes", statusObj.has("maxMemoryBytes"));
    }
    
    @Test
    public void maxSwapNotSet() throws JSONException {
        when(jobRunnerJob.getMaxSwap()).thenReturn(null);
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        assertFalse("expecting no maxSwap", statusObj.has("maxSwap"));
        assertFalse("expecting no maxSwapBytes", statusObj.has("maxSwapBytes"));
    }
    
    @Test
    public void maxProcessesNotSet() throws JSONException {
        when(jobRunnerJob.getMaxProcesses()).thenReturn(null);
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        assertFalse("expecting no maxProcesses", statusObj.has("maxProcesses"));
    }
    
    @Test
    public void maxThreadsNotSet() throws JSONException {
        when(jobRunnerJob.getMaxThreads()).thenReturn(null);
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();
        final JSONObject statusObj = status.toJsonObj();
        assertFalse("expecting no maxThreads", statusObj.has("maxThreads"));
    }
    
    @Test
    public void finishedInLsfWithError() throws Exception { 
        when(jobInfo.getStatus()).thenReturn(JobStatus.ERROR);
        when(jobRunnerJob.getJobState()).thenReturn(DrmJobState.FAILED.name());
        when(jobRunnerJob.getStatusMessage()).thenReturn("Failed on "+DateUtil.toIso8601(dateFinishedInQueue));
        when(jobRunnerJob.getStatusDate()).thenReturn(dateFinishedInQueue);
        
        Status status=new Status.Builder()
            .jobInfo(jobInfo)
            .executionLogLocation(executionLogLocation)
            .jobStatusRecord(jobRunnerJob)
        .build();
        
        final JSONObject statusObj = status.toJsonObj();
        
        assertEquals("isPending", false, statusObj.getBoolean("isPending"));
        assertEquals("isFinished", true, statusObj.getBoolean("isFinished"));
        assertEquals("hasError", true, statusObj.getBoolean("hasError"));
        assertEquals("statusFlag", "FAILED", statusObj.getString("statusFlag"));
        assertEquals("statusMessage", 
                "Failed on "+DateUtil.toIso8601(dateFinishedInQueue),
                statusObj.getString("statusMessage"));
        assertEquals("statusDate", 
                DateUtil.toIso8601(dateFinishedInQueue), statusObj.getString("statusDate"));

        assertEquals( "executionLogLocation",
                executionLogLocation,
                statusObj.getString("executionLogLocation"));
    }

    /**
     * For GP-5285, when the status from the external queue is DONE, but the files have not yet been recorded into GP,
     * make sure isFinished is false.
     */
    @Test
    public void finishedInLsf_stillProcessingInGp() {
        when(jobRunnerJob.getJobState()).thenReturn(DrmJobState.DONE.name());
        when(jobRunnerJob.getSubmitTime()).thenReturn(dateSubmittedToQueue);
        when(jobRunnerJob.getStartTime()).thenReturn(dateStartedInQueue);
        when(jobRunnerJob.getEndTime()).thenReturn(dateFinishedInQueue);
        
        when(jobInfo.getDateSubmitted()).thenReturn(dateAddedToGp);
        when(jobInfo.getDateCompleted()).thenReturn(null);
        when(jobInfo.getStatus()).thenReturn(JobStatus.PROCESSING);
        
        Status status=new Status.Builder().jobInfo(jobInfo).jobStatusRecord(jobRunnerJob).build();
        assertEquals("isFinished", false, status.getIsFinished());
    }
    
    /**
     * Expecting a list of job resource requirements in the JSON representation, e.g.
<pre>
  "resourceRequirements": [
     { "key": "job.memory", "value": "4 Gb" },
     { "key": "job.cpuSlots", "value": "1" }
  ]
</pre>
     * @throws JSONException
     */
    @Test
    public void addResourceRequirements() throws JSONException {
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
            .addResourceRequirement(JobRunner.PROP_MEMORY, "16 Gb")
        .build();
        final JSONObject statusObj=status.toJsonObj();
        assertEquals(JobRunner.PROP_MEMORY, 
                statusObj.getJSONArray("resourceRequirements").getJSONObject(0).get("key"));
        assertEquals("16 Gb", 
                statusObj.getJSONArray("resourceRequirements").getJSONObject(0).get("value"));
    }
    
    @Test
    public void resourceRequirementsFromJobRunnerJob() throws Exception {
        final Memory reqMem=Memory.fromString("8 Gb");
        final Walltime reqWalltime=Walltime.fromString("7-00:00:00");
        when(jobRunnerJob.getRequestedMemory()).thenReturn(reqMem.getNumBytes());
        when(jobRunnerJob.getRequestedCpuCount()).thenReturn(4);
        when(jobRunnerJob.getRequestedNodeCount()).thenReturn(5);
        when(jobRunnerJob.getRequestedWalltime()).thenReturn(reqWalltime.toString());
        when(jobRunnerJob.getRequestedQueue()).thenReturn("my_queue");
        Status status=new Status.Builder()
            .jobStatusRecord(jobRunnerJob)
        .build();

        assertEquals("requestedMemory", "8 GB", status.getRequestedMemory().getDisplayValue());
        assertEquals("requestedCpuCount", (Integer)4, status.getRequestedCpuCount());
        assertEquals("requestedNodeCount", (Integer)5, status.getRequestedNodeCount());
        assertEquals("requestedWalltime", reqWalltime, status.getRequestedWalltime());
        assertEquals("requestedQueue", "my_queue", status.getRequestedQueue());
        
        assertEquals("resourceRequirements.size", 5, status.getResourceRequirements().size());
        assertEquals("resourceRequirements[0].key", "job.memory", status.getResourceRequirements().get(0).getKey());
        assertEquals("resourceRequirements[1].key", "job.cpuCount", status.getResourceRequirements().get(1).getKey());
        assertEquals("resourceRequirements[2].key", "job.nodeCount", status.getResourceRequirements().get(2).getKey());
        assertEquals("resourceRequirements[3].key", "job.walltime", status.getResourceRequirements().get(3).getKey());
        assertEquals("resourceRequirements[4].key", "job.queue", status.getResourceRequirements().get(4).getKey());
    }
}

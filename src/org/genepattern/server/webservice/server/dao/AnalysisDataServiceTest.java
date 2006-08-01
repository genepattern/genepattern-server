package org.genepattern.server.webservice.server.dao;

import java.sql.ResultSet;
import java.util.*;

import org.genepattern.util.IGPConstants;
import org.genepattern.webservice.*;
import org.hibernate.Transaction;

import junit.framework.TestCase;

public class AnalysisDataServiceTest extends ServiceTestBase {

    AnalysisDataService service = AnalysisDataService.getInstance();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
      }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @see AnalysisDAO#addNewJob(int taskID, String user_id, String
     *      parameter_info, int parent_id)
     * @throws Exception
     */
    public void testAddNewJob() throws Exception {
        JobInfo jobInfo = null;
        int taskId = 1;
        String userId = "twomey";
        String parameterInfo = service.getJobInfo(1).getParameterInfo();

        int parentId = 1;
        try {
            jobInfo = service.addNewJob(taskId, userId, parameterInfo, parentId);
            assertNotNull(jobInfo);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * @see AnalaysisJobService#getTemporaryPipelineName(int)
     */
    public void testGetTemporaryPipelineName() throws Exception {
        int jobNo = 1;
        String pipelineName = "SNPFileCreator";
        assertEquals(pipelineName, service.getTemporaryPipelineName(jobNo));
    }

    /**
     * @see AnalaysisJobService#createTemporaryPipeline(String user_id, String
     *      parameter_info, String pipelineName, String lsid)
     */
    public void testCreateTemporaryPipeline() throws Exception {
        JobInfo newJob = null;
        String userId = "twomey";
        String parameterInfo = service.getJobInfo(1).getParameterInfo();
        String pipelineName = "temp";
        String lsid = "test_" + System.currentTimeMillis();

        newJob = service.createTemporaryPipeline(userId, parameterInfo, pipelineName, lsid);
        assertNotNull(newJob);

        assertEquals(userId, newJob.getUserId());
        assertEquals(parameterInfo.length(), newJob.getParameterInfo().length());
        int l = parameterInfo.length();
        assertEquals("0-30", parameterInfo.substring(0, 30), newJob.getParameterInfo().substring(0, 30));
        assertEquals(parameterInfo.substring(l - 30), newJob.getParameterInfo().substring(l - 30));
        assertEquals(pipelineName, newJob.getTaskName());
        assertEquals(lsid, newJob.getTaskLSID());

    }


    /**
     * @see AnalysisDAO#getJobInfo(int)
     */
    public void testGetJobInfo() throws Exception {
        int jobNo = 1;
        int taskId = 24;

        JobInfo jobInfo = service.getJobInfo(jobNo);
        assertEquals(taskId, jobInfo.getTaskID());

    }
   /**
     * 
     */
    public void testGetJobsByDate() throws Exception {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, 5); // June
        cal.set(Calendar.DATE, 22);
        cal.set(Calendar.YEAR, 2006);
        cal.set(Calendar.HOUR, 1);
        cal.set(Calendar.MINUTE, 1);
        cal.set(Calendar.AM_PM, Calendar.AM);
        java.util.Date completionDate = cal.getTime();
        System.out.println(completionDate);

        JobInfo[] jobs = service.getJobInfo(completionDate);
        assertEquals(186, jobs.length);
    }

    /**
     * @see AnalysisDataService#getChildren(int) *
     * @param jobNo
     * @param taskID
     * @param status
     * @param submittedDate
     * @param completedDate
     * @param parameters
     * @param userId
     * @param lsid
     * 
     */
    public void testGetChildren() throws Exception {
        int childJobId = 45;
        int parentJobId = 44;
        JobInfo[] children = service.getChildren(parentJobId);
        assertEquals(1, children.length);
        assertEquals(childJobId, children[0].getJobNumber());

        assertEquals(45, children[0].getJobNumber());
        assertEquals(31, children[0].getTaskID());
        assertEquals("2006-06-12 10:24:26.232", children[0].getDateCompleted().toString());
        assertEquals("2006-06-12 10:24:19.293", children[0].getDateSubmitted().toString());
        assertTrue(children[0].getParameterInfo().contains("Mapping50K_Xba240.CDF"));
        assertEquals("bweir@broad.mit.edu", children[0].getUserId());
        assertEquals("urn:lsid:8080.genepatt.18.103.8.161:genepatternmodules:1:14.6", children[0].getTaskLSID());

    }

    /**
     * @see AnalysisDataService#setJobDeleted(int,boolean)
     */
    public void testSetJobDeleted() throws Exception {

        int jobCount = service.getJobs("twomey", 10, 10000000, false).length;

        int jobNo = 1;
        JobInfo jobInfo = service.getJobInfo(jobNo);
        assertNotNull(jobInfo);

        service.setJobDeleted(jobNo, true);

        int newJobCount = service.getJobs("twomey", 10, 10000000, false).length;
        assertEquals(jobCount - 1, newJobCount);

    }

    /**
     * 
     * @throws Exception
     */
    public void testResetPreviouslyRunningJobs() throws Exception {
        service.resetPreviouslyRunningJobs();
    }

    /**
     * 
     * @throws Exception
     */
    public void testExecuteSql() throws Exception {
        String sql = "select count(*) from job_status";
        ResultSet rs = service.executeSQL(sql);
        if (rs.next()) {
            assertEquals(4, rs.getInt(1));
        }
        else {
            fail("No rows returned");
        }

    }

    public void testExecuteUpdate() throws Exception {
        String sql = "CHECKPOINT";
        service.executeUpdate(sql);
    }

    /**
     * @see AnalysisDataService#getJobs(String,int,int,boolean)
     */
    public void testGetJobsByUser() throws Exception {
        int jobCount = service.getJobs("jgould", 10000000,  10, false).length;
        assertEquals(10, jobCount);

        jobCount = service.getJobs("jgould", 10, 5, false).length;
        assertEquals(5, jobCount);

        jobCount = service.getJobs(null, -1, 5, false).length;
        assertEquals(5, jobCount);
        

   }
    

    /**
     * @see AnalysisDAO#getWaitingJobs(int)
     */
    public void testGetWaitingJobs() throws Exception {
        int NUM_THREADS = 20;
        Vector jobVector = null;
        int waitingStatus = 1;
        
        // Set some jobs to "waiting" 
        JobInfo [] jobs = service.getJobs("jgould", 10000000, 10, true);
        for(int i=0; i<jobs.length; i++) {
            service.updateJobStatus(jobs[i].getJobNumber(), BaseDAO.JOB_WAITING_STATUS);
        }
        
        // Get the waiting job
        jobVector = service.getWaitingJob(NUM_THREADS);
        assertEquals(jobs.length, jobVector.size());
        
        // As a side effect, the job status should have been changed to "processing"
        for(int i=0; i<jobs.length; i++) {
            JobInfo updatedJob = service.getJobInfo( jobs[i].getJobNumber() );
            assertEquals("Processing", updatedJob.getStatus());
        }
        
    }




    /**
     * @see AnalysisDataService#getParent(int)
     */
    public void testGetParent() throws Exception {
        int jobId = 45;
        int parentJobId = 44;
        JobInfo parent = service.getParent(jobId);
        assertEquals(parentJobId, parent.getJobNumber());

    }

    public void testUpdateStatus() throws Exception {
        int jobId = 45;
        int newStatusId = 2;

        JobInfo testJob = service.getJobInfo(jobId);
        String oldStatus = testJob.getStatus();

        service.updateJobStatus(jobId, newStatusId);

        JobInfo modifiedJob = service.getJobInfo(jobId);
        assertFalse(oldStatus.equals(modifiedJob.getStatus()));

    }

    /**
     * @see AnalysisDAO#updateJob(int,String, int)
     */
    public void testUpdateJob() {
        try {
            int jobNo = 1;
            int newJobStatus = 2;
            String newParameterInfo = service.getJobInfo(10).getParameterInfo();

            JobInfo jobInfo = service.getJobInfo(jobNo);
            Integer jobStatus = STATUS_IDS.get(jobInfo.getStatus());
            String parameterInfo = jobInfo.getParameterInfo();
            assertNotNull(jobStatus);
            assertFalse(jobStatus.intValue() == newJobStatus);

            service.updateJob(jobNo, newParameterInfo, newJobStatus);
            JobInfo modifiedJobInfo = service.getJobInfo(jobNo);
            Integer modifiedJobStatus = STATUS_IDS.get(modifiedJobInfo.getStatus());
            assertNotNull(modifiedJobStatus);
            assertEquals(newJobStatus, modifiedJobStatus.intValue());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * @see AnalysisDAO#recordClientJob(int taskID, String user_id,
     *      String parameter_info, int parentJobNumber)
     * @throws Exception
     */
    public void testRecordClientJob() throws Exception {
        int jobId = -1;
        int taskId = 1;
        String userId = "twomey";
        String parameterInfo = service.getJobInfo(1).getParameterInfo();

        JobInfo newJob = service.recordClientJob(taskId, userId, parameterInfo);
        assertNotNull(newJob);
        jobId = newJob.getJobNumber();

    }

    /**
     * @see AnalysisDataService#getNextLSIDIdentifier(String)
     */
    public void testGetNextLSIDIdentifier() throws Exception {
        service.getNextLSIDIdentifier(IGPConstants.TASK_NAMESPACE);
        service.getNextLSIDIdentifier(IGPConstants.SUITE_NAMESPACE);
    }

}

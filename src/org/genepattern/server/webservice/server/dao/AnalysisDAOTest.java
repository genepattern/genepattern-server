package org.genepattern.server.webservice.server.dao;

import java.sql.ResultSet;
import java.util.*;

import org.genepattern.server.util.HibernateUtil;
import org.genepattern.util.GPConstants;
import org.genepattern.util.IGPConstants;
import org.genepattern.webservice.*;
import org.hibernate.Transaction;

import junit.framework.TestCase;

public class AnalysisDAOTest extends DAOTestCase {

    AnalysisDAO dao = new AnalysisDAO();

    private static Map<String, Integer> STATUS_IDS = new HashMap();
    static {
        STATUS_IDS.put("Pending", 1);
        STATUS_IDS.put("Processing", 2);
        STATUS_IDS.put("Finished", 3);
        STATUS_IDS.put("Error", 4);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.out.println("start");
        HibernateUtil.getSession().beginTransaction();
        System.out.println(HibernateUtil.getSession().getTransaction().isActive());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.out.println("rollback");
        System.out.println(HibernateUtil.getSession().getTransaction().isActive());
        HibernateUtil.getSession().getTransaction().rollback();
    }

    /**
     * 
     * @throws Exception
     */
    public void testAddNewJob() throws Exception {
        JobInfo jobInfo = null;
        int taskId = 1;
        String userId = "twomey";
        String parameterInfo = dao.getJobInfo(1).getParameterInfo();

        int parentId = 1;
        try {
            jobInfo = dao.addNewJob(taskId, userId, parameterInfo, parentId);
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
        assertEquals(pipelineName, dao.getTemporaryPipelineName(jobNo));
    }

    /**
     * @see AnalaysisJobService#createTemporaryPipeline(String user_id, String
     *      parameter_info, String pipelineName, String lsid)
     */
    public void testCreateTemporaryPipeline() throws Exception {
        JobInfo newJob = null;
        String userId = "twomey";
        String parameterInfo = dao.getJobInfo(1).getParameterInfo();
        String pipelineName = "temp";
        String lsid = "test_" + System.currentTimeMillis();

        Integer jobNo = dao.addNewJob(BaseDAO.UNPROCESSABLE_TASKID, userId, parameterInfo, pipelineName,
                null, lsid);
        newJob = dao.getJobInfo(jobNo);

         
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
     * @see AnalaysisJobService#getJobInfo(Date)
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

        JobInfo[] jobs = dao.getJobInfo(completionDate);
        assertEquals(186, jobs.length);
    }

    /**
     * 
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
        JobInfo[] children = dao.getChildren(parentJobId);
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
     * 
     */
    public void testSetJobDeleted() throws Exception {

        int jobCount = dao.getJobs("twomey", 10, 10000000, false).length;

        int jobNo = 1;
        JobInfo jobInfo = dao.getJobInfo(jobNo);
        assertNotNull(jobInfo);

        dao.setJobDeleted(jobNo, true);

        int newJobCount = dao.getJobs("twomey", 10, 10000000, false).length;
        assertEquals(jobCount - 1, newJobCount);

    }
    
    /**
     * 
     * @throws Exception
     */
    public void testResetPreviouslyRunningJobs() throws Exception {
        dao.resetPreviouslyRunningJobs();
    }
    

    /**
     * 
     * @throws Exception
     */
    public void testExecuteSql() throws Exception {
        String sql = "select count(*) from job_status";
        ResultSet rs = dao.executeSQL(sql);
        if (rs.next()) {
            assertEquals(4, rs.getInt(1));
        }
        else {
            fail("No rows returned");
        }

    }
    
    public void testExecuteUpdate() throws Exception {
        String sql = "CHECKPOINT";
        dao.executeUpdate(sql);
    }

    /**
     * 
     */
    public void testGetJobsByUser() throws Exception {
        int jobCount = dao.getJobs("twomey", 10, 10000000, false).length;
        assertEquals(10, jobCount);

        jobCount = dao.getJobs("twomey", 10, 5, false).length;
        assertEquals(5, jobCount);

        jobCount = dao.getJobs(null, -1, 5, false).length;
        assertEquals(5, jobCount);
    }

    /**
     * 
     */
    public void testGetParent() throws Exception {
        int jobId = 45;
        int parentJobId = 44;
        JobInfo parent = dao.getParent(jobId);
        assertEquals(parentJobId, parent.getJobNumber());

    }

    public void testUpdateStatus() throws Exception {
        int jobId = 45;
        int newStatusId = 2;

        JobInfo testJob = dao.getJobInfo(jobId);
        String oldStatus = testJob.getStatus();

        dao.updateJobStatus(jobId, newStatusId);

        JobInfo modifiedJob = dao.getJobInfo(jobId);
        assertFalse(oldStatus.equals(modifiedJob.getStatus()));

    }

    /**
     * @see AnalysisHypersonicDAO#updateJob(int,String, int)
     */
    public void testUpdateJob() {
        try {
            int jobNo = 1;
            int newJobStatus = 2;
            String newParameterInfo = dao.getJobInfo(10).getParameterInfo();

            JobInfo jobInfo = dao.getJobInfo(jobNo);
            Integer jobStatus = STATUS_IDS.get(jobInfo.getStatus());
            String parameterInfo = jobInfo.getParameterInfo();
            assertNotNull(jobStatus);
            assertFalse(jobStatus.intValue() == newJobStatus);

            dao.updateJob(jobNo, newParameterInfo, newJobStatus);
            JobInfo modifiedJobInfo = dao.getJobInfo(jobNo);
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
     * 
     *      int taskID String taskDescription String parameter_info String
     *      taskInfoAttributes String user_id int access_id
     */
    public void testUpdateTask() throws Exception {

        try {
            TaskInfo task = (new AdminDAO()).getTask(1);
            TaskInfoAttributes tia = new TaskInfoAttributes();

            dao.updateTask(task.getID(), null, null, null, null, task.getAccessId());
            dao.updateTask(task.getID(), task.getDescription(), task.getParameterInfo(), task
                    .giveTaskInfoAttributes().encode(), task.getUserId(), task.getAccessId());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());

        }

    }


}

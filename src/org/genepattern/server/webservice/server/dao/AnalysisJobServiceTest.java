package org.genepattern.server.webservice.server.dao;

import java.sql.ResultSet;
import java.util.*;

import org.genepattern.util.GPConstants;
import org.genepattern.util.IGPConstants;
import org.genepattern.webservice.*;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.OmnigeneException;
import org.hibernate.Transaction;

import junit.framework.TestCase;

public class AnalysisJobServiceTest extends TestCase {

    AnalysisJobService service = AnalysisJobService.getInstance();

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

        JobInfo[] jobs = service.getJobInfo(completionDate);
        assertEquals(186, jobs.length);
    }

    /**
     * @see AnalysisJobService#getChildren(int) *
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
     * @see AnalysisJobService#setJobDeleted(int,boolean)
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
     * @see AnalysisJobService#getJobs(String,int,int,boolean)
     */
    public void testGetJobsByUser() throws Exception {
        int jobCount = service.getJobs("twomey", 10, 10000000, false).length;
        assertEquals(10, jobCount);

        jobCount = service.getJobs("twomey", 10, 5, false).length;
        assertEquals(5, jobCount);

        jobCount = service.getJobs(null, -1, 5, false).length;
        assertEquals(5, jobCount);
    }

    /**
     * @see AnalysisJobService#getParent(int)
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
     * @see AnalysisHypersonicDAO#updateJob(int,String, int)
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
     * @see AnalysisJobService#getNextLSIDIdentifier(String)
     */
    public void testGetNextLSIDIdentifier() throws Exception {
        service.getNextLSIDIdentifier(IGPConstants.TASK_NAMESPACE);
        service.getNextLSIDIdentifier(IGPConstants.SUITE_NAMESPACE);
    }


    


    public void testGetTasks() {
        String user = "twomey";
        List tasks = service.getTasks(user);
        this.assertTrue(tasks.size() > 0);
    }

    /**
     * @see AnalysisHypersonicDAO#addNewTask(String,String,int,String,String,String)
     */
    public void testAddNewTask() throws Exception {

        TaskInfo task = service.getTask(1); // Get a task to copy
        TaskInfoAttributes tia = getTestTaskInfoAttributes();
        String taskName = "test";
        int newTaskId = service.addNewTask(taskName, task.getUserId(), task.getAccessId(), task.getDescription(), task
                .getParameterInfo(), tia.encode());

        TaskInfo newTask = service.getTask(newTaskId);
        assertNotNull(newTask);
        assertEquals(newTaskId, newTask.getID());
        assertEquals(taskName, newTask.getName());
        assertEquals(task.getUserId(), newTask.getUserId());
        assertEquals(task.getDescription(), newTask.getDescription());
        assertEquals(task.getParameterInfo(), newTask.getParameterInfo());

        for (Object key : tia.keySet()) {
            assertEquals(tia.get(key), newTask.getTaskInfoAttributes().get(key));
        }

    }

    /**
     * @see AnalysisJobService#updateTask(int,String,String,String,String,int)
     *      int taskID String taskDescription String parameter_info String
     *      taskInfoAttributes String user_id int access_id
     */
    public void testUpdateTask() throws Exception {

        try {
            TaskInfo task = service.getTask(1);
            TaskInfoAttributes tia = new TaskInfoAttributes();

            service.updateTask(task.getID(), null, null, null, null, task.getAccessId());
            service.updateTask(task.getID(), task.getDescription(), task.getParameterInfo(), task
                    .giveTaskInfoAttributes().encode(), task.getUserId(), task.getAccessId());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());

        }

    }

    /**
     * @see AnalysisJobService#updateTask(int,String,String,String,int)
     *      updateTask(int taskID, String parameter_info, String
     *      taskInfoAttributes, String user_id, int access_id)
     */
    public void testUpdateTask1() throws Exception {
        TaskInfo task = service.getTask(1);
        TaskInfoAttributes tia = getTestTaskInfoAttributes();

        service.updateTask(task.getID(), null, null, null, task.getAccessId());

        service.updateTask(task.getID(), task.getParameterInfo(), tia.encode(), task.getUserId(), task.getAccessId());
    }


    /**
     * 
     * @throws Exception
     */
    public void testDeleteTask() throws Exception {

        int taskId = 1;
        TaskInfo task = service.getTask(taskId);
        assertNotNull(task);

        int count = service.getTasks("twomey").size();
        service.deleteTask(taskId);
        int postCount = service.getTasks("twomey").size();
        assertEquals(count - 1, postCount);
    }

    private TaskInfoAttributes getTestTaskInfoAttributes() {

        TaskInfoAttributes tia = new TaskInfoAttributes();
        tia.put(GPConstants.TASK_TYPE, GPConstants.TASK_TYPE_PIPELINE);
        tia.put(GPConstants.AUTHOR, "author");
        tia.put(GPConstants.USERID, "twomey");
        tia.put(GPConstants.PRIVACY, GPConstants.PUBLIC);
        tia.put(GPConstants.QUALITY, GPConstants.QUALITY_DEVELOPMENT);
        tia.put(GPConstants.LSID, "urn:lsid:8080:genepatt.18.103.8.161:genepattermodules:5.1");
        return tia;
    }

}

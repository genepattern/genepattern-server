/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2008) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.
 
 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server.dao;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

public class AnalysisDAOTest extends DAOTestCase {

    AnalysisDAO dao = new AnalysisDAO();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.out.println("start");
        HibernateUtil.beginTransaction();
        System.out.println(HibernateUtil.getSession().getTransaction().isActive());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.out.println("rollback");
        System.out.println(HibernateUtil.getSession().getTransaction().isActive());
        HibernateUtil.rollbackTransaction();
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
        assertEquals("bweir@broadinstitute.org", children[0].getUserId());
        assertEquals("urn:lsid:8080.genepatt.18.103.8.161:genepatternmodules:1:14.6", children[0].getTaskLSID());

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

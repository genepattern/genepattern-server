/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server.dao;

import static org.junit.Assert.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AnalysisDAOTest {
    private static Map<String, Integer> STATUS_IDS = new HashMap<String, Integer>();
    static {
        STATUS_IDS.put("Pending", 1);
        STATUS_IDS.put("Processing", 2);
        STATUS_IDS.put("Finished", 3);
        STATUS_IDS.put("Error", 4);
    }

    private HibernateSessionManager mgr;
    private AnalysisDAO dao;
    private GpContext userContext;

    @Before
    public void setUp() throws Exception {
        mgr=DbUtil.getTestDbSession();
        mgr.beginTransaction();
        dao = new AnalysisDAO(mgr);
        userContext=new GpContext.Builder().userId("test_user").build();
        

    }

    @After
    public void tearDown() throws Exception {
        mgr.rollbackTransaction();
    }

    @Test
    public void testGetChildrenAndParent() throws Exception {
        int parentJobId=AnalysisJobUtil.addJobToDb(mgr, userContext, -1);
        int childJobId_01=AnalysisJobUtil.addJobToDb(mgr, userContext, parentJobId);
        int childJobId_02=AnalysisJobUtil.addJobToDb(mgr, userContext, parentJobId);
        
        JobInfo[] children = dao.getChildren(parentJobId);
        assertEquals(2, children.length);
        assertEquals(childJobId_01, children[0].getJobNumber());

        assertEquals(childJobId_01, children[0].getJobNumber());
        assertEquals(childJobId_02, children[1].getJobNumber());
        
        
        assertEquals("getParent(parentJobId)", null, dao.getParent(parentJobId));
        
        JobInfo parent = dao.getParent(childJobId_01);
        assertEquals(parentJobId, parent.getJobNumber());

        
        AnalysisJobUtil.deleteJobFromDb(mgr, childJobId_01);
        AnalysisJobUtil.deleteJobFromDb(mgr, childJobId_02);
        AnalysisJobUtil.deleteJobFromDb(mgr, parentJobId);
    }

    @Test
    public void testExecuteSql() throws SQLException //{
    {
        String sql = "select count(*) from job_status";
        ResultSet rs = dao.executeSQL(sql);
        if (rs.next()) {
            assertEquals(6, rs.getInt(1));
        }
        else {
            fail("No rows returned");
        }

    }
    
    @Test
    public void testExecuteUpdate() throws SQLException {
        String sql = "CHECKPOINT";
        dao.executeUpdate(sql);
    }

    //TODO: fix me @Test
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
    //TODO: fix me @Test
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
            Assert.fail(e.getMessage());
        }
    }
    
    /**
     * 
     *      int taskID String taskDescription String parameter_info String
     *      taskInfoAttributes String user_id int access_id
     */
    // TODO: fix me @Test
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
            Assert.fail(e.getMessage());

        }
    }

}

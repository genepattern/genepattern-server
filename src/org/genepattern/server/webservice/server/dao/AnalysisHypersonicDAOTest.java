package org.genepattern.server.webservice.server.dao;

import java.sql.ResultSet;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import junit.framework.TestCase;

import org.genepattern.server.webservice.server.dao.AnalysisHypersonicDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.util.IGPConstants;
import org.genepattern.webservice.*;

public class AnalysisHypersonicDAOTest extends DaoTestCase {

    private AnalysisHypersonicDAO dao;

    private Map<String, Integer> statusIds = new HashMap();

    protected void setUp() throws Exception {
        dao = new AnalysisHypersonicDAO();

        statusIds.put("Pending", 1);
        statusIds.put("Processing", 2);
        statusIds.put("Finished", 3);
        statusIds.put("Error", 4);
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    /**
     * @see AnalysisHypersonicDAO#getWaitingJobs(int)
     */
    public void testGetWaitingJobs() throws Exception {
        int NUM_THREADS = 20;
        Vector jobVector = null;
        jobVector = dao.getWaitingJob(NUM_THREADS);
        assertEquals(0, jobVector.size());
    }



    /**
     * @see AnalysisHypersonicDAO#recordClientJob(int taskID, String user_id,
     *      String parameter_info, int parentJobNumber)
     * @throws Exception
     */
    public void testRecordClientJob() throws Exception {
        int jobId = -1;
        int taskId = 1;
        String userId = "twomey";
        String parameterInfo = dao.getJobInfo(1).getParameterInfo();
        try {
            JobInfo newJob = dao.recordClientJob(taskId, userId, parameterInfo);
            assertNotNull(newJob);
            jobId = newJob.getJobNumber();
        }
        finally {
            dao.deleteJob(jobId);
        }

    }



    /**
     * @see AnalysisHypersonicDAO#getJobInfo(int)
     */
    public void testGetJobInfo() throws Exception {
        int jobNo = 1;
        int taskId = 24;

        JobInfo jobInfo = dao.getJobInfo(jobNo);
        assertEquals(taskId, jobInfo.getTaskID());

    }

    /**
     * @see AnalysisHypersonicDAO#getJobInfo(String)
     */
    public void testGetJobInfoByUser() throws Exception {
        String userId = "twomey";
        JobInfo[] jobs = dao.getJobInfo(userId);
        assertEquals(53, jobs.length);
        assertEquals(userId, jobs[0].getUserId());

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

package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.TaskInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * database integration test for the AnalysisDAO class.
 * @author pcarr
 *
 */
public class TestAnalysisDAO {
    private static String testUser;
    private static final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    private static final String cleZip="modules/ConvertLineEndings_v2.zip";
    private static File zipFile;
    private static TaskInfo taskInfo;
    
    private GpContext gpContext;
    private AnalysisJobUtil jobUtil;
    
    private List<Integer> jobs;
    
    /**
     * Add a job to the database, hard-code to be a ConvertLineEndings job
     * 
     * @param parentJobId, when this value is '-1' create a top-level job, otherwise expecting a valid jobId 
     *     treat the newly created job as a child step in a pipeline.
     * 
     * @return
     * @throws Exception
     */
    public int addJob(int parentJobId) throws Exception {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(cleLsid);
        jobInput.addValue("input.filename", 
                "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls");
        boolean initDefault=true;
        int jobNo=jobUtil.addJobToDb(gpContext, jobInput, parentJobId, initDefault);
        jobs.add(jobNo);
        return jobNo;
    }

    public void cleanupJobs() throws Exception {
        HibernateUtil.beginTransaction();
        try {
            for(int jobId : jobs) {
                jobUtil.deleteJobFromDb(jobId);
            }
            HibernateUtil.commitTransaction();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        DbUtil.initDb();
        testUser=DbUtil.addUserToDb("test");
        zipFile=FileUtil.getDataFile(cleZip);
        taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
    }
    
    @AfterClass
    static public void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
    
    @Before
    public void setUp() throws Exception {
        jobs=new ArrayList<Integer>();
        
        jobUtil=new AnalysisJobUtil();
        gpContext=new GpContextFactory.Builder()
            .userId(testUser)
            .taskInfo(taskInfo)
            .build();
        jobUtil=new AnalysisJobUtil();
        
        // add a job
        int jobNo=addJob(-1);
        
        // add a pipeline with two steps
        int parentJobNo=addJob(-1);
        addJob(parentJobNo);
        addJob(parentJobNo);
        
        // add a job, change it's status to ERR
        int errJob=addJob(-1);
        jobUtil.setStatusInDb(errJob, 4);
        
        // add a job, change it's status to FINISHED
        int finishedJob=addJob(-1);
        jobUtil.setStatusInDb(finishedJob, 3);
    }
    
    @After
    public void tearDown() throws Exception {
        cleanupJobs();
    }

    @Test
    public void numProcessingJobs() {
        try {
            AnalysisDAO dao=new AnalysisDAO();
            int numProcessingJobs=dao.getNumProcessingJobsByUser(testUser);
            Assert.assertEquals("numProcessingJobsByUser", 2, numProcessingJobs);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    @Test
    public void numProcessingJobs_afterDelete() throws Exception {
        try {
            int jobToDelete=addJob(-1);
            jobUtil.deleteJobFromDb(jobToDelete);
            HibernateUtil.beginTransaction();
            AnalysisDAO dao=new AnalysisDAO();
            int numProcessingJobs=dao.getNumProcessingJobsByUser(testUser);
            Assert.assertEquals("numProcessingJobsByUser", 2, numProcessingJobs);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }

}

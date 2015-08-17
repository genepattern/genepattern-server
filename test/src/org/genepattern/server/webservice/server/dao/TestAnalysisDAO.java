/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webservice.server.dao;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.job.tag.dao.JobTagDao;
import org.genepattern.server.tag.Tag;
import org.genepattern.server.webservice.server.Analysis;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * database integration test for the AnalysisDAO class.
 * @author pcarr
 *
 */
public class TestAnalysisDAO { 
    private static HibernateSessionManager mgr;
    private static String testUser;
    private static final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    private static final String cleZip="modules/ConvertLineEndings_v2.zip";
    private static File zipFile;
    private static TaskInfo taskInfo;
    
    private GpConfig gpConfig;
    private GpContext gpContext;
    //private AnalysisJobUtil jobUtil;

    @ClassRule
    public static TemporaryFolder temp = new TemporaryFolder();
    
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
        int jobNo=AnalysisJobUtil.addJobToDb(mgr, gpConfig, gpContext, jobInput, parentJobId, initDefault);
        jobs.add(jobNo);
        return jobNo;
    }

    public void cleanupJobs() {
        try {
        Map<Integer, Throwable> errors=new LinkedHashMap<Integer, Throwable>();
        for(final int jobId : jobs) {
            try {
                AnalysisJobUtil.deleteJobFromDb(mgr, jobId);
            }
            catch (Throwable t) {
                errors.put(jobId, t);
            }
        }
        if (errors.size() > 0) {
            Assert.fail("Error cleaning up jobs: "+errors);
        }
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        final String userDir=temp.newFolder("users").getAbsolutePath();
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, userDir)
        .build();
        
        //DbUtil.initDb();
        mgr=DbUtil.getTestDbSession();
        testUser=DbUtil.addUserToDb(gpConfig, mgr, "test");
        zipFile=FileUtil.getDataFile(cleZip);
        taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
    }
    
    @AfterClass
    static public void afterClass() throws Exception {
        //DbUtil.shutdownDb();
    }
    
    @Before
    public void setUp() throws Exception {
        jobs=new ArrayList<Integer>();
        
        //jobUtil=new AnalysisJobUtil();
        gpConfig=new GpConfig.Builder().build();
        gpContext=new GpContext.Builder()
            .userId(testUser)
            .taskInfo(taskInfo)
            .build();
        //jobUtil=new AnalysisJobUtil();
        
        // add a job
        int jobNo=addJob(-1);
        
        // add a pipeline with two steps
        int parentJobNo=addJob(-1);
        addJob(parentJobNo);
        addJob(parentJobNo);
        
        // add a job, change it's status to ERR
        int errJob=addJob(-1);
        AnalysisJobUtil.setStatusInDb(mgr, errJob, 4);
        
        // add a job, change it's status to FINISHED
        int finishedJob=addJob(-1);
        AnalysisJobUtil.setStatusInDb(mgr, finishedJob, 3);
    }
    
    @After
    public void tearDown() throws Exception {
        cleanupJobs();
    }

    @Test
    public void numProcessingJobs() {
        try {
            AnalysisDAO dao=new AnalysisDAO(mgr);
            int numProcessingJobs=dao.getNumProcessingJobsByUser(testUser);
            Assert.assertEquals("numProcessingJobsByUser", 2, numProcessingJobs);
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
    
    @Test
    public void numProcessingJobs_afterDelete() throws Exception {
        try {
            int jobToDelete=addJob(-1);
            AnalysisJobUtil.deleteJobFromDb(mgr, jobToDelete);
            mgr.beginTransaction();
            AnalysisDAO dao=new AnalysisDAO(mgr);
            int numProcessingJobs=dao.getNumProcessingJobsByUser(testUser);
            Assert.assertEquals("numProcessingJobsByUser", 2, numProcessingJobs);
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    @Test
    public void testSearchJobTag()throws Exception
    {
        try {
            int jobToSearch=addJob(-1);
            mgr.beginTransaction();

            int pageNum=1;
            int pageSize=10;
            Analysis.JobSortOrder jobSortOrder = Analysis.JobSortOrder.JOB_NUMBER;
            boolean ascending = true;
            String tagText = "TestSearch";

            Date date = new Date();

            Tag tag = new Tag();
            tag.setUserId(testUser);
            tag.setTag(tagText);
            tag.setDateAdded(date);
            tag.setPublicTag(false);

            JobTag jobTag = new JobTag();
            jobTag.setUserId(testUser);
            jobTag.setTagObj(tag);

            AnalysisJob analysisJob = new AnalysisJob();
            analysisJob.setJobNo(jobToSearch);
            analysisJob.setParent(-1);
            jobTag.setAnalysisJob(analysisJob);

            jobTag.setDateTagged(date);

            //add a tag
            JobTagDao jobTagDao = new JobTagDao(mgr);
            boolean success = jobTagDao.insertJobTag(jobTag);
            Assert.assertTrue("jobTagSearch insert", success);

            final AnalysisDAO dao=new AnalysisDAO(mgr);
            List<JobInfo> jobInfoList = dao.getPagedJobsWithTag(tagText, null, null, null, pageNum, pageSize,jobSortOrder, ascending);
            Assert.assertEquals("jobTagSearch", 1, jobInfoList.size());
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
}

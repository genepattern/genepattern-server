/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webservice.server.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.List;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;

/**
 * database integration test for the AnalysisDAO class.
 * @author pcarr
 *
 */
//@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAnalysisDAO { 
    private static final String userId="test_analysis_dao";
    
    private static HibernateSessionManager mgr;
    private static final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    private static final String cleZip="modules/ConvertLineEndings_v2.zip";
    private static File zipFile;
    private static TaskInfo taskInfo;
    
    private static GpConfig gpConfig;
    private GpContext jobContext;

    @ClassRule
    public static TemporaryFolder temp = new TemporaryFolder();
    
    @BeforeClass
    static public void beforeClass() throws Exception {
        mgr=DbUtil.getTestDbSession();
        final String userDir=temp.newFolder("users").getAbsolutePath();
        gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, userDir)
        .build();
        DbUtil.addUserToDb(gpConfig, mgr, "test_analysis_dao");

        zipFile=FileUtil.getDataFile(cleZip);
        taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
    }
    
    @Before
    public void setUp() {
        jobContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
        .build();
    }

    /**
     * Add a job to the database, hard-code to be a ConvertLineEndings job
     * 
     * @param parentJobId, when this value is '-1' create a top-level job, otherwise expecting a valid jobId 
     *     treat the newly created job as a child step in a pipeline.
     * 
     * @return
     * @throws Exception
     */
    private int addJob(final int parentJobId) throws Exception {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(cleLsid);
        jobInput.addValue("input.filename", 
                "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.cls");
        boolean initDefault=true;
        int jobNo=AnalysisJobUtil.addJobToDb(mgr, gpConfig, jobContext, jobInput, parentJobId, initDefault);
        return jobNo;
    }

    @Test
    public void addTask() {
        final String myLsid="urn:lsid:broad.mit.edu:cancer.software.gpdev.module.analysis:00001:1";
        AnalysisDAO analysisDao = new AnalysisDAO(mgr);
        final String taskName="test_analysis_dao_module";
        final int access_id=1; // public
        final String description="";
        final String parameter_info = ""; // empty string
        final String NL="\n";
        final String taskInfoAttributes="<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + NL +
                "<java version=\"1.0\" class=\"java.beans.XMLDecoder\">" + NL +
                "<object class=\"org.genepattern.webservice.TaskInfoAttributes\">" + NL +
                "<void property=\"LSID\">" + NL +
                "<object>"+myLsid+"</object>" + NL +
                "</void>" + NL +
                "</object>" + NL +
                "</java>";        
        analysisDao.addNewTask(taskName, userId, access_id, description, parameter_info, taskInfoAttributes);
        mgr.commitTransaction();
    }
    
    @Test
    public void numProcessingJobsByUser() throws Exception {
        try {
            AnalysisDAO dao=new AnalysisDAO(mgr);
            int numProcessingJobs=dao.getNumProcessingJobsByUser(userId);
            assertEquals("numProcessingJobsByUser, before", 0, numProcessingJobs);
            // add a job
            int jobNo=addJob(-1);

            // add a pipeline with two steps
            int parentJobNo=addJob(-1);
            int child01=addJob(parentJobNo);
            int child02=addJob(parentJobNo);

            // add a job, change it's status to ERR
            int errJob=addJob(-1);
            AnalysisJobUtil.setStatusInDb(mgr, errJob, 4);

            // add a job, change it's status to FINISHED
            int finishedJob=addJob(-1);
            AnalysisJobUtil.setStatusInDb(mgr, finishedJob, 3);
            //mgr.commitTransaction();
            numProcessingJobs=dao.getNumProcessingJobsByUser(userId);
            Assert.assertEquals("numProcessingJobsByUser", 2, numProcessingJobs);
            
            AnalysisJobUtil.deleteJobFromDb(mgr, child02);
            AnalysisJobUtil.deleteJobFromDb(mgr, child01);
            AnalysisJobUtil.deleteJobFromDb(mgr, parentJobNo);
            assertEquals("numProcessingJobsByUser, after delete", 1, dao.getNumProcessingJobsByUser(userId));
            AnalysisJobUtil.deleteJobFromDb(mgr, errJob);
            AnalysisJobUtil.deleteJobFromDb(mgr, finishedJob);
            AnalysisJobUtil.deleteJobFromDb(mgr, jobNo);
            assertEquals("numProcessingJobsByUser, after delete 2", 0, dao.getNumProcessingJobsByUser(userId));
        }
        finally {
            mgr.closeCurrentSession();
        }
    }

    @Test
    public void pagedJobsWithTag() throws Exception {
        final String tagText = "TestSearch";
        try {
            int jobToSearch=addJob(-1);
            final Date date = new Date();

            Tag tag = new Tag();
            tag.setUserId(userId);
            tag.setTag(tagText);
            tag.setDateAdded(date);
            tag.setPublicTag(false);

            JobTag jobTag = new JobTag();
            jobTag.setUserId(userId);
            jobTag.setTagObj(tag);

            AnalysisJob analysisJob = new AnalysisJob();
            analysisJob.setJobNo(jobToSearch);
            analysisJob.setParent(-1);
            jobTag.setAnalysisJob(analysisJob);

            jobTag.setDateTagged(date);

            //add a tag
            JobTagDao jobTagDao = new JobTagDao(mgr);
            boolean success = jobTagDao.insertJobTag(jobTag);
            assertTrue("jobTagSearch insert", success);

            final AnalysisDAO dao=new AnalysisDAO(mgr);
            final int pageNum=1;
            final int pageSize=10;
            final Analysis.JobSortOrder jobSortOrder = Analysis.JobSortOrder.JOB_NUMBER;
            final boolean ascending = true;
            List<JobInfo> jobInfoList = dao.getPagedJobsWithTag(tagText, null, null, null, pageNum, pageSize, jobSortOrder, ascending);
            assertEquals("jobTagSearch", 1, jobInfoList.size());
            
            AnalysisJobUtil.deleteJobFromDb(mgr, jobToSearch);
        }
        finally {
            mgr.closeCurrentSession();
        }
    }
    
}

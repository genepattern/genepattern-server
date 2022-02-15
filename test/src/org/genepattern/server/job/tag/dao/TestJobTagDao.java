/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.tag.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.Demo;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.tag.Tag;
import org.genepattern.webservice.JobInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Created by nazaire on 10/8/14.
 */
public class TestJobTagDao
{
    @ClassRule
    public static TemporaryFolder temp = new TemporaryFolder();

    private static HibernateSessionManager mgr;
    private static GpConfig gpConfig;
    
    private int gpJobNo;
    private JobInfo jobInfo;
    private JobTagDao jobTagDao;
    
    @BeforeClass
    public static void beforeClass() throws IOException, DbException, ExecutionException {
        final String userDir=new File(temp.newFolder(), "users").getAbsolutePath();
        gpConfig=new GpConfig.Builder()
            .webappDir(new File("website"))
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, userDir)
        .build();
        mgr=DbUtil.getTestDbSession();
        DbUtil.addUserToDb(gpConfig, mgr, Demo.testUserId);
        DbUtil.addUserToDb(gpConfig, mgr, Demo.adminUserId);
    }

    @Before
    public void setUp() throws Exception 
    {
        // because of FK relation, must add an entry to the analysis_job table
        gpJobNo=AnalysisJobUtil.addJobToDb(mgr);
        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(gpJobNo);
        jobTagDao = new JobTagDao(mgr);
    }

    @After
    public void tearDown() throws DbException 
    {
        AnalysisJobUtil.deleteJobFromDb(mgr, gpJobNo);
    }

    /**
     * Test selecting a record in the job_tag table
     *
     * @throws org.genepattern.server.DbException
     */
    @Test
    public void selectJobTags() throws DbException
    {
        String tagText1 = "insert jobtag 1";
        Date date = new Date();

        Tag tag = new Tag();
        tag.setUserId(Demo.testUserId);
        tag.setTag(tagText1);
        tag.setDateAdded(date);
        tag.setPublicTag(false);

        JobTag jobTag = new JobTag();
        jobTag.setUserId(Demo.testUserId);
        jobTag.setTagObj(tag);

        AnalysisJob analysisJob = new AnalysisJob();
        analysisJob.setJobNo(gpJobNo);
        jobTag.setAnalysisJob(analysisJob);

        jobTag.setDateTagged(date);
        boolean success = jobTagDao.insertJobTag(jobTag);
        assertTrue("insert success", success);

        String tagText2 = "insert jobtag 2";
        Date date2 = new Date();

        Tag tag2 = new Tag();
        tag2.setUserId(Demo.adminUserId);
        tag2.setTag(tagText2);
        tag2.setDateAdded(date2);
        tag2.setPublicTag(false);

        JobTag jobTag2 = new JobTag();
        jobTag2.setUserId(Demo.adminUserId);
        jobTag2.setTagObj(tag2);

        AnalysisJob analysisJob2 = new AnalysisJob();
        analysisJob2.setJobNo(gpJobNo);
        jobTag2.setAnalysisJob(analysisJob2);

        jobTag2.setDateTagged(date2);

        jobTagDao.insertJobTag(jobTag2);

        List<JobTag> jobTagList = jobTagDao.selectJobTags(gpJobNo);
        assertEquals("num tags", 2, jobTagList.size());
    }

    /**
     * Test inserting and deleting a record in the job_tag table
     *
     */
    @Test
    public void insertAndDeleteTag()
    {
        String tagText = "insert and delete jobtag";
        Date date = new Date();

        Tag tag = new Tag();
        tag.setUserId(Demo.testUserId);
        tag.setTag(tagText);
        tag.setDateAdded(date);
        tag.setPublicTag(false);

        JobTag jobTag = new JobTag();
        jobTag.setUserId(Demo.testUserId);
        jobTag.setTagObj(tag);

        AnalysisJob analysisJob = new AnalysisJob();
        analysisJob.setJobNo(gpJobNo);
        jobTag.setAnalysisJob(analysisJob);

        jobTag.setDateTagged(date);
        jobTag.setTagObj(tag);

        //add a tag
        boolean success = jobTagDao.insertJobTag(jobTag);
        assertTrue("insert success", success);

        List<JobTag> jobTagList = jobTagDao.selectJobTags(gpJobNo);
        assertEquals("num tags", 1, jobTagList.size());
        assertEquals("job tag id", jobTag.getId(), jobTagList.get(0).getId());
        assertEquals("tag id", jobTag.getTagObj().getId(), jobTagList.get(0).getTagObj().getId());
        assertEquals("tag text", jobTag.getTagObj().getTag(), jobTagList.get(0).getTagObj().getTag());

        //now retrieve the tag
        JobTag tagResult = jobTagDao.selectJobTagById(jobTag.getId());
        assertNotNull(tagResult);
        assertEquals("tag text", jobTag.getTagObj().getTag(), tagResult.getTagObj().getTag());

        //now delete the tag
        success = jobTagDao.deleteJobTag(jobTag.getId());
        assertTrue("success", success);

        //verify tag is not still in the database
        //by attempting to retrieve the tag
        tagResult = jobTagDao.selectJobTagById(jobTag.getId());
        assertNull(tagResult);
    }
}

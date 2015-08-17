/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.job.comment.dao.JobCommentDao;
import org.genepattern.webservice.JobInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Created by nazaire on 9/30/14.
 */
public class TestJobCommentDao
{
    private HibernateSessionManager mgr;
    private GpConfig gpConfig;
    private JobCommentDao dao;

    private int gpJobNo;
    private JobInfo jobInfo;

    private String userId;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setUp() throws Exception
    {
        //DbUtil.initDb();
        mgr=DbUtil.getTestDbSession();
        final String userDir=temp.newFolder("users").getAbsolutePath();
        gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, userDir)
        .build();
        userId=DbUtil.addUserToDb(gpConfig, mgr, "test");

        // because of FK relation, must add an entry to the analysis_job table
        gpJobNo=new AnalysisJobUtil().addJobToDb(mgr);
        //jobDir = temp.newFolder("jobResults");
        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(gpJobNo);

        //jobContext=new GpContext.Builder()
        //        .jobInfo(jobInfo)
        //       .build();
        dao = new JobCommentDao(mgr);
    }

    @After
    public void tearDown()
    {
        AnalysisJobUtil.deleteJobFromDb(mgr, gpJobNo);
    }

    @AfterClass
    static public void afterClass() throws Exception {
        //DbUtil.shutdownDb();
    }

    /**
     * Test saving a record to the job_comment table
     *
     * @throws org.genepattern.server.DbException
     */
    @Test
    public void insertComment() throws DbException
    {
        String comment = "This is a test of inserting a comment";
        Date postedDate = new Date();
        int parentId = 0;

        JobComment jobComment = new JobComment();

        AnalysisJob analysisJob = new AnalysisJob();
        analysisJob.setJobNo(gpJobNo);
        analysisJob.setParent(-1);
        jobComment.setAnalysisJob(analysisJob);

        jobComment.setParentId(parentId);
        jobComment.setUserId(userId);
        jobComment.setComment(comment);

        dao.insertJobComment(jobComment);

        List<JobComment> query=dao.selectJobComments(gpJobNo);
        JobComment result = query.get(0);
        assertEquals("gpJobNo", gpJobNo, result.getAnalysisJob().getJobNo().intValue());
        assertEquals("parentId", parentId, result.getParentId());
        assertTrue("posted date is "+postedDate, Math.abs(postedDate.getTime() - result.getPostedDate().getTime()) < 100L);
        assertEquals("userId", userId, result.getUserId());
        assertEquals("comment", comment, result.getComment());
    }

    /**
     * Test saving a record to the job_comment table
     *
     * @throws org.genepattern.server.DbException
     */
    @Test
    public void updateComment() throws DbException
    {
        String comment = "This is a test of updating a comment";
        Date postedDate = new Date();
        int parentId = 0;

        JobComment jobComment = new JobComment();

        AnalysisJob analysisJob = new AnalysisJob();
        analysisJob.setJobNo(gpJobNo);
        analysisJob.setParent(-1);
        jobComment.setAnalysisJob(analysisJob);

        jobComment.setParentId(parentId);
        jobComment.setUserId(userId);
        jobComment.setComment(comment);

        dao.insertJobComment(jobComment);
        jobComment.setComment("This comment has been updated");

        boolean success = dao.updateJobComment(jobComment);
        assertEquals("success", true, success);

        List<JobComment> updatedQuery=dao.selectJobComments(gpJobNo);
        JobComment updatedResult = updatedQuery.get(0);
        assertEquals("comment", jobComment.getComment(), updatedResult.getComment());
    }

    /**
     * Test saving a record to the job_comment table
     *
     * @throws org.genepattern.server.DbException
     */
    @Test
    public void deleteComment() throws DbException
    {
        String comment = "This is a test of deleting a comment";
        Date postedDate = new Date();
        int parentId = 0;

        JobComment jobComment = new JobComment();

        AnalysisJob analysisJob = new AnalysisJob();
        analysisJob.setJobNo(gpJobNo);
        analysisJob.setParent(-1);
        jobComment.setAnalysisJob(analysisJob);

        jobComment.setParentId(parentId);
        jobComment.setUserId(userId);
        jobComment.setComment(comment);

        //add a comment
        dao.insertJobComment(jobComment);

        //now retrieve the comment
        List<JobComment> query=dao.selectJobComments(gpJobNo);
        JobComment result = query.get(0);
        assertEquals("comment", comment, result.getComment());

        //now delete the comment
        boolean success = dao.deleteJobComment(result.getId());
        assertEquals("success", true, success);

        //verify comment is not still in the database
        query=dao.selectJobComments(gpJobNo);
        assertEquals("count", 0, query.size());
    }
}

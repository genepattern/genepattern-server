package org.genepattern.server.job.comment;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.job.comment.dao.JobCommentDao;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.JobInfo;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by nazaire on 9/30/14.
 */
public class TestJobCommentDao
{
    private JobCommentDao dao;

    private int gpJobNo;
    private File jobDir;
    private GpContext jobContext;
    private JobInfo jobInfo;
    private final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";

    private String[] commandLine=new String[]{ "echo", "Hello, World!" };

    static String user;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setUp() throws Exception
    {
        DbUtil.initDb();
        user=DbUtil.addUserToDb("test");

        // because of FK relation, must add an entry to the analysis_job table
        gpJobNo=new AnalysisJobUtil().addJobToDb();
        jobDir = temp.newFolder("jobResults");
        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(gpJobNo);

        jobContext=new GpContext.Builder()
                .jobInfo(jobInfo)
                .build();
        dao = new JobCommentDao();
    }

    @After
    public void tearDown()
    {
        new AnalysisJobUtil().deleteJobFromDb(gpJobNo);
    }

    @AfterClass
    static public void afterClass() throws Exception {
        DbUtil.shutdownDb();
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
        jobComment.setUserId(user);
        jobComment.setComment(comment);

        dao.insertJobComment(jobComment);

        List<JobComment> query=dao.selectJobComments(gpJobNo);
        JobComment result = query.get(0);
        assertEquals("gpJobNo", gpJobNo, result.getAnalysisJob().getJobNo().intValue());
        assertEquals("parentId", parentId, result.getParentId());
        assertEquals("posted date", postedDate, result.getPostedDate());
        assertEquals("userId", user, result.getUserId());
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
        jobComment.setUserId(user);
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
        jobComment.setUserId(user);
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

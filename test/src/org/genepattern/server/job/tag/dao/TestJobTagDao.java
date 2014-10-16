package org.genepattern.server.job.tag.dao;

import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.tag.Tag;
import org.genepattern.webservice.JobInfo;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by nazaire on 10/8/14.
 */
public class TestJobTagDao
{
    private JobTagDao jobTagDao;

    private int gpJobNo;
    private File jobDir;
    private GpContext jobContext;
    private JobInfo jobInfo;

    static String user;
    static String admin;

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Before
    public void setUp() throws Exception
    {
        DbUtil.initDb();
        user=DbUtil.addUserToDb("test");
        admin=DbUtil.addUserToDb("admin");

        // because of FK relation, must add an entry to the analysis_job table
        gpJobNo=new AnalysisJobUtil().addJobToDb();
        jobDir = temp.newFolder("jobResults");
        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(gpJobNo);

        jobContext=new GpContext.Builder()
                .jobInfo(jobInfo)
                .build();
        jobTagDao = new JobTagDao();
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
        tag.setUserId(user);
        tag.setTag(tagText1);
        tag.setDateAdded(date);
        tag.setPublicTag(false);

        JobTag jobTag = new JobTag();
        jobTag.setUserId(user);
        jobTag.setTagObj(tag);
        jobTag.setGpJobNo(gpJobNo);
        jobTag.setDateTagged(date);
        jobTagDao.insertJobTag(jobTag);

        String tagText2 = "insert jobtag 2";
        Date date2 = new Date();

        Tag tag2 = new Tag();
        tag2.setUserId(admin);
        tag2.setTag(tagText2);
        tag2.setDateAdded(date2);
        tag2.setPublicTag(false);

        JobTag jobTag2 = new JobTag();
        jobTag2.setUserId(admin);
        jobTag2.setTagObj(tag2);
        jobTag2.setGpJobNo(gpJobNo);
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
        tag.setUserId(user);
        tag.setTag(tagText);
        tag.setDateAdded(date);
        tag.setPublicTag(false);

        JobTag jobTag = new JobTag();
        jobTag.setUserId(user);
        jobTag.setTagObj(tag);
        jobTag.setGpJobNo(gpJobNo);
        jobTag.setDateTagged(date);
        jobTag.setTagObj(tag);

        //add a tag
        jobTagDao.insertJobTag(jobTag);

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
        boolean success = jobTagDao.deleteJobTag(jobTag.getId());
        assertTrue("success", success);

        //verify tag is not still in the database
        //by attempting to retrieve the tag
        tagResult = jobTagDao.selectJobTagById(jobTag.getId());
        assertNull(tagResult);
    }
}

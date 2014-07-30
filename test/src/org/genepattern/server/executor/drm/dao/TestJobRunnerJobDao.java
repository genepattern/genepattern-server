package org.genepattern.server.executor.drm.dao;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.impl.lsf.core.CmdLineLsfRunner;
import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.JobInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test cases for initializing and CRUD operations on the JobRunnerJob class.
 * @author pcarr
 *
 */
public class TestJobRunnerJobDao {
    private JobRunnerJobDao dao;
    
    private Integer gpJobNo;
    private File jobDir;
    private GpContext jobContext;
    private JobInfo jobInfo;
    private final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";

    private String[] commandLine=new String[]{ "echo", "Hello, World!" };
    
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
        // because of FK relation, must add an entry to the analysis_job table
        gpJobNo=new AnalysisJobUtil().addJobToDb();
        jobDir = temp.newFolder("jobResults");
        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(gpJobNo);
        when(jobInfo.getTaskLSID()).thenReturn(cleLsid);
        
        jobContext=new GpContext.Builder()
            .jobInfo(jobInfo)
        .build();
        dao = new JobRunnerJobDao();
    }
    
    @After
    public void tearDown() {
        new AnalysisJobUtil().deleteJobFromDb(gpJobNo);
    }
    
    /**
     * Test saving record to job_runner_job table for a new job submissions, circa 3.9.0 release.
     * This is the way records are saved from the JobExecutor runCommand call.
     * 
     * @throws DbException
     */
    @Test
    public void insertFromDrmJobSubmission() throws DbException {
        final String jrClassname=CmdLineLsfRunner.class.getName();
        final String jrName="CmdLineLsfRunner";
        DrmJobSubmission.Builder builder=new DrmJobSubmission.Builder(jobDir)
            .jobContext(jobContext)
            .commandLine(commandLine)
            .stdoutFile(new File("stdout.txt"))
            .stderrFile(new File("stderr.txt"))
            .logFilename(".lsf.out");
        final DrmJobSubmission drmJobSubmission=builder.build();

        final JobRunnerJob jobRecord = new JobRunnerJob.Builder(jrClassname, drmJobSubmission).jobRunnerName(jrName).build();
        dao.insertJobRunnerJob(jobRecord);
        

        JobRunnerJob query=dao.selectJobRunnerJob(gpJobNo);
        assertEquals("gpJobNo", gpJobNo, query.getGpJobNo());
        assertEquals("lsid", cleLsid, query.getLsid());
    }

}

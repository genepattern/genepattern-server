package org.genepattern.server.job.status;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.genepattern.drm.DrmJobState;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.job.output.GpFileType;
import org.genepattern.server.job.output.JobOutputFile;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.genepattern.webservice.JobInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class TestJobStatusLoaderFromDb {
    private String gpUrl="http://127.0.0.1:8080/gp/";
    private GpContext jobContext;
    private final Integer gpJobNo=0;
    private final String userId="testUser";
    private File jobDir;
    private JobInfo jobInfo;

    
    JobRunnerJobDao jobRunnerDao;
    JobOutputDao jobOutputDao;
    
    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
        jobDir=FileUtil.getDataFile("jobResults/"+gpJobNo+"/");

        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(gpJobNo);
        jobContext=new GpContext.Builder()
            .userId(userId)
            .jobInfo(jobInfo)
        .build();
        
        jobRunnerDao=mock(JobRunnerJobDao.class);
        jobOutputDao=mock(JobOutputDao.class);
    }

    /**
     * Initialize Status when there is a valid JobInfo instance but no entry in the job_runner_job table,
     * jobInfo.status is PENDING
     */
    @Test
    public void fromJobInfo_nullJobRunnerJob_pending() throws DbException {
        when(jobRunnerDao.selectJobRunnerJob(gpJobNo)).thenThrow(new DbException());
        when(jobInfo.getStatus()).thenReturn(org.genepattern.server.domain.JobStatus.PENDING);
        
        JobStatusLoader loader=new JobStatusLoaderFromDb(gpUrl, jobRunnerDao, jobOutputDao);
        Status status = loader.loadJobStatus(jobContext);
        
        assertNotNull("expecting non-null status", status); 
        assertEquals("hasError", false, status.isHasError());
        assertEquals("isFinished", false, status.getIsFinished());
        assertEquals("isRunning", false, status.getIsRunning());
        assertEquals("isPending", true, status.getIsPending());
    }
    
    /**
     * Initialize Status when there is a valid JobInfo instance but no entry in the job_runner_job table,
     * jobInfo.status is PROCESSING
     */
    @Test
    public void fromJobInfo_nullJobRunnerJob_processing() throws DbException {
        when(jobRunnerDao.selectJobRunnerJob(gpJobNo)).thenThrow(new DbException());
        when(jobInfo.getStatus()).thenReturn(org.genepattern.server.domain.JobStatus.PROCESSING);
        
        JobStatusLoader loader=new JobStatusLoaderFromDb(gpUrl, jobRunnerDao, jobOutputDao);
        Status status = loader.loadJobStatus(jobContext);
        
        assertNotNull("expecting non-null status", status); 
        assertEquals("hasError", false, status.isHasError());
        assertEquals("isFinished", false, status.getIsFinished());
        assertEquals("isRunning", true, status.getIsRunning());
        assertEquals("isPending", false, status.getIsPending());
    }
    
    /**
     * Initialize Status when there is a valid JobInfo instance but no entry in the job_runner_job table,
     * jobInfo.status is FINISHED
     */
    @Test
    public void fromJobInfo_nullJobRunnerJob_finished() throws DbException {
        when(jobRunnerDao.selectJobRunnerJob(gpJobNo)).thenThrow(new DbException());
        when(jobInfo.getStatus()).thenReturn(org.genepattern.server.domain.JobStatus.FINISHED);
        
        JobStatusLoader loader=new JobStatusLoaderFromDb(gpUrl, jobRunnerDao, jobOutputDao);
        Status status = loader.loadJobStatus(jobContext);
        
        assertNotNull("expecting non-null status", status); 
        assertEquals("hasError", false, status.isHasError());
        assertEquals("isFinished", true, status.getIsFinished());
        assertEquals("isRunning", false, status.getIsRunning());
        assertEquals("isPending", false, status.getIsPending());
    }
    
    /**
     * Initialize Status when there is a valid JobInfo instance but no entry in the job_runner_job table,
     * jobInfo.status is ERROR
     */
    @Test
    public void fromJobInfo_nullJobRunnerJob_error() throws DbException {
        when(jobRunnerDao.selectJobRunnerJob(gpJobNo)).thenThrow(new DbException());
        when(jobInfo.getStatus()).thenReturn(org.genepattern.server.domain.JobStatus.ERROR);
        
        JobStatusLoader loader=new JobStatusLoaderFromDb(gpUrl, jobRunnerDao, jobOutputDao);
        Status status = loader.loadJobStatus(jobContext);
        
        assertNotNull("expecting non-null status", status); 
        assertEquals("hasError", true, status.isHasError());
        assertEquals("isFinished", true, status.getIsFinished());
        assertEquals("isRunning", false, status.getIsRunning());
        assertEquals("isPending", false, status.getIsPending());
    }
    
    /**
     * initialize Status directly from job_runner_job table, without a valid JobInfo record
     * @throws DbException
     */
    @Test
    public void fromJobRunnerJob() throws DbException {
        jobContext=new GpContext.Builder()
            .userId(userId)
            .jobNumber(gpJobNo)
        .build();
        
        JobRunnerJob jobRunnerJob=Mockito.mock(JobRunnerJob.class);
        when(jobRunnerJob.getJobState()).thenReturn(DrmJobState.DONE.name());
        when(jobRunnerDao.selectJobRunnerJob(gpJobNo)).thenReturn(jobRunnerJob);
        JobStatusLoader loader=new JobStatusLoaderFromDb(gpUrl, jobRunnerDao, jobOutputDao);
        Status status = loader.loadJobStatus(jobContext);
        assertNotNull("expecting non-null status", status); 
        assertEquals("hasError", false, status.isHasError());
        assertEquals("isFinished", true, status.getIsFinished());
        assertEquals("isRunning", false, status.getIsRunning());
        assertEquals("isPending", false, status.getIsPending());
        /* Example status.json
           "hasError": true,
           "isFinished": true,
           "isPending": false,
           "statusDate": "2014-07-22T00:37:09-04:00",
           "statusFlag": "FAILED",
           "statusMessage": "Exited with exit code 233.",
           "stderrLocation": "http://gpdev.broadinstitute.org:80/gp/jobResults/66726/stderr.txt"
         */
    }
    
    @Test
    public void links() throws DbException, IOException  {
        /* Example format
           "links": [ {
               "href": "http://127.0.0.1:8080/gp/rest/v1/jobs/66726/status.json",
               "rel": "self gp_status"
           },
           {
               "href": "http://127.0.0.1:8080/gp/rest/v1/jobs/66726",
               "rel": "gp_job"
           }
         */ 
        JobStatusLoaderFromDb loader=new JobStatusLoaderFromDb(gpUrl, jobRunnerDao, jobOutputDao);
        Status status = loader.loadJobStatus(jobContext);
        assertEquals("links.size", 2, status.getLinks().size());
        assertEquals("links[0].href", gpUrl+"rest/v1/jobs/"+gpJobNo+"/status.json", status.getLinks().get(0).getHref());
        assertEquals("links[0].rels[0]", Rel.self, status.getLinks().get(0).getRels().get(0));
        assertEquals("links[0].rels[0]", Rel.gp_status, status.getLinks().get(0).getRels().get(1));
        assertEquals("links[1].href", gpUrl+"rest/v1/jobs/"+gpJobNo, status.getLinks().get(1).getHref());
        assertEquals("links[1].rels[0]", Rel.gp_job, status.getLinks().get(1).getRels().get(0));
    }
    
    @Test
    public void gpExecutionLog() throws DbException, IOException {
        JobOutputFile executionLog=JobOutputFile.from(""+gpJobNo, jobDir, new File("gp_execution_log.txt"), null, GpFileType.GP_EXECUTION_LOG);
        final List<JobOutputFile> executionLogs=new ArrayList<JobOutputFile>();
        executionLogs.add(executionLog);
        Mockito.when(jobOutputDao.selectGpExecutionLogs(gpJobNo)).thenReturn(executionLogs);
        
        JobStatusLoader loader=new JobStatusLoaderFromDb(gpUrl, jobRunnerDao, jobOutputDao);
        Status status = loader.loadJobStatus(jobContext);
        assertEquals("status.executionLogLocation", gpUrl+"jobResults/"+gpJobNo+"/gp_execution_log.txt", status.getExecutionLogLocation());
    }
    
    @Test
    public void stderrFile() throws DbException, IOException {
        JobOutputFile stderrFile=JobOutputFile.from(""+gpJobNo, jobDir, new File("stderr.txt"), null, GpFileType.STDERR);
        final List<JobOutputFile> stderrFiles=new ArrayList<JobOutputFile>();
        stderrFiles.add(stderrFile);
        Mockito.when(jobOutputDao.selectStderrFiles(gpJobNo)).thenReturn(stderrFiles);
        
        JobStatusLoader loader=new JobStatusLoaderFromDb(gpUrl, jobRunnerDao, jobOutputDao);
        Status status = loader.loadJobStatus(jobContext);
        assertEquals("status.stderrLocation", gpUrl+"jobResults/"+gpJobNo+"/stderr.txt", status.getStderrLocation());
    }

}

package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.List;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.impl.local.LocalJobRunner;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit test cases for creating, updating, and deleting entries from the 'job_runner_job' table.
 * @author pcarr
 *
 */
public class TestDbLookup {
    private static final String jobRunnerClassname=LocalJobRunner.class.getName();
    private static final String jobRunnerName="LocalQueuingSystem-1";
    final Integer gpJobNo=0;
    final String drmJobId="DRM_"+gpJobNo;
    final String userId="test";
    final String taskName="EchoTest";
    final String cmdLine="echo <arg1>";
    final String[] commandLine={ "echo", "Hello, World!" };
    private static GpContext createJobContext(final String userId, final Integer jobNumber, final String taskName, final String cmdLine) {
        final TaskInfo taskInfo=createTask(taskName, cmdLine);
        final File taskLibDir=new File("taskLib/"+taskName+".1.0");
        final JobInfo jobInfo=new JobInfo();
        jobInfo.setJobNumber(jobNumber);
        jobInfo.setTaskName(taskName);
        final GpContext taskContext=new GpContextFactory.Builder()
            .userId(userId)
            .jobInfo(jobInfo)
            .taskInfo(taskInfo)
            .taskLibDir(taskLibDir)
            .build();
        return taskContext;
    }

    private static TaskInfo createTask(final String name, final String cmdLine) {
        TaskInfo mockTask=new TaskInfo();
        mockTask.setName(name);
        mockTask.giveTaskInfoAttributes();
        mockTask.getTaskInfoAttributes().put(GPConstants.LSID, "");
        mockTask.getTaskInfoAttributes().put(GPConstants.TASK_TYPE, "Test");
        mockTask.getTaskInfoAttributes().put(GPConstants.COMMAND_LINE, cmdLine);
        return mockTask;
    }    

    @BeforeClass
    public static void beforeClass() throws Exception{
        //some of the classes being tested require a Hibernate Session connected to a GP DB
        DbUtil.initDb();
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        DbUtil.shutdownDb();
    }
        
    @Test
    public void testCreate() {
        final GpContext jobContext=createJobContext(userId, gpJobNo, taskName, cmdLine); 
        final File workingDir=new File("jobResults/"+gpJobNo);
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(commandLine)
            .build();
        
        DbLookup dbLookup = new DbLookup(jobRunnerClassname, jobRunnerName);
        dbLookup.insertJobRecord(jobSubmission);
        DrmJobStatus drmJobStatus = new DrmJobStatus.Builder(drmJobId, DrmJobState.QUEUED).build();
        final DrmJobRecord jobRecord=dbLookup.lookupJobRecord(gpJobNo);
        dbLookup.updateJobStatus(jobRecord, drmJobStatus);
    }
    
    @Test
    public void testQuery() {
        DbLookup dbLookup=new DbLookup(jobRunnerClassname, jobRunnerName);
        final DrmJobRecord jobRecord=dbLookup.lookupJobRecord(gpJobNo);
        Assert.assertNotNull("expecting non-null jobRecord for gpJobNo="+gpJobNo, jobRecord);
        final String actualDrmJobId=jobRecord.getExtJobId();
        Assert.assertEquals("lookupDrmJobId("+gpJobNo+")", drmJobId, actualDrmJobId);
    }
    
    @Test
    public void testUpdate() {
        final GpContext jobContext=createJobContext(userId, gpJobNo+1, taskName, cmdLine); 
        final File workingDir=new File("jobResults/"+(gpJobNo+1));
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(commandLine)
            .build();

        DbLookup dbLookup=new DbLookup(jobRunnerClassname, jobRunnerName);
        dbLookup.insertJobRecord(jobSubmission);
        final DrmJobRecord jobRecord=dbLookup.lookupJobRecord(gpJobNo+1);
        final DrmJobStatus drmJobStatus = new DrmJobStatus.Builder(jobRecord.getExtJobId(), DrmJobState.DONE).build();
        dbLookup.updateJobStatus(jobRecord, drmJobStatus);        
        final JobRunnerJob jobRunnerJob=DbLookup.selectJobRunnerJob(gpJobNo+1);
        Assert.assertEquals("update job_state", DrmJobState.DONE.name(), jobRunnerJob.getJobState());
    }
    
    @Test
    public void testGetRunningDrmRecords() {
        DbLookup dbLookup=new DbLookup(jobRunnerClassname, jobRunnerName);
        List<DrmJobRecord> runningJobs=dbLookup.getRunningDrmJobRecords();
        Assert.assertNotNull("runningDrmJobRecords", runningJobs);
        Assert.assertEquals("num running jobs", 1, runningJobs.size());
    }

}

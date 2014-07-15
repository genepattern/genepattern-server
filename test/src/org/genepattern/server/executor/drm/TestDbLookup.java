package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.impl.local.LocalJobRunner;
import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * jUnit test cases for creating, updating, and deleting entries from the 'job_runner_job' table.
 * 
 * Assume that the tests are not run in parallel, but that they can be run in any order.
 * 
 * @author pcarr
 *
 */
public class TestDbLookup {
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();
    
    
    private static final String jobRunnerClassname=LocalJobRunner.class.getName();
    private static final String jobRunnerName="LocalQueuingSystem-1";
    private static final String userId="test";
    
    private static TaskInfo cle;
    private static JobInput cleInput;
    private static String[] cleCmdLine={
            "perl",
            "/taskLib/ConvertLineEndings.1.1/to_host.pl",
            "/users/test/uploads/all_aml_train.gct",
            "all_aml_train.cvt.gct"
    };

    private DrmJobSubmission jobSubmission;
    private DbLookup dbLookup;
    private File jobResultsDir;
    
    private DrmJobSubmission addJob(final TaskInfo taskInfo, final JobInput jobInput, final String[] commandLine) throws Exception {
        jobInput.setLsid(taskInfo.getLsid());
        final GpContext taskContext=new GpContextFactory.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
        .build();
        final AnalysisJobUtil jobUtil=new AnalysisJobUtil();
        final boolean initDefault=true;
        final int jobNumber=jobUtil.addJobToDb(taskContext, jobInput, initDefault);
        final JobInfo jobInfo=jobUtil.fetchJobInfoFromDb(jobNumber);
        final GpContext jobContext=new GpContextFactory.Builder()
            .userId(userId)
            .jobInfo(jobInfo)
            .taskInfo(taskInfo)
        .build();
        final File runDir=new File(jobResultsDir, ""+jobInfo.getJobNumber());
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(runDir)
            .jobContext(jobContext)
            .commandLine(commandLine)
            .stdoutFile(new File("stdout.txt"))
            .stderrFile(new File("stderr.txt"))
            .stdinFile(null)
        .build();
        return jobSubmission;
    }
    
    private void deleteJob(final int jobId) throws Exception {
        final AnalysisJobUtil jobUtil=new AnalysisJobUtil();
        jobUtil.deleteJobFromDb(jobId);
    }

    @BeforeClass
    public static void beforeClass() throws Exception{
        DbUtil.initDb();
        
        final String cleZip="modules/ConvertLineEndings_v2.zip";
        final File zipFile=FileUtil.getDataFile(cleZip);
        cle=TaskUtil.getTaskInfoFromZip(zipFile);
        cleInput=new JobInput();
        cleInput.setLsid(cle.getLsid());
        cleInput.addValue("input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct");
    }
    
    @Before
    public void before() throws Exception {
        DbUtil.deleteAllRows(JobRunnerJob.class);
        jobSubmission=addJob(cle, cleInput, cleCmdLine);
        dbLookup = new DbLookup(jobRunnerClassname, jobRunnerName);
    }
    
    @After
    public void after() throws Exception {
    }
        
    @Test
    public void testInsertJobRecord() throws Exception {
        dbLookup.insertJobRecord(jobSubmission);
    }
    
    /**
     * special-case, when the statusMessage has more characters than the DB column allows.
     * @throws Exception
     */
    @Test
    public void truncateStatusMessage() throws Exception {  
        StringBuffer sb=new StringBuffer(2025);
        sb.append("This is a status message\n");
        final String TEN_CHARS="----------";
        for(int i=0; i<200; ++i) {
            sb.append(TEN_CHARS);
        }
        String statusMessage=sb.toString();
        
        JobRunnerJob jobRecord = new JobRunnerJob.Builder()
            .jobRunnerClassname("DemoJobRunner")
            .jobRunnerName("DemoJobRunner")
            .workingDir(jobSubmission.getWorkingDir().getAbsolutePath())
            .gpJobNo(jobSubmission.getGpJobNo())
            .statusMessage(statusMessage)
        .build();
        DbLookup.insertJobRunnerJob(jobRecord);
        JobRunnerJob updated=DbLookup.selectJobRunnerJob(jobSubmission.getGpJobNo());
        Assert.assertEquals("jobRunnerJob.statusMessage.length", 2000, updated.getStatusMessage().length());
    }
    
    @Test
    public void testQuery() throws Exception {
        Assert.assertEquals("Expecting null entry before adding job_runner_job", null, 
                dbLookup.lookupJobRecord(jobSubmission.getGpJobNo()));

        dbLookup.insertJobRecord(jobSubmission);
        DrmJobRecord jobRecord=dbLookup.lookupJobRecord(jobSubmission.getGpJobNo());
        Assert.assertEquals("jobRecord.gpJobNo", jobSubmission.getGpJobNo(), jobRecord.getGpJobNo());
        Assert.assertEquals("jobRecord.extJobId", "", jobRecord.getExtJobId());
    }
    
    @Test
    public void testUpdate() throws Exception {
        dbLookup.insertJobRecord(jobSubmission);
        
        final DrmJobRecord jobRecord=dbLookup.lookupJobRecord(jobSubmission.getGpJobNo());
        final DrmJobStatus drmJobStatus = new DrmJobStatus.Builder("EXT_"+jobSubmission.getJobInfo().getJobNumber(), DrmJobState.QUEUED).build();
        dbLookup.updateJobStatus(jobRecord, drmJobStatus);        
        final JobRunnerJob jobRunnerJob=DbLookup.selectJobRunnerJob(jobSubmission.getGpJobNo());
        Assert.assertEquals("update job_state", DrmJobState.QUEUED.name(), jobRunnerJob.getJobState());
    }
    
    @Test
    public void testGetRunningDrmRecords() {
        List<DrmJobRecord> runningJobs=dbLookup.getRunningDrmJobRecords();
        Assert.assertNotNull("runningDrmJobRecords", runningJobs);
        Assert.assertEquals("num running jobs", 0, runningJobs.size());
    }
    
    @Test
    public void testEmptyExtJobId() throws Exception {
        jobResultsDir=temp.newFolder("jobResults");

        //test fk relationship
        final DrmJobSubmission jobSubmission_01=addJob(cle, cleInput, cleCmdLine);
        dbLookup.insertJobRecord(jobSubmission_01);
        //add a second job
        final DrmJobSubmission jobSubmission_02=addJob(cle, cleInput, cleCmdLine);
        dbLookup.insertJobRecord(jobSubmission_02);

        String extJobId_01=null;
        DrmJobRecord jobRecord_01=new DrmJobRecord.Builder(extJobId_01, jobSubmission_01)
        .build();
        DrmJobStatus jobStatus_01=new DrmJobStatus.Builder(extJobId_01, DrmJobState.QUEUED)
        .build();
        dbLookup.updateJobStatus(jobRecord_01, jobStatus_01);
        
        Assert.assertNotNull("job 1", dbLookup.lookupJobRecord(jobSubmission_01.getGpJobNo()));

        String extJobId_02=null;
        DrmJobRecord jobRecord_02=new DrmJobRecord.Builder(extJobId_02, jobSubmission_02)
        .build();
        DrmJobStatus jobStatus_02=new DrmJobStatus.Builder(extJobId_02, DrmJobState.QUEUED)
        .build();
        dbLookup.updateJobStatus(jobRecord_02, jobStatus_02);

        Assert.assertNotNull("job 2", dbLookup.lookupJobRecord(jobSubmission_02.getGpJobNo()));
        
        //test cascade delete
        int gpJobNo_01=jobSubmission_01.getJobInfo().getJobNumber();
        deleteJob(gpJobNo_01);
        Assert.assertNull("cascade", dbLookup.lookupJobRecord(gpJobNo_01));
    }
    
    @Test
    public void jobQueueTimes() {
        Date lsfSubmitTime=new DateTime("2014-07-15T09:00:00").toDate();
        Date lsfStartTime =new DateTime("2014-07-15T09:15:21").toDate();
        Date lsfEndTime   =new DateTime("2014-07-15T10:15:21").toDate();
        
        int gpJobNo=new AnalysisJobUtil().addJobToDb();
        
        JobRunnerJob jobRecord=new JobRunnerJob.Builder()
            .gpJobNo(gpJobNo)
            .workingDir(new File(jobResultsDir, ""+gpJobNo).getAbsolutePath())
            .jobRunnerClassname(jobRunnerClassname)
            .jobRunnerName(jobRunnerName)
            .extJobId("EXT_"+gpJobNo)
            .jobState(DrmJobState.DONE)
            .statusMessage(DrmJobState.DONE.getDescription())
            .exitCode(0)
            .submitTime(lsfSubmitTime)
            .startTime(lsfStartTime)
            .endTime(lsfEndTime)
        .build();
        dbLookup.insertJobRunnerJob(jobRecord);
        
        JobRunnerJob query=dbLookup.selectJobRunnerJob(gpJobNo);
        Assert.assertEquals("jobRunnerJob.submitTime", lsfSubmitTime, query.getSubmitTime());
        Assert.assertEquals("jobRunnerJob.startTime", lsfStartTime, query.getStartTime());
        Assert.assertEquals("jobRunnerJob.endTime", lsfEndTime, query.getEndTime());
    }

}

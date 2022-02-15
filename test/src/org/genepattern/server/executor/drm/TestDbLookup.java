/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.drm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.genepattern.drm.CpuTime;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.Memory;
import org.genepattern.drm.impl.local.LocalJobRunner;
import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.joda.time.DateTime;
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

    private HibernateSessionManager mgr;
    private GpConfig gpConfig;
    private DrmJobSubmission jobSubmission;
    private DbLookup dbLookup;
    private File jobResultsDir;
    
    private DrmJobSubmission addJob(final TaskInfo taskInfo, final JobInput jobInput, final String[] commandLine) throws Exception {
        jobInput.setLsid(taskInfo.getLsid());
        final GpContext taskContext=new GpContext.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
        .build();
        final AnalysisJobUtil jobUtil=new AnalysisJobUtil();
        final boolean initDefault=true;
        final int jobNumber=AnalysisJobUtil.addJobToDb(mgr, gpConfig, taskContext, jobInput, -1, initDefault);
        final JobInfo jobInfo=jobUtil.fetchJobInfoFromDb(mgr, jobNumber);
        final GpContext jobContext=new GpContext.Builder()
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

    @BeforeClass
    public static void beforeClass() throws Exception{
        final String cleZip="modules/ConvertLineEndings_v2.zip";
        final File zipFile=FileUtil.getDataFile(cleZip);
        cle=TaskUtil.getTaskInfoFromZip(zipFile);
        cleInput=new JobInput();
        cleInput.setLsid(cle.getLsid());
        cleInput.addValue("input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct");
    }
    
    @Before
    public void before() throws Exception {
        mgr=DbUtil.getTestDbSession();
        gpConfig=new GpConfig.Builder().build();
        DbUtil.deleteAllRows(mgr, JobRunnerJob.class);
        jobSubmission=addJob(cle, cleInput, cleCmdLine);
        dbLookup = new DbLookup(mgr, jobRunnerClassname, jobRunnerName);
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
            .lsid(jobSubmission.getJobContext().getLsid())
            .statusMessage(statusMessage)
        .build();
        DbLookup.insertJobRunnerJob(mgr, jobRecord);
        JobRunnerJob updated=new JobRunnerJobDao().selectJobRunnerJob(mgr, jobSubmission.getGpJobNo());
        Assert.assertEquals("jobRunnerJob.statusMessage.length", 2000, updated.getStatusMessage().length());
    }
    
    @Test
    public void testQuery() throws Exception {
        Assert.assertEquals("Expecting null entry before adding job_runner_job", null, 
                dbLookup.lookupJobRecord(jobSubmission.getGpJobNo()));

        dbLookup.insertJobRecord(jobSubmission);
        DrmJobRecord jobRecord=dbLookup.lookupJobRecord(jobSubmission.getGpJobNo());
        assertEquals("jobRecord.gpJobNo", jobSubmission.getGpJobNo(), jobRecord.getGpJobNo());
        assertEquals("jobRecord.extJobId", "", jobRecord.getExtJobId());
    }
    
    @Test
    public void testUpdate() throws Exception {
        dbLookup.insertJobRecord(jobSubmission);
        
        final DrmJobRecord jobRecord=dbLookup.lookupJobRecord(jobSubmission.getGpJobNo());
        final DrmJobStatus drmJobStatus = new DrmJobStatus.Builder("EXT_"+jobSubmission.getJobInfo().getJobNumber(), DrmJobState.QUEUED).build();
        dbLookup.updateJobStatus(jobRecord, drmJobStatus);        
        final JobRunnerJob jobRunnerJob=new JobRunnerJobDao().selectJobRunnerJob(mgr, jobSubmission.getGpJobNo());
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
        AnalysisJobUtil.deleteJobFromDb(mgr, gpJobNo_01);

        Assert.assertNull("cascade", dbLookup.lookupJobRecord(gpJobNo_01));
    }
    
    @Test
    public void jobQueueTimesAndResourceUsage() throws DbException {
        String queueId="genepattern_long";
        Date lsfSubmitTime=new DateTime("2014-07-15T09:00:00").toDate();
        Date lsfStartTime =new DateTime("2014-07-15T09:15:21").toDate();
        Date lsfEndTime   =new DateTime("2014-07-15T10:15:21").toDate();
        
        Memory reqMemory=Memory.fromSizeInBytes(4000L);
        
        CpuTime cpuTime=new CpuTime(900L, TimeUnit.MILLISECONDS);
        Memory maxMemory=Memory.fromSizeInBytes(1000L);
        Memory maxSwap=Memory.fromSizeInBytes(2000L);
        int maxProcesses=1;
        int maxThreads=1;
        
        int gpJobNo=AnalysisJobUtil.addJobToDb(mgr);
        
        JobRunnerJob jobRecord=new JobRunnerJob.Builder()
            .gpJobNo(gpJobNo)
            .workingDir(new File(jobResultsDir, ""+gpJobNo).getAbsolutePath())
            .jobRunnerClassname(jobRunnerClassname)
            .jobRunnerName(jobRunnerName)
            .extJobId("EXT_"+gpJobNo)
            .queueId(queueId)
            .jobState(DrmJobState.DONE)
            .statusMessage(DrmJobState.DONE.getDescription())
            .exitCode(0)
            .submitTime(lsfSubmitTime)
            .startTime(lsfStartTime)
            .endTime(lsfEndTime)
            .cpuTime(cpuTime)
            .maxMemory(maxMemory)
            .maxSwap(maxSwap)
            .maxProcesses(maxProcesses)
            .maxThreads(maxThreads)
            .requestedMemory(reqMemory)
        .build();
        DbLookup.insertJobRunnerJob(mgr, jobRecord);
        
        JobRunnerJob query=new JobRunnerJobDao().selectJobRunnerJob(mgr, gpJobNo);
        Assert.assertEquals("jobRunnerJob.queueId", queueId, query.getQueueId());
        Assert.assertEquals("jobRunnerJob.submitTime", lsfSubmitTime, query.getSubmitTime());
        Assert.assertEquals("jobRunnerJob.startTime", lsfStartTime, query.getStartTime());
        Assert.assertEquals("jobRunnerJob.endTime", lsfEndTime, query.getEndTime());
        Assert.assertEquals("jobRunnerJob.cpuTime", (Long) cpuTime.asMillis(), query.getCpuTime());
        Assert.assertEquals("jobRunnerJob.maxMemory", (Long) maxMemory.getNumBytes(), query.getMaxMemory());
        Assert.assertEquals("jobRunnerJob.maxSwap", (Long) maxSwap.getNumBytes(), query.getMaxSwap());
        Assert.assertEquals("jobRunnerJob.maxProcesses", (Integer) maxProcesses, query.getMaxProcesses());
        Assert.assertEquals("jobRunnerJob.maxThreads", (Integer) maxThreads, query.getMaxThreads());
        Assert.assertEquals("jobRunnerJob.reqMemory", (Long) reqMemory.getNumBytes(), query.getRequestedMemory());
    }
    
    @Test
    public void updateJobRecord() throws DbException {
        int gpJobNo=AnalysisJobUtil.addJobToDb(mgr);
        String jrClassname="org.genepattern.drm.impl.lsf.core.CmdLineLsfRunner";
        String jrName="CmdLineLsfRunner";
        String workingDir="/opt/genepattern/jobResults/"+gpJobNo;
        String stdoutFile="stdout.txt";
        String stderrFile="stderr.txt";
        JobRunnerJob jobRecord_01=new JobRunnerJob.Builder()
            .gpJobNo(gpJobNo)
            .jobRunnerClassname(jrClassname)
            .jobRunnerName(jrName)
            .workingDir(workingDir)
            .stdoutFile(stdoutFile)
            .stderrFile(stderrFile)
        .build();
        DbLookup.insertJobRunnerJob(mgr, jobRecord_01);

        String extJobId="EXT_"+gpJobNo;
        String queueId="genepattern_long";
        Date submitTime=DateTime.parse("20140717T10:50:21").toDate();
        Date startTime=DateTime.parse("20140717T10:51:21").toDate();
        Date endTime=DateTime.parse("20140717T11:01:21").toDate();
        CpuTime cpuTime=new CpuTime(5, TimeUnit.MINUTES);
        Memory maxMem=Memory.fromString("4 Gb");
        Memory maxSwap=Memory.fromString("8 Gb");
        Integer maxProcesses=1;
        Integer maxThreads=1;
        DrmJobStatus jobStatus=new DrmJobStatus.Builder()
             .extJobId(extJobId)
             .queueId(queueId)
             .jobState(DrmJobState.DONE)
             .jobStatusMessage(DrmJobState.DONE.getDescription())
             .exitCode(0)
             .submitTime(submitTime)
             .startTime(startTime)
             .endTime(endTime)
             .cpuTime(cpuTime)
             .memory(maxMem)
             .maxSwap(maxSwap.getNumBytes())
             .maxProcesses(maxProcesses)
             .maxThreads(maxThreads)
        .build();
        DrmJobRecord jobRecord=new DrmJobRecord.Builder()
            .gpJobNo(gpJobNo)
        .build();
        dbLookup.updateJobStatus(jobRecord, jobStatus);
        JobRunnerJob updated=new JobRunnerJobDao().selectJobRunnerJob(mgr, gpJobNo);
        assertEquals("gpJobNo", (Integer) gpJobNo, updated.getGpJobNo());
        assertEquals("jobRunnerClassname", jrClassname, updated.getJobRunnerClassname());
        assertEquals("jobRunnerName", jrName, updated.getJobRunnerName());
        assertEquals("workingDir", workingDir, updated.getWorkingDir());
        assertEquals("stdoutFile", stdoutFile, updated.getStdoutFile());
        assertEquals("stderrFile", stderrFile, updated.getStderrFile());
        assertEquals("extJobId", extJobId, updated.getExtJobId());
        assertEquals("queueId", queueId, updated.getQueueId());
        assertEquals("submitTime", submitTime, updated.getSubmitTime());
        assertEquals("startTime", startTime, updated.getStartTime());
        assertEquals("endTime", endTime, updated.getEndTime());
        assertEquals("cpuTime", (Long) cpuTime.asMillis(), updated.getCpuTime());
        assertEquals("maxMem", (Long) maxMem.getNumBytes(), updated.getMaxMemory());
        assertEquals("maxSwap", (Long) maxSwap.getNumBytes(), updated.getMaxSwap());
        assertEquals("maxProcesses", (Integer) maxProcesses, updated.getMaxProcesses());
        assertEquals("maxThreads", (Integer) maxThreads, updated.getMaxThreads());
    }

}

package org.genepattern.server.executor.drm.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.Memory;
import org.genepattern.drm.Walltime;
import org.genepattern.drm.impl.lsf.core.CmdLineLsfRunner;
import org.genepattern.junitutil.AnalysisJobUtil;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
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
    final String jrClassname=CmdLineLsfRunner.class.getName();
    final String jrName="CmdLineLsfRunner";
    private JobRunnerJobDao dao;
    private DrmJobSubmission.Builder builder;
    
    private Integer gpJobNo;
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
        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(gpJobNo);
        when(jobInfo.getTaskLSID()).thenReturn(cleLsid);
        
        jobContext=new GpContext.Builder()
            .jobInfo(jobInfo)
        .build();
        dao = new JobRunnerJobDao();
        
        final File jobDir=new File(""+gpJobNo);
        builder=new DrmJobSubmission.Builder(jobDir)
            .jobContext(jobContext)
            .commandLine(commandLine)
            .stdoutFile(new File("stdout.txt"))
            .stderrFile(new File("stderr.txt"))
            .logFilename(".lsf.out");
    }
    
    @After
    public void tearDown() {
        new AnalysisJobUtil().deleteJobFromDb(gpJobNo);
    }
    
    /**
     * Test saving record to job_runner_job table for a new job submissions.
     * This is the way records are saved from the JobExecutor runCommand call.
     * 
     * @throws DbException
     */
    @Test
    public void insertFromDrmJobSubmission() throws DbException {
        final DrmJobSubmission drmJobSubmission=builder.build();
        final JobRunnerJob jobRecord = new JobRunnerJob.Builder(jrClassname, drmJobSubmission).jobRunnerName(jrName).build();
        dao.insertJobRunnerJob(jobRecord);

        JobRunnerJob query=dao.selectJobRunnerJob(gpJobNo);
        assertEquals("gpJobNo", gpJobNo, query.getGpJobNo());
        assertEquals("lsid", cleLsid, query.getLsid());
        assertEquals("max_mem", null, query.getMaxMemory());
        assertEquals("req_mem", null, query.getRequestedMemory());
    }
    
    @Test
    public void req_jobMemory() throws DbException {
        final Memory requestedMemory=Memory.fromString("4 Gb");
        final GpConfig gpConfig = new GpConfig.Builder()
            .addProperty(JobRunner.PROP_MEMORY, "4 Gb")
            .build();
        builder.gpConfig(gpConfig);

        final DrmJobSubmission drmJobSubmission=builder.build();
        final JobRunnerJob jobRecord = new JobRunnerJob.Builder(jrClassname, drmJobSubmission).jobRunnerName(jrName).build();
        dao.insertJobRunnerJob(jobRecord);

        JobRunnerJob query=dao.selectJobRunnerJob(gpJobNo);
        assertEquals("req_mem", (Long) requestedMemory.getNumBytes(), (Long) query.getRequestedMemory());
    }
    
    @Test
    public void req_cpuCount() throws DbException {
        final Integer cpuCount=4;
        final GpConfig gpConfig = new GpConfig.Builder()
            .addProperty(JobRunner.PROP_CPU_COUNT, ""+cpuCount)
            .build();
        builder.gpConfig(gpConfig);

        final DrmJobSubmission drmJobSubmission=builder.build();
        final JobRunnerJob jobRecord = new JobRunnerJob.Builder(jrClassname, drmJobSubmission).jobRunnerName(jrName).build();
        dao.insertJobRunnerJob(jobRecord);

        JobRunnerJob query=dao.selectJobRunnerJob(gpJobNo);
        assertEquals("req_cpu_count", (Integer) cpuCount, (Integer) query.getRequestedCpuCount());
    }

    @Test
    public void req_nodeCount() throws DbException {
        final Integer nodeCount=2;
        final GpConfig gpConfig = new GpConfig.Builder()
            .addProperty(JobRunner.PROP_NODE_COUNT, ""+nodeCount)
            .build();
        builder.gpConfig(gpConfig);

        final DrmJobSubmission drmJobSubmission=builder.build();
        final JobRunnerJob jobRecord = new JobRunnerJob.Builder(jrClassname, drmJobSubmission).jobRunnerName(jrName).build();
        dao.insertJobRunnerJob(jobRecord);

        JobRunnerJob query=dao.selectJobRunnerJob(gpJobNo);
        assertEquals("req_node_count", (Integer) nodeCount, (Integer) query.getRequestedNodeCount());
    }
    
    @Test
    public void req_walltime() throws Exception {
        final Walltime walltime=Walltime.fromString("12:00:00");
        final GpConfig gpConfig = new GpConfig.Builder()
            .addProperty(JobRunner.PROP_WALLTIME, walltime.toString())
            .build();
        builder.gpConfig(gpConfig);

        final DrmJobSubmission drmJobSubmission=builder.build();
        final JobRunnerJob jobRecord = new JobRunnerJob.Builder(jrClassname, drmJobSubmission).jobRunnerName(jrName).build();
        dao.insertJobRunnerJob(jobRecord);
        
        JobRunnerJob query=dao.selectJobRunnerJob(gpJobNo);
        assertEquals("req_walltime", walltime.toString(), query.getRequestedWalltime());
    }
    
    @Test
    public void req_queue() throws Exception {
        final String queue="my_queue";
        final GpConfig gpConfig = new GpConfig.Builder()
            .addProperty(JobRunner.PROP_QUEUE, queue)
            .build();
        builder.gpConfig(gpConfig);
        
        final DrmJobSubmission drmJobSubmission=builder.build();
        final JobRunnerJob jobRecord = new JobRunnerJob.Builder(jrClassname, drmJobSubmission).jobRunnerName(jrName).build();
        dao.insertJobRunnerJob(jobRecord);
        
        JobRunnerJob query=dao.selectJobRunnerJob(gpJobNo);
        assertEquals("req_queue", queue, query.getRequestedQueue());
    }

}

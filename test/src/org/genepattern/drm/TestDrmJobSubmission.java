/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.drm;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.job.input.configparam.JobConfigParams;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * junit tests for the DrmJobSubmission class.
 * @author pcarr
 *
 */
public class TestDrmJobSubmission {
    private GpContext jobContext;
    private static final String userId="test";
    private static final Integer jobNo=1;
    private static final String taskName="EchoTest";
    private static final String cmdLineStr="echo <arg1>";
    private static final String[] cmdLineArgs={"echo", "Hello, World!"};
    
    private File jobResults;
    private File workingDir;
    
    private static GpContext createJobContext(final String userId, final Integer jobNumber, final String taskName, final String cmdLine) {
        final TaskInfo taskInfo=createTask(taskName, cmdLine);
        final File taskLibDir=new File("taskLib/"+taskName+".1.0");
        final JobInfo jobInfo=new JobInfo();
        jobInfo.setJobNumber(jobNumber);
        jobInfo.setTaskName(taskName);
        final GpContext taskContext=new GpContext.Builder()
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


    @Before
    public void before() throws Throwable {
        ConfigUtil.loadConfigFile(this.getClass(), "drm_test.yaml");
        if (ServerConfigurationFactory.instance().getInitializationErrors().size()>0) {
            throw ServerConfigurationFactory.instance().getInitializationErrors().get(0);
        }

        this.jobResults=temp.newFolder("jobResults");
        this.workingDir=new File(jobResults, ""+jobNo);
        this.jobContext=createJobContext(userId, jobNo, taskName, cmdLineStr);
    }
    
    @After
    public void after() {
        //revert back to a 'default' config.file
        ConfigUtil.loadConfigFile(null);
    }
    
    @Rule
    public TemporaryFolder temp= new TemporaryFolder();
    
    @Test
    public void testDefaultBuilder_commandLine() { 
        final DrmJobSubmission job = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(cmdLineArgs)
            .build();
        assertEquals("jobNo", jobNo, job.getGpJobNo());
        assertEquals("jobNo", jobNo.intValue(), job.getJobInfo().getJobNumber());
        assertEquals("workingDir", workingDir.getAbsolutePath(), job.getWorkingDir().getAbsolutePath());
        assertEquals("commandLine.length", cmdLineArgs.length, job.getCommandLine().size());
        assertEquals("arg[0]", cmdLineArgs[0], job.getCommandLine().get(0));
        assertEquals("arg[1]", cmdLineArgs[1], job.getCommandLine().get(1));
        
        assertEquals("env.length", 0, job.getEnvironmentVariables().size());
        try {
            job.getEnvironmentVariables().put("newArg", "newValue");
            ///CLOVER:OFF
            fail("environmentVariables should be unmodifiable");
            ///CLOVER:ON
        }
        catch (UnsupportedOperationException e) {
            //expected
        }
        try {
            job.getCommandLine().add("-P");
            ///CLOVER:OFF
            fail("commandLine should be unmodifiable");
            ///CLOVER:ON
        }
        catch (UnsupportedOperationException e) {
            //expected
        }

        assertNull("job.queue", job.getQueue()); 
        assertNull("job.walltime", job.getWalltime()); 
        assertNull("job.nodeCount", job.getNodeCount()); 
        assertNull("job.cpuCount", job.getCpuCount()); 
        assertEquals("job.extraArgs.size", 2, job.getExtraArgs().size());        
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testNullCommandLine() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .build();
        assertEquals("Expecting emtpy list", 0, drmJob.getCommandLine().size());
        //should throw an exception
        drmJob.getCommandLine().add("-P");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEmptyCommandLine() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(new String[0])
            .build();
        assertEquals("Expecting emtpy list", 0, drmJob.getCommandLine().size());
        //should throw an exception
        drmJob.getCommandLine().add("-P");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateCommandLine() {
        new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(cmdLineArgs)
            .commandLine(cmdLineArgs);
    }

    @Test
    public void testDefaultBuilder_addArg() {
        final DrmJobSubmission drmJob;
        DrmJobSubmission.Builder builder = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext);        
        for(final String arg : cmdLineArgs) {
            builder=builder.addArg(arg);
        }
        drmJob=builder.build();
        assertEquals("jobNo", jobNo.intValue(), drmJob.getJobInfo().getJobNumber());
        assertEquals("workingDir", workingDir.getAbsolutePath(), drmJob.getWorkingDir().getAbsolutePath());
        assertThat("commandLine", Arrays.asList(cmdLineArgs), CoreMatchers.is(drmJob.getCommandLine()));
    }
    
//    @Test
//    public void testAddExtraArg() {
//        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(workingDir)
//            .jobContext(jobContext)
//            .commandLine(cmdLineArgs)
//            //.addExtraArg("-P")
//            //.addExtraArg("ProjectName")
//            .build();
//
//        try {
//            drmJob.getExtraArgs().add("--extraArg=value");
//            ///CLOVER:OFF
//            fail("job.extraArgs should be unmodifiable");
//            ///CLOVER:ON
//        }
//        catch (UnsupportedOperationException e) {
//            //expected
//        }
//        assertEquals("job.extraArgs.size", 2, drmJob.getExtraArgs().size());
//        assertEquals("job.extraArgs[0]", "-P", drmJob.getExtraArgs().get(0));
//        assertEquals("job.extraArgs[1]", "ProjectName", drmJob.getExtraArgs().get(1));
//    }

    @Test
    public void testEnvironmentVariables() {
        final Map<String,String> envIn=new HashMap<String,String>();
        envIn.put("ANT_OPTS", "-Xmx2048m");
        envIn.put("JAVA_OPTS", "-XX:MaxPermSize=2g -Xmx2g");
        
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(cmdLineArgs)
            .environmentVariables(envIn)
            .build();
        
        final Map<String,String> envOut=drmJob.getEnvironmentVariables();
        try {
            envOut.put("ENV_01", "Value_01");
            ///CLOVER:OFF
            fail("job.environmentVariables should be unmodifiable");
            ///CLOVER:ON
        }
        catch (UnsupportedOperationException e) {
            //expected
        }
        assertEquals("env.length", 2, drmJob.getEnvironmentVariables().size());
        assertEquals("env['ANT_OPTS']", "-Xmx2048m", drmJob.getEnvironmentVariables().get("ANT_OPTS"));
        assertEquals("env['JAVA_OPTS']", "-XX:MaxPermSize=2g -Xmx2g", drmJob.getEnvironmentVariables().get("JAVA_OPTS"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateEnvironmentVariables() {
        final Map<String,String> envIn=new HashMap<String,String>();
        envIn.put("ANT_OPTS", "-Xmx2048m");
        envIn.put("JAVA_OPTS", "-XX:MaxPermSize=2g -Xmx2g");
        new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(cmdLineArgs)
            .environmentVariables(envIn)
            .environmentVariables(envIn);
    }

    @Test
    public void testAddEnvVar() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(cmdLineArgs)
            .addEnvVar("ANT_OPTS", "-Xmx2048m")
            .addEnvVar("JAVA_OPTS", "-XX:MaxPermSize=2g -Xmx2g")
            .build();
        
        assertEquals("env.length", 2, drmJob.getEnvironmentVariables().size());
        assertEquals("env['ANT_OPTS']", "-Xmx2048m", drmJob.getEnvironmentVariables().get("ANT_OPTS"));
        assertEquals("env['JAVA_OPTS']", "-XX:MaxPermSize=2g -Xmx2g", drmJob.getEnvironmentVariables().get("JAVA_OPTS"));
    }
    
    @Test
    public void testGetGpConfigProperty() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(cmdLineArgs)
            //.workerName("myLongPbsWorker")
            .build();
        
        final GpConfig config=ServerConfigurationFactory.instance();
        final GpContext userContext=GpContext.getContextForUser("test_user");
        Value javaXmx=config.getValue(userContext, "job.javaXmx");

        assertEquals("job.javaXmx", "2gb", drmJob.getProperty("job.javaXmx"));
    }
    
    @Test
    public void testInitFromGpConfig_memory() throws Exception {
        final GpConfig gpConfig=new GpConfig.Builder().addProperty(JobRunner.PROP_MEMORY, "4gb").build();
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
        .build();
        assertEquals("job.memory", Memory.fromString("4gb"), jobSubmission.getMemory());
    }
    
    @Test
    public void testInitFromGpConfig_cpuCount() throws Exception {
        final GpConfig gpConfig=new GpConfig.Builder().addProperty(JobRunner.PROP_CPU_COUNT, "2").build();
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
        .build();
        assertEquals("job.cpuCount", (Integer)2, jobSubmission.getCpuCount());
    }
    
    @Test
    public void testInitFromGpConfig_nodeCount() throws Exception {
        final GpConfig gpConfig=new GpConfig.Builder().addProperty(JobRunner.PROP_NODE_COUNT, "2").build();
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
        .build();
        assertEquals("job.nodeCount", (Integer)2, jobSubmission.getNodeCount());
    }
    
    @Test
    public void testInitFromGpConfig_walltime() throws Exception {
        final GpConfig gpConfig=new GpConfig.Builder().addProperty(JobRunner.PROP_WALLTIME, "7-00:00:00").build();
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
        .build();
        assertEquals("job.walltime", Walltime.fromString("7-00:00:00"), jobSubmission.getWalltime());
    }
    
    @Test
    public void testInitFromGpConfig_queue() throws Exception {
        final GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(JobRunner.PROP_QUEUE, "my_queue")
        .build();
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
        .build();
        assertEquals("getQueue", "my_queue", jobSubmission.getQueue());
        assertEquals("getQueueId", "my_queue", jobSubmission.getQueueId());
    }

    @Test
    public void testInitFromGpConfig_queue_and_virtualQueue() throws Exception {
        final GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(JobRunner.PROP_QUEUE, "my_queue")
            .addProperty(JobRunner.PROP_VIRTUAL_QUEUE, "my_virtual_queue")
        .build();
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
        .build();
        assertEquals("getQueue", "my_queue", jobSubmission.getQueue());
        assertEquals("getQueueId", "my_virtual_queue", jobSubmission.getQueueId());
    }

    @Test
    public void testInitFromGpConfig_queue_virtualQueue_only() throws Exception {
        final GpConfig gpConfig=new GpConfig.Builder()
            .addProperty(JobRunner.PROP_VIRTUAL_QUEUE, "my_virtual_queue")
        .build();
        final DrmJobSubmission jobSubmission=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
        .build();
        assertEquals("getQueue", null, jobSubmission.getQueue());
        assertEquals("getQueueId", "my_virtual_queue", jobSubmission.getQueueId());
    }

    @Test
    public void testExecutorCustomProperties() {
        final GpConfig config=ServerConfigurationFactory.instance();
        final GpContext userContext=GpContext.getContextForUser("test_user");
        //sanity check
        assertEquals("executor.props", "PbsBigMem", config.getGPProperty(userContext, "executor.props")); 
        final String executorId=config.getGPProperty(userContext, "executor");
        assertEquals("executor", "DemoPbsJobRunner", executorId);
    }
    
    @Test
    public void testCustomJobConfigParams() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext userContext=GpContext.getContextForUser("test_user");
        
        assertEquals("executor.props for test_user", 
                "PbsBigMem",  
                gpConfig.getGPProperty(userContext, "executor.props"));
        
        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, userContext);
        assertNotNull("initJobConfigParams was null", jobConfigParams);
    }
    
    @Test
    public void testCustomMemory() {
        final GpServerProperties serverProperties=new GpServerProperties.Builder()
            .addCustomProperty(JobRunner.PROP_MEMORY, "8g")
            .build();
        final GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(serverProperties)
            .build();
        DrmJobSubmission drmJobSubmission = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
            .build();
        
        assertEquals("set memory in config", Memory.fromString("8g"), 
                drmJobSubmission.getMemory());
    }
    
    @Test
    public void testCustomWalltime() throws Exception {
        final GpServerProperties serverProperties=new GpServerProperties.Builder()
            .addCustomProperty(JobRunner.PROP_WALLTIME, "7-00:00:00")
            .build();
        final GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(serverProperties)
            .build();
        DrmJobSubmission drmJobSubmission = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .gpConfig(gpConfig)
            .build();
        
        assertEquals("set walltime in config", 
                Walltime.fromString("7-00:00:00"),
                drmJobSubmission.getWalltime());
    }
    
    @Test
    public void testExtraArgs() {
        DrmJobSubmission drmJobSubmission = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .build();
        assertArrayEquals("default 'extraArgs' from 'drm_test.yaml'", 
                new String[]{"-P", "gpdev" },
                drmJobSubmission.getExtraArgs().toArray());
    }
    
    @Test
    public void testCustomValue() {
        DrmJobSubmission drmJobSubmission = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .build();
        assertArrayEquals("'customValue' from 'drm_test.yaml'", 
                new String[]{"A", "B", "C"},
                drmJobSubmission.getValue("customValue").getValues().toArray());
    }

    
    @Test(expected=IllegalArgumentException.class)
    public void testNullJobContext() {
        new DrmJobSubmission.Builder(workingDir).build();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullJobInfo() {
        jobContext=new GpContext.Builder()
            .jobNumber(jobNo)
            .build();
        new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .build();        
    }
    
    @Test
    public void testLogfileAbsolute() throws IOException {
        File allLogFiles = temp.newFolder("allLogFiles");
        File absLogFile = new File(allLogFiles, jobNo+"_lsfLog.txt").getAbsoluteFile();
        DrmJobSubmission job=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .logFile(absLogFile)
        .build();
        
        assertEquals(absLogFile.getAbsolutePath(), job.getLogFile().getAbsolutePath());
    }
    
    @Test
    public void testLogfilenameRelative() {
        DrmJobSubmission job=new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .logFilename(".lsf.out")
        .build();
        
        assertEquals(
                new File(workingDir, ".lsf.out").getAbsolutePath(), 
                job.getRelativeFile(job.getLogFile()).getAbsolutePath()); 
    }
    

}

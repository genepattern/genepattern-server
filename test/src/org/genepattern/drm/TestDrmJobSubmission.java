package org.genepattern.drm;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.job.input.configparam.JobConfigParams;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.After;
import org.junit.Assert;
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
        Assert.assertEquals("jobNo", jobNo, job.getGpJobNo());
        Assert.assertEquals("jobNo", jobNo.intValue(), job.getJobInfo().getJobNumber());
        Assert.assertEquals("workingDir", workingDir.getAbsolutePath(), job.getWorkingDir().getAbsolutePath());
        Assert.assertEquals("commandLine.length", cmdLineArgs.length, job.getCommandLine().size());
        Assert.assertEquals("arg[0]", cmdLineArgs[0], job.getCommandLine().get(0));
        Assert.assertEquals("arg[1]", cmdLineArgs[1], job.getCommandLine().get(1));
        
        Assert.assertEquals("env.length", 0, job.getEnvironmentVariables().size());
        try {
            job.getEnvironmentVariables().put("newArg", "newValue");
            ///CLOVER:OFF
            Assert.fail("environmentVariables should be unmodifiable");
            ///CLOVER:ON
        }
        catch (UnsupportedOperationException e) {
            //expected
        }
        try {
            job.getCommandLine().add("-P");
            ///CLOVER:OFF
            Assert.fail("commandLine should be unmodifiable");
            ///CLOVER:ON
        }
        catch (UnsupportedOperationException e) {
            //expected
        }

        Assert.assertNull("job.queue", job.getQueue()); 
        Assert.assertNull("job.walltime", job.getWalltime()); 
        Assert.assertNull("job.nodeCount", job.getNodeCount()); 
        Assert.assertNull("job.cpuCount", job.getCpuCount()); 
        Assert.assertEquals("job.extraArgs.size", 2, job.getExtraArgs().size());        
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testNullCommandLine() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .build();
        Assert.assertEquals("Expecting emtpy list", 0, drmJob.getCommandLine().size());
        //should throw an exception
        drmJob.getCommandLine().add("-P");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEmptyCommandLine() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .commandLine(new String[0])
            .build();
        Assert.assertEquals("Expecting emtpy list", 0, drmJob.getCommandLine().size());
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
        Assert.assertEquals("jobNo", jobNo.intValue(), drmJob.getJobInfo().getJobNumber());
        Assert.assertEquals("workingDir", workingDir.getAbsolutePath(), drmJob.getWorkingDir().getAbsolutePath());
        Assert.assertThat("commandLine", Arrays.asList(cmdLineArgs), is(drmJob.getCommandLine()));
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
//            Assert.fail("job.extraArgs should be unmodifiable");
//            ///CLOVER:ON
//        }
//        catch (UnsupportedOperationException e) {
//            //expected
//        }
//        Assert.assertEquals("job.extraArgs.size", 2, drmJob.getExtraArgs().size());
//        Assert.assertEquals("job.extraArgs[0]", "-P", drmJob.getExtraArgs().get(0));
//        Assert.assertEquals("job.extraArgs[1]", "ProjectName", drmJob.getExtraArgs().get(1));
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
            Assert.fail("job.environmentVariables should be unmodifiable");
            ///CLOVER:ON
        }
        catch (UnsupportedOperationException e) {
            //expected
        }
        Assert.assertEquals("env.length", 2, drmJob.getEnvironmentVariables().size());
        Assert.assertEquals("env['ANT_OPTS']", "-Xmx2048m", drmJob.getEnvironmentVariables().get("ANT_OPTS"));
        Assert.assertEquals("env['JAVA_OPTS']", "-XX:MaxPermSize=2g -Xmx2g", drmJob.getEnvironmentVariables().get("JAVA_OPTS"));
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
        
        Assert.assertEquals("env.length", 2, drmJob.getEnvironmentVariables().size());
        Assert.assertEquals("env['ANT_OPTS']", "-Xmx2048m", drmJob.getEnvironmentVariables().get("ANT_OPTS"));
        Assert.assertEquals("env['JAVA_OPTS']", "-XX:MaxPermSize=2g -Xmx2g", drmJob.getEnvironmentVariables().get("JAVA_OPTS"));
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

        Assert.assertEquals("job.javaXmx", "2gb", drmJob.getProperty("job.javaXmx"));
    }
    
    @Test
    public void testExecutorCustomProperties() {
        final GpConfig config=ServerConfigurationFactory.instance();
        final GpContext userContext=GpContext.getContextForUser("test_user");
        //sanity check
        Assert.assertEquals("executor.props", "PbsBigMem", config.getGPProperty(userContext, "executor.props")); 
        final String executorId=config.getGPProperty(userContext, "executor");
        Assert.assertEquals("executor", "DemoPbsJobRunner", executorId);
    }
    
    @Test
    public void testCustomJobConfigParams() {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext userContext=GpContext.getContextForUser("test_user");
        
        Assert.assertEquals("executor.props for test_user", 
                "PbsBigMem",  
                gpConfig.getGPProperty(userContext, "executor.props"));
        
        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, userContext);
        Assert.assertNotNull("initJobConfigParams was null", jobConfigParams);
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
        
        Assert.assertEquals("set memory in config", Memory.fromString("8g"), 
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
        
        Assert.assertEquals("set walltime in config", 
                Walltime.fromString("7-00:00:00"),
                drmJobSubmission.getWalltime());
    }
    
    @Test
    public void testExtraArgs() {
        DrmJobSubmission drmJobSubmission = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .build();
        Assert.assertArrayEquals("default 'extraArgs' from 'drm_test.yaml'", 
                new String[]{"-P", "gpdev" },
                drmJobSubmission.getExtraArgs().toArray());
    }
    
    @Test
    public void testCustomValue() {
        DrmJobSubmission drmJobSubmission = new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .build();
        Assert.assertArrayEquals("'customValue' from 'drm_test.yaml'", 
                new String[]{"A", "B", "C"},
                drmJobSubmission.getValue("customValue").getValues().toArray());
    }

    
    @Test(expected=IllegalArgumentException.class)
    public void testNullJobContext() {
        new DrmJobSubmission.Builder(workingDir).build();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testNullJobInfo() {
        jobContext=new GpContextFactory.Builder()
            .jobNumber(jobNo)
            .build();
        new DrmJobSubmission.Builder(workingDir)
            .jobContext(jobContext)
            .build();        
    }

}

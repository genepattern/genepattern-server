package org.genepattern.drm;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.config.Value;
import org.genepattern.server.job.input.configparam.JobConfigParams;
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
    private static final Integer jobNo=1;
    private static final String[] commandLine={"echo", "Hello, World!"};
    
    private File jobResults;
    private File workingDir;

    @Before
    public void before() throws Throwable {
        ConfigUtil.loadConfigFile(this.getClass(), "drm_test.yaml");
        if (ServerConfigurationFactory.instance().getInitializationErrors().size()>0) {
            throw ServerConfigurationFactory.instance().getInitializationErrors().get(0);
        }

        this.jobResults=temp.newFolder("jobResults");
        this.workingDir=new File(jobResults, ""+jobNo);
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
        final DrmJobSubmission job = new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(commandLine)
            .build();
        Assert.assertEquals("jobNo", jobNo, job.getGpJobNo());
        Assert.assertEquals("jobNo", jobNo.intValue(), job.getJobInfo().getJobNumber());
        Assert.assertEquals("workingDir", workingDir.getAbsolutePath(), job.getWorkingDir().getAbsolutePath());
        Assert.assertEquals("commandLine.length", commandLine.length, job.getCommandLine().size());
        Assert.assertEquals("arg[0]", commandLine[0], job.getCommandLine().get(0));
        Assert.assertEquals("arg[1]", commandLine[1], job.getCommandLine().get(1));
        
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
        Assert.assertNull("job.queue", job.getQueue()); 
        Assert.assertNull("job.memory", job.getMemory()); 
        Assert.assertNull("job.walltime", job.getWalltime()); 
        Assert.assertNull("job.nodeCount", job.getNodeCount()); 
        Assert.assertNull("job.cpuCount", job.getCpuCount()); 
        Assert.assertEquals("job.extraArgs.size", 0, job.getExtraArgs().size());        
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullCommandLine() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(jobNo, workingDir)
            .build();
        ///CLOVER:OFF
        Assert.assertNull("Expecting IllegalArgumentException", drmJob);
        Assert.fail("Expecting IllegalArgumentException");
        ///CLOVER:ON
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCommandLine() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(new String[0])
            .build();
        ///CLOVER:OFF
        Assert.assertNull("Expecting IllegalArgumentException", drmJob);
        Assert.fail("Expecting IllegalArgumentException");
        ///CLOVER:ON
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateCommandLine() {
        new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(commandLine)
            .commandLine(commandLine);
    }

    @Test
    public void testDefaultBuilder_addArg() {
        final DrmJobSubmission drmJob;
        DrmJobSubmission.Builder builder = new DrmJobSubmission.Builder(jobNo, workingDir);
        for(final String arg : commandLine) {
            builder=builder.addArg(arg);
        }
        drmJob=builder.build();
        Assert.assertEquals("jobNo", jobNo.intValue(), drmJob.getJobInfo().getJobNumber());
        Assert.assertEquals("workingDir", workingDir.getAbsolutePath(), drmJob.getWorkingDir().getAbsolutePath());
        Assert.assertThat("commandLine", Arrays.asList(commandLine), is(drmJob.getCommandLine()));
    }
    
    @Test
    public void testAddExtraArg() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(commandLine)
            .addExtraArg("-P")
            .addExtraArg("ProjectName")
            .build();

        try {
            drmJob.getExtraArgs().add("--extraArg=value");
            ///CLOVER:OFF
            Assert.fail("job.extraArgs should be unmodifiable");
            ///CLOVER:ON
        }
        catch (UnsupportedOperationException e) {
            //expected
        }
        Assert.assertEquals("job.extraArgs.size", 2, drmJob.getExtraArgs().size());
        Assert.assertEquals("job.extraArgs[0]", "-P", drmJob.getExtraArgs().get(0));
        Assert.assertEquals("job.extraArgs[1]", "ProjectName", drmJob.getExtraArgs().get(1));
    }

    @Test
    public void testEnvironmentVariables() {
        final Map<String,String> envIn=new HashMap<String,String>();
        envIn.put("ANT_OPTS", "-Xmx2048m");
        envIn.put("JAVA_OPTS", "-XX:MaxPermSize=2g -Xmx2g");
        
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(commandLine)
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
        new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(commandLine)
            .environmentVariables(envIn)
            .environmentVariables(envIn);
    }

    @Test
    public void testAddEnvVar() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(commandLine)
            .addEnvVar("ANT_OPTS", "-Xmx2048m")
            .addEnvVar("JAVA_OPTS", "-XX:MaxPermSize=2g -Xmx2g")
            .build();
        
        Assert.assertEquals("env.length", 2, drmJob.getEnvironmentVariables().size());
        Assert.assertEquals("env['ANT_OPTS']", "-Xmx2048m", drmJob.getEnvironmentVariables().get("ANT_OPTS"));
        Assert.assertEquals("env['JAVA_OPTS']", "-XX:MaxPermSize=2g -Xmx2g", drmJob.getEnvironmentVariables().get("JAVA_OPTS"));
    }
    
    /**
     * Test case for customizing the job config by setting the 'workerName' property.
     * Example entry in config file,
     * <pre>
executors:
    DemoPbsJobRunner:
        classname: org.genepattern.server.executor.drm.JobExecutor
        configuration.properties:
             jobRunnerClassname: org.genepattern.drm.impl.iu.pbs.DemoPbsJobRunner
             jobRunnerName: DemoPbsJobRunner
             lookupType: DB
             #lookupType: HASHMAP
        default.properties:
            job.queue: "defaultQueue"
            job.walltime: "02:00:00"
            job.nodeCount: "1"

            pbs.host: "example.edu"
            pbs.mem: "8gb"
            pbs.ppn: "8"
            pbs.cput: ""
            pbs.vmem: "64gb"

            # himem job.workerName
            myHiMemPbsWorker: {
                job.queue: "exampleQueue",
                job.walltime: "02:00:00",
                job.nodeCount: "1",
                pbs.host: "example.edu",
                pbs.mem: "8gb",
                pbs.cput: "",
                pbs.vmem: "500gb"
            }

            myLongPbsWorker: {
                job.queue: "exampleQueue",
                job.walltime: "72:00:00",
                job.nodeCount: "1",
                pbs.host: "example.edu",
                pbs.mem: "8gb",
                pbs.ppn: "8",
                pbs.cput: "",
                pbs.vmem: "64gb"
            }

     * </pre>
     */
    @Test
    public void testWorkerConfig() {
        Map<String,String> workerConfig=new HashMap<String,String>();
        workerConfig.put("job.queue", "exampleQueue");
        workerConfig.put("job.walltime", "72:00:00");
        workerConfig.put("job.nodeCount", "1");
        workerConfig.put("pbs.host", "example.edu");
        workerConfig.put("pbs.mem", "8gb");
        workerConfig.put("pbs.ppn", "8");
        workerConfig.put("pbs.cput", "");
        workerConfig.put("pbs.vmem", "64gb");
        
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(commandLine)
            .workerName("myLongPbsWorker")
            .workerConfig(workerConfig)
            .build();
        
        Assert.assertEquals("job.queue", "exampleQueue", drmJob.getProperty("job.queue"));
        Assert.assertEquals("job.walltime", "72:00:00", drmJob.getProperty("job.walltime"));
        Assert.assertEquals("job.nodeCount", "1", drmJob.getProperty("job.nodeCount"));
        Assert.assertEquals("pbs.host", "example.edu", drmJob.getProperty("pbs.host"));
        Assert.assertEquals("pbs.mem", "8gb", drmJob.getProperty("pbs.mem"));
        Assert.assertEquals("pbs.ppn", "8", drmJob.getProperty("pbs.ppn"));
        Assert.assertEquals("pbs.cput", "", drmJob.getProperty("pbs.cput"));
        Assert.assertEquals("pbs.vmem", "64gb", drmJob.getProperty("pbs.vmem"));
    }
    
    @Test
    public void testGetGpConfigProperty() {
        final DrmJobSubmission drmJob = new DrmJobSubmission.Builder(jobNo, workingDir)
            .commandLine(commandLine)
            .workerName("myLongPbsWorker")
            .build();
        
        final GpConfig config=ServerConfigurationFactory.instance();
        final GpContext userContext=GpContext.getContextForUser("test_user");
        Value javaXmx=config.getValue(userContext, "java.Xmx");

        Assert.assertEquals("java.Xmx", "2gb", drmJob.getProperty("java.Xmx"));
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

}

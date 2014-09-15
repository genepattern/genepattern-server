package org.genepattern.server.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.auth.GroupMembershipWrapper;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.auth.XmlGroupMembership;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpConfigLoader;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.config.Value;
import org.genepattern.webservice.JobInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for the CommandManagerFactory.
 * 
 * @author pcarr
 */
public class CommandManagerFactoryTest {
    private IGroupMembershipPlugin groupInfo;
    
    @Before
    public void setUp() throws Exception {
        File userGroups=FileUtil.getSourceFile(this.getClass(), "userGroups.xml");
        // wrapper adds the '*' wildcard group
        groupInfo=new GroupMembershipWrapper(
                new XmlGroupMembership(userGroups));

    }
    
    @After
    public void tearDown() throws Exception {
    }
    
    /**
     * assertions for all instance of CommandManager, can be called from all test cases.
     * @param cmdMgr
     */
    private static void validateCommandManager(final GpConfig gpConfig, final BasicCommandManager cmdMgr) {
        final boolean checkErrors=true;
        validateCommandManager(gpConfig, cmdMgr, checkErrors);
    }

    private static void validateCommandManager(final GpConfig gpConfig, final BasicCommandManager cmdMgr, final boolean checkErrors) {
        assertNotNull("Expecting non-null cmdMgr", cmdMgr);

        if (checkErrors) {
            //List<Throwable> errors = ServerConfigurationFactory.instance().getInitializationErrors();
            List<Throwable> errors = gpConfig.getInitializationErrors();
            if (errors != null && errors.size() > 0) {
                String errorMessage = "CommandManagerFactory initialization error, num="+errors.size();
                Throwable first = errors.get(0);
                if (first != null) {
                    errorMessage += " error[0]="+first.getMessage();
                }
                fail(errorMessage);
            }
        }
        
        assertNotNull("Expecting non-null cmdMgr.commandExecutorsMap", cmdMgr.getCommandExecutorsMap());
        //prove that the map is not modifiable
        try {
            cmdMgr.getCommandExecutorsMap().put("test", (CommandExecutor)null);
            fail("commandExecutorsMap should be unmodifiable");
        }
        catch (Exception e) {
            //expecting an exception to be thrown
        }
    }

    /**
     * assertions for the default CommandManager, when no custom configuration is found.
     * @param cmdMgr
     */
    private static void validateDefaultConfig(final GpConfig gpConfig, final BasicCommandManager cmdMgr) {
        final boolean checkErrors=true;
        validateDefaultConfig(gpConfig, cmdMgr, checkErrors);
    }

    private static void validateDefaultConfig(final GpConfig gpConfig, final BasicCommandManager cmdMgr, final boolean checkErrors) {
        validateCommandManager(gpConfig, cmdMgr, checkErrors);
        assertEquals("By default, expecting only one CommandExecutor", 1, cmdMgr.getCommandExecutorsMap().size());
    }

    /**
     * Test that the default command manager is loaded when no additional configuration is provided.
     */
    @Test
    public void defaultConfig() {
        File resourcesDir=new File("resources");
        GpConfig gpConfig=GpConfigLoader.createFromResourcesDir(resourcesDir);
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);
        validateDefaultConfig(gpConfig, cmdMgr);
    }

    /**
     * Test that the default command manager is loaded when initialized with null gpConfig.
     */
    @Test
    public void nullGpConfig() {
        BasicCommandManager cmdMgr=CommandManagerFactory.createCommandManager(null);
        validateDefaultConfig(null, cmdMgr, false);
    }

    /**
     * Test that the default command manager is loaded when there are initialization errors.
     */
    @Test
    public void initializationErrors() {
        File configFile=FileUtil.getSourceFile(this.getClass(), "filenotfound.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        assertEquals("expecting initialization errors", 1, gpConfig.getInitializationErrors().size());
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);
        validateDefaultConfig(gpConfig, cmdMgr, false);
    }
    
    @Test
    public void nullJobConfiguration() {
        GpConfig gpConfig=Mockito.mock(GpConfig.class);
        Mockito.when(gpConfig.getJobConfiguration()).thenReturn(null);
        assertNull("expecting null jobConfiguration", gpConfig.getJobConfiguration());
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);
        validateDefaultConfig(gpConfig, cmdMgr);
    }
    
    /**
     * Parse the 'config_example.yaml' file which ships with the GenePattern installer.
     */
    @Test
    public void configExample() {
        File configFile=new File("resources/config_example.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);
        validateExampleJobConfig(gpConfig, cmdMgr);
    }

    private void validateExampleJobConfig(final GpConfig gpConfig, final BasicCommandManager cmdMgr) {
        assertNotNull("Expecting non-null cmdMgr", cmdMgr);
        validateCommandManager(gpConfig, cmdMgr);
        
        Map<String,CommandExecutor> map = cmdMgr.getCommandExecutorsMap();
        assertNotNull("Expecting non-null cmdMgr.commandExecutorsMap", map);
        int numExecutors = map.size();
        assertEquals("Number of executors", 4, numExecutors);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setTaskName("SNPFileSorter");
        jobInfo.setTaskLSID("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00096:1");
        
        try {
            CommandExecutor cmdExecutor = cmdMgr.getCommandExecutor(gpConfig, jobInfo);
            String canonicalName = cmdExecutor.getClass().getCanonicalName();
            assertEquals("expecting LsfCommandExecutor for SNPFileSorter", "org.genepattern.server.executor.drm.JobExecutor", canonicalName);
            //assertTrue("expecting LsfCommandExecutor for SNPFileSorter but found "+cmdExecutor.getClass().getCanonicalName()+" instead", (cmdExecutor instanceof LsfCommandExecutor));
        }
        catch (Exception e) {
            fail("Exception thrown in getCommandExecutor: "+e.getLocalizedMessage());
        }
        CommandProperties jobProperties = cmdMgr.getCommandProperties(gpConfig, jobInfo);
        assertNotNull("", jobProperties);
        assertEquals("checking job properties: lsf.max.memory", "12", ""+jobProperties.getProperty("lsf.max.memory"));
        assertEquals("checking job properties: java_flags", "-Xmx12g", jobProperties.getProperty("java_flags"));
        assertEquals("checking job properties: job.project", "genepattern", jobProperties.getProperty("job.project"));
        assertEquals("checking job properties: job.queue", "genepattern", jobProperties.getProperty("job.queue"));
        assertEquals("checking job properties: job.logFile", ".lsf.out", jobProperties.getProperty("job.logFile"));
        //assertEquals("checking job properties: lsf.use.pre.exec.command", "false", ""+jobProperties.getProperty("lsf.use.pre.exec.command"));
        //assertNull("checking job properties: lsf.extra.bsub.args", jobProperties.getProperty("lsf.extra.bsub.args"));
    }

    /**
     * Test default and custom properties in yaml configuration file.
     */
    @Test
    public void testYamlConfig() {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_config.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
            .groupInfo(groupInfo)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);
        validateCommandManager(gpConfig, cmdMgr);
        assertEquals("Expecting 2 executors", 2, cmdMgr.getCommandExecutorsMap().size() );
        validateYamlConfig(gpConfig, cmdMgr);
        
        //additional tests for user and group properties
        assertTrue("userA is in admingroup", groupInfo.isMember("userA", "admingroup"));
        assertTrue("userA is in broadgroup", groupInfo.isMember("userA", "broadgroup"));
        assertTrue("adminuser is in admingroup", groupInfo.isMember("adminuser", "admingroup"));

        String userId = "adminuser";
        String taskName = "ComparativeMarkerSelection";
        String taskLsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:6";
        String expectedCmdExecId = "Test";
        String expectedFilename = "stdout_admingroup.out";
        boolean expectingException = false;
        
        validateJobConfig(gpConfig, cmdMgr,
                userId,
                taskName, 
                taskLsid, 
                expectedCmdExecId, 
                expectedFilename, 
                expectingException);
    }

    /**
     * Test default and custom properties in yaml configuration file which has no indenting.
     */
    @Test
    public void testYamlConfigNoIndent() {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_config_noindent.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);
        validateCommandManager(gpConfig, cmdMgr);
        assertEquals("Expecting 2 executors", 2, cmdMgr.getCommandExecutorsMap().size() );
        validateYamlConfig(gpConfig, cmdMgr);
    }

    private void validateYamlConfig(GpConfig gpConfig, BasicCommandManager cmdMgr) {
        String userId = "test_user";
        String taskName = "ConvertLineEndings";
        String taskLsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
        String expectedCmdExecId = "Test";
        String expectedFilename = "stdout.txt";
        boolean expectingException = false;

        validateJobConfig(gpConfig, cmdMgr,
                userId,
                taskName, 
                taskLsid, 
                expectedCmdExecId, 
                expectedFilename, 
                expectingException);
        
        validateJobConfig(gpConfig, cmdMgr,
                userId,
                "ComparativeMarkerSelection", 
                "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:2",
                "RuntimeExec",
                "CMS.out",
                false);
        
        validateJobConfig(gpConfig, cmdMgr,
                userId,
                "ComparativeMarkerSelection",
                "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:4",
                "RuntimeExec",
                "CMS.v4.out",
                false);

        validateJobConfig(gpConfig, cmdMgr,
                userId,
                "ComparativeMarkerSelection",
                "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:5",
                "Test",
                "stdout.txt",
                false);

        validateJobConfig(gpConfig, cmdMgr,
                userId,
                "moduleA",
                (String)null,
                "RuntimeExec",
                "runtimeexec.out",
                false);
        
        validateJobConfig(gpConfig, cmdMgr,
                userId,
                "moduleB",
                (String)null,
                "Test",
                "moduleB.out",
                false);

        validateJobConfig(gpConfig, cmdMgr,
                userId,
                "moduleC",
                (String)null,
                "Test",
                "stdout.txt",
                true);
    }
    
    private static void validateJobConfig(GpConfig gpConfig, BasicCommandManager cmdMgr, String userId, String taskName, String taskLsid, String expectedCmdExecId, String exepectedStdoutFilename, boolean expectingException) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId(userId);
        jobInfo.setTaskName(taskName);
        jobInfo.setTaskLSID(taskLsid);
        
        CommandExecutor cmdExec = null;
        try {
            cmdExec = cmdMgr.getCommandExecutor(gpConfig, jobInfo);
            if (expectingException) {
                fail("Expecting CommandExecutorNotFoundException, but it wasn't thrown");
            }
        }
        catch (CommandExecutorNotFoundException e) {
            if (!expectingException) {
                fail(""+e.getLocalizedMessage());
            }
            return;
        }
        CommandProperties cmdProps = cmdMgr.getCommandProperties(gpConfig, jobInfo);
        String cmdExecId = cmdMgr.getCommandExecutorId(cmdExec);

        assertNotNull("expecting non-null CommandExecutor", cmdExec);
        assertEquals("expecting default CommandExecutor", expectedCmdExecId, cmdExecId);
        assertNotNull("expecting non-null Properties", cmdProps);
        assertEquals("checking # of properties", 3, cmdProps.size());

        assertEquals("checking 'executor' property", expectedCmdExecId, cmdProps.getProperty("executor"));
        assertEquals("checking 'stdout.filename' property", exepectedStdoutFilename, cmdProps.getProperty("stdout.filename"));
        assertEquals("checking 'java_flags' property", "-Xmx4g", cmdProps.getProperty("java_flags"));
    }
       
    @Test
    public void testLsfConfig() {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_config_lsf.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);
        
        JobInfo jobInfo = new JobInfo();
        jobInfo.setTaskName("ComparativeMarkerSelection");
        jobInfo.setTaskLSID("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:5");
        CommandProperties props = cmdMgr.getCommandProperties(jobInfo);
        assertEquals("checking 'lsf.output.filename'", ".lsf.out", props.getProperty("lsf.output.filename"));
    }
    
    @Test
    public void testReloadConfiguration() throws CommandExecutorNotFoundException {
        //ServerConfigurationFactory.reloadConfiguration("test_config.yaml");
        //ServerConfigurationFactory.reloadConfiguration("test_config_reload.yaml");
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_config_reload.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
            .groupInfo(groupInfo)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);

        
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("testuser");
        jobInfo.setTaskName("PreprocessDataset");
        jobInfo.setTaskLSID(null);
        
        CommandExecutor cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        CommandProperties cmdProps = cmdMgr.getCommandProperties(jobInfo);
        String cmdExecId = cmdMgr.getCommandExecutorId(cmdExec);

        assertEquals("changed default java_flags", "-Xmx16g", cmdProps.getProperty("java_flags"));
        assertEquals("changed default executor", "RuntimeExec", cmdExecId);
        assertEquals("changed default stdout.filename for RuntimeExec", "runtimeexec_modified.out", cmdProps.getProperty("stdout.filename"));
        
        jobInfo = new JobInfo();
        jobInfo.setUserId("testuser");
        jobInfo.setTaskName("ComparativeMarkerSelection");
        cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        cmdProps = cmdMgr.getCommandProperties(jobInfo);
        cmdExecId = cmdMgr.getCommandExecutorId(cmdExec);
        
        assertEquals("set java_flags for ComparativeMarkerSelection", "-Xmx2g", cmdProps.getProperty("java_flags"));
        assertEquals("executor for ComparativeMarkerSelection", "RuntimeExec", cmdExecId);
    }
    
    /**
     * Check properties set in the 'default.properties' section for a given executor.
     * @throws CommandExecutorNotFoundException
     */
    @Test
    public void testExecutorDefaultProperties() throws CommandExecutorNotFoundException {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_executor_defaults.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("admin");
        jobInfo.setTaskName("testEchoSleeper");
        
        CommandExecutor cmdExec = cmdMgr.getCommandExecutor(gpConfig, jobInfo);
        CommandProperties cmdProps = cmdMgr.getCommandProperties(gpConfig, jobInfo);
        
        assertEquals("Expecting LSF", "LSF", cmdMgr.getCommandExecutorId(cmdExec));
        assertEquals("checking property set in executors->LSF->default.properties ", ".lsf.out", cmdProps.getProperty("lsf.output.filename"));
        assertEquals("checking property override in executors->LSF->default.properties ", "-Xmx4g", cmdProps.getProperty("java_flags"));
    }
    
    /**
     * Unit tests to validate setting a null value.
     */
    @Test
    public void testNullValues() throws CommandExecutorNotFoundException {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_null_values.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("test");
        jobInfo.setTaskName("testEchoSleeper");
        
        CommandExecutor cmdExec = cmdMgr.getCommandExecutor(gpConfig, jobInfo);
        CommandProperties cmdProps = cmdMgr.getCommandProperties(gpConfig, jobInfo);
        assertEquals("Expecting LSF", "LSF", cmdMgr.getCommandExecutorId(cmdExec));
        assertEquals("default.properties->debug.mode", "true", cmdProps.getProperty("debug.mode"));
        
        jobInfo = new JobInfo();
        jobInfo.setUserId("adminuser");
        jobInfo.setTaskName("testEchoSleeper");
        cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        cmdProps = cmdMgr.getCommandProperties(jobInfo);

        assertEquals("Expecting RuntimeExec", "RuntimeExec", cmdMgr.getCommandExecutorId(cmdExec));
        assertEquals("", cmdProps.getProperty("debug.mode"));
        
        jobInfo = new JobInfo();
        jobInfo.setUserId("testuser");
        jobInfo.setTaskName("testEchoSleeper");
        cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        cmdProps = cmdMgr.getCommandProperties(jobInfo);
        assertEquals("Expecting LSF", "LSF", cmdMgr.getCommandExecutorId(cmdExec));
        assertEquals("Expecting empty string", "", cmdProps.getProperty("debug.mode"));
    }
    
    /**
     * Unit tests for custom pipeline executors.
     */
    @Test
    public void testCustomPipelineExecutor() {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_custom_pipeline_executor.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);

        assertEquals("# of command executors", 2, cmdMgr.getCommandExecutorsMap().size());
        CommandExecutor runtimeExec=cmdMgr.getCommandExecutorsMap().get("RuntimeExec");
        CommandExecutor pipelineExec=cmdMgr.getCommandExecutorsMap().get("PipelineExec");
        
        assertNotNull("runtimeExec", runtimeExec);
        assertNotNull("pipelineExec", pipelineExec);
        assertEquals("custom PipelineExec class", CustomPipelineExecutor.class.getName(), pipelineExec.getClass().getName());
    }
    
    @Test
    public void testCommandProperties() {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_CommandProperties.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("admin");
        
        CommandProperties cmdProps = cmdMgr.getCommandProperties(gpConfig, jobInfo);
        Value val = cmdProps.get("prop.not.set");
        assertNull("Expecting 'null' value when property is not in the config file", val);

        testCommandProperty(cmdProps, "arg.empty", (String) null, new String[] { null });
        testCommandProperty(cmdProps, "arg.null", (String) null, new String[] { null });
        testCommandProperty(cmdProps, "arg.list.empty", (String) null, new String[] {});
        testCommandProperty(cmdProps, "arg.list.null", (String) null, new String[] { null });

        testCommandProperty(cmdProps, "arg.list.01.a", "item01", new String[] { "item01" });
        testCommandProperty(cmdProps, "arg.list.01.b", "item01", new String[] { "item01" });
        testCommandProperty(cmdProps, "arg.list.02", "item01", new String[] { "item01", "item02" });
        testCommandProperty(cmdProps, "arg.list.03", "-X", new String[] { "-X", "4", "3.14", "1.32e6", "true", "false" });
        testCommandProperty(cmdProps, "arg.list.04", "-X", new String[] { "-X", "4", "3.14", "1.32e6", "true", "false", null, "null" });
        
        //special-case, the parser uses the toString method to convert from Number or Boolean
        testCommandProperty(cmdProps, "arg.err.int", "4", new String[] { "4" });
        testCommandProperty(cmdProps, "arg.err.number.a", "3.14", new String[] { "3.14" });
        testCommandProperty(cmdProps, "arg.err.number.b", "1.32E-6", new String[] { "1.32E-6" });
        testCommandProperty(cmdProps, "arg.err.boolean.true", "true", new String[] { "true" });
        testCommandProperty(cmdProps, "arg.err.boolean.false", "false", new String[] { "false" });

        //test executor -> default.properties
        jobInfo.setUserId("test");
        cmdProps = cmdMgr.getCommandProperties(gpConfig, jobInfo);
        val = cmdProps.get("arg.override.to.null");
        assertNull("executor.default.properties: Override default property to 'null'", val.getValue());
        testCommandProperty(cmdProps, "arg.empty", (String) null, new String[] { null });
        testCommandProperty(cmdProps, "arg.null", (String) null, new String[] { null });
        testCommandProperty(cmdProps, "arg.list.empty", (String) null, new String[] {});
        testCommandProperty(cmdProps, "arg.list.null", (String) null, new String[] { null });

        testCommandProperty(cmdProps, "arg.list.01.a", "overrideItem01", new String[] { "overrideItem01" });
        testCommandProperty(cmdProps, "arg.list.01.b", "overrideItem01", new String[] { "overrideItem01" });
        testCommandProperty(cmdProps, "arg.list.02", "overrideItem01", new String[] { "overrideItem01", "overrideItem02" });
        testCommandProperty(cmdProps, "arg.list.03", "userOverride.x", new String[] { "userOverride.x", "uo4", "uo3.14", "uo1.32e6", "uoTrue", "uoFalse" });

        //error case
        testCommandProperty(cmdProps, "arg.err.int", "4", new String[] { "4" });
        testCommandProperty(cmdProps, "arg.err.number.a", "3.14", new String[] { "3.14" });
        testCommandProperty(cmdProps, "arg.err.number.b", "1.32E-6", new String[] { "1.32E-6" });
        testCommandProperty(cmdProps, "arg.err.boolean.true", "true", new String[] { "true" });
        testCommandProperty(cmdProps, "arg.err.boolean.false", "false", new String[] { "false" });
        
        //test module.properties
        jobInfo.setUserId("admin");
        jobInfo.setTaskName("module01");
        cmdProps = cmdMgr.getCommandProperties(gpConfig, jobInfo);

        val = cmdProps.get("arg.override.to.null");
        assertNull("module.default.properties: Override default property to 'null'", val.getValue());
        testCommandProperty(cmdProps, "arg.list.03", "moduleOverride.x", new String[] { "moduleOverride.x", "mo4", "mo3.14", "mo1.32e6", "moTrue", "moFalse" });
    }
    
    @Test
    public void testUserProperties() {

        File configFile=FileUtil.getSourceFile(this.getClass(), "test_user_properties.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(
                    new GpServerProperties.Builder()
                        .useSystemProperties(true)
                        .initFromSystemProperties(false)
                    .build()
            )
            .configFile(configFile)
            .groupInfo(groupInfo)
        .build();

        System.setProperty("system.prop", "SYSTEM");
        System.setProperty("system.prop.override", "SYSTEM_VALUE");
        System.setProperty("system.prop.override.to.null", "NOT_NULL");

        assertTrue("userA is in admingroup", groupInfo.isMember("userA", "admingroup"));
        assertTrue("userA is in broadgroup", groupInfo.isMember("userA", "broadgroup"));
        assertTrue("userC is in broadgroup", groupInfo.isMember("userC", "broadgroup"));
        assertFalse("userC is not in admingroup", groupInfo.isMember("userC", "admingroup"));
        assertTrue("adminuser is in admingroup", groupInfo.isMember("adminuser", "admingroup"));

        
        //ServerConfigurationFactory.reloadConfiguration();

        //tests for 'test' user, use 'default.properties', no overrides
        GpContext userContext = GpContext.getContextForUser("test");
        assertNull("unset property",  gpConfig.getGPProperty(userContext, "NOT_SET"));
        assertEquals("property which is only set in System.properties", 
                "SYSTEM", gpConfig.getGPProperty(userContext, "system.prop"));
        assertEquals("override a system property", 
                "SERVER_DEFAULT", 
                gpConfig.getGPProperty(userContext, "system.prop.override"));
        assertNull(gpConfig.getGPProperty(userContext, "override a system property with a null value"));
        assertEquals("default property", "DEFAULT_VAL", gpConfig.getGPProperty(userContext, "default.prop"));
        assertNull("default null value", gpConfig.getGPProperty(userContext, "default.prop.null"));

        //tests for 'userA', with group overrides, userA is in two groups
        userContext = GpContext.getContextForUser("userA");
        assertEquals("override default prop in group.properties", "admingroup_val", gpConfig.getGPProperty(userContext, "default.prop"));
        
        //tests for 'userC', with group overrides, userC is in one group
        userContext = GpContext.getContextForUser("userC");
        assertEquals("user override", "userC_val", gpConfig.getGPProperty(userContext, "default.prop") );
        assertEquals("group override", "-Xmx256m -Dgroup=broadgroup", gpConfig.getGPProperty(userContext, "java_flags"));
        
        //tests for 'userD' with user overrides, userD is not in any group
        userContext = GpContext.getContextForUser("userD");
        assertEquals("user override", "userD_val", gpConfig.getGPProperty(userContext, "default.prop"));
    }
    
    @Test
    public void testOverrideSystemProperty() {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_user_properties.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .configFile(configFile)
            .groupInfo(groupInfo)
            .serverProperties(new GpServerProperties.Builder()
                .useSystemProperties(false)
            .build())
        .build();
        
        System.setProperty("system.prop.override", "SYSTEM_VALUE");
        GpContext userContext = GpContext.getContextForUser("test");
        assertEquals("override a system property", 
                "SERVER_DEFAULT", 
                gpConfig.getGPProperty(userContext, "system.prop.override"));
    }
    
    /**
     * Test getProperty.
     */
    @Test
    public void testServerProperties() {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_user_properties.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(
                    new GpServerProperties.Builder()
                        .initFromSystemProperties(true)
                        .gpProperties(FileUtil.getSourceFile(this.getClass(), "genepattern.properties"))
                        .customProperties(FileUtil.getSourceFile(this.getClass(), "custom.properties"))
                    .build()
            )
            .resourcesDir(FileUtil.getSourceDir(this.getClass()))
            .configFile(configFile)
            .groupInfo(groupInfo)
        .build();

        GpContext context = GpContext.getServerContext();
        
        assertEquals("3.3.1", gpConfig.getGPProperty(context, "GenePatternVersion"));
        assertEquals("'lsid.authority' set in genepattern.properties file", "8080.gp-trunk-dev.120.0.0.1", gpConfig.getGPProperty(context, "lsid.authority"));
        assertEquals("true", gpConfig.getGPProperty(context, "require.password"));

        assertEquals("set_in_custom.properties", gpConfig.getGPProperty(context, "prop.test.01"));
        assertEquals("added_in_custom.properties", gpConfig.getGPProperty(context, "prop.test.02"));
        
        String userId = "admin";
        GpContext userContext = GpContext.getContextForUser(userId);
        boolean allowBatchProcess = gpConfig.getGPBooleanProperty(userContext, "allow.batch.process");
        assertEquals("testing getBooleanProperty in genepattern.properties and config.yaml", true, allowBatchProcess);
        
        assertEquals("test-case, a property whic is set in genepattern.properties, but modified in config.yaml", 
                "test.case.YAML_DEFAULT", gpConfig.getGPProperty(userContext, "prop.test.case"));
        
        //test-case, a property only in genepattern.properties
        assertEquals("test-case, property set in genepattern.properties", "SET_IN_GP_PROPERTIES", gpConfig.getGPProperty(userContext, "only.in.gp.properties"));
    }
    
    private void testCommandProperty(CommandProperties cmdProps, String propName, String expectedValue, String[] expectedValues) {
        Value val = cmdProps.get(propName);
        assertNotNull("Unexpected null value in '"+propName+"'", val);
        assertEquals("numValues in '"+propName+"'", expectedValues.length, val.getNumValues());
        assertEquals("getValue in '"+propName+"'", expectedValue, val.getValue());
        int i=0;
        for(String expectedValI : expectedValues) {
            assertEquals("getValues["+i+"] in '"+propName+"'", expectedValI, val.getValues().get(i));
            ++i;
        }
    }

    /**
     * Allow lists of strings as legal values in the configuration file.
     */
    @Test
    public void testExtraBsubArgs() throws CommandExecutorNotFoundException {
        File configFile=FileUtil.getSourceFile(this.getClass(), "test_config_lsf_extraBsubArgs.yaml");
        GpConfig gpConfig=new GpConfig.Builder()
            .resourcesDir(FileUtil.getSourceDir(this.getClass()))
            .configFile(configFile)
            .groupInfo(groupInfo)
        .build();
        BasicCommandManager cmdMgr = CommandManagerFactory.createCommandManager(gpConfig);

        List<Throwable> errors = gpConfig.getInitializationErrors();
        for(Throwable t : errors) {
            fail(""+t.getLocalizedMessage());
        }
        assertEquals("# of command executors", 6, cmdMgr.getCommandExecutorsMap().size());
                
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("admin");
        
        for(int i=1; i<=6; ++i) {
            jobInfo.setTaskName("mod0"+i);
            CommandExecutor cmdExec = cmdMgr.getCommandExecutor(jobInfo);
            assertEquals("", "exec0"+i, cmdMgr.getCommandExecutorId(cmdExec));
        }

        jobInfo.setTaskName("mod01");
        
        Value value = getValue(cmdMgr, "mod01", "lsf.extra.bsub.args");
        assertNull("lsf.extra.bsub.args not set", value);

        checkModuleProperty(cmdMgr, "mod02", (String)null);
        checkModuleProperty(cmdMgr, "mod03", (String)null);
        checkModuleProperty(cmdMgr, "mod04", "");
        checkModuleProperty(cmdMgr, "mod05", new String[] { "arg1" } );
        checkModuleProperty(cmdMgr, "mod06", new String[] { "arg1", "arg2" } );
        
        checkModuleProperty(cmdMgr, "mod10", (String)null);
        checkModuleProperty(cmdMgr, "mod11", (String)null);
        checkModuleProperty(cmdMgr, "mod12", "");
        checkModuleProperty(cmdMgr, "mod13", "arg1");
        checkModuleProperty(cmdMgr, "mod14", new String[] {});
        checkModuleProperty(cmdMgr, "mod15", new String[] { null });
        checkModuleProperty(cmdMgr, "mod16", new String[] { "" });
        checkModuleProperty(cmdMgr, "mod17", new String[] { "arg1" });
        checkModuleProperty(cmdMgr, "mod18", new String[] { "arg1", "arg2" });
    }

    private Value getValue(CommandManager cmdMgr, String taskName, String key) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setTaskName(taskName);
        CommandProperties props = cmdMgr.getCommandProperties(jobInfo);
        Value valueObj = props.get(key);
        return valueObj;
    }

    private void checkModuleProperty(CommandManager cmdMgr, String taskName, String expectedValue) {
        Value valueObj = getValue(cmdMgr, taskName, "lsf.extra.bsub.args");
        String value = valueObj.getValue();
        assertEquals(expectedValue, value);
    }

    private void checkModuleProperty(CommandManager cmdMgr, String taskName, String[] expectedValues) {
        Value valueObj = getValue(cmdMgr, taskName, "lsf.extra.bsub.args");
        assertEquals("", expectedValues.length, valueObj.getNumValues() );
        int i=0;
        for(String expected : expectedValues) {
            assertEquals("values["+i+"]", expected, valueObj.getValues().get(i));
            ++i;
        }
    }

}

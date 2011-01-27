package org.genepattern.server.executor;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HsqlDbUtil;
import org.genepattern.server.executor.CommandProperties.Value;
import org.genepattern.server.user.User;
import org.genepattern.webservice.JobInfo;

/**
 * Unit tests for the CommandManagerFactory.
 * 
 * @author pcarr
 */
public class CommandManagerFactoryTest extends TestCase {
    private static boolean isDbInitialized = false;
    
    public void setUp() throws Exception {
        super.setUp();

        //some of the classes being tested require a Hibernate Session connected to a GP DB
        if (!isDbInitialized) {
            //TODO: use DbUnit to improve Hibernate and DB configuration for the unit tests 
            System.setProperty("hibernate.configuration.file", "hibernate.junit.cfg.xml");
            
            //String args = System.getProperty("HSQL.args", " -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb");
            System.setProperty("HSQL.args", " -port 9001  -database.0 file:testdb/GenePatternDB -dbname.0 xdb");
            System.setProperty("hibernate.connection.url", "jdbc:hsqldb:hsql://127.0.0.1:9001/xdb");
            System.setProperty("GenePatternVersion", "3.3.1");

            File resourceDir = new File("resources");
            String pathToResourceDir = resourceDir.getAbsolutePath();
            System.setProperty("genepattern.properties", pathToResourceDir);
            System.setProperty("resources", pathToResourceDir);

            try {
                isDbInitialized = true;
                HsqlDbUtil.startDatabase();
            }
            catch (Throwable t) {
                //the unit tests can pass even if db initialization fails, so ...
                // ... try commenting this out if it gives you problems
                throw new Exception("Error initializing test database", t);
            }
        }
    }
    
    /**
     * assertions for all instance of CommandManager, can be called from all test cases.
     * @param cmdMgr
     */
    private static void validateCommandManager(CommandManager cmdMgr) {
        assertNotNull("Expecting non-null cmdMgr", cmdMgr);
        
        List<Throwable> errors = CommandManagerFactory.getInitializationErrors();
        if (errors != null && errors.size() > 0) {
            fail(errors.get(0).getLocalizedMessage());
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
    private static void validateDefaultConfig(CommandManager cmdMgr) {
        validateCommandManager(cmdMgr);
        assertEquals("By default, expecting only one CommandExecutor", 1, cmdMgr.getCommandExecutorsMap().size());
    }

    /**
     * Test that the default command manager factory is loaded when no additional configuration is provided.
     */
    public void testDefaultConfiguration() {
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateDefaultConfig(cmdMgr);
    }

    /**
     * Test that the default command manager factory is loaded when initialized with null input.
     */
    public void testNullProperties() {
        CommandManagerFactory.initializeCommandManager(null);
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateDefaultConfig(cmdMgr);
    }

    /**
     * Test that the default command manager factory is loaded when initialized with empty properties.
     */
    public void testEmptyProperties() {
        Properties props = new Properties();
        CommandManagerFactory.initializeCommandManager(props);
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateDefaultConfig(cmdMgr);
    }
    
    /**
     * Test when the default command manager factory is loaded with the given config file is set, but it is not a path to a readable file.
     * Either because the config file does not exist, or because the file is not readable.
     */
    public void testMissingConfigFile() {
        Properties props = new Properties();
        props.put("command.manager.parser", BasicCommandManagerParser.class.getCanonicalName());
        //load the config file from the same directory as this class file
        //Note: make sure your test build copies the test files into the classpath
        String classname = this.getClass().getCanonicalName();
        String filepath = "test/src/"+classname.replace('.', '/')+"/filenotfound.yaml";
        props.put("command.manager.config.file", filepath);
        CommandManagerFactory.initializeCommandManager(props);
        assertEquals("expecting initializion error", 1, CommandManagerFactory.getInitializationErrors().size()); 
        //now, clear the errors
        CommandManagerFactory.getInitializationErrors().clear();
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateDefaultConfig(cmdMgr);
    }
    
    public void testSampleYamlConfigFromSystemProps() {
        File resourceDir = new File("resources");
        String pathToResourceDir = resourceDir.getAbsolutePath();
        String parserClass=BasicCommandManagerParser.class.getCanonicalName();
        System.setProperty("genepattern.properties", pathToResourceDir);
        System.setProperty("command.manager.parser", parserClass);
        System.setProperty("command.manager.config.file", "job_configuration_example.yaml");

        CommandManagerFactory.initializeCommandManager(System.getProperties());
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateExampleJobConfig(cmdMgr);
    }

    public void testSampleYamlConfigFromProps() throws Exception {
        File resourceDir = new File("resources");
        String pathToResourceDir = resourceDir.getAbsolutePath();
        System.setProperty("genepattern.properties", pathToResourceDir);

        Properties props = new Properties();
        String parserClass=BasicCommandManagerParser.class.getCanonicalName();
        props.put("command.manager.parser", parserClass);
        props.put("command.manager.config.file", "job_configuration_example.yaml");

        CommandManagerFactory.initializeCommandManager(props);
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateExampleJobConfig(cmdMgr);
    }

    private void validateExampleJobConfig(CommandManager cmdMgr) {
        assertNotNull("Expecting non-null cmdMgr", cmdMgr);
        validateCommandManager(cmdMgr);
        
        Map<String,CommandExecutor> map = cmdMgr.getCommandExecutorsMap();
        assertNotNull("Expecting non-null cmdMgr.commandExecutorsMap", map);
        int numExecutors = map.size();
        assertEquals("Number of executors", 3, numExecutors);

        JobInfo jobInfo = new JobInfo();
        jobInfo.setTaskName("SNPFileSorter");
        jobInfo.setTaskLSID("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00096:1");
        
        try {
            CommandExecutor cmdExecutor = cmdMgr.getCommandExecutor(jobInfo);
            String canonicalName = cmdExecutor.getClass().getCanonicalName();
            assertEquals("expecting LsfCommandExecutor for SNPFileSorter", "org.genepattern.server.executor.lsf.LsfCommandExecutor", canonicalName);
            //assertTrue("expecting LsfCommandExecutor for SNPFileSorter but found "+cmdExecutor.getClass().getCanonicalName()+" instead", (cmdExecutor instanceof LsfCommandExecutor));
        }
        catch (Exception e) {
            fail("Exception thrown in getCommandExecutor: "+e.getLocalizedMessage());
        }
        CommandProperties jobProperties = cmdMgr.getCommandProperties(jobInfo);
        assertNotNull("", jobProperties);
        assertEquals("checking job properties: lsf.max.memory", "12", ""+jobProperties.getProperty("lsf.max.memory"));
        assertEquals("checking job properties: java_flags", "-Xmx12g", jobProperties.getProperty("java_flags"));
        assertEquals("checking job properties: lsf.project", "genepattern", jobProperties.getProperty("lsf.project"));
        assertEquals("checking job properties: lsf.queue", "broad", jobProperties.getProperty("lsf.queue"));
        assertEquals("checking job properties: lsf.job.report.file", ".lsf.out", jobProperties.getProperty("lsf.job.report.file"));
        assertEquals("checking job properties: lsf.use.pre.exec.command", "false", ""+jobProperties.getProperty("lsf.use.pre.exec.command"));
        assertEquals("checking job properties: lsf.extra.bsub.args", "", jobProperties.getProperty("lsf.extra.bsub.args"));
    }

    /**
     * Helper class which returns the parent File of this source file.
     * @return
     */
    private static File getSourceDir() {
        String cname = CommandManagerFactoryTest.class.getCanonicalName();
        int idx = cname.lastIndexOf('.');
        String dname = cname.substring(0, idx);
        dname = dname.replace('.', '/');
        File sourceDir = new File("test/src/" + dname);
        return sourceDir;
    }

    /**
     * Helper class which initializes (or reinitializes) the CommandManager to parse the given
     * yaml config file from the same location as the source files for the unit tests.
     *
     * @param filename
     */
    private void initializeYamlConfigFile(String filename) {
        File resourceDir = getSourceDir();
        System.setProperty("genepattern.properties", resourceDir.getAbsolutePath());
        
        Properties props = new Properties();
        String parserClass=BasicCommandManagerParser.class.getCanonicalName();
        props.put("command.manager.parser", parserClass);
        props.put("command.manager.config.file", filename);
        CommandManagerFactory.initializeCommandManager(props);
        
        validateCommandManager(CommandManagerFactory.getCommandManager());
    }

    /**
     * Test default and custom properties in yaml configuration file.
     */
    public void testYamlConfig() {
        initializeYamlConfigFile("test_config.yaml");
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateCommandManager(cmdMgr);
        assertEquals("Expecting 2 executors", 2, cmdMgr.getCommandExecutorsMap().size() );
        validateYamlConfig();
        
        //additional tests for user and group properties
        //set up group membership
        UserAccountManager.instance().refreshUsersAndGroups();
        IGroupMembershipPlugin groups = UserAccountManager.instance().getGroupMembership();
        assertTrue("userA is in admingroup", groups.isMember("userA", "admingroup"));
        assertTrue("userA is in broadgroup", groups.isMember("userA", "broadgroup"));
        assertTrue("adminuser is in admingroup", groups.isMember("adminuser", "admingroup"));

        String userId = "adminuser";
        String taskName = "ComparativeMarkerSelection";
        String taskLsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:6";
        String expectedCmdExecId = "Test";
        String expectedFilename = "stdout_admingroup.out";
        boolean expectingException = false;
        
        validateJobConfig(
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
    public void testYamlConfigNoIndent() {
        initializeYamlConfigFile("test_config_noindent.yaml");
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateCommandManager(cmdMgr);
        assertEquals("Expecting 2 executors", 2, cmdMgr.getCommandExecutorsMap().size() );
        validateYamlConfig();
    }

    private void validateYamlConfig() {
        String userId = "test_user";
        String taskName = "ConvertLineEndings";
        String taskLsid = "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
        String expectedCmdExecId = "Test";
        String expectedFilename = "stdout.txt";
        boolean expectingException = false;

        validateJobConfig(
                userId,
                taskName, 
                taskLsid, 
                expectedCmdExecId, 
                expectedFilename, 
                expectingException);
        
        validateJobConfig(
                userId,
                "ComparativeMarkerSelection", 
                "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:2",
                "RuntimeExec",
                "CMS.out",
                false);
        
        validateJobConfig(
                userId,
                "ComparativeMarkerSelection",
                "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:4",
                "RuntimeExec",
                "CMS.v4.out",
                false);

        validateJobConfig(
                userId,
                "ComparativeMarkerSelection",
                "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:5",
                "Test",
                "stdout.txt",
                false);

        validateJobConfig(
                userId,
                "moduleA",
                (String)null,
                "RuntimeExec",
                "runtimeexec.out",
                false);
        
        validateJobConfig(
                userId,
                "moduleB",
                (String)null,
                "Test",
                "moduleB.out",
                false);

        validateJobConfig(
                userId,
                "moduleC",
                (String)null,
                "Test",
                "stdout.txt",
                true);
    }
    
    private static void validateJobConfig(String userId, String taskName, String taskLsid, String expectedCmdExecId, String exepectedStdoutFilename, boolean expectingException) {
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId(userId);
        jobInfo.setTaskName(taskName);
        jobInfo.setTaskLSID(taskLsid);
        
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        
        CommandExecutor cmdExec = null;
        try {
            cmdExec = cmdMgr.getCommandExecutor(jobInfo);
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
        CommandProperties cmdProps = cmdMgr.getCommandProperties(jobInfo);
        String cmdExecId = CommandManagerFactory.getCommandExecutorId(cmdExec);

        assertNotNull("expecting non-null CommandExecutor", cmdExec);
        assertEquals("expecting default CommandExecutor", expectedCmdExecId, cmdExecId);
        assertNotNull("expecting non-null Properties", cmdProps);
        assertEquals("checking # of properties", 3, cmdProps.size());

        assertEquals("checking 'executor' property", expectedCmdExecId, cmdProps.getProperty("executor"));
        assertEquals("checking 'stdout.filename' property", exepectedStdoutFilename, cmdProps.getProperty("stdout.filename"));
        assertEquals("checking 'java_flags' property", "-Xmx4g", cmdProps.getProperty("java_flags"));
    }
        
    public void testLsfConfig() {
        initializeYamlConfigFile("test_config_lsf.yaml");
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        
        JobInfo jobInfo = new JobInfo();
        jobInfo.setTaskName("ComparativeMarkerSelection");
        jobInfo.setTaskLSID("urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:5");
        CommandProperties props = cmdMgr.getCommandProperties(jobInfo);
        assertEquals("checking 'lsf.output.filename'", ".lsf.out", props.getProperty("lsf.output.filename"));
    }
    
    public void testReloadConfiguration() throws CommandExecutorNotFoundException {
        initializeYamlConfigFile("test_config.yaml");
        CommandManagerFactory.reloadConfigFile("test_config_reload.yaml");
        
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("testuser");
        jobInfo.setTaskName("PreprocessDataset");
        jobInfo.setTaskLSID(null);
        
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        CommandExecutor cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        CommandProperties cmdProps = cmdMgr.getCommandProperties(jobInfo);
        String cmdExecId = CommandManagerFactory.getCommandExecutorId(cmdExec);

        assertEquals("changed default java_flags", "-Xmx16g", cmdProps.getProperty("java_flags"));
        assertEquals("changed default executor", "RuntimeExec", cmdExecId);
        assertEquals("changed default stdout.filename for RuntimeExec", "runtimeexec_modified.out", cmdProps.getProperty("stdout.filename"));
        
        jobInfo = new JobInfo();
        jobInfo.setUserId("testuser");
        jobInfo.setTaskName("ComparativeMarkerSelection");
        cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        cmdProps = cmdMgr.getCommandProperties(jobInfo);
        cmdExecId = CommandManagerFactory.getCommandExecutorId(cmdExec);
        
        assertEquals("set java_flags for ComparativeMarkerSelection", "-Xmx2g", cmdProps.getProperty("java_flags"));
        assertEquals("executor for ComparativeMarkerSelection", "RuntimeExec", cmdExecId);
    }
    
    /**
     * Check properties set in the 'default.properties' section for a given executor.
     * @throws CommandExecutorNotFoundException
     */
    public void testExecutorDefaultProperties() throws CommandExecutorNotFoundException {
        initializeYamlConfigFile("test_executor_defaults.yaml");
        
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("admin");
        jobInfo.setTaskName("testEchoSleeper");
        
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        CommandExecutor cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        CommandProperties cmdProps = cmdMgr.getCommandProperties(jobInfo);
        
        assertEquals("Expecting LSF", "LSF", CommandManagerFactory.getCommandExecutorId(cmdExec));
        assertEquals("checking property set in executors->LSF->default.properties ", ".lsf.out", cmdProps.getProperty("lsf.output.filename"));
        assertEquals("checking property override in executors->LSF->default.properties ", "-Xmx4g", cmdProps.getProperty("java_flags"));
    }
    
    /**
     * Unit tests to validate setting a null value.
     */
    public void testNullValues() throws CommandExecutorNotFoundException {
        initializeYamlConfigFile("test_null_values.yaml");
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("test");
        jobInfo.setTaskName("testEchoSleeper");
        
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        CommandExecutor cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        CommandProperties cmdProps = cmdMgr.getCommandProperties(jobInfo);
        assertEquals("Expecting LSF", "LSF", CommandManagerFactory.getCommandExecutorId(cmdExec));
        assertEquals("default.properties->debug.mode", "true", cmdProps.getProperty("debug.mode"));
        
        jobInfo = new JobInfo();
        jobInfo.setUserId("adminuser");
        jobInfo.setTaskName("testEchoSleeper");
        cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        cmdProps = cmdMgr.getCommandProperties(jobInfo);

        assertEquals("Expecting RuntimeExec", "RuntimeExec", CommandManagerFactory.getCommandExecutorId(cmdExec));
        assertEquals("", cmdProps.getProperty("debug.mode"));
        
        jobInfo = new JobInfo();
        jobInfo.setUserId("testuser");
        jobInfo.setTaskName("testEchoSleeper");
        cmdExec = cmdMgr.getCommandExecutor(jobInfo);
        cmdProps = cmdMgr.getCommandProperties(jobInfo);
        assertEquals("Expecting LSF", "LSF", CommandManagerFactory.getCommandExecutorId(cmdExec));
        assertEquals("Expecting empty string", "", cmdProps.getProperty("debug.mode"));
    }
    
    /**
     * Unit tests for custom pipeline executors.
     */
    public void testCustomPipelineExecutor() {
        initializeYamlConfigFile("test_custom_pipeline_executor.yaml");
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        assertEquals("# of command executors", 2, cmdMgr.getCommandExecutorsMap().size());
    }
    
    public void testCommandProperties() {
        initializeYamlConfigFile("test_CommandProperties.yaml");
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("admin");
        
        CommandProperties cmdProps = cmdMgr.getCommandProperties(jobInfo);
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
        cmdProps = cmdMgr.getCommandProperties(jobInfo);
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
        cmdProps = cmdMgr.getCommandProperties(jobInfo);

        val = cmdProps.get("arg.override.to.null");
        assertNull("module.default.properties: Override default property to 'null'", val.getValue());
        testCommandProperty(cmdProps, "arg.list.03", "moduleOverride.x", new String[] { "moduleOverride.x", "mo4", "mo3.14", "mo1.32e6", "moTrue", "moFalse" });
    }
    
    public void testUserProperties() {
        initializeYamlConfigFile("test_user_properties.yaml");
        UserAccountManager.instance().refreshUsersAndGroups();
        IGroupMembershipPlugin groups = UserAccountManager.instance().getGroupMembership();
        assertTrue("userA is in admingroup", groups.isMember("userA", "admingroup"));
        assertTrue("userA is in broadgroup", groups.isMember("userA", "broadgroup"));
        assertTrue("userC is in broadgroup", groups.isMember("userC", "broadgroup"));
        assertFalse("userC is not in admingroup", groups.isMember("userC", "admingroup"));
        assertTrue("adminuser is in admingroup", groups.isMember("adminuser", "admingroup"));

        System.setProperty("system.prop", "SYSTEM");
        System.setProperty("system.prop.override", "SYSTEM_VALUE");
        System.setProperty("system.prop.override.to.null", "NOT_NULL");

        //tests for 'test' user, use 'default.properties', no overrides
        CommandProperties props = getPropsForUser("test");
        assertNull("unset property", props.getProperty("NOT_SET"));
        assertEquals("property which is only set in System.properties", "SYSTEM", props.getProperty("system.prop"));        
        assertEquals("override a system property", "SERVER_DEFAULT", props.getProperty("system.prop.override"));
        assertNull(props.getProperty("override a system property with a null value"));
        assertEquals("default property", "DEFAULT_VAL", props.getProperty("default.prop"));
        assertNull("default null value", props.getProperty("default.prop.null"));

        //tests for 'userA', with group overrides, userA is in two groups
        props = getPropsForUser("userA");
        assertEquals("override default prop in group.properties", "admingroup_val", props.getProperty("default.prop"));
        
        //tests for 'userC', with group overrides, userC is in one group
        props = getPropsForUser("userC");
        assertEquals("user override", "userC_val", props.getProperty("default.prop") );
        assertEquals("group override", "-Xmx256m -Dgroup=broadgroup", props.getProperty("java_flags"));
        
        //tests for 'userD' with user overrides, userD is not in any group
        props = getPropsForUser("userD");
        assertEquals("user override", "userD_val", props.getProperty("default.prop"));
    }
    
    private static CommandProperties getPropsForUser(String userId) {
        Context context = new Context();
        User user = new User();
        user.setUserId(userId);
        context.setUser(user);
        return ServerConfiguration.Factory.instance().getGPProperties(context);
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
    public void testExtraBsubArgs() throws CommandExecutorNotFoundException {
        initializeYamlConfigFile("test_config_lsf_extraBsubArgs.yaml");
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        List<Throwable> errors = CommandManagerFactory.getInitializationErrors();
        for(Throwable t : errors) {
            fail(""+t.getLocalizedMessage());
        }
        assertEquals("# of command executors", 6, cmdMgr.getCommandExecutorsMap().size());
                
        JobInfo jobInfo = new JobInfo();
        jobInfo.setUserId("admin");
        
        for(int i=1; i<=6; ++i) {
            jobInfo.setTaskName("mod0"+i);
            CommandProperties props = cmdMgr.getCommandProperties(jobInfo);
            CommandExecutor cmdExec = cmdMgr.getCommandExecutor(jobInfo);
            assertEquals("", "exec0"+i, CommandManagerFactory.getCommandExecutorId(cmdExec));
        }

        jobInfo.setTaskName("mod01");
        CommandProperties props = cmdMgr.getCommandProperties(jobInfo);
        
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

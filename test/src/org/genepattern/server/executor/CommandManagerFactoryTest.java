package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.genepattern.server.database.HsqlDbUtil;
import org.genepattern.webservice.JobInfo;

import junit.framework.TestCase;

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

            //String args = System.getProperty("HSQL.args", " -port 9001  -database.0 file:../resources/GenePatternDB -dbname.0 xdb");
            System.setProperty("HSQL.args", " -port 9001  -database.0 file:testdb/GenePatternDB -dbname.0 xdb");
            System.setProperty("hibernate.connection.url", "jdbc:hsqldb:hsql://127.0.0.1:9001/xdb");
            System.setProperty("GenePatternVersion", "3.2.3");

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
        validateSampleJobConfigYaml(cmdMgr);
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
        validateSampleJobConfigYaml(cmdMgr);
    }

    private void validateSampleJobConfigYaml(CommandManager cmdMgr) {
        assertNotNull("Expecting non-null cmdMgr", cmdMgr);
        
        Map<String,CommandExecutor> map = cmdMgr.getCommandExecutorsMap();
        assertNotNull("Expecting non-null cmdMgr.commandExecutorsMap", map);
        int numExecutors = map.size();
        assertEquals("Expecting 3 executors in configuration", 3, numExecutors);
        
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
        Properties jobProperties = cmdMgr.getCommandProperties(jobInfo);
        assertNotNull("", jobProperties);
        assertEquals("checking job properties: lsf.max.memory", "12", ""+jobProperties.get("lsf.max.memory"));
        assertEquals("checking job properties: java_flags", "-Xmx12g", jobProperties.get("java_flags"));
        assertEquals("checking job properties: lsf.project", "genepattern", jobProperties.get("lsf.project"));
        assertEquals("checking job properties: lsf.queue", "broad", jobProperties.get("lsf.queue"));
        assertEquals("checking job properties: lsf.wrapper.script", "", jobProperties.get("lsf.wrapper.script"));
        assertEquals("checking job properties: lsf.job.report.file", ".lsf.out", jobProperties.get("lsf.job.report.file"));
        assertEquals("checking job properties: lsf.use.pre.exec.command", "false", ""+jobProperties.get("lsf.use.pre.exec.command"));
        assertEquals("checking job properties: lsf.extra.bsub.args", "", jobProperties.get("lsf.extra.bsub.args"));
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
    private static void initializeYamlConfigFile(String filename) {
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
        Properties cmdProps = cmdMgr.getCommandProperties(jobInfo);
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
        Properties props = cmdMgr.getCommandProperties(jobInfo);
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
        Properties cmdProps = cmdMgr.getCommandProperties(jobInfo);
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
        Properties cmdProps = cmdMgr.getCommandProperties(jobInfo);
        
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
        Properties cmdProps = cmdMgr.getCommandProperties(jobInfo);
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

}

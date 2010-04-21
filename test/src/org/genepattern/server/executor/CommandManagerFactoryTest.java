package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.genepattern.server.executor.lsf.LsfCommandExecutor;
import org.genepattern.webservice.JobInfo;

import junit.framework.TestCase;

/**
 * Unit tests for the CommandManagerFactory.
 * 
 * @author pcarr
 */
public class CommandManagerFactoryTest extends TestCase {
    private void validateDefaultConfig(CommandManager cmdMgr) {
        assertNotNull("Expecting non-null cmdMgr", cmdMgr);
        assertNotNull("Expecting non-null cmdMgr.commandExecutorsMap", cmdMgr.getCommandExecutorsMap());
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
        props.put("command.manager.config.parser", YamlConfigParser.class.getCanonicalName());
        //load the config file from the same directory as this class file
        //Note: make sure your test build copies the test files into the classpath
        String classname = this.getClass().getCanonicalName();
        String filepath = "test/src/"+classname.replace('.', '/')+"/filenotfound.yaml";
        props.put("command.manager.config.file", filepath);
        CommandManagerFactory.initializeCommandManager(props);
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateDefaultConfig(cmdMgr);
    }
    
    public void testYamlConfigFromSystemProps() {
        File resourceDir = new File("resources");
        String pathToResourceDir = resourceDir.getAbsolutePath();
        System.setProperty("genepattern.properties", pathToResourceDir);
        String parserClass=YamlConfigParser.class.getCanonicalName();
        System.setProperty("command.manager.config.parser", parserClass);
        System.setProperty("command.manager.config.file", "job_configuration.yaml");

        CommandManagerFactory.initializeCommandManager(System.getProperties());
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateJobConfigYaml(cmdMgr);
    }

    public void testYamlConfigFromProps() throws Exception {
        File resourceDir = new File("resources");
        String pathToResourceDir = resourceDir.getAbsolutePath();
        System.setProperty("genepattern.properties", pathToResourceDir);

        Properties props = new Properties();
        String parserClass=YamlConfigParser.class.getCanonicalName();
        props.put("command.manager.config.parser", parserClass);
        props.put("command.manager.config.file", "job_configuration.yaml");

        CommandManagerFactory.initializeCommandManager(props);
        CommandManager cmdMgr = CommandManagerFactory.getCommandManager();
        validateJobConfigYaml(cmdMgr);
    }

    private void validateJobConfigYaml(CommandManager cmdMgr) {
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
            assertTrue("expecting LsfCommandExecutor for SNPFileSorter but found "+cmdExecutor.getClass().getCanonicalName()+" instead", (cmdExecutor instanceof LsfCommandExecutor));
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
        assertEquals("checking job properties: lsf.wrapper.script", "lsf_wrapper.sh", jobProperties.get("lsf.wrapper.script"));
        assertEquals("checking job properties: lsf.output.filename", ".lsf.out", jobProperties.get("lsf.output.filename"));
        assertEquals("checking job properties: lsf.use.pre.exec.command", "false", ""+jobProperties.get("lsf.use.pre.exec.command"));
        assertEquals("checking job properties: lsf.extra.bsub.args", "null", jobProperties.get("lsf.extra.bsub.args"));
    }

}

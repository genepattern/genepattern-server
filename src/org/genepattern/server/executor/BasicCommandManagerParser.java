package org.genepattern.server.executor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandProperties.Value;
import org.yaml.snakeyaml.Yaml;

/**
 * Parse job configuration properties from a file in YAML format.
 * 
 * @author pcarr
 */
public class BasicCommandManagerParser implements CommandManagerParser {
    private static Logger log = Logger.getLogger(BasicCommandManagerParser.class);
    
    private String configFilename = null;
    private File configFile = null;
    private BasicCommandManager commandManager = null;
    
    public BasicCommandManagerParser() {
        this.commandManager = new BasicCommandManager();
    }

    public CommandManager parseConfigFile(String pathToConfiguration) throws Exception {
        setConfigFilename(pathToConfiguration);
        JobConfigObj jobConfigObj = this.parse(this.configFile);
        this.commandManager = this.initializeCommandManager(jobConfigObj);
        return this.commandManager;
    }
    
    public void reloadConfigFile(CommandManager cmdMgr, String pathToConfiguration) throws Exception {
        if (!(cmdMgr instanceof BasicCommandManager)) {
            log.error("Expecting an instanceof "+this.getClass().getCanonicalName());
            return;
        }
        this.commandManager = (BasicCommandManager) cmdMgr;
        setConfigFilename(pathToConfiguration);
        synchronized(commandManager) {
            JobConfigObj jobConfigObj = this.parse(this.configFile);
            reloadCommandManagerProperties(jobConfigObj);
        }
    }
    
    private void setConfigFilename(String s) {
        this.configFilename = s;
        this.configFile = CommandManagerFactory.getConfigurationFile(configFilename);
        if (this.configFile == null) {
            throw new RuntimeException("Error in setConfigFilename("+s+"): Using default job configuration instead.");
        }
    }
    
    /**
     * Parse the config file, creating a new JobConfigObj.
     * @param configFile
     * @return
     */
    private JobConfigObj parse(File configurationFile) throws Exception {
        JobConfigObj configObj = new JobConfigObj();
        Reader reader = null;
        try {
            reader = new FileReader(configurationFile);
            Yaml yaml = new Yaml();
            Object obj = yaml.load(reader);
            if (obj != null) {
                if (obj instanceof Map<?,?>) {
                    Map<?,?> config = (Map<?,?>) obj;
                    Object executors = config.get("executors");
                    parseExecutors(configObj, (Map<?,?>) executors);
                    Object defaultProperties = config.get("default.properties");
                    parseDefaultProperties(configObj, defaultProperties);
                    Object moduleProperties = config.get("module.properties");
                    parseModuleProperties(configObj, moduleProperties);
                    
                    Object groupProperties = config.get("group.properties"); 
                    configObj.addGroupPropertiesObj(groupProperties);
                    Object userProperties = config.get("user.properties");
                    configObj.addUserPropertiesObj(userProperties);
                }
            }
        }
        catch (Throwable t) {
            throw new Exception("Error parsing job configuration file, "+configurationFile.getPath()+".\nError message: "+t.getLocalizedMessage(), t);
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return configObj;
    }
    
    private void parseExecutors(JobConfigObj configObj, Map<?,?> map) throws Exception {
        for(Object key : map.keySet()) {
            String cmdExecId = ""+key;
            Object val = map.get(key);
            ExecutorConfig cmdExecConfigObj = new ExecutorConfig(val);
            configObj.addExecutor(cmdExecId, cmdExecConfigObj);
        }
    }

    private void parseDefaultProperties(JobConfigObj configObj, Object defaultPropertiesObj) {
        if (defaultPropertiesObj == null) {
            log.info("No 'default.properties' in configuration");
            return;
        }
        if (!(defaultPropertiesObj instanceof Map<?,?>)) {
            String errorMessage = "Error in 'default.properties' section of configuration file, expected a map, but found a ";
            if (defaultPropertiesObj != null) {
                errorMessage += defaultPropertiesObj.getClass().getCanonicalName();
            }
            else {
                errorMessage += "null object";
            }
            log.error(errorMessage);
            return;
        }
        Map<?,?> map = (Map<?,?>) defaultPropertiesObj;
        for(Entry<?,?> entry : map.entrySet()) {
            String key = ""+entry.getKey();
            String value = ""+entry.getValue();
            configObj.addDefaultProperty(key, value);
        }
    }
    
    private void parseModuleProperties(JobConfigObj configObj, Object modulePropertiesObj) {
        if (modulePropertiesObj == null) {
            log.info("No 'module.properties' in configuration");
            return;
        }
        if (!(modulePropertiesObj instanceof Map<?,?>)) {
            log.error("Error in 'module.properties' section of configuration file, expected a Map, but found a "+modulePropertiesObj.getClass());
            return;
        }
        Map<?,?> modulePropertiesMap = (Map<?,?>) modulePropertiesObj;
        for(Object keyObj : modulePropertiesMap.keySet()) {
            Object valObj = modulePropertiesMap.get(keyObj);
            if ((valObj instanceof Map<?,?>)) {
                String key = ""+keyObj;
                Map<?,?> val = (Map<?,?>) valObj;
                configObj.addModuleProperty(key, val);
            }
        }
    }
    
    /**
     * Create a new instance of a CommandManager, initialize the list of CommandExecutors, and store
     * all default and custom properties.
     * 
     * @param jobConfigObj
     * @return
     * @throws Exception
     */
    private BasicCommandManager initializeCommandManager(JobConfigObj jobConfigObj) throws Exception {
        BasicCommandManager cmdMgr = new BasicCommandManager();
        initializeCommandExecutors(cmdMgr, jobConfigObj);
        setCommandManagerProperties(cmdMgr, jobConfigObj);
        return cmdMgr;
    }
    
    //initialize executors list
    private void initializeCommandExecutors(BasicCommandManager cmdMgr, JobConfigObj jobConfigObj) throws Exception {
        for(String execId : jobConfigObj.getExecutors().keySet()) {
            ExecutorConfig execObj = jobConfigObj.getExecutors().get(execId);
            CommandExecutor cmdExecutor = initializeCommandExecutor(execObj);
            cmdMgr.addCommandExecutor(execId, cmdExecutor);
        }
    }

    private void reloadCommandManagerProperties(JobConfigObj jobConfigObj) throws Exception {
        CommandManagerProperties config = this.commandManager.getConfigProperties();
        config.clear();
        setCommandManagerProperties(this.commandManager, jobConfigObj);
    }

    private void setCommandManagerProperties(BasicCommandManager cmdMgr, JobConfigObj jobConfigObj) throws Exception {
        CommandManagerProperties config = cmdMgr.getConfigProperties();

        for(String execId : jobConfigObj.getExecutors().keySet()) {
            ExecutorConfig execObj = jobConfigObj.getExecutors().get(execId);
            //load executor->default.properties
            if (execObj.defaultProperties != null) { 
                PropObj propObj = config.getPropsForExecutor(execId);
                for (String key : (Set<String>) (Set) execObj.defaultProperties.keySet()) {
                    Value value = execObj.defaultProperties.get(key);
                    propObj.addDefaultProperty(key, value);
                }
            }
        }
        //store top level default.properties
        config.getTop().setDefaultProperties(jobConfigObj.getDefaultProperties());
        //store top level module.properties
        config.getTop().setModuleProperties(jobConfigObj.getModuleProperties());
        //store custom group.properties
        initializeCustomProperties(config, jobConfigObj.getGroupPropertiesObj(), true);
        //store custom user.properties
        initializeCustomProperties(config, jobConfigObj.getUserPropertiesObj(), false);
    }

    /**
     * Initialize an instance of a CommandExecutor from the settings stored in the given CmdExecConfigObj.
     * This method calls the constructor and [optionally] calls setConfigurationFilename and setConfigurationProperties.
     * @param execObj
     * @return
     */
    private CommandExecutor initializeCommandExecutor(ExecutorConfig execObj) {
        CommandExecutor cmdExecutor = null;
        //1) load cmdExecutor from classname
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class svcClass = Class.forName(execObj.classname, false, classLoader);
            if (!CommandExecutor.class.isAssignableFrom(svcClass)) {
                log.error(""+svcClass.getCanonicalName()+" does not implement "+CommandExecutor.class.getCanonicalName());
            }
            cmdExecutor = (CommandExecutor) svcClass.newInstance();
        }
        catch (Throwable t) {
            log.error("Error loading CommandExecutor for classname: "+execObj.classname+", "+t.getLocalizedMessage(), t);
        }
        if (cmdExecutor == null) {
            return cmdExecutor;
        }
        //2) optionally set configuration parameters
        if (execObj.configurationFile != null) {
            cmdExecutor.setConfigurationFilename(execObj.configurationFile);
        }
        if (execObj.configurationProperties != null) {
            cmdExecutor.setConfigurationProperties(execObj.configurationProperties);
        }
        return cmdExecutor;
    }

    private void initializeCustomProperties(CommandManagerProperties config, Object userOrGroupPropertiesObj, boolean forGroup) throws Exception {
        if (userOrGroupPropertiesObj == null) {
            return;
        }
        //for logging and debugging
        String parentKey = forGroup ? "group.properties" : "user.properties";

        if (userOrGroupPropertiesObj == null) {
            log.debug("No '"+parentKey+"' in configuration");
            return;
        }
        if (!(userOrGroupPropertiesObj instanceof Map)) {
            String errorMessage = "Error in '"+parentKey+"' section of configuration file, expected a map, but found a ";
            if (userOrGroupPropertiesObj != null) {
                errorMessage += userOrGroupPropertiesObj.getClass().getCanonicalName();
            }
            else {
                errorMessage += "null object";
            }
            log.error(errorMessage);
            return;
        }
        Map<?,?> groupPropertiesMap = (Map<?,?>) userOrGroupPropertiesObj;
        for(Entry<?,?> entry : groupPropertiesMap.entrySet()) {
            String userOrGroupId = "" + entry.getKey();
            PropObj propObj = null;
            if (forGroup) {
                propObj = config.getPropsForGroup(userOrGroupId);
            }
            else {
                propObj = config.getPropsForUser(userOrGroupId);
            }
            initializePropertiesInto(propObj, userOrGroupId, entry.getValue());
        }
    }
    
    private void initializePropertiesInto(PropObj propObj, String groupOrUserId, Object propertiesObj) throws Exception {
        Map<?,?> map = (Map<?,?>) propertiesObj;
        for(Entry<?,?> entry : map.entrySet() ) {
            String propname = "" + entry.getKey();
            if ("module.properties".equals(propname)) {
                initializeModulePropertiesInto(propObj, groupOrUserId, entry.getValue());
            }
            else {
                Value value = Value.parse( entry.getValue() );
                propObj.addDefaultProperty(propname, value);
            }
        }
    }

    private void initializeModulePropertiesInto(PropObj propObj, String groupOrUserId, Object modulePropertiesMapObj) throws Exception {
        if (modulePropertiesMapObj == null) {
            log.debug("No module.properties set for: "+groupOrUserId);
            return;
        }
        if (!(modulePropertiesMapObj instanceof Map<?,?>)) {
            String errorMessage = "Error in 'module.properties' section of configuration file for: "+groupOrUserId+". Expected a map, but found a ";
            if (modulePropertiesMapObj != null) {
                errorMessage += modulePropertiesMapObj.getClass().getCanonicalName();
            }
            else {
                errorMessage += "null object";
            }
            log.error(errorMessage);
            throw new Exception(errorMessage);
        }
        Map<?,?> map = (Map<?,?>) modulePropertiesMapObj;
        for(Entry<?,?> entry : map.entrySet()) {
            String moduleId = ""+entry.getKey();
            Object modulePropertiesObj = entry.getValue();
            if (modulePropertiesObj instanceof Map<?,?>) {
                for(Entry<?,?> propEntry : ((Map<?,?>)modulePropertiesObj).entrySet()) {
                    String propKey = ""+propEntry.getKey();
                    Value propValue = Value.parse(propEntry.getValue());
                    propObj.addModuleProperty(moduleId, propKey, propValue);
                }
            }
        }
    }
}

//helper class for yaml parser
final class JobConfigObj {
    private CommandProperties defaultProperties = new CommandProperties();
    private Map<String,ExecutorConfig> executors = new LinkedHashMap<String,ExecutorConfig>();
    private Map<String,Map<?,?>> moduleProperties = new LinkedHashMap<String,Map<?,?>>();

    private Object groupPropertiesObj = null;
    private Object userPropertiesObj = null;

    public void addExecutor(String cmdExecId, ExecutorConfig cmdExecConfigObj) {
        this.executors.put(cmdExecId, cmdExecConfigObj);            
    }
    
    public void addDefaultProperty(String key, String value) {
        defaultProperties.put(key, value);
    }
    
    public void addModuleProperty(String key, Map<?,?> val) {
        moduleProperties.put(key, val);
    }

    public void addGroupPropertiesObj(Object obj) {
        this.groupPropertiesObj = obj;
    }
    
    public void addUserPropertiesObj(Object obj) {
        this.userPropertiesObj = obj;
    }
    
    public Map<String,ExecutorConfig> getExecutors() {
        return executors;
    }
    
    public CommandProperties getDefaultProperties() {
        return defaultProperties;
    }
    
    public Map<String,Map<?,?>> getModuleProperties() {
        return moduleProperties;
    }
    
    public Object getGroupPropertiesObj() {
        return this.groupPropertiesObj;
    }
    
    public Object getUserPropertiesObj() {
        return this.userPropertiesObj;
    }
}

/**
 * internal representation of a configuration file entry in the list of 'executors'.
 * @author pcarr
 */
final class ExecutorConfig {
    String classname;
    String configurationFile;
    CommandProperties configurationProperties = new CommandProperties();
    CommandProperties defaultProperties = new CommandProperties();
    
    ExecutorConfig(Object yamlObj) throws Exception {
        if (yamlObj instanceof String) {
            // <id>:<classname>
            this.classname = (String) yamlObj;
        }
        else if (yamlObj instanceof Map<?,?>) {
            // <id>:<map>
            parseMap((Map<?,?>) yamlObj);            
        }
        else {
            String errorMessage = "Invalid input in config file, expecting a String or a Map but found a ";
            if (yamlObj != null) {
                errorMessage += yamlObj.getClass().getCanonicalName();
            }
            else {
                errorMessage += "null object";
            }
            throw new Exception(errorMessage);
        }
    }

    private void parseMap(Map<?,?> map) throws Exception {
        // <id>: 
        //    classname: <classname>
        //    [configuration.file: <configuration_file>| configuration.properties: <map>]
        //    default.properties: <map>
        Object classname = map.get("classname");
        if (!(classname instanceof String)) {
            throw new Exception("Invalid or missing value for property, 'classname'");
        }
        this.classname = (String) classname;

        Object configFileObj = map.get("configuration.file");
        if (configFileObj != null) {
            if (configFileObj instanceof String) {
                this.configurationFile = (String) configFileObj;
            }
            else {
                throw new Exception("'configuration.file' is not of type String");
            }
        }
        Object configPropsObj = map.get("configuration.properties");
        if (configPropsObj != null) {
            if (!(configPropsObj instanceof Map<?,?>)) {
                throw new Exception("'configuration.properties' is not of type Map");
            }
            Map<?,?> configPropsMap = (Map<?,?>) configPropsObj;
            for(Entry<?,?> entry : configPropsMap.entrySet()) {
                String key = ""+entry.getKey();
                Value value = Value.parse(entry.getValue());
                this.configurationProperties.put(key, value);
            }
        }
        
        Object defaultPropertiesObj = map.get("default.properties");
        if (defaultPropertiesObj != null && defaultPropertiesObj instanceof Map<?,?>) {
            //perform string conversion here
            Map<?,?> defaultPropertiesMap = (Map<?,?>) defaultPropertiesObj;
            for(Entry<?,?> entry : defaultPropertiesMap.entrySet()) {
                String key = ""+entry.getKey();
                Value value = Value.parse(entry.getValue());
                this.defaultProperties.put(key, value);
            }
        } 
    }

}

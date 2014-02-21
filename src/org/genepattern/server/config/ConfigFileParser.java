package org.genepattern.server.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.ConfigurationException;
import org.yaml.snakeyaml.Yaml;

/**
 * Parse configuration properties from a file in YAML format.
 * 
 * @author pcarr
 */
public class ConfigFileParser {
    private static Logger log = Logger.getLogger(ConfigFileParser.class);
    
    //----input
    //the absolute or relative path to the config file, if relative it is relative to the resources dir
    private String configFilepath = null;
    private File configFile = null;
    //----output
    private CommandManagerProperties config = new CommandManagerProperties();
    private JobConfigObj jobConfigObj = null;

    public ConfigFileParser() {
    }
    
    public String getConfigFilepath() {
        return configFilepath;
    }
    
    public File getConfigFile() {
        return configFile;
    }
    
    public CommandManagerProperties getConfig() {
        return config;
    }
    
    public JobConfigObj getJobConfig() {
        return jobConfigObj;
    }

    public void parseConfigFile(String pathToConfiguration) throws ConfigurationException {
        reloadConfigFile(pathToConfiguration);
    }
    
    public void reloadConfigFile(String pathToConfiguration) throws ConfigurationException {
        setConfigFilename(pathToConfiguration);
        synchronized(config) {
            jobConfigObj = parse(configFile);            
            reloadCommandManagerProperties(jobConfigObj);
        }
    }
    
    private void setConfigFilename(String s) throws ConfigurationException {
        this.configFilepath = s;
        this.configFile = ServerConfigurationFactory.getConfigurationFile(configFilepath);
    }
    
    /**
     * Parse the config file, creating a new JobConfigObj.
     * @param configFile
     * @return
     */
    private JobConfigObj parse(File configurationFile) throws ConfigurationException {
        Reader reader = null;
        try {
            reader = new FileReader(configurationFile);
            Yaml yaml = new Yaml();
            Object obj = yaml.load(reader);
            if (obj != null) {
                JobConfigObj configObj = new JobConfigObj(obj);
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
                    
                    // optional 'jobRunner.properties'
                    Object executorProperties=config.get("executor.properties");
                    if (executorProperties != null) {
                        if (executorProperties instanceof Map<?,?>) {
                            configObj.addExecutorPropertiesMap( (Map<?,?>)executorProperties);
                        }
                    }
                }
                return configObj;
            }
        }
        catch (Throwable t) {
            throw new ConfigurationException("Error parsing job configuration file, "+configurationFile.getPath()+".\nError message: "+t.getLocalizedMessage(), t);
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
        return new JobConfigObj(null);
    }
    
    private void parseExecutors(JobConfigObj configObj, Map<?,?> map) throws Exception {
        for(Object key : map.keySet()) {
            String cmdExecId = ""+key;
            Object val = map.get(key);
            ExecutorConfig cmdExecConfigObj = new ExecutorConfig(val);
            configObj.addExecutor(cmdExecId, cmdExecConfigObj);
        }
    }

    private void parseDefaultProperties(JobConfigObj configObj, Object defaultPropertiesObj) throws Exception {
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
            Value value = Value.parse(entry.getValue());
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

    private void reloadCommandManagerProperties(JobConfigObj jobConfigObj) throws ConfigurationException {
        if (config == null) {
            config = new CommandManagerProperties();
        }
        config.clear();
        setCommandManagerProperties(jobConfigObj);
    }

    private void setCommandManagerProperties(JobConfigObj jobConfigObj) throws ConfigurationException {
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

    private void initializeCustomProperties(CommandManagerProperties config, Object userOrGroupPropertiesObj, boolean forGroup) throws ConfigurationException {
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
    
    private void initializePropertiesInto(PropObj propObj, String groupOrUserId, Object propertiesObj) throws ConfigurationException {
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

    private void initializeModulePropertiesInto(PropObj propObj, String groupOrUserId, Object modulePropertiesMapObj) throws ConfigurationException {
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
            throw new ConfigurationException(errorMessage);
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

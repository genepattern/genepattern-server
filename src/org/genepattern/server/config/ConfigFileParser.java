package org.genepattern.server.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.auth.IGroupMembershipPlugin;
import org.yaml.snakeyaml.Yaml;

/**
 * Parse configuration properties from a file in YAML format.
 * 
 * @author pcarr
 */
public class ConfigFileParser {
    private static Logger log = Logger.getLogger(ConfigFileParser.class);

    /**
     * Get a File object for the named configuration file as specified in the 'genepattern.properties' file. E.g.
     * <code>
     *     config.file=config_default.yaml
     *     or
     *     config.file=/fully/qualified/path/to/config.yaml
     * </code>
     * If a relative path is given, load the file relative to the resources directory as specified by the 
     * system property, 'genepattern.properties'. 
     * @param configuration
     * @return a valid File or null
     * 
     * @deprecated, prefer to declare the config file rather than look for it in the resources directory.
     */
    public static File initConfigurationFile(String configuration) {
        if (configuration == null || configuration.length() == 0) {
            return null;
        }
        File f = new File(configuration);
        if (!f.isAbsolute()) {
            //load the configuration file from the resources directory
            File parent = getResourceDirectory();
            if (parent != null) {
                f = new File(parent, configuration);
            }
        }
        return f;
    }

    /**
     * Get the resource directory, the parent directory of the genepattern.properties file.
     * @return a File or null if there is a configuration error 
     */
    private static File getResourceDirectory() {
        File rval = null;
        String pathToResourceDir = System.getProperty("genepattern.properties");
        if (pathToResourceDir != null) {
            rval = new File(pathToResourceDir);
        }
        else {
            log.error("Missing required system property, 'genepattern.properties'");
        }
        return rval;
    }
    
    //----input
    //the absolute or relative path to the config file, if relative it is relative to the resources dir
    private final File configFile;
    //----output
    private CommandManagerProperties config = new CommandManagerProperties();
    private JobConfigObj jobConfigObj = null;

    public ConfigFileParser(final File configFile) {
        this.configFile=configFile;
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

    synchronized public void parse() throws ConfigurationException {
        if (configFile==null) {
            throw new ConfigurationException("Configuration file is null");
        }
        if (!configFile.canRead()) {
            if (!configFile.exists()) {
                throw new ConfigurationException("Configuration file does not exist: "+configFile.getAbsolutePath());
            }
            else {
                throw new ConfigurationException("Cannot read configuration file: "+configFile.getAbsolutePath());
            }
        }
        jobConfigObj = parse(configFile);            
        reloadCommandManagerProperties(jobConfigObj);
    }
    
    public static ConfigFromYaml parseYamlFile(final File configYaml, final IGroupMembershipPlugin groupInfo) throws ConfigurationException {
        JobConfigObj jobConfigObj=parse(configYaml);
        ConfigYamlProperties configYamlProps=initConfigYamlProperties(jobConfigObj, groupInfo);
        return new ConfigFromYaml(jobConfigObj, configYamlProps);
    }
    
    /**
     * Parse the config file, creating a new JobConfigObj.
     * @param configFile
     * @return
     */
    private static JobConfigObj parse(final File configurationFile) throws ConfigurationException {
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
    
    private static void parseExecutors(final JobConfigObj configObj, final Map<?,?> map) throws Exception {
        for(Object key : map.keySet()) {
            String cmdExecId = ""+key;
            Object val = map.get(key);
            ExecutorConfig cmdExecConfigObj = new ExecutorConfig(val);
            configObj.addExecutor(cmdExecId, cmdExecConfigObj);
        }
    }

    private static void parseDefaultProperties(JobConfigObj configObj, Object defaultPropertiesObj) throws Exception {
        if (defaultPropertiesObj == null) {
            log.info("No 'default.properties' in configuration");
            return;
        }
        if (!(defaultPropertiesObj instanceof Map<?,?>)) {
            final String errorMessage = "Error in 'default.properties' section of configuration file, expected a map, but found a "+
                    defaultPropertiesObj.getClass().getCanonicalName();
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
    
    private static void parseModuleProperties(JobConfigObj configObj, Object modulePropertiesObj) {
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

//    private static CommandManagerProperties initCmdMgrProps(final JobConfigObj jobConfigObj) throws ConfigurationException {
//        CommandManagerProperties config=new CommandManagerProperties();
//        for(final String execId : jobConfigObj.getExecutors().keySet()) {
//            final ExecutorConfig execObj = jobConfigObj.getExecutors().get(execId);
//            //load executor->default.properties
//            if (execObj.defaultProperties != null) { 
//                PropObj propObj = config.getPropsForExecutor(execId);
//                for (String key : (Set<String>) (Set) execObj.defaultProperties.keySet()) {
//                    Value value = execObj.defaultProperties.get(key);
//                    propObj.addDefaultProperty(key, value);
//                }
//            }
//        }
//        //store top level default.properties
//        config.getTop().setDefaultProperties(jobConfigObj.getDefaultProperties());
//        //store top level module.properties
//        config.getTop().setModuleProperties(jobConfigObj.getModuleProperties());
//        //store custom group.properties
//        initializeCustomProperties(config, jobConfigObj.getGroupPropertiesObj(), true);
//        //store custom user.properties
//        initializeCustomProperties(config, jobConfigObj.getUserPropertiesObj(), false);
//        return config;
//    }
    
    private static ConfigYamlProperties initConfigYamlProperties(final JobConfigObj jobConfigObj, final IGroupMembershipPlugin groupInfo) throws ConfigurationException {
        final ConfigYamlProperties config=new ConfigYamlProperties(jobConfigObj, groupInfo);
        for(final Entry<String,ExecutorConfig> entry : jobConfigObj.getExecutors().entrySet()) {
            final String execId=entry.getKey();
            final ExecutorConfig execObj=entry.getValue();
            //load executor->default.properties
            if (execObj.defaultProperties != null) { 
                final PropObj propObj = config.getPropsForExecutor(execId);
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
        return config;
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

    private static void initializeCustomProperties(CommandManagerProperties config, Object userOrGroupPropertiesObj, boolean forGroup) throws ConfigurationException {
        //for logging and debugging
        final String parentKey = forGroup ? "group.properties" : "user.properties";
        if (userOrGroupPropertiesObj == null) {
            log.debug("No '"+parentKey+"' in configuration");
            return;
        }

        if (!(userOrGroupPropertiesObj instanceof Map)) {
            String errorMessage = "Error in '"+parentKey+"' section of configuration file, expected a map, but found a "+
                    userOrGroupPropertiesObj.getClass().getCanonicalName();
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
    
    private static void initializeCustomProperties(final ConfigYamlProperties config, final Object userOrGroupPropertiesObj, boolean forGroup) throws ConfigurationException {
        //for logging and debugging
        final String parentKey = forGroup ? "group.properties" : "user.properties";
        if (userOrGroupPropertiesObj == null) {
            log.debug("No '"+parentKey+"' in configuration");
            return;
        }

        if (!(userOrGroupPropertiesObj instanceof Map)) {
            String errorMessage = "Error in '"+parentKey+"' section of configuration file, expected a map, but found a "+
                    userOrGroupPropertiesObj.getClass().getCanonicalName();
            log.error(errorMessage);
            return;
        }
        Map<?,?> groupPropertiesMap = (Map<?,?>) userOrGroupPropertiesObj;
        for(Entry<?,?> entry : groupPropertiesMap.entrySet()) {
            final String userOrGroupId = "" + entry.getKey();
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

    private static void initializePropertiesInto(PropObj propObj, String groupOrUserId, Object propertiesObj) throws ConfigurationException {
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

    private static void initializeModulePropertiesInto(PropObj propObj, String groupOrUserId, Object modulePropertiesMapObj) throws ConfigurationException {
        if (modulePropertiesMapObj == null) {
            log.debug("No module.properties set for: "+groupOrUserId);
            return;
        }
        if (!(modulePropertiesMapObj instanceof Map<?,?>)) {
            String errorMessage = "Error in 'module.properties' section of configuration file for: "+groupOrUserId+". Expected a map, but found a "+
                modulePropertiesMapObj.getClass().getCanonicalName();
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

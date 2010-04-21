package org.genepattern.server.executor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

/**
 * Configure command execution and job properties from a YAML configuration file.
 * 
 * @author pcarr
 */
public class YamlConfigParser implements CommandManagerConfigParser {
    private static Logger log = Logger.getLogger(YamlConfigParser.class);
    
    private String configFilename = null;
    private File configFile = null;
    private BasicCommandManager commandManager = null;
    
    public YamlConfigParser() {
        this.commandManager = new BasicCommandManager();
    }

    public CommandManager parseConfigFile(String pathToConfiguration) throws Exception {
        setConfigFilename(pathToConfiguration);
        JobConfigObj jobConfigObj = this.parse(this.configFile);
        this.commandManager = this.initializeFromJobConfigObj(jobConfigObj);
        return this.commandManager;
    }
    
    public void reloadConfigFile(CommandManager cmdMgr, String pathToConfiguration) throws Exception {
        if (cmdMgr != commandManager) {
            log.error("Error: attempt to reload config file for a different instance of the CommandManager.");
            return;
        }
        setConfigFilename(pathToConfiguration);
        synchronized(commandManager) {
            log.error("Method not implemented!");
            
        }
    }
    
    private void setConfigFilename(String s) {
        this.configFilename = s;
        this.configFile = getFileFromPath(configFilename);
        if (this.configFile == null) {
            throw new RuntimeException("//TODO: default init");
        }
    }
    
    /**
     * Get a File object for the named configuration file, e.g.
     * command.executor.factory.configuration=job_configuration.yaml
     * command.executor.factory.configuration=/fully/qualified/path/to/job_configuration.yaml
     *     
     * @param configuration
     * @return
     */
    private File getFileFromPath(String configuration) {
        File f = new File(configuration);
        if (!f.isAbsolute()) {
            //load the configuration file from the resources directory
            File resourceDir = getResourceDir();
            if (resourceDir != null) {
                f = new File(getResourceDir(), configuration);
            }
        }
        if (!f.canRead()) {
            if (!f.exists()) {
                log.error("Configuration file not found: "+f.getAbsolutePath());
            }
            else {
                log.error("Cannot read configuration file: "+f.getAbsolutePath());
            }
            f = null;
        }
        return f;
    }

    /**
     * Get the resource directory, the parent directory of the genepattern.properties file.
     * @return a File or null if there is a configuration error 
     */
    private File getResourceDir() {
        File resourceDir = null;
        String pathToResourceDir = System.getProperty("genepattern.properties");
        if (pathToResourceDir != null) {
            resourceDir = new File(pathToResourceDir);
        }
        else {
            log.error("Missing required system property, 'genepattern.properties'");
        }
        return resourceDir;
    }

    /**
     * Parse the config file, creating a new JobConfigObj.
     * @param configFile
     * @return
     */
    private JobConfigObj parse(File configurationFile) {
        JobConfigObj configObj = new JobConfigObj();
        Reader reader = null;
        try {
            reader = new FileReader(configurationFile);
            Yaml yaml = new Yaml();
            Object obj = yaml.load(reader);
            if (obj != null) {
                if (obj instanceof Map) {
                    Map config = (Map) obj;
                    Object executors = config.get("executors");
                    parseExecutors(configObj, (Map) executors);
                    Object defaultProperties = config.get("default.properties");
                    parseDefaultProperties(configObj, defaultProperties);
                    Object customProperties = config.get("custom.properties");
                    parseCustomProperties(configObj, customProperties);
                }
            }
        }
        catch (Throwable t) {
            //TODO: handle exception
            t.printStackTrace();
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
    
    private void parseExecutors(JobConfigObj configObj, Map map) throws Exception {
        for(Object cmdExecId : map.keySet()) {
            Object val = map.get(cmdExecId);
            CmdExecConfigObj cmdExecConfigObj = new CmdExecConfigObj(val);
            configObj.executors.put((String)cmdExecId, cmdExecConfigObj);
        }
    }

    private void parseDefaultProperties(JobConfigObj configObj, Object defaultPropertiesObj) {
        if (defaultPropertiesObj == null) {
            log.info("No 'default.properties' in configuration");
            return;
        }
        if (!(defaultPropertiesObj instanceof Map)) {
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
        Map map = (Map) defaultPropertiesObj;
        for(Object key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof String) {
                configObj.defaultProperties.put((String)key, (String) value);
            }
            else {
                String errorMessage = "Error in 'default.properties' section of configuration file, expected a string, but found a ";
                if (value != null) {
                    errorMessage += value.getClass().getCanonicalName();
                }
                else {
                    errorMessage += "null object";
                }
                log.error(errorMessage);
            }
        }
    }
    
    private void parseCustomProperties(JobConfigObj configObj, Object customPropertiesObj) {
        if (customPropertiesObj == null) {
            log.info("No 'custom.properties' in configuration, using default settings for all jobs");
            return;
        }
        if (!(customPropertiesObj instanceof Map)) {
            log.error("Error in 'custom.properties' section of configuration file, expected a Map, but found a "+customPropertiesObj.getClass());
            return;
        }
        Map fromConfig = (Map) customPropertiesObj;
        for( Object key : fromConfig.keySet() ) {
            Object val = fromConfig.get(key);
            if ((val instanceof LinkedHashMap<?,?>)) {
                configObj.customProperties.put((String) key, (LinkedHashMap<String,String>) val);
            }
        }
    }
    
    private BasicCommandManager initializeFromJobConfigObj(JobConfigObj configObj) throws Exception {
        BasicCommandManager cmdMgr = new BasicCommandManager();
        for(String execId : configObj.executors.keySet()) {
            CmdExecConfigObj execObj = configObj.executors.get(execId);
            CommandExecutor cmdExecutor = initializeCommandExecutor(execObj);
            cmdMgr.addCommandExecutor(execId, cmdExecutor);
            if (execObj.jobProperties != null) {
                cmdMgr.setJobProperties(execId, execObj.jobProperties);
            }
        }
        cmdMgr.setDefaultProperties(configObj.defaultProperties);
        cmdMgr.setCustomProperties(configObj.customProperties);
        return cmdMgr;
    }
    
    private CommandExecutor initializeCommandExecutor(CmdExecConfigObj execObj) {
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
}

//helper class for yaml parser
class JobConfigObj {
    Properties defaultProperties = new Properties();
    Map<String,CmdExecConfigObj> executors = new LinkedHashMap<String,CmdExecConfigObj>();
    Map<String,Map<String,String>> customProperties = new LinkedHashMap<String,Map<String,String>>();
}

class CmdExecConfigObj {
    String classname;
    String configurationFile;
    Properties configurationProperties = new Properties();
    Properties jobProperties = new Properties();
    
    CmdExecConfigObj(Object yamlObj) throws Exception {
        if (yamlObj instanceof String) {
            // <id>:<classname>
            this.classname = (String) yamlObj;
        }
        else if (yamlObj instanceof Map) {
            // <id>:<map>
            parseMap((Map) yamlObj);            
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

    private void parseMap(Map map) throws Exception {
        // <id>: 
        //    classname: <classname>
        //    [configuration.file: <configuration_file>| configuration.properties: <map>]
        //    job.properties: <map>
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
            if (configPropsObj instanceof Map) {
                this.configurationProperties.putAll( (Map) configPropsObj);
            }
            else {
                throw new Exception("'configuration.properties' is not of type Map");
            }
        }
        
        Object jobPropsObj = map.get("job.properties");
        if (jobPropsObj != null) {
            if (jobPropsObj instanceof Map) {
                this.jobProperties.putAll( (Map) jobPropsObj );
            }
        } 
    }

}

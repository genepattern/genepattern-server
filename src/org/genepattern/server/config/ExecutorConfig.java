package org.genepattern.server.config;

import java.util.Map;
import java.util.Map.Entry;

import org.genepattern.server.executor.CommandProperties;

/**
 * internal representation of a configuration file entry in the list of 'executors'.
 * @author pcarr
 */
public class ExecutorConfig {
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
    
    public String getClassname() {
        return classname;
    }
    
    public String getConfigurationFile() {
        return configurationFile;
    }
    
    public CommandProperties getConfigurationProperties() {
        return configurationProperties;
    }
    
    public CommandProperties getDefaultProperties() {
        return defaultProperties;
    }
}

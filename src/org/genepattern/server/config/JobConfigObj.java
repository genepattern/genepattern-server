package org.genepattern.server.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.genepattern.server.executor.CommandProperties;

/**
 * Helper class for yaml parser.
 * @author pcarr
 */
public class JobConfigObj {
    private CommandProperties defaultProperties = new CommandProperties();
    private Map<String,ExecutorConfig> executors = new LinkedHashMap<String,ExecutorConfig>();
    private Map<String,Map<?,?>> moduleProperties = new LinkedHashMap<String,Map<?,?>>();

    
    public JobConfigObj(final Object yamlObj) {
        this.yamlObj=yamlObj;
    }
    
    private final Object yamlObj;
    private Object groupPropertiesObj = null;
    private Object userPropertiesObj = null;
    private Map<?,?> executorPropertiesMap = null;

    public void addExecutor(String cmdExecId, ExecutorConfig cmdExecConfigObj) {
        this.executors.put(cmdExecId, cmdExecConfigObj);            
    }
    
    public void addDefaultProperty(String key, Value value) {
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
    
    public void addExecutorPropertiesMap(final Map<?, ?> executorPropertiesMap) {
        this.executorPropertiesMap = executorPropertiesMap;
    }

    /**
     * The yaml object parsed from the entire config file.
     */
    public Object getYamlObj() {
        return yamlObj;
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
    
    public Map<?,?> getExecutorPropertiesMap() {
        return executorPropertiesMap;
    }
}

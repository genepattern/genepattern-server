package org.genepattern.server.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Lsid;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.executor.ConfigurationException;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for managing default properties and module specific properties loaded from the configuration file. 
 * 
 * Instances of this class are used to hold default settings as well as executor, group and user specific customizations.
 * @author pcarr
 */
public class PropObj {
    private static Logger log = Logger.getLogger(PropObj.class);

    private CommandProperties defaultProperties = new CommandProperties();
    private Map<String,CommandProperties> modulePropertiesMap = new HashMap<String,CommandProperties>();

    public void clearDefaultProperties() {
        defaultProperties.clear();
    }

    public void addDefaultProperty(String key, Value value) {
        defaultProperties.put(key, value);
    }
    
    public void setDefaultProperties(CommandProperties props) {
        clearDefaultProperties();
        this.defaultProperties.putAll(props);
    }
    
    public CommandProperties getDefaultProperties() {
        return defaultProperties;
    }

    public void clearModuleProperties() {
        modulePropertiesMap.clear();
    }
    
    public void addModuleProperty(String moduleId, String propKey, Value propValue) {
        CommandProperties moduleProperties = modulePropertiesMap.get(moduleId);
        if (moduleProperties == null) {
            moduleProperties = new CommandProperties();
            modulePropertiesMap.put(moduleId, moduleProperties);
        }
        moduleProperties.put(propKey, propValue);
    }

    public void setModuleProperties(Map<String,Map<?,?>> map) throws ConfigurationException {
        clearModuleProperties();
        for(Entry<String,Map<?,?>> mapEntry : map.entrySet()) {
            String moduleId = mapEntry.getKey();
            for(Entry<?,?> entry : mapEntry.getValue().entrySet()) {
                String propKey = ""+entry.getKey();
                Value propValue = Value.parse(entry.getValue());
                addModuleProperty(moduleId, propKey, propValue);
            }
        }
    }

    public String getProperty(JobInfo jobInfo, String propertyKey) {
        return getProperty(jobInfo, propertyKey, null);
    }

    public String getProperty(JobInfo jobInfo, String propertyKey, String defaultValue) {
        String value = defaultProperties.getProperty(propertyKey, defaultValue);
        if (jobInfo == null) {
            log.error("null jobInfo");
            return value;
        }
        return getModuleProperty(jobInfo, propertyKey, value);
    }

    public String getDefaultProperty(String key) {
        return getDefaultProperty(key, null);
    }
    
    public Value getDefaultValue(String key) {
        return defaultProperties.get(key);
    }

    public String getDefaultProperty(String key, String defaultValue) {
        return defaultProperties.getProperty(key, defaultValue);
    }

    public String getModuleProperty(JobInfo jobInfo, String propertyKey, String defaultValue) {
        String value = defaultValue;
        //1. override default by taskName
        String taskName = jobInfo.getTaskName();
        if (taskName != null) {
            value = getModulePropertyFromMap(taskName, propertyKey, value);
        }
        //2. override by lsid, no version
        String taskLsid = jobInfo.getTaskLSID();
        Lsid lsid = null;
        if (taskLsid != null) {
            lsid = new Lsid(jobInfo.getTaskLSID());
            value = getModulePropertyFromMap(lsid.getLsidNoVersion(), propertyKey, value);
            //3. override by lsid, including version
            value = getModulePropertyFromMap(lsid.getLsid(), propertyKey, value);
        }
        return value;
    }

    public CommandProperties getModuleProperties(JobInfo jobInfo) {
        if (jobInfo==null) {
            log.error("jobInfo==null");
            return new CommandProperties();
        }
        return getModuleProperties(jobInfo.getTaskName(), jobInfo.getTaskLSID());
    }

    public CommandProperties getModuleProperties(final TaskInfo taskInfo) {
        if (taskInfo==null) {
            log.error("taskInfo==null");
            return new CommandProperties();
        }
        return getModuleProperties(taskInfo.getName(), taskInfo.getLsid());
    }
    
    private CommandProperties getModuleProperties(final String taskName, final String taskLsid) {
        CommandProperties props = new CommandProperties();
        //1. override default by taskName
        if (taskName != null) {
            CommandProperties taskNameProps = this.modulePropertiesMap.get(taskName);
            if (taskNameProps != null) {
                props.putAll(taskNameProps);
            }
        }
        //2. override by lsid, no version
        Lsid lsid = null;
        if (taskLsid != null) {
            lsid = new Lsid(taskLsid);
            CommandProperties lsidNoVersionProps = this.modulePropertiesMap.get(lsid.getLsidNoVersion());
            if (lsidNoVersionProps != null) {
                props.putAll(lsidNoVersionProps);
            }
            //3. override by lsid, including version
            CommandProperties lsidProps = this.modulePropertiesMap.get(lsid.getLsid());
            if (lsidProps != null) {
                props.putAll(lsidProps);
            }
        }
        return props;
    }

    private String getModulePropertyFromMap(String moduleId, String propertyKey, String defaultValue) {
        if (!modulePropertiesMap.containsKey(moduleId)) {
            return defaultValue;
        }
        if (!modulePropertiesMap.get(moduleId).containsKey(propertyKey)) {
            return defaultValue;
        }
        return modulePropertiesMap.get(moduleId).getProperty(propertyKey);
    }
}
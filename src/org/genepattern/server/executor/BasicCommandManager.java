package org.genepattern.server.executor;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Lsid;
import org.genepattern.webservice.JobInfo;

/**
 * Default implementation of the CommandManager interface.
 * 
 * @author pcarr
 */
public class BasicCommandManager implements CommandManager {
    private static Logger log = Logger.getLogger(BasicCommandManager.class);
    
    //map cmdExecId - commandExecutor
    private LinkedHashMap<String,CommandExecutor> cmdExecutorsMap = new LinkedHashMap<String,CommandExecutor>();
    //job specific properties to be set for all jobs
    private Properties defaultProperties = new Properties();
    //map cmdExecId to properties for jobs specific to the command executor
    private Map<String,Properties> jobPropertiesMap = new HashMap<String,Properties>();
    //map job to custom properties to override the defaults
    private Map<String,Map<String,String>> customPropertiesMap = new LinkedHashMap<String,Map<String,String>>();
    
    public void addCommandExecutor(String id, CommandExecutor cmdExecutor) throws Exception {
        if (cmdExecutorsMap.containsKey(id)) {
            throw new Exception("duplicate id: "+id);
        }
        cmdExecutorsMap.put(id, cmdExecutor);
    }
    
    public CommandExecutor getCommandExecutorById(String cmdExecutorId) {
        return cmdExecutorsMap.get(cmdExecutorId);
    }
    
    //implement the CommandExecutorMapper interface
    public CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException {
        CommandExecutor cmdExec = null;
        //initialize to default executor
        String cmdExecId = getCommandExecutorId(jobInfo);
        cmdExec = cmdExecutorsMap.get(cmdExecId);
        if (cmdExec == null) {
            String errorMessage = "no CommandExecutor found for job: ";
            if (jobInfo != null) {
                errorMessage += (jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
            }
            else {
                errorMessage += "null";
            }
            throw new CommandExecutorNotFoundException(errorMessage);
        }
        return cmdExec;
    }

    private String getFirstCmdExecId() {
        String firstKey = null;
        for(String key : cmdExecutorsMap.keySet()) {
            firstKey = key;
            break;
        }
        return firstKey;
    }
    
    private String getCommandExecutorId(JobInfo jobInfo) {
        //initialize to default
        String cmdExecId = this.defaultProperties.getProperty("executor");
        //special-case, if none is set, use the first item on the list of executors
        if (cmdExecId == null) {
            cmdExecId = getFirstCmdExecId();
        }

        if (jobInfo == null) {
            log.error("null jobInfo");
            return cmdExecId;
        }

        //1. override default by taskName
        String taskName = jobInfo.getTaskName();
        if (taskName != null) {
            cmdExecId = getCustomValue(taskName, "executor", cmdExecId);
        }
        //2. override by lsid, no version
        String taskLsid = jobInfo.getTaskLSID();
        Lsid lsid = null;
        if (taskLsid != null) {
            lsid = new Lsid(jobInfo.getTaskLSID());
            cmdExecId = getCustomValue(lsid.getLsidNoVersion(), "executor", cmdExecId);
            //3. override by lsid, including version
            cmdExecId = getCustomValue(lsid.getLsid(), "executor", cmdExecId);
        }
        return cmdExecId;
    }
    
    private String getCustomValue(String jobInfoKey, String propertyKey, String defaultValue) {
        if (!customPropertiesMap.containsKey(jobInfoKey)) {
            return defaultValue;
        }
        if (!customPropertiesMap.get(jobInfoKey).containsKey(propertyKey)) {
            return defaultValue;
        }
        return customPropertiesMap.get(jobInfoKey).get(propertyKey);
    }
    
    public void clearCustomProperties() {
        customPropertiesMap.clear();
    }
    
    public void setCustomProperties(Map<String,Map<String,String>> props) {
        this.customPropertiesMap.clear();
        if (props != null) {
            this.customPropertiesMap.putAll(props);
        }
    }
    
    public void addCustomProperties(String key, LinkedHashMap<String,String> props) {
        this.customPropertiesMap.put(key, props);
    }
    
    public void setJobProperties(String key, Properties jobProperties) {
        this.jobPropertiesMap.put(key, jobProperties);
    }
    
    public void clearDefaultProperties() {
        this.defaultProperties.clear();
    }
    public void setDefaultProperties(Properties props) {
        this.defaultProperties.clear();
        if (props == null) {
            return;
        }
        defaultProperties.putAll(props);
    }
    public void setDefaultProperty(String key, String value) {
        this.defaultProperties.put(key, value);
    }

    public Properties getCommandProperties(JobInfo jobInfo) {
        // 1) initialize from global properties
        Properties props = new Properties();
        props.putAll(defaultProperties);
        
        if (jobInfo == null) {
            log.error("Unexpected null arg");
            return props;
        }
        
        // 2) add/replace with executor specific defaults
        String cmdExecId = this.getCommandExecutorId(jobInfo);
        Properties additionalProps = jobPropertiesMap.get(cmdExecId);
        if (additionalProps != null) {
            props.putAll(additionalProps);
        }
        
        // 3) add/replace with custom properties ...
        final String taskName = jobInfo.getTaskName();
        // 3a) ... by taskname ...
        Map<String,String> customMap = customPropertiesMap.get(taskName);
        if (customMap != null) {
            props.putAll(customMap);
        }
        // 3b) ... by task lsid no version ...
        String taskLsid = jobInfo.getTaskLSID();
        if (taskLsid != null) {
            final Lsid lsid = new Lsid(jobInfo.getTaskLSID());
            customMap = customPropertiesMap.get(lsid.getLsidNoVersion());
            if (customMap != null) {
                props.putAll(customMap);
            }
            // 3c) ... by task lsid (with version)
            customMap = customPropertiesMap.get(lsid.getLsid());
            if (customMap != null) {
                props.putAll(customMap);
            }
        }
        return props;
    }

    public Map<String, CommandExecutor> getCommandExecutorsMap() {
        return Collections.unmodifiableMap(cmdExecutorsMap);
    }

    /**
     * call this at system startup to initialize the list of CommandExecutorService instances.
     */
    public void startCommandExecutors() {
        for(String cmdExecId : cmdExecutorsMap.keySet()) {
            CommandExecutor cmdExec = cmdExecutorsMap.get(cmdExecId);
            if (cmdExec == null) {
                log.error("null CommandExecutor for cmdExecId: '"+cmdExecId+"'");
            }
            else {
                try {
                    cmdExec.start();
                }
                catch (Throwable t) {
                    log.error("Error starting CommandExecutor, for class: "+cmdExec.getClass().getCanonicalName()+": "+t.getLocalizedMessage(), t);
                }
            }
        }
    }
    
    /**
     * call this at system shutdown to stop the list of running CommandExecutorService instances.
     */
    public void stopCommandExecutors() {
        for(String cmdExecId : cmdExecutorsMap.keySet()) {
            CommandExecutor cmdExec = cmdExecutorsMap.get(cmdExecId);
            try {
                cmdExec.stop();
            }
            catch (Throwable t) {
                log.error("Error stopping CommandExecutorService, for class: "+cmdExec.getClass().getCanonicalName()+": "+t.getLocalizedMessage(), t);
            }
        }
    }
}

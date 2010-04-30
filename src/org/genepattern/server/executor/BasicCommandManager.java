package org.genepattern.server.executor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.AnalysisManager;
import org.genepattern.server.AnalysisTask;
import org.genepattern.webservice.JobInfo;

/**
 * Default implementation of the CommandManager interface.
 * 
 * @author pcarr
 */
public class BasicCommandManager implements CommandManager {
    private static Logger log = Logger.getLogger(BasicCommandManager.class);
    
    public void startAnalysisService() {
        log.info("starting analysis service...");
        AnalysisManager.getInstance();
        AnalysisTask.startQueue();
        log.info("...analysis service started!");
    }
    
    public void shutdownAnalysisService() {
        log.info("shutting down analysis service...done!");
    }
    
    private CommandManagerProperties configProperties = new CommandManagerProperties();
    public CommandManagerProperties getConfigProperties() {
        return configProperties;
    }
    public void setConfigProperties(CommandManagerProperties configProperties) {
        this.configProperties = configProperties;
    }
    
    //map cmdExecId - commandExecutor
    private LinkedHashMap<String,CommandExecutor> cmdExecutorsMap = new LinkedHashMap<String,CommandExecutor>();
    
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
        String cmdExecId = this.configProperties.getCommandExecutorId(jobInfo);
        if (cmdExecId == null) {
            log.info("no commandExecutorId found for job, use the first one from the list.");
            cmdExecId = getFirstCmdExecId();
        }
        
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
        String firstKey = cmdExecutorsMap.keySet().iterator().next();
        return firstKey;
    }
    
    public Properties getCommandProperties(JobInfo jobInfo) {
        return this.configProperties.getCommandProperties(jobInfo);
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

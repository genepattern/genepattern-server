package org.genepattern.server.queue;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.webservice.JobInfo;

final class DefaultCommandExecutorMapper implements CommandExecutorMapper {
    private static Logger log = Logger.getLogger(DefaultCommandExecutorMapper.class);

    private String defaultCmdExecId="queue.default";
    private Map<String,CommandExecutor> mapCmdExecIdToCmdExec = new HashMap<String,CommandExecutor>();
    private Map<String,String> mapTaskNameToCmdExecId = new HashMap<String,String>();
    
    public void setDefaultCmdExecId(String defaultCmdExecId) {
        this.defaultCmdExecId=defaultCmdExecId;
    }

    public void addCommandExecutor(String cmdExecId, CommandExecutor cmdExec) {
        mapCmdExecIdToCmdExec.put(cmdExecId, cmdExec);        
    }

    public void appendTask(String taskName, String cmdExecId) {
        mapTaskNameToCmdExecId.put(taskName, cmdExecId);
    }

    public CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException {
        String taskName = jobInfo.getTaskName();
        CommandExecutor cmdExec = null;
        String cmdExecId = null;

        cmdExecId = mapTaskNameToCmdExecId.get(taskName);
        if (cmdExecId == null) {
            cmdExecId = this.defaultCmdExecId;
        }

        cmdExec = mapCmdExecIdToCmdExec.get(cmdExecId);
        //TODO: handle null
        if (cmdExec == null) {
            String errorMessage = "no CommandExecutor found for job: "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName();
            throw new CommandExecutorNotFoundException(errorMessage);
        }
        return cmdExec;
    }

}

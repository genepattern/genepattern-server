package org.genepattern.server.executor;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.Lsid;
import org.genepattern.webservice.JobInfo;

final class DefaultCommandExecutorMapper implements CommandExecutorMapper {
    private static Logger log = Logger.getLogger(DefaultCommandExecutorMapper.class);

    private String defaultCmdExecId="executor.default";
    private Map<String,CommandExecutor> mapCmdExecIdToCmdExec = new HashMap<String,CommandExecutor>();
    private Map<String,String> mapTaskToCmdExecId = new HashMap<String,String>();
    
    public void setDefaultCmdExecId(String defaultCmdExecId) {
        this.defaultCmdExecId=defaultCmdExecId;
    }

    public void addCommandExecutor(String cmdExecId, CommandExecutor cmdExec) {
        mapCmdExecIdToCmdExec.put(cmdExecId, cmdExec);        
    }

    public void appendTask(String taskName, String cmdExecId) {
        mapTaskToCmdExecId.put(taskName, cmdExecId);
    }

    public CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException {
        CommandExecutor cmdExec = null;
        String cmdExecId = null;
        
        if (jobInfo == null) {
            log.error("null jobInfo");
        }
        else {
            String taskName = jobInfo.getTaskName();
            //1. map by task name
            if (taskName != null) {
                cmdExecId = mapTaskToCmdExecId.get(taskName);
            }
            if (cmdExecId == null) {
                String taskLsid = jobInfo.getTaskLSID();
                Lsid lsid = new Lsid(jobInfo.getTaskLSID());
                //2. map by lsid (including version)
                cmdExecId = mapTaskToCmdExecId.get(lsid.getLsid());
                if (cmdExecId == null) {
                    //3. map by lsid (no version)
                    cmdExecId = mapTaskToCmdExecId.get(lsid.getLsidNoVersion());
                } 
            } 
        }
        // otherwise, use default 
        if (cmdExecId == null) {
            cmdExecId = this.defaultCmdExecId;
        }

        cmdExec = mapCmdExecIdToCmdExec.get(cmdExecId);

        if (cmdExec == null) {
            String errorMessage = "no CommandExecutor found for job: "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName();
            throw new CommandExecutorNotFoundException(errorMessage);
        }
        return cmdExec;
    }

}

package org.genepattern.server.queue;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.webservice.JobInfo;

final class DefaultCommandExecutorMapper implements CommandExecutorMapper {
    private static Logger log = Logger.getLogger(DefaultCommandExecutorMapper.class);

    private String defaultQueueId="queue.default";
    private Map<String,CommandExecutor> map = new HashMap<String,CommandExecutor>();
    private Map<String,String> map2 = new HashMap<String,String>();
    
    public void setDefaultQueueId(String defaultQueueId) {
        this.defaultQueueId=defaultQueueId;
    }

    public void addQueue(String queueId, CommandExecutor cmdExec) {
        map.put(queueId, cmdExec);        
    }

    public void appendTask(String taskName, String queueId) {
        map2.put(taskName, queueId);
    }

    public CommandExecutor getCommandExecutor(JobInfo jobInfo) throws CommandExecutorNotFoundException {
        String taskName = jobInfo.getTaskName();
        CommandExecutor cmdExec = null;
        String queueId = null;

        queueId = map2.get(taskName);
        if (queueId == null) {
            queueId = this.defaultQueueId;
        }

        cmdExec = map.get(queueId);
        //TODO: handle null
        if (cmdExec == null) {
            String errorMessage = "no CommandExecutor found for job: "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName();
            throw new CommandExecutorNotFoundException(errorMessage);
        }
        return cmdExec;
    }

}

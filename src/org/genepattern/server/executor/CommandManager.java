package org.genepattern.server.executor;

import java.util.Map;

/**
 * Manage the list of command executors for a GenePattern Server.
 * 
 * @author pcarr
 */
public interface CommandManager extends CommandExecutorMapper {
    //configuration file for the manager
    //void setConfiguration(String config);
    
    void reloadMapperConfiguration() throws Exception;
    
    void startCommandExecutors();
    void stopCommandExecutors();

    //CommandExecutorMapper getCommandExecutorMapper();
    //List<CommandExecutor> getCommandExecutors();
    Map<String,CommandExecutor> getCommandExecutorsMap();
    
}

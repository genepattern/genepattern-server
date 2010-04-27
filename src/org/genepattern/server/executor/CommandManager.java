package org.genepattern.server.executor;

import java.util.Map;

/**
 * Manage the list of command executors for a GenePattern Server.
 * 
 * @author pcarr
 */
public interface CommandManager extends CommandExecutorMapper {  
    void startCommandExecutors();
    void stopCommandExecutors();

    Map<String,CommandExecutor> getCommandExecutorsMap();
}

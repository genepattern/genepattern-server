package org.genepattern.server.executor;

import java.util.List;

public interface CommandExecutorFactory {
    void start();
    void stop();
    CommandExecutorMapper getCommandExecutorMapper();
    List<CommandExecutor> getCommandExecutors();
    
    void reloadMapperConfiguration() throws Exception;
}

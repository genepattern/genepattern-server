package org.genepattern.server.executor;

public interface CommandExecutorFactory {
    void start();
    void stop();
    CommandExecutorMapper getCommandExecutorMapper();
}

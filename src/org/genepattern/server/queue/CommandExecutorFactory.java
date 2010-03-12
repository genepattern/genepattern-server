package org.genepattern.server.queue;

public interface CommandExecutorFactory {
    void start();
    void stop();
    CommandExecutorMapper getCommandExecutorMapper();
}

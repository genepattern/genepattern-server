package org.genepattern.server.executor;

public class CommandExecutorException extends Exception {
    public CommandExecutorException() {
    }
    public CommandExecutorException(String message) {
        super(message);
    }
    public CommandExecutorException(String message, Exception cause) {
        super(message, cause);
    }
}
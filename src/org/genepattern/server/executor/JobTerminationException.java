package org.genepattern.server.executor;

public class JobTerminationException extends Exception {
    public JobTerminationException(Throwable t) {
        this("Error terminating job", t);
    }
    public JobTerminationException(String message, Throwable t) {
        super(message, t);
    }
    public JobTerminationException(String message) {
        super(message);
    }
}

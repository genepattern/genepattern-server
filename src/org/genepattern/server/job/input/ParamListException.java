package org.genepattern.server.job.input;

public class ParamListException extends Exception {
    public ParamListException() {
        super();
    }
    public ParamListException(String message) {
        super(message);
    }
    public ParamListException(String message, Throwable t) {
        super(message, t);
    }
}

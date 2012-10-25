package org.genepattern.server.eula;

public class InitException extends Exception {
    public InitException(String message) {
        super(message);
    }
    public InitException(String message, Throwable t) {
        super(message,t);
    }
}

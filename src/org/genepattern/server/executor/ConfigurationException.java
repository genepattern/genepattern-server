package org.genepattern.server.executor;

public class ConfigurationException extends Exception {
    public ConfigurationException(String message) {
        super(message);
    }
    public ConfigurationException(Throwable t) {
        super(t);
    }
    public ConfigurationException(String message, Throwable t) {
        super(message,t);
    }
}

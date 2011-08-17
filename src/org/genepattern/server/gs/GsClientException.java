package org.genepattern.server.gs;

public class GsClientException extends Exception {
    public GsClientException(String message) {
        super(message);
    }
    public GsClientException(String message, Exception e) {
        super(message, e);
    }
}

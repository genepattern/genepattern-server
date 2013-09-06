package org.genepattern.server.job.input.cache;

public class DownloadException extends Exception {
    public DownloadException(final String message) {
        super(message);
    }
    
    public DownloadException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

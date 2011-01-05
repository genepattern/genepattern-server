package org.genepattern.server.executor;

/**
 * Thrown when there is an error deleting a job.
 * @author pcarr
 */
public class JobDeletionException extends Exception {
    public JobDeletionException(String message) {
        super(message);
    }
    public JobDeletionException(Throwable t) {
        this("Error deleting job", t);
    }
    public JobDeletionException(String message, Throwable t) {
        super(message, t);
    }
}


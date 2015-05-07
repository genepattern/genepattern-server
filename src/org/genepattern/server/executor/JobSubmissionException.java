/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

/**
 * thrown when a job is not added to the queue.
 * @author pcarr
 */
public class JobSubmissionException extends Exception {
    public JobSubmissionException(String message) {
        super(message);
    }
    public JobSubmissionException(Throwable t) {
        this("Error adding job to the queue", t);
    }
    public JobSubmissionException(String message, Throwable t) {
        super(message, t);
    }
}


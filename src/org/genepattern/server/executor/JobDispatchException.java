/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

/**
 * An error occurred while starting the given job.
 * 
 * @author pcarr
 */
public class JobDispatchException extends Exception {
    public JobDispatchException(Throwable t) {
        this("Error adding job to the queue", t);
    }
    public JobDispatchException(String message, Throwable t) {
        super(message, t);
    }
    public JobDispatchException(String message) {
        super(message);
    }
    
}


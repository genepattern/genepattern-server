/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
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

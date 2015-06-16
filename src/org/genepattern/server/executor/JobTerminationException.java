/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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

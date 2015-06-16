/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

public class CommandExecutorException extends Exception {
    public CommandExecutorException() {
    }
    public CommandExecutorException(String message) {
        super(message);
    }
    public CommandExecutorException(String message, Throwable cause) {
        super(message, cause);
    }
}
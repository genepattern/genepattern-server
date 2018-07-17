/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
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
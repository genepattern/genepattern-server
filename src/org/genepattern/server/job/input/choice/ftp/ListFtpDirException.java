/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice.ftp;

public final class ListFtpDirException extends Exception {
    public ListFtpDirException(final String message) {
        super(message);
    }
    
    public ListFtpDirException(final String message, final Throwable t) {
        super(message, t);
    }
}
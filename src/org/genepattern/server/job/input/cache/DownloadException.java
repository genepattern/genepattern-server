/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.cache;

public class DownloadException extends Exception {
    public DownloadException(final String message) {
        super(message);
    }

    public DownloadException(final Throwable cause) {
        super(cause);
    }
    
    public DownloadException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

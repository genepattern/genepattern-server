/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.rest;

/**
 * For GP server exceptions thrown while processing RESTful api calls.
 * @author pcarr
 *
 */
public class GpServerException extends Exception {
    public GpServerException() {
    }
    public GpServerException(String message) {
        super(message);
    }
    public GpServerException(String message, Throwable cause) {
        super(message, cause);
    }
}

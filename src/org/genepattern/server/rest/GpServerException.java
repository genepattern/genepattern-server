/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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

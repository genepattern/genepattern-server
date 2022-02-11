/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

public class InitException extends Exception {
    public InitException(String message) {
        super(message);
    }
    public InitException(String message, Throwable t) {
        super(message,t);
    }
}

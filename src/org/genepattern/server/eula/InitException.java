/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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

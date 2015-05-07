/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

/**
 * Thrown when database operations fail.
 * @author pcarr
 *
 */
public class DbException extends Exception {
    public DbException() {
        super();
    }
    
    public DbException(String message) {
        super(message);
    }
    
    public DbException(Throwable t) {
        super(t);
    }
    
    public DbException(String message, Throwable t) {
        super(message, t);
    }

}

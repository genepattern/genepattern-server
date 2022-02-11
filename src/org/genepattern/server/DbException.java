/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
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

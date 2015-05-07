/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

/**
 * General exception thrown for errors originating with GenomeSpace
 * @author tabor
 */
public class GenomeSpaceException extends Exception {
    private static final long serialVersionUID = 2148975260653604235L;
    
    /**
     * Create the GenomeSpace Exception and pass along the error message to the superclass
     * @param message
     */
    public GenomeSpaceException(String message) {
        super(message);
    }
    
    /**
     * Create the GenomeSpaceException with the given message and exception.
     */
    public GenomeSpaceException(String message, Exception e) {
        super(message, e);
    }
}

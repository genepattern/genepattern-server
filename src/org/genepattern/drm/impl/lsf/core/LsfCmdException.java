/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm.impl.lsf.core;

/**
 * Thrown when there are problems running one of the LSF command lines.
 * @author pcarr
 *
 */
public class LsfCmdException extends Exception {
    
    public LsfCmdException(String message) {
        super(message);
    }
    
    public LsfCmdException(String message, Exception e) {
        super(message, e);
    }
}
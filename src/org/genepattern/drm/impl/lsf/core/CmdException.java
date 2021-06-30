/*******************************************************************************
 * Copyright (c) 2003-2021 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.drm.impl.lsf.core;

/**
 * Thrown when there are problems running one of the LSF command lines.
 * @author pcarr
 *
 */
public class CmdException extends Exception {
    
    public CmdException(String message) {
        super(message);
    }
    
    public CmdException(String message, Exception e) {
        super(message, e);
    }
}
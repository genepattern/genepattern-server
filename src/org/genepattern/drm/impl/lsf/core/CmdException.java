/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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
/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.lsf;

/**
 * Implement this interface so that we can provide more meaningful error message and exitCode
 * when an LSF job failed because of LSF specific problems, as opposed to module errors.
 * 
 * For example,
 *     1) when the job was terminated by LSF because of insufficient memory
 *     2) when the job was terminated by the bkill command
 * 
 * @author pcarr
 *
 */
public interface ILsfErrorChecker {    
    LsfErrorStatus getStatus();    
}

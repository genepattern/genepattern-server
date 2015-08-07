/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula;

import java.io.File;


/**
 * This interface was created so that I can implement alternate ways to get the <libdir> for a 
 * GenePattern module. The libdir is the File location on the server file system where 
 * the task (module or pipeline) is installed.
 * For example, the default libdir for ComparativeMarkerSelection v. 4, is here. 
 *     /Applications/GenePatternServer/taskLib/ComparativeMarkerSelection.4.12
 *     
 * The legacy method is not ideal.
 * 
 * @author pcarr
 *
 */
public interface LibdirStrategy {
    /**
     * Get the libdir for the given task.
     * 
     * @param taskInfo
     * @return the path on the file system where the given task is installed. E.g.
     *     /Applications/GenePatternServer/taskLib/ComparativeMarkerSelection.4.12
     */
    File getLibdir(String moduleLsid) throws InitException;
}


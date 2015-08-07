/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server;

import java.io.File;

/**
 * @author Joshua Gould
 */
public interface SaveTaskPlugin {
    public void taskSaved(File zipFile);
}

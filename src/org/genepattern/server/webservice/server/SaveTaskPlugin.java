/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.webservice.server;

import java.io.File;

/**
 * @author Joshua Gould
 */
public interface SaveTaskPlugin {
    public void taskSaved(File zipFile);
}

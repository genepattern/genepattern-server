/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.drm.impl.lsf.core;

import java.util.List;

/**
 * Run an external command and gather output into a list of string.
 * @author pcarr
 *
 */
public interface CmdRunner {
    List<String> runCmd(final List<String> cmd) throws CmdException;
}

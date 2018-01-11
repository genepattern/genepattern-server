/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

/**
 * Parse the 'numValues' property from the manifest file for a module.
 * 
 * @author pcarr
 */
public interface NumValuesParser {
    NumValues parseNumValues(String numValues) throws Exception;
}


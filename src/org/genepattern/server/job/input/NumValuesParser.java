/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
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


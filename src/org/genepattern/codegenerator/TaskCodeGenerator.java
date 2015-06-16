/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.codegenerator;

import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.ParameterInfo;

/**
 * Generates code for invocation of a single task
 * 
 * @author Joshua Gould
 */
public interface TaskCodeGenerator {

    /**
     * Returns a string that contains the code to call the job with the given
     * parameters.
     * 
     * @param job
     * @param params
     * @return the code.
     */
    public String generateTask(AnalysisJob job, ParameterInfo[] params);

    /**
     * Returns the language (e.g. Java, MATLAB)
     * 
     * @return the language.
     */
    public String getLanguage();

    /**
     * Returns the file extension
     * 
     * @return the file extension.
     */
    public String getFileExtension();

}

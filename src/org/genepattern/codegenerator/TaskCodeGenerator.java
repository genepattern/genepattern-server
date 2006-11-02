/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

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
     * Returns the language (e.g. Java, MATLAB)/
     * @return the language.
     */
    public String getLanguage();

}

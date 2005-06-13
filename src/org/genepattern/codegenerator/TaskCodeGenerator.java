package org.genepattern.codegenerator;

import org.genepattern.webservice.*;


/**
 *  Generates code for invocation of a single task
 *
 * @author    Joshua Gould
 */
public interface TaskCodeGenerator {

   public String generateTask(AnalysisJob job,
         ParameterInfo[] params);
     

}

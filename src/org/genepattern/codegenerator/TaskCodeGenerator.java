package org.genepattern.codegenerator;

import org.genepattern.webservice.ParameterInfo;


/**
 *  Generates code for invocation of a single task
 *
 * @author    Joshua Gould
 */
public interface TaskCodeGenerator {

   public String generateTask(String lsid,
         ParameterInfo[] params);
         
   public String getLanguage();

}

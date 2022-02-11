/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import org.genepattern.webservice.ParameterInfo;

public interface ChoiceInfoParser {

    /**
     * For a given input parameter (initialized by parsing the manifest for a module),
     * get the list of choices to display in the GUI.
     * 
     * Can be null if the parameter does not have a list of choices.
     * 
     * @param param
     * @return
     */
    ChoiceInfo initChoiceInfo(ParameterInfo param);
}

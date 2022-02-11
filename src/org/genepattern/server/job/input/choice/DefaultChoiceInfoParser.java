/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.webservice.ParameterInfo;

public class DefaultChoiceInfoParser implements ChoiceInfoParser {
    final static private Logger log = Logger.getLogger(DefaultChoiceInfoParser.class);

    @Override
    public ChoiceInfo initChoiceInfo(ParameterInfo param) {
        final Map<String,String> choices;
        //the new way (>= 3.7.0), check for choice= in manifest
        final String declaredChoicesStr= (String) param.getAttributes().get("choice");
        if (declaredChoicesStr != null) {
            log.debug("parsing choice entry from manifest for parm="+param.getName());
            choices=ParameterInfo._initChoicesFromString(declaredChoicesStr);
        }
        else {
            //the old way
            choices=param.getChoices();
        }

        if (choices==null) {
            log.debug("choices is null, param="+param.getName());
            return null;
        }
        if (choices.size()==0) {
            log.debug("choices.size()==0, param="+param.getName());
            return null;
        }
        
        final ChoiceInfo choiceInfo=new ChoiceInfo(param.getName());
        for(final Entry<String,String> choiceEntry : choices.entrySet()) {
            Choice choice = new Choice(choiceEntry.getKey(), choiceEntry.getValue());
            choiceInfo.add(choice);
        }
        return choiceInfo;
    }

}

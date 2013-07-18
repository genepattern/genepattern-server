package org.genepattern.server.job.input.choice;

import org.apache.log4j.Logger;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChoiceInfoHelper {
    final static private Logger log = Logger.getLogger(ChoiceInfoHelper.class);

    final static public ChoiceInfo initChoiceInfo(final ParameterInfo pinfo) {
        try {
            return ChoiceInfo.getChoiceInfoParser().initChoiceInfo(pinfo);
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
    }
    
    final static public JSONArray initChoiceJson(final ChoiceInfo choiceInfo) {
        if (choiceInfo==null) {
            log.error("Unexpected null value");
            return null;
        }
        //create a json array of the choices
        try {
            JSONArray choices=new JSONArray();
            for(final Choice choice : choiceInfo.getChoices()) {
                JSONObject choiceObj=new JSONObject();
                choiceObj.put("label", choice.getLabel());
                choiceObj.put("value", choice.getValue());
                choices.put(choiceObj);
            }
            return choices;
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
    }
}

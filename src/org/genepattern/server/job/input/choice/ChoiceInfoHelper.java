/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Strings;

/**
 * 
 * @author pcarr
 *
 */
public class ChoiceInfoHelper {
    final static private Logger log = Logger.getLogger(ChoiceInfoHelper.class);
    
    @SuppressWarnings("serial")
    final static public class Ex extends Exception {
        public Ex() {
            super();
        }
        public Ex(String str) {
            super(str);
        }
    }
    
    final static public boolean isSet(final String in) {
        if (in == null || in.length()==0) {
            return false;
        }
        return true;
    }

    /**
     * Helper method for initializing a Choice instance when parsing a list of choices from the manifest for a module.
     * Call this method for each item in the ';' delimited list of entries for the drop-down.
     * 
     * entry: <actualValue>=<displayValue>
     * 
     * @param entry, must not be null or will throw IllegalArgumentException
     * @return
     */
    public static final Choice initChoiceFromManifestEntry(final String entry) {
        if (entry==null) {
            throw new IllegalArgumentException("entry==null");
        }
        final String[] splits=entry.split(Pattern.quote("="), 2);
        if (splits.length==1) {
            //special-case
            if (entry.startsWith("=")) {
                //empty actual value
                return new Choice(entry.substring(1), "");
            }
            else if (entry.endsWith("=")) {
                //empty display value
                return new Choice("", entry.substring(0, entry.length()-1));
            }
            else {
                //it's the actual value, use that as the display value
                return new Choice(entry);
            }
        }
        else if (splits.length==2) {
            return new Choice(splits[1], splits[0]);
        }
        log.error("Parse error in choice entry='"+entry+"' split into too many parts: "+splits.length);
        return null;
    }
    
    public static final List<Choice> initChoicesFromManifestEntry(final String choicesString) {
        if (!isSet(choicesString)) {
            return Collections.emptyList();
        }
        final String[] choicesArray = choicesString.split(";");
        if (choicesArray==null) {
            return Collections.emptyList();
        }
        List<Choice> choices=new ArrayList<Choice>();
        for(final String entry : choicesArray) {
            Choice choice=initChoiceFromManifestEntry(entry);
            choices.add(choice);
        }
        return choices;
    }


    /**
     * Generate the string representation (for the gp manifest file) for the Choice instance.
     * Format is, <actualValue>=<displayValue>
     * @param choice
     * @return
     */
    public static final String initManifestEntryFromChoice(final Choice choice) {
        if (choice==null) {
            throw new IllegalArgumentException("choice==null");
        }
        if (choice.getLabel()==null) {
            if (choice.getValue()==null) {
                return "";
            }
            return choice.getValue();
        }
        // <actualValue>=<displayValue>
        if (choice.getValue()==null) {
            return "="+choice.getLabel();
        }
        if (choice.getValue().equals(choice.getLabel())) {
            return choice.getValue();
        }
        return choice.getValue()+"="+choice.getLabel();
    }
    
    public static final String initManifestEntryFromChoices(final List<Choice> choices) {
        if (choices==null) {
            return "";
        }
        final StringBuilder sb=new StringBuilder();
        boolean first=true;
        final String SEP=";";
        for(final Choice choice : choices) {
            if (first) {
                first=false;
            }
            else {
                sb.append(SEP);
            }
            sb.append(initManifestEntryFromChoice(choice));
        }
        return sb.toString();
    }
    
    final static public ChoiceInfo initChoiceInfo(final ParameterInfo pinfo) {
        final boolean initDropdown=true;
        return initChoiceInfo(pinfo, initDropdown);
    }
    final static public ChoiceInfo initChoiceInfo(final ParameterInfo pinfo, final boolean initDropdown) {
        try {
            return ChoiceInfo.getChoiceInfoParser(initDropdown).initChoiceInfo(pinfo);
        }
        catch (Throwable t) {
            log.error(t);
            return null;
        }
    }

    public static final String initChoiceInfoHref(final String parentHref, final ChoiceInfo choiceInfo) {
        if (choiceInfo==null) {
            return null;
        }
        if (Strings.isNullOrEmpty(parentHref)) {
            return null;
        }
        if (Strings.isNullOrEmpty(choiceInfo.getParamName())) {
            return null;
        }
        final String choiceInfoHref = parentHref + "/" + choiceInfo.getParamName()  + "/choiceInfo.json";
        return choiceInfoHref;
    }
    
    /**
     * Get the JSON representation for the given choiceInfo.
     */
    final static public JSONObject initChoiceInfoJson(final String parentHref, final ChoiceInfo choiceInfo) throws JSONException {
        if (choiceInfo==null) {
            throw new IllegalArgumentException("choiceInfo==null");
        }
        final JSONObject json=new JSONObject();
        final String choiceInfoHref=initChoiceInfoHref(parentHref, choiceInfo);
        if (!Strings.isNullOrEmpty(choiceInfoHref)) {
            json.put("href", choiceInfoHref);
        }
        if (isSet(choiceInfo.getChoiceDir())) {
            json.put(ChoiceInfo.PROP_CHOICE_DIR, choiceInfo.getChoiceDir());
        }
        if (choiceInfo.getStatus() != null) {
            final JSONObject statusObj=new JSONObject();
            statusObj.put("flag", choiceInfo.getStatus().getFlag().name());
            statusObj.put("message", choiceInfo.getStatus().getMessage());
            json.put("status", statusObj);
        }
        if (choiceInfo.getSelected() != null) {
            json.put("selectedValue", choiceInfo.getSelected().getValue());
        }
        final JSONArray choices=initChoiceJson(choiceInfo);
        json.put("choices", choices);
        json.put(ChoiceInfo.PROP_CHOICE_ALLOW_CUSTOM_VALUE, choiceInfo.isAllowCustomValue());
        return json;
    }
    
    final static private JSONArray initChoiceJson(final ChoiceInfo choiceInfo) {
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
    
    @SuppressWarnings("unchecked")
    public static void appendChoice(final Choice choice, final ParameterInfo pinfo) {
        //append choice to beginning of menu
        final String choicesStrIn=(String)pinfo.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        //<value>=<label>;
        final String choicesStr=initManifestEntryFromChoice(choice)+";"+choicesStrIn;
        pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE, choicesStr);
        pinfo.setValue(choicesStr);
    }

}

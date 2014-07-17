package org.genepattern.server.job.input.choice;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.cache.CachedFtpFile;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author pcarr
 *
 */
public class ChoiceInfoHelper {
    final static private Logger log = Logger.getLogger(ChoiceInfoHelper.class);
    
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

    /**
     * Get the JSON representation for the given choiceInfo.
     * 
     * @see TasksResource#getChoiceInfo(javax.ws.rs.core.UriInfo, String, String, HttpServletRequest)
     * 
     * @param pinfo
     * @return
     */
    final static public JSONObject initChoiceInfoJson(final HttpServletRequest request, final TaskInfo taskInfo, final ChoiceInfo choiceInfo) throws JSONException {
        if (choiceInfo==null) {
            throw new IllegalArgumentException("choiceInfo==null");
        }
        final JSONObject json=new JSONObject();
        if (request != null && taskInfo != null && isSet(choiceInfo.getParamName())) {
            final String href=TasksResource.getChoiceInfoPath(request, taskInfo, choiceInfo.getParamName());
            json.put("href", href);
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
    
    public static void appendChoice(final Choice choice, final ParameterInfo pinfo) {
        //append choice to beginning of menu
        final String choicesStrIn=(String)pinfo.getAttributes().get(ChoiceInfo.PROP_CHOICE);
        //<value>=<label>;
        final String choicesStr=initManifestEntryFromChoice(choice)+";"+choicesStrIn;
        pinfo.getAttributes().put(ChoiceInfo.PROP_CHOICE, choicesStr);
        pinfo.setValue(choicesStr);
    }

}

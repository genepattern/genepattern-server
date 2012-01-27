package org.genepattern.pipelines;

import org.apache.log4j.Logger;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InputJSON extends JSONObject {
    public static Logger log = Logger.getLogger(InputJSON.class);
    
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String TYPE = "type";
    public static final String KINDS = "kinds";
    public static final String REQUIRED = "required";
    public static final String PROMPT_WHEN_RUN = "promptWhenRun";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String CHOICES = "choices";
    
    public static final String VALUE = "value";
    
    public InputJSON(ParameterInfo param) {
        try {
            this.setName(param.getName());
            this.setDescription(param.getDescription());
            this.setType((String) param.getAttributes().get("type"));
            this.buildKinds((String) param.getAttributes().get("fileFormat"));
            this.determineRequired(param.getAttributes().get("optional"));
            this.determinePromptWhenRun();
            this.setDefaultValue((String) param.getAttributes().get("default_value"));
            this.buildChoices(param.getValue());
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing InputJSON from ParameterInfo: " + param.getName());
        }
    }
    
    public InputJSON(ParameterInfo param, boolean promptWhenRun) {
        try {
            this.setName(param.getName());
            this.setPromptWhenRun(promptWhenRun);
            this.setValue(param.getValue());
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing InputJSON from InputJSON");
        }
    }
    
    public String getName() throws JSONException {
        return this.getString(NAME);
    }
    
    public void setName(String name) throws JSONException {
        this.put(NAME, name);
    }
    
    public String getDescription() throws JSONException {
        return this.getString(DESCRIPTION);
    }
    
    public void setDescription(String description) throws JSONException {
        this.put(DESCRIPTION, description);
    }
    
    public String getType() throws JSONException {
        return this.getString(TYPE);
    }
    
    public void setType(String type) throws JSONException {
        this.put(TYPE, type);
    }
    
    public JSONArray getKinds() throws JSONException {
        return this.getJSONArray(KINDS);
    }
    
    public void setKinds(JSONArray kinds) throws JSONException {
        this.put(KINDS, kinds);
    }
    
    public void buildKinds(String kinds) throws JSONException {
        JSONArray kindsJSON = new JSONArray();
        if (kinds != null) {
            String[] parts = kinds.split(";");
            if (parts.length > 0) { 
                for (String i : parts) {
                    if (i.length() == 0) { continue; }
                    kindsJSON.put(i);
                }
            }
        }
        this.setKinds(kindsJSON);
    }
    
    public Boolean getRequired() throws JSONException {
        return this.getBoolean(REQUIRED);
    }
    
    public void setRequired(Boolean required) throws JSONException {
        this.put(REQUIRED, required);
    }
    
    public void determineRequired(Object optional) throws JSONException {
        if (optional == null) {
            this.setRequired(true);
        }
        else if (optional.equals("on")) {
            this.setRequired(false);
        }
        else {
            this.setRequired(true);
        }
    }
    
    public Boolean getPromptWhenRun() throws JSONException {
        return this.getBoolean(PROMPT_WHEN_RUN);
    }
    
    public void setPromptWhenRun(Boolean promptWhenRun) throws JSONException {
        this.put(PROMPT_WHEN_RUN, promptWhenRun);
    }
    
    // TODO: Implement this properly once we can load pipelines with promptWhenRun
    public void determinePromptWhenRun() throws JSONException {
        this.setPromptWhenRun(false);
    }
    
    public String getDefaultValue() throws JSONException {
        return this.getString(DEFAULT_VALUE);
    }
    
    public void setDefaultValue(String defaultValue) throws JSONException {
        this.put(DEFAULT_VALUE, defaultValue);
    }
    
    public String getValue() throws JSONException {
        return this.getString(VALUE);
    }
    
    public void setValue(String value) throws JSONException {
        this.put(VALUE, value);
    }
    
    public JSONArray getChoices() throws JSONException {
        return this.getJSONArray(CHOICES);
    }
    
    public void setChoices(JSONArray choices) throws JSONException {
        this.put(CHOICES, choices);
    }
    
    public void buildChoices(String choicesString) throws JSONException {
        JSONArray choicesJSON = new JSONArray();
        if (choicesString != null) {
            String[] parts = choicesString.split(";");
            if (parts.length > 0) { 
                for (String i : parts) {
                    if (i.length() == 0) { continue; }
                    choicesJSON.put(i);
                }
            }
        }
        this.setChoices(choicesJSON);
    }
}

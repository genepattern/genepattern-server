/*
 * Copyright 2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

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
    public static final String NUM_VALUES = "numValues";
    
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
            this.buildChoices(param.getValue(), this.getDefaultValue());
            this.determineNumValues((String) param.getAttributes().get("numValues"));
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing InputJSON from ParameterInfo: " + param.getName());
        }
    }
    
    public InputJSON(ParameterInfo param, boolean promptWhenRun) {
        try {
            JSONArray pwrArray = null;
            if (promptWhenRun) {
                pwrArray = new JSONArray();
                pwrArray.put(param.getAttributes().get("altName"));
                pwrArray.put(param.getAttributes().get("altDescription"));
            }
            
            this.setName(param.getName());
            this.setPromptWhenRun(pwrArray);
            this.determineNumValues((String) param.getAttributes().get("numValues"));
            
            // Hack to fix broken GP 3.3.3 pipelines
            if (param.isInputFile() && param.getValue().contains("<GenePatternURL>") && !param.getValue().contains("<LSID>")) {
                this.setValue(param.getValue());
                return;
            }
            
            this.setValue(param.getValue());
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing InputJSON from InputJSON");
        }
    }
    
    public InputJSON(JSONObject param) {
        try {
            this.setName(param.getString(NAME));
            if (!param.isNull(PROMPT_WHEN_RUN)) {
                this.setPromptWhenRun(param.getJSONArray(PROMPT_WHEN_RUN));
            }
            else {
                this.setPromptWhenRun(null);
            }
            
            this.setValue(param.getString(VALUE));
            this.determineNumValues(param.getString(NUM_VALUES));
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing InputJSON from ParameterInfo: " + param);
        }
    }
    
    public String getNumValues() throws JSONException {
        return this.getString(NUM_VALUES);
    }
    
    public void setNumValues(String numValues) throws JSONException {
        this.put(NUM_VALUES, numValues);
    }
    
    public void determineNumValues(String rawNumValues) throws JSONException {
        Boolean required = this.getRequired();
        if (required == null) required = false;
        if (rawNumValues == null && required) {
            this.setNumValues("1");
        }
        else if (rawNumValues == null && !required) {
            this.setNumValues("0-1");
        }
        else {
            this.setNumValues(rawNumValues);
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
    
    public JSONArray getPromptWhenRun() throws JSONException {
        try {
            return this.getJSONArray(PROMPT_WHEN_RUN);
        }
        catch (JSONException e) {
            return null;
        }
    }

    public void setPromptWhenRun(JSONArray promptWhenRun) throws JSONException {
        this.put(PROMPT_WHEN_RUN, promptWhenRun);
    }
    
    // TODO: Implement this properly once we can load pipelines with promptWhenRun
    public void determinePromptWhenRun() throws JSONException {
        this.setPromptWhenRun((JSONArray) null);
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
    
    public void buildChoices(String choicesString, String defaultValue) throws JSONException {
        JSONArray choicesJSON = new JSONArray();  
        if (choicesString != null) {
            // Specify variable used in testing for defaults
            boolean foundDefaultValue = false;
            String fallBackValue = null;
            String[] parts = choicesString.split(";");
            if (parts.length > 1) { 
                for (String i : parts) {
                    if (i.length() == 0) { continue; }
                    choicesJSON.put(i);
                    
                    // Do testing to make sure the default value exists in the parameter
                    String[] listParts = i.split("=");
                    if (listParts.length >= 1) {
                        if (listParts[0].equals(defaultValue)) foundDefaultValue = true;
                        if (fallBackValue == null) fallBackValue = listParts[0];
                    }
                }
                
                // Do final test and alert if default value has no match
                if (!foundDefaultValue) {
                    if (defaultValue != null && defaultValue.length() > 0) {
                        // only alert when the default value has been set, and it doesn't match one of the choices
                        log.warn("WARNING: No default value '" + defaultValue + "' in " + this.getName() + ", " + choicesString);
                    }
                    if (fallBackValue != null) {
                        this.setDefaultValue(fallBackValue);
                    }
                }
            }
        }
        this.setChoices(choicesJSON);
    }
}

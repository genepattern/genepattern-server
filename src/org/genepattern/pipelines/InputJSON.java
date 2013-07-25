/*
 * Copyright 2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

package org.genepattern.pipelines;

import org.apache.log4j.Logger;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.server.job.input.choice.ChoiceInfoParser;
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
    public static final String FILE_CHOICE = "fileChoice";
    
    public static final String VALUE = "value";
    
    public InputJSON(final ParameterInfo param) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        if (param.getAttributes()==null) {
            throw new IllegalArgumentException("param.attributes==null");
        }
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
            this.determineFileChoice(param);
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing InputJSON from ParameterInfo: " + param.getName(), e);
        }
    }
    
    public InputJSON(final ParameterInfo param, final boolean promptWhenRun) {
        if (param==null) {
            throw new IllegalArgumentException("param==null");
        }
        if (param.getAttributes()==null) {
            throw new IllegalArgumentException("param.getAttributes()==null");
        }
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
            this.determineFileChoice(param);
            
            // Hack to fix broken GP 3.3.3 pipelines
            if (param.isInputFile() && param.getValue().contains("<GenePatternURL>") && !param.getValue().contains("<LSID>")) {
                this.setValue(param.getValue());
                return;
            }
            
            this.setValue(param.getValue());
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing InputJSON from param="+param.getName()+", promptWhenRun="+promptWhenRun, e);
        }
    }
    
    /**
     * Helper class to get a value from the jsonObject or a 'nullValue' if the  
     * jsonObject does not have a value for the specified key.
     * 
     * @param key
     * @param nullValue, can be null
     * @return
     */
    private static String getStringOrNull(final JSONObject jsonObject, final String key, final String nullValue) {
        if (jsonObject==null) {
            log.error("jsonObject==null");
            return nullValue;
        }
        if (key==null) {
            log.error("key==null");
            return nullValue;
        }
        if (jsonObject.isNull(key)) {
            log.debug(key+"=<not set or is null>");
            return nullValue;
        }
        try {
            return jsonObject.getString(key);
        }
        catch (JSONException e) {
            log.error("error in getString("+key+")", e);
        }
        catch (Throwable t) {
            log.error("unexpected throwable in getString("+key+")", t);
        }
        return nullValue;
    }
    
    public InputJSON(final JSONObject param) {
        try {
            final String name=getStringOrNull(param, NAME, "");
            this.setName(name);
            if (!param.isNull(PROMPT_WHEN_RUN)) {
                this.setPromptWhenRun(param.getJSONArray(PROMPT_WHEN_RUN));
            }
            else {
                this.setPromptWhenRun(null);
            }
            
            this.setValue(param.getString(VALUE));
            final String numValues = getStringOrNull(param, NUM_VALUES, null);
            this.determineNumValues(numValues);
            this.setFileChoice(param.getBoolean(FILE_CHOICE));
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
    
    public boolean getFileChoice() throws JSONException {
        return this.getBoolean(FILE_CHOICE);
    }
    
    public void setFileChoice(boolean fileChoice) throws JSONException {
        this.put(FILE_CHOICE, fileChoice);
    }
    
    public void determineFileChoice(ParameterInfo info) throws JSONException {
        ChoiceInfoParser choiceInfoParser = ChoiceInfo.getChoiceInfoParser();
        boolean hasChoice = choiceInfoParser.hasChoiceInfo(info); 
        this.setFileChoice(hasChoice);
    }
    
    public void determineNumValues(final String rawNumValues) throws JSONException {
        if (rawNumValues == null) {
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

    /**
     * if there is a non-null default value return it.
     * else return empty string
     * @return
     * @throws JSONException
     */
    public String getDefaultValue() throws JSONException {
        if (!this.has(DEFAULT_VALUE)) {
            log.debug(DEFAULT_VALUE+" not set");
            return "";
        }
        if (this.get(DEFAULT_VALUE)==null) {
            log.debug(DEFAULT_VALUE+"==null");
            return "";
        }
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
                        log.debug("WARNING: No default value '" + defaultValue + "' in " + this.getName() + ", " + choicesString);
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

/*
 * Copyright © 2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

package org.genepattern.pipelines;

import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.server.cm.CategoryManager;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ModuleJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ModuleJSON.class);
    
    public static final String LSID = "lsid";
    public static final String NAME = "name";
    public static final String VERSION = "version";
    public static final String WRITE = "write";
    public static final String TYPE = "type";
    public static final String CATEGORY = "category";
    public static final String TYPE_MODULE = "module";
    public static final String TYPE_VISUALIZER = "visualizer";
    public static final String TYPE_PIPELINE = "pipeline";
    public static final String INPUTS = "inputs";
    public static final String OUTPUTS = "outputs";
    
    public static final String KEY = "modules";
    public static final String ID = "id";
    public static final String TOP = "top";
    public static final String LEFT = "left";
    
    public ModuleJSON(TaskInfo info, String username) {
        try {
            this.setLsid(info.getLsid());
            this.setName(info.getName());
            this.extractVersion(info.getLsid());
            this.determineWrite(info, username);
            this.determineType(info);
            this.setCategory(new JSONArray(CategoryManager.getCategoriesFromManifest(info)));
            this.constructInputs(info.getParameterInfoArray());
            this.constructOutputs(info.getTaskInfoAttributes());
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing ModuleJSON from TaskInfo: " + info.getName());
        }
    }
    
    @SuppressWarnings("unchecked")
    public ModuleJSON(Integer id, JobSubmission job) {
        try {
            this.setId(id);
            this.setLsid(job.getLSID());
            this.constructInputs(job.getParameters(), job.getRuntimePrompt());
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing ModuleJSON from JobSubmission: " + job.getName());
        }
    }
    
    public ModuleJSON(JSONObject object) {
        try {
            this.setId(object.getInt(ID));
            this.setLsid(object.getString(LSID));
            this.constructInputs(object.getJSONArray(INPUTS));
            this.setTop(object.getString(TOP));
            this.setLeft(object.getString(LEFT));
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing ModuleJSON from TaskInfo: " + object);
        }
    }
    
    public void determineWrite(TaskInfo info, String username) throws JSONException {
        if (info.getUserId().equals(username)) {
            this.put(WRITE, true);
        }
        else {
            this.put(WRITE, false);
        }
        
    }
    
    public void constructInputs(Vector<ParameterInfo> params, boolean[] prompts) throws JSONException {
        JSONArray inputs = new JSONArray();
        
        for (int i = 0; i < prompts.length; i++) {
            boolean promptWhenRun = prompts[i];
            ParameterInfo param = params.get(i);
            InputJSON input = new InputJSON(param, promptWhenRun);
            inputs.put(i, input);
        }
        
        this.put(INPUTS, inputs);
    }
    
    public JSONArray getCategory() throws JSONException {
        return this.getJSONArray(CATEGORY);
    }
    
    public void setCategory(JSONArray category) throws JSONException {
        this.put(CATEGORY, category);
    }
    
    public Integer getId() throws JSONException {
        return this.getInt(ID);
    }
    
    public void setId(Integer id) throws JSONException {
        this.put(ID, id);
    }
    
    public String getTop() throws JSONException {
        return this.getString(TOP);
    }
    
    public void setTop(String top) throws JSONException {
        this.put(TOP, top);
    }
    
    public String getLeft() throws JSONException {
        return this.getString(LEFT);
    }
    
    public void setLeft(String left) throws JSONException {
        this.put(LEFT, left);
    }
    
    public String getLsid() throws JSONException {
        return this.getString(LSID);
    }
    
    public void setLsid(String lsid) throws JSONException {
        this.put(LSID, lsid);
    }
    
    public String getName() throws JSONException {
        return this.getString(NAME);
    }
    
    public void setName(String name) throws JSONException {
        this.put(NAME, name);
    }
    
    public String getVersion() throws JSONException {
        return this.getString(VERSION);
    }
    
    public void setVersion(String version) throws JSONException {
        this.put(VERSION, version);
    }
    
    public void extractVersion(String lsid) throws JSONException {
        String[] parts = lsid.split(":");
        String versionString = parts[parts.length - 1];
        this.put(VERSION, versionString);
    }
    
    public String getType() throws JSONException {
        return this.getString(TYPE);
    }
    
    public void setType(String type) throws JSONException {
        this.put(TYPE, type);
    }
    
    public void determineType(TaskInfo info) throws JSONException {
        boolean visualizer = TaskInfo.isVisualizer(info.getTaskInfoAttributes());
        boolean pipeline = info.isPipeline();
        
        if (!visualizer && !pipeline) {
            this.setType(TYPE_MODULE);
        }
        else if (pipeline) {
            this.setType(TYPE_PIPELINE);
        }
        else if (visualizer) {
            this.setType(TYPE_VISUALIZER);
        }
    }
    
    public JSONArray getInputs() throws JSONException {
        return this.getJSONArray(INPUTS);
    }
    
    public void setInputs(JSONArray inputs) throws JSONException {
        this.put(INPUTS, inputs);
    }
    
    public void constructInputs(JSONArray params) throws JSONException {
        JSONArray inputs = new JSONArray();
        for (int i = 0; i < params.length(); i++) {
            InputJSON param = new InputJSON(params.getJSONObject(i));
            inputs.put(param);
        }
        this.put(INPUTS, inputs);
    }
    
    public void constructInputs(ParameterInfo[] params) throws JSONException {
        JSONArray inputs = new JSONArray();
        
        for (ParameterInfo i : params) {
            InputJSON param = new InputJSON(i);
            inputs.put(param);
        }
        
        this.put(INPUTS, inputs);
    }
    
    public JSONArray getOutputs() throws JSONException {
        return this.getJSONArray(OUTPUTS);
    }
    
    public void setOutputs(JSONArray outputs) throws JSONException {
        this.put(OUTPUTS, outputs);
    }
    
    public void constructOutputs(TaskInfoAttributes tia) throws JSONException {
        JSONArray outputs = new JSONArray();
        String formatString = tia.get("fileFormat");
        if (formatString != null && formatString.length() > 0) {
            String[] parts = formatString.split(";");
            for (String i : parts) {
                if (i.length() > 0) outputs.put(i);
            }
        }
        
        this.put(OUTPUTS, outputs);
    }
    
    @SuppressWarnings("unchecked")
    public static ModuleJSON[] extract(JSONObject json) {
        try {
            JSONObject object = (JSONObject) json.get(ModuleJSON.KEY);
            ModuleJSON[] modules = new ModuleJSON[object.length()];
            int i = 0;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                modules[i] = new ModuleJSON((JSONObject) object.getJSONObject(keys.next()));
                i++;
            }
            return modules;
        }
        catch (JSONException e) {
            log.error("Unable to extract ModuleJSON from saved bundle");
            return null;
        }
    }
}

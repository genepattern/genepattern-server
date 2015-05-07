/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/

package org.genepattern.pipelines;

import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONException;
import org.json.JSONObject;

public class PipeJSON extends JSONObject {
    public static Logger log = Logger.getLogger(PipeJSON.class);
    
    public static final String KEY = "pipes";
    
    public static final String OUTPUT_MODULE = "outputModule";
    public static final String OUTPUT_PORT = "outputPort";
    public static final String INPUT_MODULE = "inputModule";
    public static final String INPUT_PORT = "inputPort";
    
    public PipeJSON(String outputModule, String outputPort, String inputModule, String inputPort) {
        try {
            this.put(OUTPUT_MODULE, outputModule);
            this.put(OUTPUT_PORT, outputPort);
            this.put(INPUT_MODULE, inputModule);
            this.put(INPUT_PORT, inputPort);
        }
        catch (JSONException e) {
            log.error("Error creating pipe JSON for: " + outputModule + " " + outputPort + " " + inputModule + " " + inputPort);
        }  
    }
    
    public PipeJSON(JSONObject object) {
        try {
            this.put(OUTPUT_MODULE, object.get(OUTPUT_MODULE));
            this.put(OUTPUT_PORT, object.get(OUTPUT_PORT));
            this.put(INPUT_MODULE, object.get(INPUT_MODULE));
            this.put(INPUT_PORT, object.get(INPUT_PORT));
        }
        catch (JSONException e) {
            log.error("Error creating pipe JSON for: " + object);
        }  
    }
    
    public Integer getOutputModule() throws JSONException {
        return this.getInt(OUTPUT_MODULE);
    }
    
    public Integer getInputModule() throws JSONException {
        return this.getInt(INPUT_MODULE);
    }
    
    public String getOutputPort() throws JSONException {
        return this.getString(OUTPUT_PORT);
    }
    
    public String getInputPort() throws JSONException {
        return this.getString(INPUT_PORT);
    }
    
    @SuppressWarnings("unchecked")
    public static ResponseJSON createPipeList(Vector<JobSubmission> jobs, PipelineQueryServlet servlet) {
        ResponseJSON listObject = new ResponseJSON();
        Integer moduleCounter = 0;
        Integer pipeCounter = 0;
        
        for (JobSubmission i : jobs) {
            Vector<ParameterInfo> params = i.getParameters();
            for (ParameterInfo j : params) {
                String selector = (String) j.getAttributes().get("inheritFilename");
                String moduleId = (String) j.getAttributes().get("inheritTaskname");
                
                // Translate manifest representation to dsl
                selector = servlet.manifestToDsl(selector);
                
                if (selector != null && moduleId != null) {
                    PipeJSON pipe = new PipeJSON(moduleId, selector, moduleCounter.toString(), j.getName());
                    listObject.addChild(pipeCounter, pipe);
                    pipeCounter++;
                }
            }
            moduleCounter++;
        }
        
        return listObject;
    }
    
    @SuppressWarnings("unchecked")
    public static PipeJSON[] extract(JSONObject json) {
        try {
            JSONObject object = (JSONObject) json.get(PipeJSON.KEY);
            PipeJSON[] pipes = new PipeJSON[object.length()];
            int i = 0;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                pipes[i] = new PipeJSON((JSONObject) object.getJSONObject(keys.next()));
                i++;
            }
            return pipes;
        }
        catch (JSONException e) {
            log.error("Unable to extract PipeJSON from saved bundle");
            return null;
        }
    }
}
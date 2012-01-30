package org.genepattern.pipelines;

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
    
    public static ResponseJSON createPipeList(Vector<JobSubmission> jobs) {
        ResponseJSON listObject = new ResponseJSON();
        Integer moduleCounter = 0;
        Integer pipeCounter = 0;
        
        for (JobSubmission i : jobs) {
            Vector<ParameterInfo> params = i.getParameters();
            for (ParameterInfo j : params) {
                String selector = (String) j.getAttributes().get("inheritFilename");
                String moduleId = (String) j.getAttributes().get("inheritTaskname");
                
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
}
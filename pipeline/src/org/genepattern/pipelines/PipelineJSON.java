package org.genepattern.pipelines;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
import org.json.JSONException;
import org.json.JSONObject;

public class PipelineJSON extends JSONObject {
    public static Logger log = Logger.getLogger(PipelineJSON.class);
    
    public static final String NAME = "pipelineName";
    public static final String DESCRIPTION = "pipelineDescription";
    public static final String AUTHOR = "pipelineAuthor";
    public static final String PRIVACY = "pipelinePrivacy";
    public static final String VERSION = "pipelineVersion";
    public static final String VERSION_COMMENT = "pipelineVersionComment";
    public static final String DOCUMENTATION = "pipelineDocumentation";
    public static final String LSID = "pipelineLsid";
    
    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    
    public static final String KEY = "pipeline";
    
    public PipelineJSON(PipelineModel pipeline, TaskInfo info) {
        try {
            this.put(NAME, pipeline.getName());
            this.put(DESCRIPTION, pipeline.getDescription());
            this.put(AUTHOR, pipeline.getAuthor());
            this.put(PRIVACY, pipeline.isPrivate() ? PRIVATE : PUBLIC);
            this.put(VERSION, extractVersion(pipeline.getLsid()));
            this.put(VERSION_COMMENT, pipeline.getVersion());
            this.put(DOCUMENTATION, getDocumentation(pipeline, info));
            this.put(LSID, pipeline.getLsid());
        }
        catch (JSONException e) {
            log.error("Error creating pipeline JSON for: " + pipeline.getName());
        }  
    }
    
    private String getDocumentation(PipelineModel pipeline, TaskInfo info) {
        List<String> docList = TaskInfoCache.instance().getDocFilenames(info.getID(), pipeline.getLsid());
        if (docList.size() < 1) {
            return "";
        }
        else {
            return docList.get(0);
        }
    }
    
    private String extractVersion(String lsid) {
        if (lsid == null) {
            return "";
        }
        
        String[] parts = lsid.split(":");
        String versionString = parts[parts.length - 1];
        return versionString;
    }
}
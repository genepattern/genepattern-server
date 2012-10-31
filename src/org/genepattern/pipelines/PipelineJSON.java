/*
 * Copyright © 2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

package org.genepattern.pipelines;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.EulaManager;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
import org.json.JSONArray;
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
    public static final String LICENSE = "pipelineLicense";
    public static final String LSID = "pipelineLsid";
    public static final String FILES = "pipelineFiles";
    
    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    
    public static final String KEY = "pipeline";
    
    public PipelineJSON(JSONObject object) {
        try {
            this.put(NAME, object.get(NAME));
            this.put(DESCRIPTION, object.get(DESCRIPTION));
            this.put(AUTHOR, object.get(AUTHOR));
            this.put(PRIVACY, object.get(PRIVACY));
            this.put(VERSION, object.get(VERSION));
            this.put(VERSION_COMMENT, object.get(VERSION_COMMENT));
            this.put(DOCUMENTATION, object.get(DOCUMENTATION));
            this.put(LICENSE, object.get(LICENSE));
            this.put(LSID, object.get(LSID));
            this.put(FILES, object.get(FILES));
        }
        catch (JSONException e) {
            log.error("Unable to create PipelineJSON from generic JSONObject");
        }
    }

    public PipelineJSON(String username, PipelineModel pipeline, TaskInfo info) {
        try {
            this.put(NAME, pipeline.getName());
            this.put(DESCRIPTION, pipeline.getDescription());
            this.put(AUTHOR, pipeline.getAuthor());
            this.put(PRIVACY, pipeline.isPrivate() ? PRIVATE : PUBLIC);
            this.put(VERSION, extractVersion(pipeline.getLsid()));
            this.put(VERSION_COMMENT, pipeline.getVersion());
            this.put(LICENSE, getLicense(username, pipeline, info));
            this.put(DOCUMENTATION, getDocumentation(pipeline, info));
            this.put(LSID, pipeline.getLsid());
        }
        catch (JSONException e) {
            log.error("Error creating pipeline JSON for: " + pipeline.getName());
        }  
    }
    
    public String getName() throws JSONException {
        return this.getString(NAME);
    }
    
    public String getDescription() throws JSONException {
        return this.getString(DESCRIPTION);
    }
    
    public String getDocumentation() throws JSONException {
        return this.getString(DOCUMENTATION);
    }
    
    public String getLicense() throws JSONException {
        return this.getString(LICENSE);
    }
    
    public String getAuthor() throws JSONException {
        return this.getString(AUTHOR);
    }
    
    public String getPrivacy() throws JSONException {
        return this.getString(PRIVACY);
    }
    
    public String getVersion() throws JSONException {
        return this.getString(VERSION);
    }

    public String getVersionComment() throws JSONException {
        return this.getString(VERSION_COMMENT);
    }
    
    public String getLsid() throws JSONException {
        return this.getString(LSID);
    }
    
    public void setLsid(String lsid) throws JSONException {
        this.put(LSID, lsid);
    }
    
    public List<String> getFiles() throws JSONException {
        List<String> files = new ArrayList<String>();
        JSONArray array = this.getJSONArray(FILES);
        
        for (int i = 0; i < array.length(); i++) {
            files.add(array.getString(i));
        }
        
        return files;
    }
    
    private String getDocumentation(PipelineModel pipeline, TaskInfo info) throws JSONException {
        List<String> docList = TaskInfoCache.instance().getDocFilenames(info.getID(), pipeline.getLsid());
        if (docList.size() < 1) {
            return "";
        }
        else {
            String doc = docList.get(0);
            if (doc.equals(this.get(LICENSE))) {
                return "";
            }
            else {
                return doc;
            }
        }
    }

    private String getLicense(String username, PipelineModel pipeline, TaskInfo info) {
        Context taskContext = Context.getContextForUser(username);
        taskContext.setTaskInfo(info);
        List<EulaInfo> eulaList = EulaManager.instance(taskContext).getEulas(info);
        if (eulaList.size() < 1) return "";
        EulaInfo eula = eulaList.get(0);
        return eula.getLicense();
    }
    
    private String extractVersion(String lsid) {
        if (lsid == null) {
            return "";
        }
        
        String[] parts = lsid.split(":");
        String versionString = parts[parts.length - 1];
        return versionString;
    }
    
    public static JSONObject parseBundle(String bundle) {
        JSONObject pipelineJSON = null;
        try {
            pipelineJSON = new JSONObject(bundle);
        }
        catch (JSONException e) {
            log.error("Error parsing JSON in the saved bundle");
        }
        return pipelineJSON;
    }
    
    public static PipelineJSON extract(JSONObject json) {
        try {
            JSONObject object = (JSONObject) json.get(PipelineJSON.KEY);
            return new PipelineJSON(object);
        }
        catch (JSONException e) {
            log.error("Unable to extract PipelineJSON from saved bundle");
            return null;
        }
    }
}
/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.pipelines;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class FileJSON extends JSONObject {
    public static Logger log = Logger.getLogger(FileJSON.class);
    
    public static final String KEY = "files";
    
    public static final String NAME = "name";
    public static final String PATH = "path";
    public static final String TOP = "top";
    public static final String LEFT = "left";
    
    public FileJSON(JSONObject object) {
        try {
            this.setName(object.getString(NAME));
            this.setPath(object.getString(PATH));
            this.setTop(object.getString(TOP));
            this.setLeft(object.getString(LEFT));
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing FileJSON from JSONObject: " + object);
        }
    }
    
    public FileJSON(String name, String path, String top, String left) {
        try {
            this.setName(name);
            this.setPath(path);
            this.setTop(top);
            this.setLeft(left);
        }
        catch (JSONException e) {
            log.error("Error parsing JSON and initializing FileJSON from Strings");
        }
    }
    
    public String getName() throws JSONException {
        return this.getString(NAME);
    }
    
    public void setName(String name) throws JSONException {
        this.put(NAME, name);
    }
    
    public String getPath() throws JSONException {
        return this.getString(PATH);
    }
    
    public void setPath(String path) throws JSONException {
        this.put(PATH, path);
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
    
    @SuppressWarnings("unchecked")
    public static Map<String, FileJSON> extract(JSONObject json) {
        try {
            JSONObject object = (JSONObject) json.get(FileJSON.KEY);
            Map<String, FileJSON> files = new HashMap<String, FileJSON>();
            int i = 0;
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                FileJSON parsedFile = new FileJSON((JSONObject) object.getJSONObject(keys.next()));
                files.put(parsedFile.getName(), parsedFile);
                i++;
            }
            return files;
        }
        catch (JSONException e) {
            log.error("Unable to extract ModuleJSON from saved bundle");
            return null;
        }
    }
}

package org.genepattern.server.webapp.serverfiles;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFilePath;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ServerFileTreeJSON extends JSONArray {
    public static Logger log = Logger.getLogger(ServerFileTreeJSON.class);
    
    public static final String EMPTY = "EMPTY_DIRECTORY";
    public static final String SAVE_TREE = "SAVE_TREE";
    
    public static final String DATA = "data";
    public static final String STATE = "state";
    public static final String STATE_CLOSED = "closed";
    public static final String CHILDREN = "children";  
    public static final String METADATA = "metadata";
    public static final String TITLE = "title";
    public static final String ATTR = "attr";
    
    public ServerFileTreeJSON(List<GpFilePath> files) {
        this(files, "");
    }
    
    public ServerFileTreeJSON(List<GpFilePath> files, String code) {
        try {
            if (code.equals(EMPTY)) {
                JSONObject fj = makeEmptyDirectory();
                this.put(fj);
            }
            else if (code.equals(SAVE_TREE)) {
                for (GpFilePath gsf: files) {
                    if (gsf.isDirectory()) {
                        JSONObject fj = makeFileJSON(gsf, true);
                        this.put(fj);
                    }
                }
            }
            else {
                for (GpFilePath gsf: files) {
                    JSONObject fj = makeFileJSON(gsf);
                    this.put(fj);
                }
            }
        }
        catch (Exception e) {
            log.error("Unable to attach empty to TreeJSON Object: " + code);
        }
    }
    
    public static JSONObject makeEmptyDirectory() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(DATA, "<em>Empty Directory</em>");
        return object;
    }
    public static JSONObject makeFileJSON(GpFilePath file) throws Exception {
        return makeFileJSON(file, false);
    }
    
    public static JSONObject makeFileJSON(GpFilePath file, boolean dirOnly) throws Exception {
        JSONObject object = new JSONObject();
        
        JSONObject data = new JSONObject();
        if ("".equals(file.getName())) {
            data.put(TITLE, "/ ");
        }
        else {
            data.put(TITLE, file.getName() + " ");
        } 
        
        JSONObject attr = new JSONObject();
        attr.put("href", file.getUrl());
        if (dirOnly) { attr.put("onclick", "JavaScript:handleServerFileClick(this); return false;"); }
        else { attr.put("onclick", "JavaScript:handleServerFileClick(this); return false;"); }
        attr.put("name", file.getName());
        
        data.put(ATTR, attr);
        
        if (file.isDirectory()) {
            JSONArray children = new JSONArray();
            for (GpFilePath child : file.getChildren()) {
                if (child.isDirectory() || !dirOnly) {
                    JSONObject childJSON = makeFileJSON((ServerFilePath) child, dirOnly);
                    children.put(childJSON);
                }
            }
            object.put(CHILDREN, children);
        }  
        
        JSONObject metadata = new JSONObject();
        metadata.put("id", file.getRelativePath().replaceAll("[^a-zA-Z0-9]", "_"));
        object.put(METADATA, metadata);
        
        object.put(DATA, data);
        if (file.isDirectory()) {
            object.put(STATE, STATE_CLOSED);
        }
        
        return object;
    }
}

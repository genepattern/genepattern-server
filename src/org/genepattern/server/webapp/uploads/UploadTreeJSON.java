package org.genepattern.server.webapp.uploads;

import java.util.*;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UploadTreeJSON extends JSONArray {
    public static Logger log = Logger.getLogger(UploadTreeJSON.class);
    
    public static final String EMPTY = "EMPTY_DIRECTORY";
    public static final String SAVE_TREE = "SAVE_TREE";
    
    public static final String DATA = "data";
    public static final String STATE = "state";
    public static final String STATE_CLOSED = "closed";
    public static final String CHILDREN = "children";  
    public static final String METADATA = "metadata";
    public static final String TITLE = "title";
    public static final String ATTR = "attr";
    
    public UploadTreeJSON(List<GpFilePath> files, String code) {
        try {
            List<JSONObject> toAdd = new ArrayList<JSONObject>();
            
            if (code.equals(EMPTY)) {
                JSONObject fj = makeEmptyDirectory();
                toAdd.add(fj);
            }
            else if (code.equals(SAVE_TREE)) {
                for (GpFilePath gsf: files) {
                    if (gsf.isDirectory()) {
                        JSONObject fj = makeFileJSON(gsf, true);
                        toAdd.add(fj);
                    }
                }
            }
            else {
                for (GpFilePath gsf: files) {
                    JSONObject fj = makeFileJSON(gsf);
                    toAdd.add(fj);
                }
            }
            
            for (JSONObject obj : toAdd) {
                this.put(obj);
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
    private static String makeTaskString(SortedSet<TaskInfo> tasks) {
        List<String> toReturn = new ArrayList<String>();
        for (TaskInfo task : tasks) {
            toReturn.add('"' + task.getLsid() + '"');
        }
        return toReturn.toString();
    }

    public static JSONObject makeFileJSON(GpFilePath file) throws Exception {
        return makeFileJSON(file, false);
    }

    public static JSONObject makeFileJSON(GpFilePath file, boolean dirOnly) throws Exception {
        JSONObject object = new JSONObject();
        
        JSONObject data = new JSONObject();
        data.put(TITLE, file.getName() + " ");
        
        JSONObject attr = new JSONObject();
        attr.put("href", file.getUrl());
        if (dirOnly) { attr.put("onclick", "JavaScript:handleSaveClick(this); return false;"); }
        else { attr.put("onclick", "JavaScript:handleTreeClick(this); return false;"); }
        attr.put("name", file.getName());

        // Add the Kind data
        String kind = file.getKind();
        attr.put("data-kind", kind);

        // Add partial file data
        boolean isPartial = (file.getNumParts() != file.getNumPartsRecd()) && (file.getNumParts() != 0);
        attr.put("data-partial", isPartial);
        
        data.put(ATTR, attr);

        if (file.isDirectory()) {
            SortedSet<GpFilePath> sortedChildren=new TreeSet<GpFilePath>(UploadFileServlet.dirFirstComparator);
            sortedChildren.addAll(file.getChildren());
            List<JSONObject> children = new ArrayList<JSONObject>();
            for (GpFilePath child : sortedChildren) {
                if (child.isDirectory() || !dirOnly) {
                    JSONObject childJSON = makeFileJSON(child, dirOnly);
                    children.add(childJSON);
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

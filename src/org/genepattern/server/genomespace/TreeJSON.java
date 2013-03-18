package org.genepattern.server.genomespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TreeJSON extends JSONArray {
    public static Logger log = Logger.getLogger(TreeJSON.class);
    
    public static final String EMPTY = "EMPTY_DIRECTORY";
    public static final String SAVE_TREE = "SAVE_TREE";
    
    public static final String DATA = "data";
    public static final String STATE = "state";
    public static final String STATE_CLOSED = "closed";
    public static final String CHILDREN = "children";  
    public static final String METADATA = "metadata";
    public static final String TITLE = "title";
    public static final String ATTR = "attr";
    
    public TreeJSON(List<GenomeSpaceFile> files) {
        this(files, "");
    }
    
    public TreeJSON(List<GenomeSpaceFile> files, String code) {
        try {
            List<JSONObject> toAdd = new ArrayList<JSONObject>();
            
            if (code.equals(EMPTY)) {
                JSONObject fj = makeEmptyDirectory();
                toAdd.add(fj);
            }
            else if (code.equals(SAVE_TREE)) {
                for (GenomeSpaceFile gsf: files) {
                    if (gsf.isDirectory()) {
                        JSONObject fj = makeFileJSON(gsf, true);
                        toAdd.add(fj);
                    }
                }
            }
            else {
                for (GenomeSpaceFile gsf: files) {
                    JSONObject fj = makeFileJSON(gsf);
                    toAdd.add(fj);
                }
            }
            
            // Sort the list alphabetically
            Collections.sort(toAdd, new TreeComparator());
            
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
    public static JSONObject makeFileJSON(GenomeSpaceFile file) throws Exception {
        return makeFileJSON(file, false);
    }
    
    public static JSONObject makeFileJSON(GenomeSpaceFile file, boolean dirOnly) throws Exception {
        JSONObject object = new JSONObject();
        
        JSONObject data = new JSONObject();
        data.put(TITLE, file.getName() + " ");
        
        JSONObject attr = new JSONObject();
        attr.put("href", file.getUrl());
        if (dirOnly) { attr.put("onclick", "JavaScript:handleSaveClick(this); return false;"); }
        else { attr.put("onclick", "JavaScript:handleTreeClick(this); return false;"); }
        attr.put("name", file.getFormattedId());
        
        data.put(ATTR, attr);
        
        if (file.isDirectory()) {
            List<JSONObject> children = new ArrayList<JSONObject>();
            for (GenomeSpaceFile child : file.getChildFilesNoLoad()) {
                if (child.isDirectory() || !dirOnly) {
                    JSONObject childJSON = makeFileJSON(child, dirOnly);
                    children.add(childJSON);
                }
            }
            
            // Sort the list alphabetically
            Collections.sort(children, new TreeComparator());
            
            object.put(CHILDREN, children);
        }  
        
        JSONObject metadata = new JSONObject();
        metadata.put("id", file.getFormattedId());
        object.put(METADATA, metadata);
        
        object.put(DATA, data);
        if (file.isDirectory()) {
            object.put(STATE, STATE_CLOSED);
        }
        
        return object;
    }
    
    public static class TreeComparator implements Comparator<JSONObject> {
        @Override
        public int compare(JSONObject obj1, JSONObject obj2) {
            String name1;
            String name2;
            try {
                JSONObject data1 = obj1.getJSONObject("data");
                JSONObject data2 = obj2.getJSONObject("data");
                name1 = data1.getString(TITLE);
                name2 = data2.getString(TITLE);
            }
            catch (JSONException e) {
                log.error("ERROR in TreeJSON getting title for sort: " + obj1 + " " + obj2);
                name1 = "ERROR1";
                name2 = "ERROR2";
            }
            
            return name1.compareToIgnoreCase(name2);
        }
    }
}

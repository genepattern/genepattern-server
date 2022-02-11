/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.serverfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.genepattern.drm.Memory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
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
    
    public ServerFileTreeJSON(final HttpServletRequest request, final List<GpFilePath> files) {
        this(request, files, "");
    }
    
    public ServerFileTreeJSON(final HttpServletRequest request, final List<GpFilePath> files, final String code) {
        try {
            List<JSONObject> toAdd = new ArrayList<JSONObject>();
            
            if (code.equals(EMPTY)) {
                JSONObject fj = makeEmptyDirectory();
                toAdd.add(fj);
            }
            else if (code.equals(SAVE_TREE)) {
                if (files != null) {
                    for (GpFilePath gsf: files) {
                        if (gsf.isDirectory()) {
                            JSONObject fj = makeFileJSON(request, gsf, true);
                            toAdd.add(fj);
                        }
                    }
                }
            }
            else {
                if (files != null) {
                    for (GpFilePath gsf: files) {
                        JSONObject fj = makeFileJSON(request, gsf);
                        toAdd.add(fj);
                    }
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
    
    protected static JSONObject makeEmptyDirectory() throws JSONException {
        JSONObject object = new JSONObject();
        object.put(DATA, "<em>Empty Directory</em>");
        return object;
    }

    protected static JSONObject makeFileJSON(final HttpServletRequest request, final GpFilePath file) throws Exception {
        return makeFileJSON(request, file, false);
    }
    
    protected static JSONObject makeFileJSON(final HttpServletRequest request, final GpFilePath file, final boolean dirOnly) throws Exception {
        JSONObject object = new JSONObject();
        
        JSONObject data = new JSONObject();
        if ("".equals(file.getName())) {
            data.put(TITLE, "/ ");
        }
        else
        {
            String title = file.getName();

            if(!file.isDirectory())
            {
                title += "  (" + Memory.formatNumBytes(file.getFileLength()) + ")" + " ";
            }

            data.put(TITLE, title);
        }

        JSONObject attr = new JSONObject();

        final String href=UrlUtil.getHref(request, file);        
        attr.put("href", href);
        if (dirOnly) { attr.put("onclick", "JavaScript:handleServerFileClick(event, this); return false;"); }
        else { attr.put("onclick", "JavaScript:handleServerFileClick(event, this); return false;"); }
        attr.put("name", file.getName());

        data.put(ATTR, attr);
        
        if (file.isDirectory()) {
            List<JSONObject> children = new ArrayList<JSONObject>();
            for (GpFilePath child : file.getChildren()) {
                if (child.isDirectory() || !dirOnly) {
                    JSONObject childJSON = makeFileJSON(request, (ServerFilePath) child, dirOnly);
                    children.add(childJSON);
                }
            }
            
            // Sort the list alphabetically
            Collections.sort(children, new TreeComparator());
            
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

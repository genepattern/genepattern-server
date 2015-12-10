/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.uploads;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.webapp.jsf.JobHelper;
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
    
    public UploadTreeJSON(final HttpServletRequest request, final List<GpFilePath> files, final String code) {
        try {
            List<JSONObject> toAdd = new ArrayList<JSONObject>();
            
            if (code.equals(EMPTY)) {
                JSONObject fj = makeEmptyDirectory();
                toAdd.add(fj);
            }
            else if (code.equals(SAVE_TREE)) {
                for (GpFilePath gsf: files) {
                    if (gsf.isDirectory()) {
                        JSONObject fj = makeFileJSON(request, gsf, true);
                        toAdd.add(fj);
                    }
                }
            }
            else {
                for (GpFilePath gsf: files) {
                    JSONObject fj = makeFileJSON(request, gsf);
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

    public static JSONObject makeFileJSON(final HttpServletRequest request, final GpFilePath file) throws Exception {
        return makeFileJSON(request, file, false);
    }

    public static JSONObject makeFileJSON(final HttpServletRequest request, final GpFilePath file, final boolean dirOnly) throws Exception {
        JSONObject object = new JSONObject();
        
        JSONObject data = new JSONObject();
        data.put(TITLE, file.getName() + " ");
        
        JSONObject attr = new JSONObject();
        final String href=UrlUtil.getHref(request, file);
        attr.put("href", href);
        if (dirOnly) { attr.put("onclick", "JavaScript:handleSaveClick(this); return false;"); }
        else { attr.put("onclick", "JavaScript:handleTreeClick(this); return false;"); }
        attr.put("name", file.getName());

        // Add the Kind data
        String kind = file.getKind();
        attr.put("data-kind", kind);

        // Add partial file data
        boolean isPartial = (file.getNumParts() > file.getNumPartsRecd());
        attr.put("data-partial", isPartial);

        // Add tooltip text
        if (!"directory".equals(kind)) {
            String hoverString = "";
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            if (file.getLastModified() !=  null) {
                hoverString += df.format(file.getLastModified()) + " - ";
            }
            attr.put("title", hoverString + JobHelper.getFormattedSize(file.getFileLength()));
        }
        
        data.put(ATTR, attr);

        if (file.isDirectory()) {
            SortedSet<GpFilePath> sortedChildren=new TreeSet<GpFilePath>(UploadFileServlet.dirFirstComparator);
            sortedChildren.addAll(file.getChildren());
            List<JSONObject> children = new ArrayList<JSONObject>();
            for (GpFilePath child : sortedChildren) {
                if (child.isDirectory() || !dirOnly) {
                    JSONObject childJSON = makeFileJSON(request, child, dirOnly);
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

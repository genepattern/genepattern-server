/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpSession;

public class GenomeSpaceTreeJSON extends JSONArray {
    public static Logger log = Logger.getLogger(GenomeSpaceTreeJSON.class);

    public static final String EMPTY = "EMPTY_DIRECTORY";
    public static final String SAVE_TREE = "SAVE_TREE";

    public static final String DATA = "data";
    public static final String STATE = "state";
    public static final String STATE_CLOSED = "closed";
    public static final String CHILDREN = "children";
    public static final String METADATA = "metadata";
    public static final String TITLE = "title";
    public static final String ATTR = "attr";

    public GenomeSpaceTreeJSON(HttpSession session, List<GenomeSpaceFile> files) {
        this(session, files, "");
    }

    public GenomeSpaceTreeJSON(HttpSession session, List<GenomeSpaceFile> files, String code) {
        try {
            List<JSONObject> toAdd = new ArrayList<JSONObject>();

            if (code.equals(EMPTY)) {
                JSONObject fj = makeEmptyDirectory();
                toAdd.add(fj);
            }
            else if (code.equals(SAVE_TREE)) {
                for (GenomeSpaceFile gsf: files) {
                    if (gsf.isDirectory()) {
                        JSONObject fj = makeFileJSON(session, gsf, true);
                        toAdd.add(fj);
                    }
                }
            }
            else {
                for (GenomeSpaceFile gsf: files) {
                    JSONObject fj = makeFileJSON(session, gsf);
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
    public static JSONObject makeFileJSON(HttpSession session, GenomeSpaceFile file) throws Exception {
        return makeFileJSON(session, file, false);
    }

    private static String makeKindString(Set<String> formats) {
        List<String> toReturn = new ArrayList<String>();
        for (String kind : formats) {
            toReturn.add('"' + kind + '"');
        }
        return toReturn.toString();
    }

    private static String makeClientString(Set<String> formats, HttpSession session) {
        Set<String> allClients = new TreeSet<String>();
        Map<String, Set<String>> kindToTools = GenomeSpaceManager.getKindToToolsLazy(session);
        for (String kind : formats) {
            Set<String> tools = kindToTools.get(kind);
            if (tools != null) {
                for (String tool: tools) {
                    allClients.add('"' + tool + '"');
                }
            }
        }

        return allClients.toString();
    }

    private static String makeConversionString(Map<String, String> conversionMap) throws UnsupportedEncodingException, JSONException {
        JSONObject toReturn = new JSONObject();

        for (Map.Entry entry : conversionMap.entrySet()) {
            String kind = (String) entry.getKey();
            String url = URLDecoder.decode((String) entry.getValue(), "UTF-8");
            toReturn.put(kind, url);
        }

        return toReturn.toString();
    }

    public static JSONObject makeFileJSON(HttpSession session, GenomeSpaceFile file, boolean dirOnly) throws Exception {
        JSONObject object = new JSONObject();

        JSONObject data = new JSONObject();
        data.put(TITLE, file.getName() + " ");

        JSONObject attr = new JSONObject();
        attr.put("href", file.getUrl());
        if (dirOnly) { attr.put("onclick", "JavaScript:handleSaveClick(this); return false;"); }
        else { attr.put("onclick", "JavaScript:openFileWidget(this, '#menus-genomespace'); return false;"); }
        attr.put("name", file.getFormattedId());

        // Add the kind data
        Set<String> formats = file.getAvailableFormats();
        attr.put("data-kind", makeKindString(formats));

        // Add conversion URLs
        Map<String, String> conversionMap = file.getConversionUrls();
        attr.put("data-converturls", makeConversionString(conversionMap));

        // Add the directory data
        attr.put("data-directory", file.isDirectory());

        // Add the send to GenomeSpace client data
        String clientsString = makeClientString(formats, session);
        attr.put("data-clients", clientsString);

        data.put(ATTR, attr);

        if (file.isDirectory()) {
            List<JSONObject> children = new ArrayList<JSONObject>();
            for (GenomeSpaceFile child : file.getChildFilesNoLoad()) {
                if (child.isDirectory() || !dirOnly) {
                    JSONObject childJSON = makeFileJSON(session, child, dirOnly);
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

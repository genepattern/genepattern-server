/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.popupmenu;

import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class used to represent the JSON structure necessary for programmatically creating popup menus on the client
 * @author tabor
 *
 * This accepts a JSON object with the given format:
 * {
 *      id: "idOfMenu",
 *      items: [
 *          {
 *              "value": "displayValueOfOption",
 *              "attr": {
 *                  "href": "attributes of link",
 *                  "onclick": "no link is created if attr is empty array or null"
 *              }
 *          },
 *          "SEPERATOR" // Constant for inserting a seperator into the menu
 *      ]
 * }
 */
public class PopupMenuJSON extends JSONObject {
    public static final String SEPERATOR = "SEPERATOR";
    private String id = null;
    private JSONArray items = new JSONArray();
    
    public PopupMenuJSON(String id) {
        super();
        this.setId(id);
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public JSONArray getItems() {
        return items;
    }
    
    public void setItems(JSONArray items) {
        this.items = items;
    }
    
    /**
     * Adds a seperator to the menu
     */
    public void addSeperator() {
        items.put(SEPERATOR);
    }
    
    /**
     * Adds a menu item to the menu with a link containing the HTML attributes provided in the map
     * If the attribute map is null or empty will result in a menu item without a link
     * @param value
     * @param attr
     * @throws JSONException 
     */
    public void addMenuItem(String value, Map<String, String> attr) throws JSONException {
        items.put(new MenuItemJSON(value, attr));
    }
    
    /**
     * Adds a menu item to the menu without a link
     * @param value
     * @throws JSONException 
     */
    public void addMenuItem(String value) throws JSONException {
        items.put(new MenuItemJSON(value, null));
    }
    
    /**
     * Inner class used to represent a single item in a popup menu
     * @author tabor
     */
    public class MenuItemJSON extends JSONObject {
        private String value = null;
        private JSONObject attr = new JSONObject();
        
        public MenuItemJSON(String value, Map<String, String> attr) throws JSONException {
            this.setValue(value);
            if (attr == null) return;
            for (String i : attr.keySet()) {
                this.attr.put(i, attr.get(i));
            }
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public JSONObject getAttr() {
            return attr;
        }

        public void setAttr(JSONObject attr) {
            this.attr = attr;
        }
    }
}

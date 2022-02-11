/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.modules;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import org.apache.log4j.Logger;

/**
 * User: nazaire 
 * a copy of ResponseJSON class in the org.genepattern.pipelines package
 */

public class ResponseJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ResponseJSON.class);

    public void addChild(Object key, JSONArray object) {
        try {
            this.put(key.toString(), object);
        }
        catch (JSONException e) {
            log.error("Error attaching object to ResponseJSON: " + object);
        }
    }

    public void addChild(Object key, JSONObject object) {
        try {
            this.put(key.toString(), object);
        }
        catch (JSONException e) {
            log.error("Error attaching object to ResponseJSON: " + object);
        }
    }

    public void addChild(Object key, String value) {
        try {
            this.put(key.toString(), value);
        }
        catch (JSONException e) {
            log.error("Error attaching String to ResponseJSON: " + value);
        }
    }

    public void addMessage(String message) {
        try {
            this.put("MESSAGE", message);
        }
        catch (JSONException e) {
            log.error("Error attaching message to ResponseJSON: " + message);
        }
    }

    public void addError(String message) {
        try {
            this.put("ERROR", message);
        }
        catch (JSONException e) {
            log.error("Error attaching error to ResponseJSON: " + message);
        }
    }
}

package org.genepattern.pipelines;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class ResponseJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ResponseJSON.class);

    public void addChild(Object key, JSONObject object) {
        try {
            this.put(key.toString(), object);
        }
        catch (JSONException e) {
            log.error("Error attaching object to ResponseJSON: " + object);
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

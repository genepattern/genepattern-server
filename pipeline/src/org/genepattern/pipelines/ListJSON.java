package org.genepattern.pipelines;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class ListJSON extends JSONObject {
    public static Logger log = Logger.getLogger(ListJSON.class);
    
    private Integer counter = 0;
    
    public void addChild(JSONObject object) {
        try {
            this.put(counter.toString(), object);
        }
        catch (JSONException e) {
            log.error("Error attaching object to ListJSON: " + object);
        }
        counter++;
    }
    
    public void addError(String message) {
        try {
            this.put("ERROR", message);
        }
        catch (JSONException e) {
            log.error("Error attaching error to ListJSON: " + message);
        }
    }
}

package org.genepattern.server.genomespace;

import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TreeJSON extends JSONArray {
    public static Logger log = Logger.getLogger(TreeJSON.class);
    
    public static final String DATA = "data";
    public static final String STATE = "state";
    public static final String STATE_CLOSED = "closed";
    public static final String CHILDREN = "children";
    
    public TreeJSON(List<GenomeSpaceFile> files) {
        try {
            
            for (GenomeSpaceFile gsf: files) {
                JSONObject fj = makeFileJSON(gsf);
                this.put(fj);
            }
        }
        catch (JSONException e) {
            log.error("Unable to attach file array to TreeJSON Object: " + files);
        }
    }
    
    public static JSONObject makeFileJSON(GenomeSpaceFile file) throws JSONException {
        JSONObject object = new JSONObject();
        
        object.put(DATA, file.getName());
        object.put(STATE, STATE_CLOSED);
        
        return object;
    }
}

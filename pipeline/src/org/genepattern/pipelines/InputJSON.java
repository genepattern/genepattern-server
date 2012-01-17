package org.genepattern.pipelines;

import org.json.JSONArray;
import org.json.JSONObject;

public class InputJSON extends JSONObject {
    String name = null;
    String type = null;
    JSONArray kinds = null;
    Boolean required = null;
    Boolean promptWhenRun = null;
    String defaultValue = null;
    JSONArray choices = null;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public JSONArray getKinds() {
        return kinds;
    }
    
    public void setKinds(JSONArray kinds) {
        this.kinds = kinds;
    }
    
    public Boolean getRequired() {
        return required;
    }
    
    public void setRequired(Boolean required) {
        this.required = required;
    }
    
    public Boolean getPromptWhenRun() {
        return promptWhenRun;
    }
    
    public void setPromptWhenRun(Boolean promptWhenRun) {
        this.promptWhenRun = promptWhenRun;
    }
}

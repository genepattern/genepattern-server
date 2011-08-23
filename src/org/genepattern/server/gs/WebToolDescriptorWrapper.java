package org.genepattern.server.gs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebToolDescriptorWrapper {
    private String tool;
    private Map<String, Boolean> typeMap = new HashMap<String, Boolean>();
    private boolean init = false;
    private Map<String, List<String>> gsClientTypes;
    
    
    public WebToolDescriptorWrapper(String tool, Map<String, List<String>> gsClientTypes) {
        this.tool = tool;
        this.gsClientTypes = gsClientTypes;
    }
    
    public String getTool() {
        return tool;
    }
    
    public void setTool(String tool) {
        this.tool = tool;
    }
    
    public Map<String, Boolean> getTypeMap() {
        if (!init) {
            List<String> types = gsClientTypes.get(tool);
            for (String i : types) {
                typeMap.put(i, true);
            }
            init = true;
        }
        
        return typeMap;
    }
    
    public void setTypeMap(Map<String, Boolean> typeMap) {
        this.typeMap = typeMap;
    }
}

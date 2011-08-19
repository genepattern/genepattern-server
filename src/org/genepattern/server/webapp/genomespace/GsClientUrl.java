package org.genepattern.server.webapp.genomespace;

import java.net.URL;

public class GsClientUrl {
    private String tool;
    private URL url;
    
    public GsClientUrl(String tool, URL url) {
        this.tool = tool;
        this.url = url;
    }
    
    public String getTool() {
        return tool;
    }
    
    public void setTool(String tool) {
        this.tool = tool;
    }
    
    public URL getUrl() {
        return url;
    }
    
    public void setUrl(URL url) {
        this.url = url;
    }
}

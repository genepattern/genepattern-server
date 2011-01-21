package org.genepattern.server.webapp.uploads;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.TaskInfo;

public class UploadFileInfo {

    String filename;
    String path;
    String url;
    String genePatternUrl;
    List<KeyValuePair> moduleInputParameters;
    List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();

    public UploadFileInfo(String aFileName) {
        this.filename = aFileName;
    }

    public String getFilename() {
        return filename;
    }

    public void setUrl(String u) {
        url = u;
    }

    public String getUrl() {
        return url;
    }

    /**
     * URL used for local genepattern access only, not viable for other uses
     * 
     * @return
     */
    public String getGenePatternUrl() {
        return genePatternUrl;
    }

    public void setGenePatternUrl(String genePatternUrl) {
        this.genePatternUrl = genePatternUrl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<KeyValuePair> getModuleInputParameters() {
        return moduleInputParameters;
    }

    public void setModuleInputParameters(
            List<KeyValuePair> moduleInputParameters) {
        this.moduleInputParameters = moduleInputParameters;
    }

    public List<KeyValuePair> getModuleMenuItems() {
        return moduleMenuItems;
    }

    public void setModuleMenuItems(List<KeyValuePair> moduleMenuItems) {
        this.moduleMenuItems = moduleMenuItems;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getKind() {
        int dotIndex = filename.lastIndexOf(".");
        String extension = null;
        if (dotIndex > 0) {
            extension = filename.substring(dotIndex + 1, filename.length());
        }
        else {
            return null;
        }
        return extension;
    }

}

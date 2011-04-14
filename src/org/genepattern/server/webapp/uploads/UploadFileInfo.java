package org.genepattern.server.webapp.uploads;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.genepattern.server.webapp.jsf.KeyValuePair;

public class UploadFileInfo {
    
    static SimpleDateFormat formatter = new SimpleDateFormat();
    static {
        formatter.applyPattern("MMM dd hh:mm:ss aaa");
    }

    String filename;
    String path;
    String url;
    String genePatternUrl;
    List<KeyValuePair> moduleInputParameters;
    List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
    long modified;
    boolean directUpload = false;
    int copies = 1;
    
    public boolean getPartial() {
        if (filename.endsWith(".part")) {
            return true;
        }
        else {
            return false;
        }
    }
    
    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }
    
    public void incrementCopies() {
        this.copies += 1;
    }

    public boolean getDirectUpload() {
        return directUpload;
    }

    public void setDirectUpload(boolean directUpload) {
        this.directUpload = directUpload;
    }

    public String getFormattedModified() {
        return formatter.format(new Date(modified));
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }

    public UploadFileInfo(String aFileName) {
        this.filename = aFileName;
    }

    public String getFilename() {
        return filename;
    }
    
    public String getEncodedFilename() {
        return filename.replaceAll(" ", "%20");
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

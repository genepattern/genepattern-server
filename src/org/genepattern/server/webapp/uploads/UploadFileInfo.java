package org.genepattern.server.webapp.uploads;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

public class UploadFileInfo {
    
    static final String FILEMAP = "UPLOAD_FILE_INFO_MAP";
    static SimpleDateFormat formatter = new SimpleDateFormat();
    static {
        formatter.applyPattern("MMM dd hh:mm:ss aaa");
    }

    private File file;
    private String filename;
    private String path;
    private String url;
    private String genePatternUrl;
    private List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
    private long modified;
    boolean directUpload = false;
    private int copies = 1;
    
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

    public UploadFileInfo(File file) {
        this.file = file;
        if (file != null) {
            this.filename = file.getName();
        }
    }
    
    public File getFile() {
        return file;
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
        Map<String, List<KeyValuePair>> fileMap = (Map<String, List<KeyValuePair>>) UIBeanHelper.getSession().getAttribute(FILEMAP);
        
        // Lazily create if the map is null
        if (fileMap == null) {
            fileMap = new HashMap<String, List<KeyValuePair>>();
            UIBeanHelper.getSession().setAttribute(FILEMAP, fileMap);
        }
        
        return fileMap.get(this.getPath() + "/" + this.getFilename());
    }

    public void setModuleInputParameters(List<KeyValuePair> moduleInputParameters) {
        Map<String, List<KeyValuePair>> fileMap = (Map<String, List<KeyValuePair>>) UIBeanHelper.getSession().getAttribute(FILEMAP);
        
        // Lazily create if the map is null
        if (fileMap == null) {
            fileMap = new HashMap<String, List<KeyValuePair>>();
            UIBeanHelper.getSession().setAttribute(FILEMAP, fileMap);
        }
        
        fileMap.put(this.getPath() + "/" + this.getFilename(), moduleInputParameters);
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

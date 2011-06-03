package org.genepattern.server.webapp.genomespace;

import java.util.*;

import org.genomespace.datamanager.core.GSFileMetadata;
import org.genepattern.server.webapp.jsf.KeyValuePair;

public class GenomeSpaceFileInfo {

    public GSFileMetadata gsFile;
    public String filename;
    String url;
    List<KeyValuePair> moduleInputParameters;
    List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
    GenomeSpaceDirectory dir;

    public GenomeSpaceFileInfo(GSFileMetadata md, GenomeSpaceDirectory parent){
        gsFile = md;
        filename = md.getName();
        dir = parent;
    }
    
    public GenomeSpaceDirectory getDir() {
        return dir;
    }


    public void setDir(GenomeSpaceDirectory dir) {
        this.dir = dir;
    }


    public GSFileMetadata getGsFile() {
        return gsFile;
    }


    public void setGsFile(GSFileMetadata file) {
        this.gsFile = file;
    }

    public void setUrl(String u){
        url = u;
    }
    
    public String getUrl() {
        return url;
    }
    
    
    public String getFilename() {
        return filename;
    }


    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public List<KeyValuePair> getModuleInputParameters() {
        return moduleInputParameters;
    }

    public void setModuleInputParameters(List<KeyValuePair> moduleInputParameters) {
        this.moduleInputParameters = moduleInputParameters;
    }

    public List<KeyValuePair> getModuleMenuItems() {
        return moduleMenuItems;
    }

    public void setModuleMenuItems(List<KeyValuePair> moduleMenuItems) {
        this.moduleMenuItems = moduleMenuItems;
    }
}

package org.genepattern.server.gs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.webservice.ParameterInfo;

public class GenomeSpaceFileInfo {
    public static final String DIRECTORY = "directory";
    
    public String filename;
    String url;
    List<KeyValuePair> moduleInputParameters;
    List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
    GenomeSpaceDirectory dir;
    Set<String> toolUrls;
    Set<String> availableDataFormats;
    boolean directory = false;;
    Date lastModified;
    Object metadata;

    public GenomeSpaceFileInfo(GenomeSpaceDirectory parent, String filename, String url, Set<String> availableDataFormats, Date lastModified, Object metadata) {
        this.filename = filename;
        this.dir = parent;
        this.url = url;
        this.lastModified = lastModified;
        this.metadata = metadata;
        if (availableDataFormats == null) {
            this.availableDataFormats = new HashSet<String>();
        }
        else {
            this.availableDataFormats = availableDataFormats;
        }
        if (url.equals(GenomeSpaceFileInfo.DIRECTORY)) {
            directory = true;
        }
    }
    
    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
    
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
    
    public GenomeSpaceDirectory getDir() {
        return dir;
    }

    public void setDir(GenomeSpaceDirectory dir) {
        this.dir = dir;
    }
    
    public boolean isDirectory() {
        return directory;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
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
    
    public String getType() {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    public List<ParameterInfo> getSendToParameters() {
        GenomeSpaceBeanHelper gsb = (GenomeSpaceBeanHelper)UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
        String type = getType();
        return gsb.getSendToParameters(type);
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
    
    public String getKey() {
        return super.toString();
    }
    
    public Set<String> getConversions() {
        return availableDataFormats;
    }
    
    public Set<String> getRelevantTools() {
        if (toolUrls == null) {
            toolUrls = new HashSet<String>();
            GenomeSpaceBeanHelper gsb = (GenomeSpaceBeanHelper)UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
            Set<String> types = getConversions();
            Map<String, List<String>> gsClientTypes = gsb.getGsClientTypes();
            for (String i : gsClientTypes.keySet()) {
                for (String j : gsClientTypes.get(i)) {
                    for (String k : types) {
                        if (j.equals(k)) {
                            toolUrls.add(i);
                        }
                    }
                }
            }
        }
        return toolUrls;
    }
}

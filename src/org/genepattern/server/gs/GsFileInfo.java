package org.genepattern.server.gs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.genepattern.server.webapp.jsf.KeyValuePair;

public class GsFileInfo {
    public static final String DIRECTORY = "directory";
    
    private GsDirectoryInfo parent;
    private String filename;
    private String url;
    private List<KeyValuePair> moduleInputParameters;
    private List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
    private Set<String> toolUrls;
    private Set<String> availableDataFormats;
    private boolean directory = false;;
    private Date lastModified;
    //Object metadata;
    private List<GsClientUrl> gsClientUrls = new ArrayList<GsClientUrl>();

    public GsFileInfo() {
    }
//    public GsFileInfo(GsDirectoryInfo parent, String filename, String url, Set<String> availableDataFormats, Date lastModified, Object metadata, Map<String, List<String>> gsClientTypes) {
//        this.parent = parent;
//        this.filename = filename;
//        this.url = url;
//        this.lastModified = lastModified;
//        this.metadata = metadata;
//        if (availableDataFormats == null) {
//            this.availableDataFormats = new HashSet<String>();
//        }
//        else {
//            this.availableDataFormats = availableDataFormats;
//        }
//        if (url.equals(GenomeSpaceFileInfo.DIRECTORY)) {
//            directory = true;
//        }
//        initRelevantTools(gsClientTypes);
//    }

    public void setParent(GsDirectoryInfo parent) {
        this.parent = parent;
    }
    public GsDirectoryInfo getParent() {
        return parent;
    }
    
//    public Object getMetadata() {
//        return metadata;
//    }
//
//    public void setMetadata(Object metadata) {
//        this.metadata = metadata;
//    }
    
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
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
    
//    public List<ParameterInfo> getSendToParameters() {
//        GenomeSpaceBeanHelper gsb = (GenomeSpaceBeanHelper)UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
//        String type = getType();
//        return gsb.getSendToParameters(type);
//    }

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
        return toolUrls;
    }
    
//    private Set<String> initRelevantTools(Map<String, List<String>> gsClientTypes) {
//        Set<String> relevantTools = new HashSet<String>();
//        Set<String> types = getConversions();
//        for (String i : gsClientTypes.keySet()) {
//            for (String j : gsClientTypes.get(i)) {
//                for (String k : types) {
//                    if (j.equals(k)) {
//                        relevantTools.add(i);
//                    }
//                }
//            }
//        }
//        return relevantTools;
//    }

}

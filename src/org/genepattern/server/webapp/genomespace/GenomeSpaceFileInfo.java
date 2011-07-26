package org.genepattern.server.webapp.genomespace;

import java.util.*;

import org.genomespace.atm.model.WebToolDescriptor;
import org.genomespace.client.exceptions.InternalServerException;
import org.genomespace.datamanager.core.GSDataFormat;
import org.genomespace.datamanager.core.GSFileMetadata;
import org.genepattern.server.webapp.genomespace.GenomeSpaceBean.GSClientUrl;
import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.webservice.ParameterInfo;

public class GenomeSpaceFileInfo {

    public GSFileMetadata gsFile;
    public String filename;
    String url;
    List<KeyValuePair> moduleInputParameters;
    List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
    GenomeSpaceDirectory dir;
    Set<String> toolUrls;

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
    
    public String getType() {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    public List<ParameterInfo> getSendToParameters() {
        GenomeSpaceBean gsb = (GenomeSpaceBean)UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
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
        Set<String> types = new HashSet<String>();
        for (GSDataFormat i : gsFile.getAvailableDataFormats()) {
            types.add(i.getName());
        }
        types.add(this.getType());
        return types;
    }
    
    public Set<String> getRelevantTools() throws InternalServerException {
        if (toolUrls == null) {
            toolUrls = new HashSet<String>();
            GenomeSpaceBean gsb = (GenomeSpaceBean)UIBeanHelper.getManagedBean("#{genomeSpaceBean}");
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

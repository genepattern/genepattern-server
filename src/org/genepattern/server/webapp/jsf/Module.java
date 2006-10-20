package org.genepattern.server.webapp.jsf;

import java.util.*;

import javax.faces.model.SelectItem;

import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;

/**
 * Represents a task or pipeline including all versions. 
 * @author jrobinso
 *
 */
public class Module implements java.io.Serializable {
    
    private boolean selected = false;
    private String name;
    private String shortName;
    private String userId;
    boolean pipeline;
    private List<SelectItem> versions = new ArrayList<SelectItem>();
    private String selectedVersion = "latest";  // LSID of selected version
    
    public String getSelectedVersion() {
        return selectedVersion;
    }

    public void setSelectedVersion(String selectedVersion) {
        this.selectedVersion = selectedVersion;
    }

    public Module(TaskInfo ti, LSID lsid) {
        this.userId = ti.getUserId();
        this.name = ti.getName();
        this.shortName = ti.getShortName();
        pipeline = ti.isPipeline();
        
        // Add the "latest" version option by stripping out the version #
        versions.add(new SelectItem(lsid.toStringNoVersion(), "latest"));
                    
        versions.add(new SelectItem(lsid.toString(), lsid.getVersion()));
    }
    
    public void addVersion(LSID lsid) {
        versions.add(new SelectItem(lsid.toString(), lsid.getVersion()));
   }

    public String getName() {
        return name;
    }
    
    public String getShortName() {
        return shortName;
    }

     
    public boolean isPipeline() {
        return pipeline;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        System.out.println("Setting selected state=" + selected);
        this.selected = selected;
    }

    public List<SelectItem> getVersions() {
        return versions;
    }


    public String getUserId() {
        return userId;
    }


    public List<SelectItem> getVersionSelectItems() {
        Collections.sort(versions, new Comparator() {           
            public int compare(Object o1, Object o2) {
                String v1 = ((SelectItem) o1).getLabel();
                String v2 = ((SelectItem) o2).getLabel();
                if(v1.toLowerCase().equals("latest")) return -1;
                else if(v2.toLowerCase().equals("latest")) return 1;
                else return Integer.parseInt(v2) - Integer.parseInt(v1);
            }

        });
        return versions;
        
    }


}

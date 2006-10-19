package org.genepattern.server.webapp.jsf;

import java.util.*;

import javax.faces.model.SelectItem;

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
    private List<Integer> versions = new ArrayList<Integer>();
    private int selectedVersion = -1;
    
    public int getSelectedVersion() {
        return selectedVersion;
    }

    public void setSelectedVersion(int selectedVersion) {
        this.selectedVersion = selectedVersion;
    }

    public Module(TaskInfo ti, int version) {
        this.userId = ti.getUserId();
        this.name = ti.getName();
        this.shortName = ti.getShortName();
        pipeline = ti.isPipeline();
        versions.add(version);
    }
    
    public void addVersion(int version) {
        versions.add(version);
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

    public List<Integer> getVersions() {
        return versions;
    }


    public String getUserId() {
        return userId;
    }


    public List<SelectItem> getVersionSelectItems() {
        Collections.sort(versions, new Comparator() {           
            public int compare(Object o1, Object o2) {
                return ((Number) o2).intValue() - ((Number) o1).intValue();
            }

        });
 
        List<SelectItem> items = new ArrayList();
        for(Integer version : versions) {
            
            items.add(new SelectItem(version, version.toString()));
        }
        return items;
    }


}

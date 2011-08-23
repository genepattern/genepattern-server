package org.genepattern.server.gs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper class for GenomeSpace directory listing, which can be used from a JSF page.
 * 
 * @author pcarr
 */
public class GsDirectoryInfo {
    public GsDirectoryInfo() {
    }
    
    int level;
    private String name;
    boolean expanded;
    private List<GenomeSpaceFileInfo> gsFiles = new ArrayList<GenomeSpaceFileInfo>();
    private List<GsDirectoryInfo> gsDirectories = new ArrayList<GsDirectoryInfo>();
    private List<GenomeSpaceFileInfo> recursiveGsFiles = new ArrayList<GenomeSpaceFileInfo>();
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    public boolean isExpanded() {
        return expanded;
    }

    public void setLevel(int i) {
        this.level = i;
    }

    public int getLevel() {
        return this.level;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }

    public List<GenomeSpaceFileInfo> getGsFiles() {
        return Collections.unmodifiableList(gsFiles);
    }
    public List<GenomeSpaceFileInfo> getRecursiveGsFiles() {    
        return Collections.unmodifiableList(recursiveGsFiles);
    }
    public List<GsDirectoryInfo> getGsDirectories() {
        return Collections.unmodifiableList(gsDirectories);
    }
    
    public void addDirectory(GsDirectoryInfo dir) {
        this.gsDirectories.add(dir);
    }
    
    public void addFile(GenomeSpaceFileInfo file) {
        this.gsFiles.add( file );
    }
}

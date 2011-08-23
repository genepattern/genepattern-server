package org.genepattern.server.gs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.genepattern.webservice.TaskInfo;

public interface GenomeSpaceDirectory {
    void setGsFileList(Object gsSessionObj, String name, Set<GenomeSpaceFileInfo> files, Map<String, Set<TaskInfo>> kindToModules, Map<String, List<GsClientUrl>> clientUrls);
    String getName();
    void setName(String name);
    List<GenomeSpaceFileInfo> getRecursiveGsFiles();
    List<GenomeSpaceFileInfo> getGsFiles();
    void setGsFiles(List<GenomeSpaceFileInfo> gsFiles);
    List<GenomeSpaceDirectory> getGsDirectories();
    void setGsDirectories(List<GenomeSpaceDirectory> gsDirectories);
    int getLevel();
    void setLevel(int level);
    boolean isExpanded();
    void setExpanded(boolean expanded);
}
package org.genepattern.server.gs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.genepattern.webservice.TaskInfo;

public interface GenomeSpaceDirectory {

    public abstract void setGsFileList(String name, Set<GenomeSpaceFileInfo> files, Map<String, Set<TaskInfo>> kindToModules, GenomeSpaceBeanHelper genomeSpaceBean);

    public abstract String getName();

    public abstract void setName(String name);

    public abstract List<GenomeSpaceFileInfo> getRecursiveGsFiles();

    public abstract List<GenomeSpaceFileInfo> getGsFiles();

    public abstract void setGsFiles(List<GenomeSpaceFileInfo> gsFiles);

    public abstract List<GenomeSpaceDirectory> getGsDirectories();

    public abstract void setGsDirectories(List<GenomeSpaceDirectory> gsDirectories);

    public abstract int getLevel();

    public abstract void setLevel(int level);

    public abstract boolean isExpanded();

    public abstract void setExpanded(boolean expanded);

}
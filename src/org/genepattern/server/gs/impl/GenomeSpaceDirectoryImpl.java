package org.genepattern.server.gs.impl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.genepattern.server.gs.GenomeSpaceBeanHelper;
import org.genepattern.server.gs.GenomeSpaceDirectory;
import org.genepattern.server.gs.GenomeSpaceFileInfo;
import org.genepattern.server.webapp.jsf.KeyValuePair;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.TaskInfo;
import org.genomespace.client.DataManagerClient;
import org.genomespace.datamanager.core.GSDataFormat;
import org.genomespace.datamanager.core.GSDirectoryListing;
import org.genomespace.datamanager.core.GSFileMetadata;

public class GenomeSpaceDirectoryImpl implements GenomeSpaceDirectory {
    public GSFileMetadata dir;
    public String name;
    public List<GenomeSpaceFileInfo> gsFiles;
    public List<GenomeSpaceDirectory> gsDirectories;
    public int level = 0;
    public boolean expanded = true;
    private static final Comparator<KeyValuePair> COMPARATOR = new KeyValueComparator();
    
    private GenomeSpaceDirectoryImpl() {
        gsFiles = new ArrayList<GenomeSpaceFileInfo>();
        gsDirectories = new ArrayList<GenomeSpaceDirectory>();
    }
    
    public GenomeSpaceDirectoryImpl(String name, int level, GSDirectoryListing adir, DataManagerClient dmClient, Map<String, Set<TaskInfo>> kindToModules, GenomeSpaceBeanHelper genomeSpaceBean) {
        this(); 
        this.name = name;
        this.level = level;
        List<GSFileMetadata> metadatas = adir.findFiles();
        Set<GenomeSpaceFileInfo> files = new HashSet<GenomeSpaceFileInfo>();
        for (GSFileMetadata i : metadatas) {
            Set<String> formats = new HashSet<String>();
            for (GSDataFormat j : i.getAvailableDataFormats()) {
                formats.add(j.getName());
            }
            String url = getFileURL(i, dmClient);
            ((GenomeSpaceBeanHelperImpl) genomeSpaceBean).getMetadatas().put(url, i);
            files.add(new GenomeSpaceFileInfo(this, i.getName(), url, formats, i.getLastModified()));
        }

        for (GSFileMetadata gsdir: adir.findDirectories()) {
            gsDirectories.add(new GenomeSpaceDirectoryImpl(gsdir.getName(), level + 1, dmClient.list(gsdir), dmClient, kindToModules, genomeSpaceBean));
        }
        setGsFileList(name, files, kindToModules, genomeSpaceBean);
    }

    
    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#setGsFileList(org.genomespace.datamanager.core.GSDirectoryListing, java.util.Map, org.genepattern.server.gs.GenomeSpaceBeanHelper)
     */
    public void setGsFileList(String name, Set<GenomeSpaceFileInfo> files, Map<String, Set<TaskInfo>> kindToModules, GenomeSpaceBeanHelper genomeSpaceBean) {
        this.gsFiles = new ArrayList<GenomeSpaceFileInfo>();
        for (GenomeSpaceFileInfo info: files){
            this.gsFiles.add(info);
            
            String kind = SemanticUtil.getKind(new File(info.getFilename()));
            Collection<TaskInfo> modules;
            List<KeyValuePair> moduleMenuItems = new ArrayList<KeyValuePair>();
            modules = kindToModules.get(kind);
           
            if (modules != null) {
                for (TaskInfo t : modules) {
                    KeyValuePair mi = new KeyValuePair(t.getShortName(), UIBeanHelper.encode(t.getLsid()));
                    moduleMenuItems.add(mi);
                }
                Collections.sort(moduleMenuItems, COMPARATOR);
            }
            info.setModuleMenuItems(moduleMenuItems);
            genomeSpaceBean.addToClientUrls(info);
        }
    
    }
    
    private String getFileURL(GSFileMetadata gsFile, DataManagerClient dmClient) {
        if (gsFile == null) return null;
        URL s3Url = dmClient.getFileUrl(gsFile, null);
        return s3Url.toString();
    }
    

    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#getDir()
     */
    public GSFileMetadata getDir() {
        return dir;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#setDir(org.genomespace.datamanager.core.GSFileMetadata)
     */
    public void setDir(GSFileMetadata dir) {
        this.dir = dir;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#getName()
     */
    public String getName() {
        return name;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#getRecursiveGsFiles()
     */
    public List<GenomeSpaceFileInfo> getRecursiveGsFiles() {
        List<GenomeSpaceFileInfo> allFiles = new ArrayList<GenomeSpaceFileInfo>();
        allFiles.addAll(gsFiles);
        for (GenomeSpaceDirectory i : gsDirectories) {
            allFiles.addAll(i.getRecursiveGsFiles());
        }
        return allFiles;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#getGsFiles()
     */
    public List<GenomeSpaceFileInfo> getGsFiles() {
        return gsFiles;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#setGsFiles(java.util.List)
     */
    public void setGsFiles(List<GenomeSpaceFileInfo> gsFiles) {
        this.gsFiles = gsFiles;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#getGsDirectories()
     */
    public List<GenomeSpaceDirectory> getGsDirectories() {
        return gsDirectories;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#setGsDirectories(java.util.List)
     */
    public void setGsDirectories(List<GenomeSpaceDirectory> gsDirectories) {
        this.gsDirectories = gsDirectories;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#getLevel()
     */
    public int getLevel() {
        return level;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#setLevel(int)
     */
    public void setLevel(int level) {
        this.level = level;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#isExpanded()
     */
    public boolean isExpanded() {
        return expanded;
    }


    /* (non-Javadoc)
     * @see org.genepattern.server.gs.IGenomeSpaceDirectory#setExpanded(boolean)
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
    
    private static class KeyValueComparator implements Comparator<KeyValuePair> {
        public int compare(KeyValuePair o1, KeyValuePair o2) {
            return o1.getKey().compareToIgnoreCase(o2.getKey());
        }
    }
}

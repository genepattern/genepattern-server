package org.genepattern.server.gs.impl;


/**
 * Factory method for creating instances of GenomeSpaceFileInfo and GenomeSpaceDirectory instances.
 * This class can use genomespace libraries as method arguments but must return classes which 
 * do not depend on genomespace libraries.
 * 
 * Note: this is better than creating an interface to wrap a Bean type.
 * @author pcarr
 *
 */
public class GsFileFactory {
//    public GenomeSpaceFileInfo createFileInfo() {
//    }
    
//    public GenomeSpaceDirectory createDirectory() {
//    }
//    /** 
//     * Initialize the GenomeSpace directory for the current session.
//     * @param gsSessionObj
//     * @return
//     */
//    static public List<GsDirectoryInfo> initUserDirs(Object gsSessionObj) {
//        List<GsDirectoryInfo> userDirs = new ArrayList<GsDirectoryInfo>();
//        if (gsSessionObj instanceof GsSession) {
//        }
//        else {
//            log.error("Invalid gsSessionObj, returning empty list");
//            return userDirs;
//        }
//        GsSession gsSession = (GsSession) gsSessionObj;
//        if (! gsSession.isLoggedIn()) {
//            return userDirs;            
//        }
//        DataManagerClient dmClient = gsSession.getDataManagerClient();
//        GSDirectoryListing rootDir = dmClient.listDefaultDirectory();
//        
//        GsDirectoryInfo userDir = createDirectory( rootDir );
//        userDirs.add( userDir );
//        return userDirs;
//    }

//    private static GsDirectoryInfo createDirectory(GSDirectoryListing rootDir) { 
//        GsDirectoryInfo createDir(
//        String name = "GenomeSpace Files";
//        int level = 0;
//        List<GSFileMetadata> files = rootDir.findFiles();
//        for(GSFileMetadata fileMetadata : files) {
//            GsFileInfo gsFileInfo = createFile(fileMetadata);
//        }
//        List<GSFileMetadata> dirs = rootDir.findDirectories();
//        for(GSFileMetadata dir : dirs) {
//        }
//        
//
//    }
//    private static GsFileInfo createFile(GsDirectoryInfo parent, GSFileMetadata metadata) {
//        GsFileInfo gsFileInfo = new GsFileInfo();
//        gsFileInfo.setParent(parent)
//        
//    }
//    
//        public GenomeSpaceDirectoryImpl(String name, int level, GSDirectoryListing adir, DataManagerClient dmClient, Map<String, Set<TaskInfo>> kindToModules) {
//        this(); 
//        this.name = name;
//        this.level = level;
//        List<GSFileMetadata> metadatas = adir.findFiles();
//        Set<GenomeSpaceFileInfo> files = new HashSet<GenomeSpaceFileInfo>();
//        for (GSFileMetadata i : metadatas) {
//            Set<String> formats = new HashSet<String>();
//            for (GSDataFormat j : i.getAvailableDataFormats()) {
//                formats.add(j.getName());
//            }
//            String url = getFileURL(i, dmClient);
//            files.add(new GenomeSpaceFileInfo(this, i.getName(), url, formats, i.getLastModified(), i));
//        }
//
//        for (GSFileMetadata gsdir: adir.findDirectories()) {
//            gsDirectories.add(new GenomeSpaceDirectoryImpl(gsdir.getName(), level + 1, dmClient.list(gsdir), dmClient, kindToModules));
//        }
//        setGsFileList(name, files, kindToModules);
//    }
//
 
}

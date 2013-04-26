package org.genepattern.server.process;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.FileUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;

/**
 * Example implementation which purges files from the user upload directory,
 * by walking the directory tree on the server's file system.
 * 
 * @see UserUploadPurger
 * 
 * @author pcarr
 */
public class UserUploadPurgerDirectoryWalker extends DirectoryWalker<File> {
    private static Logger log = Logger.getLogger(UserUploadPurgerDirectoryWalker.class);
    
    private UserUploadPurger uup;
    private Context userContext;
    private long dateCutoff;
    private boolean purgeAll;
    private GpFilePath userUploadDirPath;

    //by default, this should be false, when porting this code for syncing the uploads directory,
    //  we have the option of following symbolic links
    //  for deleting files, we don't want to inadvertently delete files which weren't directly uploaded to the GP server
    private boolean followSymLinks = false;
    private Set<String> visitedDirs;

    
    //private ExecutorService exec;
    
    private static FilenameFilter fileExcludesFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            if ( DataManager.FILE_EXCLUDES.contains( name ) ) {
                return false;
            }
            return true;
        }
    };

    public UserUploadPurgerDirectoryWalker(UserUploadPurger uup) {
        this.uup = uup;
        this.userContext = uup.getUserContext();
        this.dateCutoff = uup.getDateCutoff();
        this.purgeAll=ServerConfiguration.instance().getGPBooleanProperty(userContext, UserUploadPurger.PROP_PURGE_ALL, false);
    }
    
    public void purgeFromFileTree() throws Exception {
        this.userUploadDirPath = UserUploadManager.getUserUploadDir(userContext);
        File uploadDir = userUploadDirPath.getServerFile();
        log.debug("walking directory tree, uploadDir="+uploadDir);
        List<File> results = new ArrayList<File>();
        walk(uploadDir, results); 
    }

    private GpFilePath getGpFilePath(File file) {
        GpFilePath gpFilePath = null;
        try {
            //get relative path
            File relativePath = FileUtil.relativizePath(userUploadDirPath.getServerFile(), file);
            boolean initMetaData = true;
            gpFilePath = UserUploadManager.getUploadFileObj(userContext, relativePath, initMetaData);
        }
        catch (Throwable t) {
            //this is probably a sym link which should be ignored
            String message = "Ignoring uploadFile='"+file.getPath()+"', Error: "+t.getLocalizedMessage();
            log.debug(message, t);
        }
        return gpFilePath;
    }

    /**
     * Called before traversing the contents of the directory.
     * 
     * @return false if we should ignore this directory.
     */
    protected boolean handleDirectory(File directory, int depth, Collection<File> results) throws IOException {
        if (directory == null) {
            log.error("Invalid null arg");
            return false;
        }
        log.debug("handleDirectory: "+directory.getPath());
        
        //skip anything that matches the filter
        boolean accepted = fileExcludesFilter.accept(directory.getParentFile(), directory.getName());
        if (!accepted) {
            log.debug("skipping: dir is excluded");
            return false;
        }

        //don't follow sym links
        if (!followSymLinks) {
            if (FileUtils.isSymlink(directory)) {
                log.debug("skipping: symbolic link");
                return false;
            }
        }
        else {
            //check for cycles
            String dirPath = directory.getCanonicalPath();
            if (visitedDirs.contains(dirPath)) {
                log.debug("skipping: previously visisted dir");
                return false;
            }
            visitedDirs.add(dirPath);
        }
        if (directory.exists() && directory.canRead()) {
            return true;
        }
        log.debug("skipping");
        return false;
    }

    /**
     * Called for each file, if it meets the criteria, purge it.
     */
    protected void handleFile(File file, int depth, Collection<File> results) {
        if (file == null) {
            log.error("Unexpected null arg");
            return;
        }
        
        log.debug("handleFile(file="+file+")");

        //skip anything that matches the filter
        boolean accepted = fileExcludesFilter.accept(file.getParentFile(), file.getName());
        if (!accepted) {
            log.debug("skipping: file is excluded");
            return;
        }

        if (file.lastModified() >= dateCutoff) {
            log.debug("skipping: lastModified >= cutoff");
            return;
        }

        final GpFilePath gpFilePath = getGpFilePath(file);
        if (gpFilePath == null) {
            log.debug("skipping: gpFilePath==null");
            return;
        }

        boolean isPartial = false;
        if (gpFilePath.getNumPartsRecd() != gpFilePath.getNumParts()) {
            isPartial = true;
            log.debug("partialUpload");
        }

        if (!purgeAll && !isPartial) {
            log.debug("skipping");
            return;
        }

        log.debug("purging");
        uup.purgeUserUploadFile(gpFilePath);
    }

}

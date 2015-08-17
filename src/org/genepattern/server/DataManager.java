/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.dm.userupload.dao.UserUpload;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;

/**
 * Utility class for managing data files.
 * 
 * @author pcarr
 *
 */
public class DataManager {
    private static Logger log = Logger.getLogger(DataManager.class);
    public final static List<String> FILE_EXCLUDES = new ArrayList<String>();
    static {
        FILE_EXCLUDES.add(".DS_Store");
        FILE_EXCLUDES.add("Thumbs.db");
    }
    
    /**
     * Helper method for initializing the relative File object from the web input form.
     * There are several ways to declare a file with no parent directory:
     *     parentPath==null, or
     *     parentPath==".", or
     *     parentPath=="./"
     * 
     * @param parentPath, can be null, ".", "./"
     * @param name
     * @return
     */
    public static File initSubdirectory(final String parentPath, final String name) {
        File relativePath = null;
        //numerous tests for the parentPath ...
        if (parentPath == null || parentPath.length() == 0 || ".".equals(parentPath) || "./".equals(parentPath) ) {
            //mkdir in the user's upload dir
            relativePath = new File(name);
        }
        else {
            //mkdir in a path relative to the user's upload dir
            relativePath = new File(parentPath, name);
        }
        return relativePath;
    }
    
    /**
     * Is the given relativePath the root temp directory for the given user?
     * 
     * @param userContext
     * @param relativePath
     * @return
     */
    final static public boolean isTmpDir(final GpContext userContext, final File relativePath) {
        //current implementation is: './tmp/'
        File parent=relativePath.getParentFile();
        if (parent != null && !".".equals(parent.getName())) {
            //it's a subdirectory, so it can't be the root tmp dir
            return false;
        }
        if (UserUploadDao.TMP_DIR.equals(relativePath.getName())) {
            return true;
        }
        return false;
    }
    
    final static public boolean isTmpDir(final GpFilePath gpFilePath) {
        boolean isInSubdir=gpFilePath.getRelativeFile().getParent() != null;
        if (!isInSubdir) {
            if (UserUploadDao.TMP_DIR.equals(gpFilePath.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a sub-directory in the user's upload directory and records the entry in the DB.
     * 
     *  TODO: re-factor this into the UserUploadManager (or at least into the dm package)
     * 
     * @param userContext, requires a valid userId
     * @param relativePath, the relative path to the parent directory, specified relative to the user's upload directory.
     * 
     * @return true if the directory was successfully created
     */
    public static boolean createSubdirectory(final HibernateSessionManager mgr, final GpContext userContext, final File relativePath) {
        GpFilePath subdirRef = null;
        try {
            //another option ... subdirRef = GpFileObjFactory.getUserUploadFile(userContext, relativePath);
            boolean initMetaData = false;
            subdirRef = UserUploadManager.getUploadFileObj(mgr, userContext, relativePath, initMetaData);
        }
        catch (Throwable t) {
            log.error(t.getLocalizedMessage());
            return false;
        }
        File dir = subdirRef.getServerFile();
        boolean success = false;
        try {
            success = dir.mkdir();
        }
        catch (Throwable t) {
            log.error("system error creating directory: "+subdirRef.getRelativeUri()+": "+t.getLocalizedMessage());
            return false;
        } 
        if (success) {
            //update the DB
            try {
                UserUploadManager.createUploadFile(mgr, userContext, subdirRef, 1);
                UserUploadManager.updateUploadFile(mgr, userContext, subdirRef, 1, 1);
            }
            catch (Throwable t) {
                log.error(t);
                success = false;
            }
        }
        return success;
    }

    /**
     * Copies a file to a new user upload location
     * @param user
     * @param from
     * @param to
     * @return
     */
    public static boolean copyToUserUpload(final HibernateSessionManager mgr, final String user, final GpFilePath from, final GpFilePath to) {
        boolean copied = false;

        File fromFile = from.getServerFile();
        File toFile = to.getServerFile();
        boolean directory = fromFile.isDirectory();

        // If the file are legit
        if (fromFile.exists() && !toFile.exists()) {
            try {
                // Do the file system copy
                if (!directory) {
                    FileUtils.copyFile(fromFile, toFile);
                    copied = true;
                }
                else {
                    FileUtils.copyDirectory(fromFile, toFile);
                    copied = true;
                }
            }
            catch (IOException e) {
                log.error("Failed to copy file from " + fromFile.getAbsolutePath() + " to " + toFile.getAbsolutePath());
            }

            // Update the database
            boolean inTransaction = mgr.isInTransaction();
            try {
                if (!directory) {
                    // Begin a new transaction
                    @SuppressWarnings("deprecation")
                    GpContext context = GpContext.getContextForUser(user);
                    UserUploadManager.createUploadFile(mgr, context, to, 1);
                    UserUploadManager.updateUploadFile(mgr, context, to, 1, 1);
                    if (!inTransaction) {
                        mgr.commitTransaction();
                    }
                }
                else {
                    syncUploadFiles(mgr, user);
                    copied = true;
                }
            }
            catch  (Throwable t) {
                copied = false;
                // Error updating the DB
                log.error("Error copying to user upload file record in db, '" + to.getRelativeUri() + "'", t);
                mgr.rollbackTransaction();
            }
        }

        return copied;
    }

    /**
     * Moves a file to a new user upload location
     * @param user
     * @param from
     * @param to
     * @return
     */
    public static boolean moveToUserUpload(HibernateSessionManager mgr, String user, GpFilePath from, GpFilePath to) {
        boolean moved = false;

        File fromFile = from.getServerFile();
        File toFile = to.getServerFile();
        boolean directory = fromFile.isDirectory();

        // If the file are legit
        if (fromFile.exists() && !toFile.exists()) {
            try {
                // Do the file system copy
                if (!directory) {
                    FileUtils.moveFile(fromFile, toFile);
                    moved = true;
                }
                else {
                    FileUtils.moveDirectory(fromFile, toFile);
                    moved = true;
                }
            }
            catch (IOException e) {
                log.error("Failed to move file from " + fromFile.getAbsolutePath() + " to " + toFile.getAbsolutePath());
            }

            // Update the database
            boolean inTransaction = mgr.isInTransaction();
            try {
                if (!directory) {
                    // Begin a new transaction
                    @SuppressWarnings("deprecation")
                    GpContext context = GpContext.getContextForUser(user);
                    UserUploadManager.deleteUploadFile(mgr, from);
                    UserUploadManager.createUploadFile(mgr, context, to, 1);
                    UserUploadManager.updateUploadFile(mgr, context, to, 1, 1);
                    if (!inTransaction) {
                        mgr.commitTransaction();
                    }
                }
                else {
                    syncUploadFiles(mgr, user);
                    moved = true;
                }
            }
            catch  (Throwable t) {
                moved = false;
                // Error updating the DB
                log.error("Error copying move to user upload file record in db, '" + to.getRelativeUri() + "'", t);
                mgr.rollbackTransaction();
            }
        }
        return moved;
    }

    /**
     * Renames a user upload file
     * @param user
     * @param filePath
     * @param name
     * @return
     */
    public static boolean renameUserUpload(final HibernateSessionManager mgr, final String user, final GpFilePath filePath, final String name) {
        File oldFile = filePath.getServerFile();
        File newFileAbsolute = new File(oldFile.getParentFile(), name);
        File newFileRelative = new File(filePath.getRelativeFile().getParent(), name);
        boolean renamed = false;
        boolean directory = oldFile.isDirectory();

        // If the file exists, rename the file on the file system
        if (oldFile.exists()) {
            renamed = oldFile.renameTo(newFileAbsolute);
            if (!renamed) {
                log.error("Error renaming file: " + oldFile.getPath());
                return renamed;
            }
        }
        else {
            log.error("File to rename not found: " + oldFile.getAbsolutePath());
            return renamed;
        }

        // Change the record in the database
        boolean inTransaction = mgr.isInTransaction();
        try {
            if (!directory) {
                @SuppressWarnings("deprecation")
                GpContext context = GpContext.getContextForUser(user);
                GpFilePath newPath = GpFileObjFactory.getUserUploadFile(context, newFileRelative);
                newPath.initMetadata();

                // Begin a new transaction
                UserUploadDao dao = new UserUploadDao(mgr);
                int renamedCount = dao.renameUserUpload(context, filePath, newPath);
                if (renamedCount < 1) {
                    renamed = false;
                    log.error("Error renaming user upload file record in db, userId=" + user + ", path= '" + filePath.getRelativePath()+"'. numDeleted=" + renamedCount);
                }
                if (!inTransaction) {
                    mgr.commitTransaction();
                }
            }
            else {
                syncUploadFiles(mgr, user);
                renamed = true;
            }
        }
        catch  (Throwable t) {
            renamed = false;
            // Error updating the DB
            log.error("Error renaming user upload file record in db, '" + filePath.getRelativeUri() + "'", t);
            mgr.rollbackTransaction();
        }

        return renamed;
    }

    /**
     * Delete the user upload file from the server file system, checking permissions based on the given userId.
     * 
     * @param userId, the current user who is requesting to delete the file
     * @param uploadedFileObj, the record of the user upload file to delete
     * 
     * @return true if the file was deleted
     */
    public static boolean deleteUserUploadFile(final HibernateSessionManager mgr, final String userId, final GpFilePath uploadedFileObj) {
        File file = uploadedFileObj.getServerFile();
        
        //1) if it exists, delete the file from the file system
        boolean deleted = false;
        boolean canDelete = canDelete(userId, uploadedFileObj);
        boolean directory = file.isDirectory();
        if (!canDelete) {
            return false;
        }
        if (!file.exists()) {
            //indicate success even if the file doesn't exist
            deleted = true;
        }
            
        if (file.exists()) {
            if (directory) {
                try {
                    FileUtils.deleteDirectory(file);
                    deleted = true;
                }
                catch (IOException e) {
                    deleted = false;
                }
            }
            else {
                deleted = file.delete();
            }
            if (!deleted) {
                log.error("Error deleting file: "+file.getPath());
            }
        }
        //2) remove the record from the DB, even if it doesn't exist in the file system
        if (!file.exists()) {
            //if we are in a transaction, don't commit and close
            boolean inTransaction = mgr.isInTransaction();
            try {
                //this begins a new transaction
                UserUploadDao dao = new UserUploadDao(mgr);
                int numDeleted = dao.deleteUserUploadRecursive(userId, uploadedFileObj);
                if (numDeleted < 1) {
                    log.error("Error deleting user upload file record from db, userId="+userId+", path= '"+uploadedFileObj.getRelativePath()+"'. numDeleted="+numDeleted);
                }
                if (!inTransaction) {
                    mgr.commitTransaction();
                }
            }
            catch  (Throwable t) {
                deleted = false;
                //possible error updating the DB
                log.error("Error deleting user upload file record from db, '"+uploadedFileObj.getRelativeUri()+"'", t);
                mgr.rollbackTransaction();
            }
        } 
        return deleted;
    }

    /**
     * Checks whether the given user has permission to delete the server file at the given filePath reference.
     * 
     * Note: If for some reason the file no longer exists on the server, still return true.
     * TODO: should have better error handling/doc for when the file is still in the DB but not in the file system.
     * 
     * @param currentUserId
     * @param uf
     * @return
     */
    private static boolean canDelete(String currentUserId, GpFilePath uf) {
        if (uf == null) {
            return false;
        }
        File toDel = uf.getServerFile();
        if (!toDel.exists()) {
            //Note: returning true to simplify code
            log.error("Attempt to delete a file which doesn't exist: "+toDel.getPath());
            return true;
        }
        
        if (currentUserId == null) {
            //require a userid
            log.error("Require a valid userId to delete file: " + toDel.getPath());
            return false;
        }
        if (!toDel.canWrite()) {
            log.error("Server error, GP server doesn't have permission to delete file: "+toDel.getPath());
            return false;
        }

        //TODO: come up with an improved policy for ACL for admin users
        boolean isAdmin = false;
        isAdmin = AuthorizationHelper.adminJobs(currentUserId);
        if (isAdmin) {
            return true;
        }
        
        return currentUserId.equals(uf.getOwner());
    }

    /**
     * Checks whether a file is on the excluded files list.
     * Used when syncing the file system and database.
     * @param file
     * @return
     */
    private static boolean isExcludedFile(File file) {
        for (String i : FILE_EXCLUDES) {
            if (file.getName().equalsIgnoreCase(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wipes all of a user's uploads from the database, then crawls the upload directory for a given user 
     * and adds database entries for all found files, except those whose filenames match a name on the 
     * exclude files list (used to ignore system files)
     * @param userId
     */
    public static void syncUploadFiles(final HibernateSessionManager mgr, final String userId) {
        log.debug("syncUploadFiles(userId='"+userId+"') ...");
        try {
            @SuppressWarnings("deprecation")
            final GpContext userContext = GpContext.getContextForUser(userId);
            File uploadDir = ServerConfigurationFactory.instance().getUserUploadDir(userContext);
            if (uploadDir == null) {
                log.error("Unable to get the user's upload directory in syncUploadFiles()");
                return;
            }
            
            // Remove all the old database entries
            log.debug("deleting old entries ...");
            int numDeleted = new UserUploadDao(mgr).deleteAllUserUpload(userId);
            log.debug("deleted "+numDeleted+" entries from DB");
            mgr.commitTransaction();

            // Add new entries to the database
            String[] relPath = new String[0];
            Set<String> visitedDirs = new HashSet<String>();
            final UserUploadDao dao = new UserUploadDao(mgr);
            for (File file : uploadDir.listFiles()) {
                handleFileSync(dao, visitedDirs, relPath, file, userContext);
            }

            // Commit
            mgr.commitTransaction();
        }
        catch (Throwable t) {
            log.error("Error syncing upload files for user="+userId, t);
            mgr.rollbackTransaction();
        }
        finally {
            mgr.closeCurrentSession();
        }
        log.debug("syncUploadFiles(userId='"+userId+"') ... Done!");
    }
    
    /**
     * Updates the database  with a particular file found when syncing the file system and database.
     * 
     * @param dao
     * @param file
     * @param userContext
     * @throws Exception
     */
    private static void handleFileSync(UserUploadDao dao, Set<String> visitedDirs, String[] relPath, File file, GpContext userContext) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("file==null");
        }

        //skip files which we can't read
        if (!file.canRead()) {
            log.error("ignoring file, can't read file="+file);
            return;
        }

        //skip files which don't exist or aren't regular files or directories
        if (!( (file.isFile() && file.exists()) || file.isDirectory() )) {
            log.debug("ignoring non-regular file="+file);
            return;
        }

        // Exclude file on exclude list (ex: .DS_Store)
        if (isExcludedFile(file)) {
            log.debug("ignoring excluded file="+file);
            return;
        }

        // avoid circular references, from symbolic links
        if (file.isDirectory()) {
            final String canonicalPath = file.getCanonicalPath();
            if (visitedDirs.contains( canonicalPath )) {
                log.debug("skipping dir, because it was already visited: "+canonicalPath);
                return;
            }
            else {
                visitedDirs.add( canonicalPath );
            }
        }

        //need a valid userId to proceed
        if (userContext == null) {
            throw new Exception("Missing required parameter, userContext is null");
        }
        if (userContext.getUserId() == null) {
            throw new Exception("Missing required parameter, userContext.userId is null");
        }
        
        //add this file to the DB
        final UserUpload uploadFile = new UserUpload();
        final String[] newRelPath = new String[relPath.length + 1];
        for(int idx = 0; idx<relPath.length; ++idx) {
            newRelPath[idx] = relPath[idx];
        }
        newRelPath[relPath.length] = file.getName();
        final String relativePath = join(newRelPath, "/");
        uploadFile.setPath(relativePath);
        uploadFile.setUserId(userContext.getUserId());
        uploadFile.setNumParts(1);
        uploadFile.setNumPartsRecd(1);
        uploadFile.init(file);
        dao.saveOrUpdate(uploadFile); 
        
        //if it's a directory, add children
        if (!file.isDirectory()) {
            return;
        }
        //if we're here, it means the file is a directory
        for(File child : file.listFiles()) {
            try {
                handleFileSync(dao, visitedDirs, newRelPath, child, userContext);
            }
            catch (Throwable t) {
                log.error("Error syncing user upload file, parent="+file+", child="+child, t);
            }
        }
    }

    private static String join(String[] arr, String sep) {
        String rval = "";
        boolean first = true;
        for(String s : arr) {
            if (first) {
                rval = s;
                first = false;
            }
            else {
                rval += (sep + s);
            }
        }
        return rval;
    }

}

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.dm.userupload.dao.UserUpload;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;

/**
 * Helper class for purging user upload files for the given user.
 * 
 * The current implementation purges all partial uploads which have not been touched
 * in the past 24 hours.
 * 
 * There are a number of different ways to purge data files from a user upload directory.
 * From a user's perspective, the use-cases are:
 *     1) only purge stalled partial uploads. For example, as a result of canceling a file upload from the jumploader applet
 *     2) purge all files which are older than the threshold date, which is the same as is done for job results.
 * 
 * Special cases can result from advanced use or inadvertent use of the GP server. 
 *     1) a file on the server path, which is not in the DB
 *     2) a symbolic link on the server path, which references an external part of the file system
 *     3) a symbolic link on the server path, which causes a cycle to another directory within the 
 *         user upload folder
 * 
 * This class must handle these special cases.
 * 
 * @author pcarr
 */
public class UserUploadPurger {
    private static Logger log = Logger.getLogger(UserUploadPurger.class);
    
    final static public String PROP_PURGE_ALL="upload.purge.all";
    final static public String PROP_PURGE_TMP="upload.purge.tmp";
    final static public String PROP_PURGE_PARTIAL="upload.purge.partial";

    final private ExecutorService exec;
    final private GpContext userContext;
    final private Date cutoffDate;
    final private boolean purgeAll;
    final private boolean purgeTmp;
    final private boolean purgePartial;

    /**
     * @deprecated - for GP <= 3.7.2, pass in a global dateCutoff as a long
     * @param exec
     * @param userContext
     * @param dateCutoff
     */
    public UserUploadPurger(final ExecutorService exec, final GpContext userContext, final long dateCutoff) {
        this(exec, userContext, new Date(dateCutoff));
    }
    
    public UserUploadPurger(final ExecutorService exec, final GpContext userContext, final Date cutoffDate) {
        if (userContext == null) {
            throw new IllegalArgumentException("userContext == null");
        }
        if (userContext.getUserId() == null) {
            throw new IllegalArgumentException("userContext.userId == null");
        }

        this.userContext = userContext;
        this.cutoffDate = cutoffDate;

        this.purgeAll=ServerConfigurationFactory.instance().getGPBooleanProperty(userContext, PROP_PURGE_ALL, false);
        this.purgeTmp=ServerConfigurationFactory.instance().getGPBooleanProperty(userContext, PROP_PURGE_TMP, true);
        this.purgePartial=ServerConfigurationFactory.instance().getGPBooleanProperty(userContext, PROP_PURGE_PARTIAL, true);
        
        if (exec==null) {
            log.debug("creating new singleThreadExecutor");
            this.exec = Executors.newSingleThreadExecutor();
        }
        else {
            this.exec=exec;
        }
    }
    
    public GpContext getUserContext() {
        return userContext;
    }

    public void purge() throws Exception { 
        //special handling for partial uploads, they are purged after a hard-coded cutoff of 1 day
        if (purgePartial) {
            purgePartialUploadsFromDb();
        }
        // purgeAll means ... purge all files from the User Uploads tab except for the tmp files
        if (purgeAll) {
            purgeAll();
        }
        // purgeTmp means ... purge only the tmp files
        if (purgeTmp) {
            purgeTmpFiles();
        }
    }
    
    private void purgeAll() {
        purgeAllFilesByDateCutoff();
    }

    /**
     * Delete stalled partial uploads.
     * This deletes files from the file system, but only based on existing records in the DB. 
     * It does not walk the file tree.
     * 
     * The default setting is 24 hours.
     */
    private void purgePartialUploadsFromDb() {
        //get all of the files which are partial, which have not been updated in the last 24 hours
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        final Date maxModDate = cal.getTime();
        purgePartialUploadsFromDb(maxModDate);
    }

    private void purgePartialUploadsFromDb(Date maxModDate) {
        List<UserUpload> userUploads;
        try {
            UserUploadDao dao = new UserUploadDao();
            userUploads = dao.selectStalledPartialUploadsForUser(userContext.getUserId(), maxModDate);
        }
        catch (Throwable t) {
            userUploads=Collections.emptyList();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        for(final UserUpload userUpload : userUploads) {
            try {
                final GpFilePath gpFilePath = initGpFilePath(userUpload);
                purgeUserUploadFile(gpFilePath); 
            }
            catch (Throwable t) {
                log.error("Error purging partial upload, userId="+userContext.getUserId()+", path="+userUpload.getPath(), t);
            }
        }
    }
    
    /**
     * Delete temporary input files, which were uploaded from the job input form.
     * It also deletes any external urls which were transferred in as part of a file list input.
     * 
     * The rule of identifying a temporary file is hard-coded into the UserUploadDao class.
     *     'tmp/*'
     * Any file which is in the 'tmp' folder of the given user's User Uploads tab.
     */
    private void purgeTmpFiles() {
        //TODO: improve purge algorithm, to prevent deleting input files for active jobs.
        purgeTmpFilesByDateCutoff();
    }
    
    /**
     * This implementation matches functionality in <= GP 3.5.0.
     * We crudely delete all temp files whose timestamp is greater than the cutoff date
     * set by the 'Purge Jobs After' field in the File Purge Settings of the admin page.
     * 
     * Note: this method opens a DB connection and doesn't close it.
     */
    private void purgeTmpFilesByDateCutoff() {
        if (userContext==null) {
            log.error("userContext==null");
            return;
        }
        if (cutoffDate == null) {
            log.debug("No cutoff date for user="+userContext.getUserId());
            return;
        }
        
        List<UserUpload> tmpFiles;
        try {
            final String userId=userContext.getUserId();
            log.debug("purging tmp files for user: "+userId+", cutoffDate="+cutoffDate);
            UserUploadDao dao = new UserUploadDao();
            tmpFiles = dao.selectTmpUserUploadsToPurge(userId, cutoffDate);
        }
        catch (Throwable t) {
            log.error("Unexpected error selecting tmp user uploads to purge", t);
            tmpFiles=Collections.emptyList();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        purgeFiles(tmpFiles);
    }
    
    /**
     * Delete all (non tmp) files from the Uploads tab for the given user,
     * which are older than the system configured cutoff date.
     * 
     * This implementation matches functionality in <= GP 3.5.0.
     * We crudely delete all files whose timestamp is greater than the cutoff date
     * set by the 'Purge Jobs After' field in the File Purge Settings of the admin page.
     */
    private void purgeAllFilesByDateCutoff() {
        if (userContext==null) {
            log.error("userContext==null");
            return;
        }
        if (cutoffDate == null) {
            log.debug("No cutoff date for user="+userContext.getUserId());
            return;
        }
        final String userId=userContext.getUserId();
        log.debug("purging tmp files for user: "+userId);
        
        List<UserUpload> selectedFiles;
        try {
            UserUploadDao dao = new UserUploadDao();
            final boolean includeTempFiles=false;
            selectedFiles=dao.selectAllUserUpload(userId, includeTempFiles, cutoffDate);
        }
        catch (Throwable t) {
            log.error("Unexpeted error getting selectedFiles", t);
            selectedFiles=Collections.emptyList();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        purgeFiles(selectedFiles);
    }

    /**
     * Create a GpFilePath instance from a UserUpload instance, fetched from the DB.
     * 
     * @param userUploadRecord
     * @return
     */
    private GpFilePath initGpFilePath(UserUpload userUploadRecord) throws Exception {
        GpFilePath gpFilePath = UserUploadManager.getUploadFileObj(userContext, new File(userUploadRecord.getPath()), userUploadRecord);
        return gpFilePath;
    }

    /**
     * Delete each UserUpload file from the given list.
     * Make sure to delete child files before parent directories.
     * This method only deletes non-empty directories.
     * 
     * @param filesToDelete
     */
    private void purgeFiles(final List<UserUpload> filesToDelete) {
        final List<GpFilePath> tmpDirs = new ArrayList<GpFilePath>();
        final List<GpFilePath> missingFiles = new ArrayList<GpFilePath>();
        
        //quick and dirty way to delete files and parent directories without conflicts
        //on the first pass, delete all files, don't delete any directories
        int numFilesToPurge=0;
        int numDirsToPurge=0;
        for(UserUpload userUpload : filesToDelete) {
            try {
                GpFilePath gpFilePath = initGpFilePath(userUpload);
                if (gpFilePath.isFile()) {
                    ++numFilesToPurge;
                    purgeUserUploadFile(gpFilePath); 
                }
                else if (gpFilePath.isDirectory()) {
                    ++numDirsToPurge;
                    tmpDirs.add(gpFilePath);
                }
                else {
                    log.debug("gpFilePath is neither a file nor a directory: "+gpFilePath.getServerFile().getPath());
                    missingFiles.add(gpFilePath);
                }
            }
            catch (Throwable t) {
                log.error("Error purging file from Uploads tab, userId="+userContext.getUserId()+", path="+userUpload.getPath(), t);
            }
        }
        log.debug("numFilesToPurge: "+numFilesToPurge);
        if (numDirsToPurge != tmpDirs.size()) {
            log.error("numDirsToPurge != tmpDirs.size(), unexpected server error");
            log.debug("numDirsToPurge: "+numDirsToPurge);
            log.debug("tmpDirs.size: "+tmpDirs.size());
        }
        log.debug("missingFiles.size: "+missingFiles.size());

        //on the second pass, delete directories
        //iterate the sorted list in reverse order to ensure that we don't try to delete a parent directory
        //before we have deleted all of the child directories
        //this code assumes that the relativePath to a child directory is always alphabetically after any of its
        //ancestor directories
        for(int i=tmpDirs.size(); --i>=0;) {
            GpFilePath tmpDir=tmpDirs.get(i);
            try {
                purgeUserUploadFile(tmpDir);
            }
            catch (Throwable t) {
                log.error("Error purging tmpDir, userId="+userContext.getUserId()+", path="+tmpDir.getRelativePath(), t);
            }
        }
        
        //on the third pass, remove records from the DB for files which no longer exist
        for(final GpFilePath missingFile : missingFiles) {
            try {
                boolean success=DataManager.deleteUserUploadFile(HibernateUtil.instance(), userContext.getUserId(), missingFile);
                if (!success) {
                    log.error("Did not deleteUserUploadFile: "+missingFile.getServerFile().getPath());
                }
            }
            catch (Throwable t) {
                log.error("Error removing UserUploadFile from DB, "+missingFile.getServerFile().getPath(), t);
            }
        }
    }

    /**
     * Schedule the file to be purged.
     * @param gpFilePath
     */
    public void purgeUserUploadFile(final GpFilePath gpFilePath) {
        Future<Boolean> task = exec.submit(new Callable<Boolean> () {
            public Boolean call() throws Exception {
                log.debug("deleting relativeUri='"+gpFilePath.getRelativeUri()+"' ...");
                boolean deleted = false;
                try {
                    HibernateUtil.beginTransaction();
                    deleted = DataManager.deleteUserUploadFile(HibernateUtil.instance(), userContext.getUserId(), gpFilePath);
                    HibernateUtil.commitTransaction();
                    return deleted;
                }
                catch (Throwable t) {
                    log.error("Error deleting relativeUri='"+ gpFilePath.getRelativeUri()+"': "+t.getLocalizedMessage(), t);
                    HibernateUtil.rollbackTransaction();
                    return false;
                }
                finally {
                    log.debug("deleted="+deleted);
                }
            }
        });
    }

}

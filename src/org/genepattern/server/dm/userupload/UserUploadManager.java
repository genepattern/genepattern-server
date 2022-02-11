/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.userupload;

import java.io.File;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpDirectoryNode;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.GpFilePathException;
import org.genepattern.server.dm.userupload.dao.UserUpload;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.job.input.dao.JobInputValueRecorder;
import org.genepattern.webservice.ParameterFormatConverter;
import org.genepattern.webservice.ParameterInfo;

public class UserUploadManager {
    private static final Logger log = Logger.getLogger(UserUploadManager.class);

    /**
     * Server configuration setting (config.yaml file), when this is true, it means, hide the tmp directory
     * and it's contents from the listing of files in the Uploads tab.
     * Default is: true 
     */
    public static final String PROP_UPLOAD_HIDE_TMP="upload.hide.tmp";

    public static final GpFilePath getUploadFileObj(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext userContext, final File relativePath, final boolean initMetaData)
    throws Exception {
        final GpFilePath uploadFilePath = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, relativePath);
        return getUploadFileObj(mgr, userContext, uploadFilePath, initMetaData);
    }
     
    /**
     * Create an instance of a GpFilePath object for the user upload file. 
     * If there is already a record in the DB, initialize the file meta data.
     * 
     * @param userContext, must contain the valid userId of the user who is uploading the file.
     * @param relativePath, must be a relative file path, relative to the current user's upload directory.
     * @return
     * @throws Exception
     */
    public static final GpFilePath getUploadFileObj(final HibernateSessionManager mgr, final GpContext userContext, final GpFilePath uploadFilePath, final boolean initMetaData)
    throws Exception {
        //if there is a record in the DB ... 
        boolean isInTransaction = false;
        try {
            isInTransaction = mgr.isInTransaction();
        }
        catch (Throwable t) {
            String message = "DB connection error: "+t.getLocalizedMessage();
            log.error(message, t);
            throw new Exception(message);
        }
        
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            UserUpload fromDb = dao.selectUserUpload(userContext.getUserId(), uploadFilePath);

            if (initMetaData && fromDb != null) {
                initMetadata(uploadFilePath, fromDb);
            }
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return uploadFilePath;
        }
        catch (Throwable t) {
            String message = "DB error getting file meta data: "+t.getLocalizedMessage();
            log.error(message, t);
            throw new Exception(message);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public static final boolean isUploadInDB(final HibernateSessionManager mgr, final GpContext userContext, final GpFilePath uploadFilePath) 
    throws Exception {
        // if there is a record in the DB ...
        boolean isInTransaction = false;
        try {
            isInTransaction = mgr.isInTransaction();
        }
        catch (Throwable t) {
            String message = "DB connection error: "+t.getLocalizedMessage();
            log.error(message, t);
            throw new Exception(message);
        }

        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            UserUpload fromDb = dao.selectUserUpload(userContext.getUserId(), uploadFilePath);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return fromDb != null;
        }
        catch (Throwable t) {
            String message = "DB error getting file meta data: "+t.getLocalizedMessage();
            log.error(message, t);
            throw new Exception(message);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    /**
     * Create an instance of a GpFilePath object from a UserUpload record.
     */
    public static final GpFilePath getUploadFileObj(final GpConfig gpConfig, final GpContext userContext, final File relativePath, final UserUpload fromDb) throws Exception {
        GpFilePath uploadFilePath = GpFileObjFactory.getUserUploadFile(gpConfig, userContext, relativePath);
        initMetadata(uploadFilePath, fromDb);
        return uploadFilePath;
    }

    /**
     * Set all file metadata for the given uploadFilePath from the settings in the DB record.
     * 
     * @param uploadFilePath
     * @param fromDb
     */
    private static void initMetadata(final GpFilePath uploadFilePath, final UserUpload fromDb) {
        if (fromDb == null) {
            log.error("Unexpected null arg");
            return;
        }
        uploadFilePath.setName(fromDb.getName());
        uploadFilePath.setLastModified(fromDb.getLastModified());
        uploadFilePath.setExtension(fromDb.getExtension());
        uploadFilePath.setFileLength(fromDb.getFileLength());
        uploadFilePath.setKind(fromDb.getKind());
        uploadFilePath.setNumParts(fromDb.getNumParts());
        uploadFilePath.setNumPartsRecd(fromDb.getNumPartsRecd()); 
    }

    /**
     * Add a record of the user upload file into the database. 
     * 
     * Iff the current thread is not already in a DB transaction, this method will commit the transaction. 
     * Otherwise, it is up to the calling method to commit or rollback the transaction.
     * 
     * @param userContext, requires a valid userId,
     * @param gpFileObj, a GpFilePath to the upload file
     * @param numParts, the number of parts this file is broken up into, based on the jumploader applet.
     * @throws Exception if a duplicate entry for the file is found in the database
     */
    public static final UserUpload createUploadFile(HibernateSessionManager mgr, GpContext userContext, GpFilePath gpFileObj, int numParts) throws Exception {
        return createUploadFile(mgr, userContext, gpFileObj, numParts, false);
    }
    
    /**
     * Add a record of the user upload file into the database. 
     * 
     * Iff the current thread is not already in a DB transaction, this method will commit the transaction. 
     * Otherwise, it is up to the calling method to commit or rollback the transaction.
     * 
     * @param mgr, database session
     * @param userContext, requires a valid userId,
     * @param gpFileObj, a GpFilePath to the upload file
     * @param numParts, the number of parts this file is broken up into, based on the jumploader applet.
     * @param modDuplicate, whether an existing duplicate entry is updated or if an error is thrown
     * @throws Exception if a duplicate entry for the file is found in the database and modDuplicate is false
     */
    public static final UserUpload createUploadFile(final HibernateSessionManager mgr, final GpContext userContext, final GpFilePath gpFileObj, final int numParts, final boolean modDuplicate) 
    throws GpFilePathException, DbException 
    {
        final boolean inTransaction = mgr.isInTransaction(); 
        log.debug("inTransaction="+inTransaction);
        try {
            // constructor begins a DB connection
            mgr.beginTransaction();
            UserUploadDao dao = new UserUploadDao(mgr);
            UserUpload uu = dao.selectUserUpload(userContext.getUserId(), gpFileObj);
            if (uu != null && !modDuplicate) {
                log.error("Duplicate entry found in the database for relativePath=" + gpFileObj.getRelativePath());
                throw new GpFilePathException("Duplicate entry found in the database for file: " + gpFileObj.getRelativePath());
            }
            try {
                uu = UserUpload.initFromGpFileObj(userContext.getUserId(), uu, gpFileObj);
                uu.setNumParts(numParts);
                dao.saveOrUpdate( uu );
                if (!inTransaction) {
                    mgr.commitTransaction();
                }
                
                return uu;
            }
            catch (Throwable t) {
                log.error("Error in createUploadFile() for relativePath="+gpFileObj.getRelativePath(), t);
                mgr.rollbackTransaction();
                throw new DbException("Runtime exception creating upload file: " + gpFileObj.getRelativePath());
            }
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    /**
     * Update the record of the user upload file in the database.

     * Iff the current thread is not already in a DB transaction, this method will commit the transaction. 
     * Otherwise, it is up to the calling method to commit or rollback the transaction.
     * 
     * @param userContext
     * @param gpFilePath
     * @param partNum, part numbers start with 1, e.g. a single chunk upload would have partNum=1 and totalParts=1.
     * @param totalParts
     * @throws Exception, if a part is received out of order.
     */
    public static final void updateUploadFile(final HibernateSessionManager mgr, final GpContext userContext, final GpFilePath gpFilePath, final int partNum, final int totalParts)
    throws DbException 
    {
        boolean inTransaction = mgr.isInTransaction();
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            UserUpload uu = dao.selectUserUpload(userContext.getUserId(), gpFilePath);
            if (uu.getNumParts() != totalParts) {
                throw new Exception("Expecting numParts to be " + uu.getNumParts() + " but it was " + totalParts);
            }
            uu.setNumPartsRecd(partNum);
            uu.init(gpFilePath.getServerFile());
            dao.saveOrUpdate(uu);
            if (!inTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error(t);
            mgr.rollbackTransaction();
            throw new DbException("Error updating upload file record for file '" + gpFilePath.getRelativePath() + "': " + t, t);
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    /**
     * Update the record of the user upload file in the database.

     * Iff the current thread is not already in a DB transaction, this method will commit the transaction. 
     * Otherwise, it is up to the calling method to commit or rollback the transaction.
     * 
     * @param userContext
     * @param gpFilePath
     * @param partNum, part numbers start with 1, e.g. a single chunk upload would have partNum=1 and totalParts=1.
     * @param totalParts
     * @throws Exception, if a part is received out of order.
     */
    public static final void updateUploadDirectory(final HibernateSessionManager mgr, final GpContext userContext, final GpFilePath gpFilePath, final int partNum, final int totalParts)
    throws DbException 
    {
        boolean inTransaction = mgr.isInTransaction();
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            UserUpload uu = dao.selectUserUpload(userContext.getUserId(), gpFilePath);
            if (uu.getNumParts() != totalParts) {
                throw new Exception("Expecting numParts to be " + uu.getNumParts() + " but it was " + totalParts);
            }
            uu.setNumPartsRecd(partNum);
            uu.initDirectory(gpFilePath.getServerFile());
            dao.saveOrUpdate(uu);
            if (!inTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new DbException("Error updating upload file record for file '" + gpFilePath.getRelativePath() + "': " + t.getLocalizedMessage(), t);
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                
    
    
    
    /**
     * Delete the record of the user upload file from the database, only if there is one.
     * 
     * @param gpFilePath
     * @return the number of records which were deleted, usually 0 or 1.
     * @throws Exception
     */
    public static final int deleteUploadFile(final HibernateSessionManager mgr, final GpFilePath gpFilePath) throws Exception {
        boolean inTransaction = mgr.isInTransaction();
        try {
            UserUploadDao dao = new UserUploadDao(mgr);
            int numDeleted=dao.deleteUserUpload(gpFilePath.getOwner(), gpFilePath);
            if (!inTransaction) {
                mgr.commitTransaction();
            }
            return numDeleted;
        }
        catch (Throwable t) {
            mgr.rollbackTransaction();
            throw new Exception("Error deleting upload file record for file '" + gpFilePath.getRelativePath() + "': " + t.getLocalizedMessage(), t);
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    /**
     * Get the entire tree of user upload files, rooted at the upload directory for the given user.
     * The root element is the user's upload directory, which typically is not displayed.
     */
    public static final GpDirectoryNode getFileTree(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext userContext) throws Exception { 
        final GpFilePath userDir = GpFileObjFactory.getUserUploadDir(gpConfig, userContext);
        final GpDirectoryNode root = new GpDirectoryNode(userDir);

        //get the list from the DB
        final List<UserUpload> all = getAllFiles(mgr, gpConfig, userContext);
        
        //initialize the list of GpFilePath objects
        final SortedMap<String,GpDirectoryNode> allDirs = new TreeMap<String,GpDirectoryNode>();
        final SortedMap<String,GpFilePath> allFiles = new TreeMap<String,GpFilePath>();
        
        final File userUploadDir = gpConfig.getUserUploadDir(userContext);
        for(UserUpload userUpload : all) {
            GpFilePath uploadFilePath = GpFileObjFactory.getUserUploadFile(userContext, userUploadDir, new File(userUpload.getPath()));
            initMetadata(uploadFilePath, userUpload);
            if (UserUpload.isDirectory(userUpload)) {
                GpDirectoryNode gpDirectory = new GpDirectoryNode(uploadFilePath);
                allDirs.put(userUpload.getPath(), gpDirectory);
            }
            else {
                allFiles.put(userUpload.getPath(),uploadFilePath);
            }
        }
        
        //now build the tree
        for(GpDirectoryNode dir : allDirs.values()) {
            GpDirectoryNode parentDir = root;
            final String parentPath = getParentPath(dir.getValue());
            if (parentPath != null) {
                if (allDirs.containsKey(parentPath)) {
                    parentDir = allDirs.get(parentPath);
                }
            }
            parentDir.addChild(dir); 
        }
        for(GpFilePath file : allFiles.values()) {
            GpDirectoryNode parentDir = root;
            final String parentPath = getParentPath(file);
            if (parentPath != null) {
                if (allDirs.containsKey(parentPath)) {
                    parentDir = allDirs.get(parentPath);
                }
            }
            parentDir.addChild(file); 
        }
        
        //for debugging, walk the tree
        //List<String> allPaths = new ArrayList<String>();
        //walk(allPaths,root);
        return root;
    }

//    //just for debugging
//    private static void walk(List<String> allPaths, GpDirectory dir) {
//        String relPath = dir.getValue().getRelativePath();
//        log.debug(relPath);
//        allPaths.add(relPath);
//        for(Node<GpFilePath> child : dir.getChildren()) {
//            if (child instanceof GpDirectory) {
//                walk( allPaths, (GpDirectory) child );
//            }
//            else {
//                final String childPath = child.getValue().getRelativePath();
//                log.debug(childPath);
//                allPaths.add(childPath);
//            }
//        }
//    }

    private static String getParentPath(final GpFilePath file) {
        if (file == null) {
            log.error("Unexpected null arg");
            return null;
        }
        String path = file.getRelativePath();
        int idx = path.lastIndexOf("/");
        if (idx < 0) {
            //has no parent
            return null;
        }
        String parentPath = path.substring(0, idx);
        return parentPath;
    }
    
    /**
     * query all files from the DB, for the given user.
     * @param userContext
     * @return
     */
    protected static List<UserUpload> getAllFiles(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext userContext) {
        final String userId=userContext.getUserId();
        final boolean hideTmp=gpConfig.getGPBooleanProperty(userContext, PROP_UPLOAD_HIDE_TMP, true);
        final boolean includeTempFiles=!hideTmp;
        UserUploadDao dao = new UserUploadDao(mgr);
        return dao.selectAllUserUpload(userId, includeTempFiles);
    }
    
    
    
    public static final void deleteUploadedFiles(final HibernateSessionManager mgr, GpContext gpContext, AnalysisJob aJob, boolean dontCheckForOtherJobsUsing){
        ParameterInfo[] params = ParameterFormatConverter.getParameterInfoArray(aJob.getParameterInfo());
        GpConfig config = ServerConfigurationFactory.instance();
        JobInputValueRecorder jobInputRecorder = new JobInputValueRecorder(mgr);
        
        for (int i=0; i< params.length; i++){
            try {
            ParameterInfo aParam = params[i];
            if ("java.io.File".equals(aParam.getAttributes().get("type"))){
                String filePath = aParam.getValue();
                
                GpFilePath gpfp = GpFileObjFactory.getRequestedGpFileObj(config, filePath);
                
                // this list will include us so pull it
                List<Integer> jobsUsingInput = jobInputRecorder.fetchMatchingJobs(gpfp.getUrl().toString());
                
                jobsUsingInput.remove(aJob.getJobNo());
                
                boolean deleteOK = ((jobsUsingInput.size() == 0) || dontCheckForOtherJobsUsing);
                if (deleteOK){ 
                    UserUploadManager.deleteUploadFile(mgr, gpfp);
                    gpfp.getServerFile().delete();
                    if (DataManager.isUseS3NonLocalFiles(gpContext)) {
                        DataManager.getExternalFileManager(gpContext).deleteFile(gpContext, gpfp.getServerFile());
                    }
                }
            }
            } catch (Exception e){
                // ignore it and hope the purger gets the file later
                log.error("Could not delete uploaded file with job deletion.", e);
            }
            
        }      
    }
        
    
}


/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.dm.userupload;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.domain.AnalysisJob;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.PropsTable;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.hibernate.Query;
import org.hibernate.SQLQuery;

/**
 * Support class developed for migrating user upload files from GP 3.3.2 to GP 3.3.3.
 * 
 * In GP 3.3.3 a new db table is used for caching file meta data for user upload files.
 * In GP 3.3.2 the upload dir is initialized to this path:
 *     ../users/<user_id>/user.uploads
 * In GP 3.3.3 it is initialized to this path:
 *     ../users/<user_id>/uploads
 * 
 * This method checks for and renames (if unambiguous) 'user.uploads' to 'uploads' for each user account.
 * Then it syncs the DB to the new format, simply by rereading each filename and metadata from the file system.
 * 
 * @author pcarr
 */
public class MigrationTool {
    private static Logger log = Logger.getLogger(MigrationTool.class);

    /**
     * Check for a flag in the DB indicating whether 
     * user uploads have already been migrated to the new format.
     * 
     * @return true if 'sync.user.uploads.complete' is set to true for this version of GenePattern;
     *     also return true if there was some kind of DB connection error.
     */
    protected static boolean checkDbForSyncUserUploadsComplete(final HibernateSessionManager mgr) throws DbException {
            PropsTable row=PropsTable.selectRow(mgr, "sync.user.uploads.complete");
            if (row != null) {
                return true;
            }
            return false;
    }
    
    /**
     * Migrate all user upload files from GP 3.3.2 format to GP 3.3.3 format.
     * Store a record of this in the DB so that we only do this once.
     * This should be called from the StartupServlet after the DB is initialized.
     */
    public static void migrateUserUploads(final HibernateSessionManager mgr) throws DbException {
        boolean dbcheck=checkDbForSyncUserUploadsComplete(mgr);
        if (dbcheck) {
            return;
        }
        
        log.info("migrating user upload directories ...");
        
        //1) clean up old partial uploads
        deleteAllPartialUploads(mgr);
        
        //2) set up GP 3.3.3 user upload directories
        migrateUserUploadDirs(mgr);
        
        //3) resync the DB entries
        syncUserUploadFiles(mgr);
        
        //4) clear the legacy UPLOAD_FILE table
        deleteAllOldUploadFileRecords(mgr);
        
        //5) update prev job input parameters
        updatePrevJobInputParams(mgr);
        
        //6) finally, update the flag in the DB, so that we don't do this again
        boolean success=PropsTable.saveProp(mgr, "sync.user.uploads.complete", "true");
        log.info("migrated="+success);
    }

    /**
     * Get the list of all jobs which reference 'user.uploads' anywhere in the parameter_info clob.
     */
    protected static List<Integer> getUserUploadsJobNos(final HibernateSessionManager mgr) {
        List<Integer> jobNos = new ArrayList<Integer>();
        try {
            mgr.beginTransaction();
            final String hql = "select a.jobNo from "+AnalysisJob.class.getName()+" a where a.parameterInfo like :match and a.deleted = :isDeleted";
            Query query = mgr.getSession().createQuery(hql);
            query.setString("match", "%user.uploads%");
            query.setBoolean("isDeleted", false);
            List<?> jobNoObjs = query.list();
            for(Object jobNoObj : jobNoObjs) {
                int jobNo = -1;
                if (jobNoObj instanceof Number) {
                    jobNo = ((Number) jobNoObj).intValue();
                }
                else if (jobNoObj instanceof String) {
                    try {
                        jobNo = Integer.parseInt(jobNoObj.toString());
                    }
                    catch (NumberFormatException e) {
                        log.error("Error parsing job_no from sql query result set: "+jobNoObj, e);
                    }
                }
                if (jobNo >= 0) {
                    jobNos.add( jobNo );
                }
            }
        }
        catch (Throwable t) {
            log.error("Error getting list of jobIds with 'user.uploads' in PARAMETER_INFO clob", t);
        }
        finally {
            mgr.closeCurrentSession();
        }
        return jobNos;
    }
    
    /**
     * Update all previous jobs which have a user upload file as an input parameter.
     * Replace the 3.3.2 style link with the 3.3.3 style link. E.g.,
     * change 
     *     http://127.0.0.1:8080/gp/data//Applications/GenePatternServer/users/admin/user.uploads/all_aml_test.gct
     * to
     *     <GenePatternURL>users/admin/all_aml_test.gct
     */
    private static void updatePrevJobInputParams(final HibernateSessionManager mgr) {
        List<Integer> jobNos = getUserUploadsJobNos(mgr);
        int numJobsChanged = 0;
        int totalNumParamsChanged = 0;
        //for each job in the list, update input parameters which reference a GP 3.3.2 user upload file
        for(int jobNo : jobNos) {
            try {
                boolean save_update = false;
                int numParamsChanged = 0;
                AnalysisDAO dao = new AnalysisDAO(mgr);
                JobInfo jobInfo = dao.getJobInfo(jobNo);
                if (jobInfo == null) {
                    log.error("jobInfo is null, for jobNo: "+jobNo);
                    break;
                }
                ParameterInfo[] parameterInfoArray = jobInfo.getParameterInfoArray();
                for(ParameterInfo pInfo : parameterInfoArray) {
                    //Note: hard-coded just to match GP 3.3.2 user uploaded input files (which are treated as URL_INPUT_MODE)
                    //    pInfo#isInputFile() returns a bogus value
                    boolean isInputFile = false;
                    Object mode = pInfo.getAttributes().get(ParameterInfo.MODE);
                    isInputFile = ParameterInfo.URL_INPUT_MODE.equals( mode );
                    if (isInputFile) {
                        String origValue = pInfo.getValue();
                        String newValue = migrateInputParameterValue(origValue);
                        if (!origValue.equals(newValue)) {
                            log.info("migrating user upload input parameter for "+jobNo+", param="+pInfo.getName());
                            log.info("\told="+origValue);
                            log.info("\tnew="+newValue);
                            save_update = true;
                            pInfo.setValue(newValue);
                            ++numParamsChanged;
                        }
                    }
                }
                if (save_update) {
                    AnalysisJobDAO analysisJobDao = new AnalysisJobDAO();
                    AnalysisJob aJob = analysisJobDao.findById(jobNo);
                    String paramString = jobInfo.getParameterInfo();
                    aJob.setParameterInfo(paramString);
                    mgr.getSession().update(aJob);
                    mgr.commitTransaction();
                    log.info("committed change to DB");
                    ++numJobsChanged;
                    totalNumParamsChanged += numParamsChanged;
                }
            }
            catch (Throwable t) { 
                log.error("Error updating parameterInfo for jobNo="+jobNo, t);
                mgr.rollbackTransaction();
            }
            finally {
                mgr.closeCurrentSession();
            }
        }
        
        log.info("updated "+totalNumParamsChanged+" parameters in "+numJobsChanged+" jobs");
    }
    
    /**
     * For example, change 
     *     http://127.0.0.1:8080/gp/data//Applications/GenePatternServer/users/admin/user.uploads/all_aml_test.gct
     * to
     *     <GenePatternURL>users/admin/all_aml_test.gct
     *     
     * For example on win, change
     *     http://gpdevwin64.lab.broad:8080/gp/data/C%3A%5CGenepatternServer%5CGP_332_prod%5Cusers%5Cpcarr%40broadinstitute.org%5Cuser.uploads%5Call%20%5D%20aml%20test.gct
     * to 
     *     <GenePatternURL>users/pcarr%40broadinstitute.org/all%20%5D%20aml%20test.gct </td>
     */
    public static String migrateInputParameterValue(String origValue) {
        if (!origValue.contains("user.uploads")) {
            //no change
            return origValue;
        }
        int idx0 = origValue.indexOf("/gp/data/");
        if (idx0 < 0) {
            return origValue;
        }
        int idx1 = origValue.indexOf("/users/", idx0);
        if (idx1 >= 0) {
            idx1 += "/users/".length();
        }
        else {
            idx1 = origValue.indexOf("%5Cusers%5C", idx0);
            if (idx1 >= 0) {
                idx1 += "%5Cusers%5C".length();
            }
        }
        if (idx1 < 0) {
            return origValue;
        }
        int idx2 = origValue.indexOf("/", idx1);
        if (idx2 < 0) {
            idx2 = origValue.indexOf("%5C", idx1);
        }
        if (idx2 < 0) {
            return origValue;
        }
        String userId = origValue.substring(idx1, idx2);
        int idx3 = origValue.indexOf("/user.uploads/", idx2);
        if (idx3 >= 0) {
            idx3 += "/user.uploads/".length();
        }
        else {
            idx3 = origValue.indexOf("%5Cuser.uploads%5C");
            if (idx3 >= 0) {
                idx3 += "%5Cuser.uploads%5C".length();
            }
        }
        if (idx3 < 0) {
            return origValue;
        }
        String filepath = origValue.substring(idx3);
        
        //String newValue = origValue.substring(0, idx0);
        //newValue += "/gp/users/";
        String newValue = "<GenePatternURL>users/";
        newValue += userId;
        newValue += "/"+filepath;
        return newValue;
    }
    
    private static void syncUserUploadFiles(HibernateSessionManager mgr) {
        List<String> userIds = new ArrayList<String>();
        try {
            UserDAO userDao = new UserDAO(mgr);
            List<User> users = userDao.getAllUsers();
            for(User user : users) {
                userIds.add( user.getUserId() );
            }
        }
        catch (Throwable t) {
            log.error(t);
        }
        finally {
            mgr.closeCurrentSession();
        } 
        
        for(String userId : userIds) {
            DataManager.syncUploadFiles(mgr, userId);
        }
    }
    
    private static enum StatusCode {
        SERVER_ERROR(-2, "Unexpected server error"),
        ERROR(-1, "Error, 'user.uploads' and 'uploads' both exist and contain files"),
        NO_PREV(0, "No need to migrate, 'user.uploads' doesn't exist"),
        DELETE_PREV(1, "Migrated by deleting empty 'user.uploads'"),
        RENAME_PREV(2, "Migrated by renaming 'user.uploads' to 'uploads'");
        
        private int code;
        private String message;
        private StatusCode(int code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public int getCode() {
            return code;
        }
        public String getMessage() {
            return message;
        }
    }

    /**
     * When updating from GP 3.3.2 to GP 3.3.3 the path to the uploads directory changed from
     *     ../users/<user_id>/user.uploads to
     *     ../users/<user_id>/uploads
     * 
     * To facilitate migration, automatically rename user.upload to uploads if and only if
     * there is a user.uploads directory and there is not an uploads directory.
     * 
     * This method is designed to work only for an update from a clean 3.3.2 installation 
     * (with no custom settings in the config file related to the path to user uplaods).
     * 
     * TODO: should only need this when updating from 3.3.2. But we don't have a simple way of knowing this from within this method.
     * 
     */
    private static void migrateUserUploadDirs(final HibernateSessionManager mgr) {
        List<String> userIds = new ArrayList<String>();
        try {
            UserDAO userDao = new UserDAO(mgr);
            List<User> users = userDao.getAllUsers();
            for(User user : users) {
                userIds.add( user.getUserId() );
            }
        }
        catch (Throwable t) {
            log.error(t);
        }
        finally {
            mgr.closeCurrentSession();
        } 
        
        for(String userId : userIds) {
            StatusCode status = migrateUserUploadDir(userId);
            if (status.getCode() < 0) {
                log.error(status.getMessage());
            }
            else if (status.getCode() > 0) {
                log.info("For user "+userId+": "+status.getMessage());
            }
        }
    }

    /**
     * Attempt to migrate from a GP 3.3.2 to a GP 3.3.3 uploads directory for the given user.
     * Return a status code indicating what happened.
     *
     * -2: Unexpected server error
     * -1: Error, 'user.uploads' and 'uploads' both exist and both contain files.
     *  0: No need to migrate, 'user.uploads' doesn't exist
     *  1: Migrated by deleting empty 'user.uploads'
     *  2: Migrated by renaming 'user.uploads' to 'uploads'
     *  3: Migrated by deleting empty 'uploads' and then renaming 'user.uploads' to 'uploads'
     *  
     * @param userId
     * @return
     */
    private static StatusCode migrateUserUploadDir(String userId) {
        @SuppressWarnings("deprecation")
        GpContext userContext = GpContext.getContextForUser(userId);
        File userDir = ServerConfigurationFactory.instance().getUserDir(userContext);
        if (userDir == null) {
            log.error("userDir is null for userId="+userId);
            return StatusCode.SERVER_ERROR;
        }
        if (!userDir.exists()) {
            log.error("userDir doesn't exist: "+userDir.getAbsolutePath());
            return StatusCode.SERVER_ERROR;
        }
        
        File prevUploadDir = new File(userDir, "user.uploads");
        File currUploadDir = new File(userDir, "uploads");
        String[] prevFiles = null;
        String[] currFiles = null;

        //if there is no user.uploads dir, exit
        if (!prevUploadDir.exists()) {
            return StatusCode.NO_PREV;
        }

        //if we are here, the user.uploads dir exists
        //if user.uploads is empty, delete it
        prevFiles = prevUploadDir.list();
        if (prevFiles != null && prevFiles.length == 0) {
            //assume an empty directory
            boolean success = prevUploadDir.delete();
            if (success) {
                log.info("deleted empty GP 3.3.2 'user.uploads' directory for user: "+userId);
            }
            else {
                log.error("server error deleting file: "+prevUploadDir.getAbsolutePath());
                return StatusCode.SERVER_ERROR;
            }
            return StatusCode.DELETE_PREV;
        }
        
        
        //if we are here, the user.uploads dir exists and is non-empty
        log.debug("Migrating 'user.uploads' dir for user: "+userId);
        
        if (currUploadDir.exists()) {
            currFiles = currUploadDir.list();
            if (currFiles != null && currFiles.length == 0) {
                //'uploads' dir exists but is empty, just delete it so we can rename the 'user.uploads' directory
                boolean success = currUploadDir.delete();
                if (!success) {
                    log.error("server error deleting file: "+currUploadDir.getAbsolutePath());
                    return StatusCode.SERVER_ERROR;
                }
            }
        }
        if (!currUploadDir.exists()) {
            // if it didn't exist (or if it was empty and just got deleted) ...
            boolean success = prevUploadDir.renameTo(currUploadDir);
            if (success) {
                log.info("moved GP 3.3.2 'user.uploads' dir to GP 3.3.3 'uploads' dir for user: "+userId);
                return StatusCode.RENAME_PREV;
            }
            else {
                log.error("server error migrating GP 3.3.2 'user.uploads' directory. ("
                        +prevUploadDir.getAbsolutePath()+").renameTo("
                        +currUploadDir.getAbsolutePath()+") did not succeed.");
                return StatusCode.SERVER_ERROR;
            }
        }
        
        //if we are here, it means the user.uploads dir and the uploads dir exist and are both non-empty
        log.debug("Unable to migrate 'user.uploads' to 'uploads' for userId="+userId+". Both directories exist and contain files");
        return StatusCode.ERROR;
    }
    
    /**
     * Remove all partially uploaded files listed in the GP 3.3.2 UPLOAD_FILE table.
     */
    protected static void deleteAllPartialUploads(final HibernateSessionManager mgr) {
        int numRecordsDeleted = 0;
        int numFilesDeleted = 0;
        
        log.info("deleting all partial uploads from GP 3.3.2 server ...");
        List<String> paths = getAllPartialUploads(mgr);
        log.info("numRecords: "+paths.size());
        for(String path : paths) {
            boolean deleted = false;
            File file = new File(path);
            if (!file.isFile()) {
                deleted = true;
            }
            if (!file.exists()) {
                deleted = true;
            }
            if (!file.canWrite()) {
                deleted = true;
            }
            
            if (file.isFile() && file.exists() && file.canWrite()) {
                deleted = file.delete();
                if (deleted) {
                    ++numFilesDeleted;
                }
                else {
                    log.error("System error deleting file: "+file.getAbsolutePath());
                }
            }
            
            if (deleted) {
                boolean recordDeleted = deleteUploadFileRecord(mgr, path);
                if (recordDeleted) {
                    ++numRecordsDeleted;
                }
            }
        }
        log.info("deleted "+numFilesDeleted+" files from the file system");
        log.info("deleted "+numRecordsDeleted+" records from the database");
    }
    
    /**
     * get the list of partial upload files from the GP 3.3.2 UPLOAD_FILE table.
     * @return
     */
    private static List<String> getAllPartialUploads(final HibernateSessionManager mgr) {
        try {
            mgr.beginTransaction();
            final String sql = "select path from upload_file where status != 0";
            SQLQuery query = mgr.getSession().createSQLQuery(sql);
            @SuppressWarnings("unchecked")
            List<String> paths = query.list();
            return paths;
        }
        catch (Throwable t) {
            log.error("Error getting all partial uploads from GP 3.3.2 UPLOAD_FILE table: "+t.getLocalizedMessage(), t);
        }
        finally {
            mgr.closeCurrentSession();
        }
        return Collections.emptyList();
    }
    
    /**
     * Delete the record from the DB.
     * @param path
     */
    private static boolean deleteUploadFileRecord(final HibernateSessionManager mgr, final String path) {
        try {
            mgr.beginTransaction();
            final String sqlDel = "delete from upload_file where path = :path";
            SQLQuery delQuery = mgr.getSession().createSQLQuery(sqlDel);
            delQuery.setString("path", path);
            int numDeleted = delQuery.executeUpdate();
            if (numDeleted != 1) {
                log.error("Error deleting record from UPLOAD_FILE, path="+path+", numDeleted="+numDeleted);
            }
            else {
                mgr.commitTransaction();
                return true;
            }
        }
        catch (Throwable t) {
            log.error("Error deleting record from UPLOAD_FILE, path="+path+", Error: "+t.getLocalizedMessage(), t);
        }
        finally {
            mgr.closeCurrentSession();
        }
        return false;
    }

    /**
     * Delete the record from the DB.
     * @param path
     */
    private static boolean deleteAllOldUploadFileRecords(final HibernateSessionManager mgr) {
        log.info("deleting all records from old UPLOAD_FILE table ...");
        try {
            mgr.beginTransaction();
            final String sqlDel = "delete from upload_file";
            SQLQuery delQuery = mgr.getSession().createSQLQuery(sqlDel);
            int numDeleted = delQuery.executeUpdate();
            mgr.commitTransaction();
            log.info("deleted "+numDeleted+" records");
            return true;
        }
        catch (Throwable t) {
            log.error("Error deleting all records from UPLOAD_FILE table, Error: "+t.getLocalizedMessage(), t);
        }
        finally {
            mgr.closeCurrentSession();
        }
        return false;
    }

}

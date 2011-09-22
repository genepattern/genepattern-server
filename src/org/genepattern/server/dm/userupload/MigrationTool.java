package org.genepattern.server.dm.userupload;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
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
     * Migrate all user upload files from GP 3.3.2 format to GP 3.3.3 format.
     * Store a record of this in the DB so that we only do this once.
     * This should be called from the StartupServlet after the DB is initialized.
     */
    public static void migrateUserUploads() {
        try {
            HibernateUtil.beginTransaction();
            String sql = "select value from PROPS where key = :key";
            SQLQuery query = HibernateUtil.getSession().createSQLQuery(sql);
            query.setString("key", "sync.user.uploads.complete");
            List<String> rval = query.list();
            if (rval != null && rval.size() > 0) {
                log.debug("sync.user.uploads.complete="+rval.get(0));
                return;
            }
            HibernateUtil.closeCurrentSession();
        }
        catch (Throwable t) {
            log.error("Server error: "+t.getLocalizedMessage(), t);
            HibernateUtil.closeCurrentSession();
            return;
        }
        
        log.info("migrating user upload directories ...");
        
        //1) clean up old partial uploads
        deleteAllPartialUploads();
        
        //2) set up GP 3.3.3 user upload directories
        migrateUserUploadDirs();
        
        //3) resync the DB entries
        syncUserUploadFiles();
        
        //4) finally, tab the DB, so that we don't do this again
        try {
            HibernateUtil.beginTransaction();
            //insert into props ( key, value ) values ('sync.user.uploads.complete', 'true')
            final String sql = "insert into PROPS ( key, value ) values ( :key, :value )";
            final SQLQuery query = HibernateUtil.getSession().createSQLQuery(sql);
            query.setString("key", "sync.user.uploads.complete");
            query.setString("value", "true");
            int num = query.executeUpdate();
            if (num != 1) {
                String message = "Error updating db, expecting 1 result but received "+num+". \n"+
                        "\t insert into PROPS ( key, value ) values ( 'sync.user.uploads.complete', 'true')";
                log.error(message);
                HibernateUtil.rollbackTransaction();
            }
            HibernateUtil.commitTransaction();
        }
        catch (Throwable t) {
            log.error(t);
            HibernateUtil.rollbackTransaction();
        } 
    }
    
    private static void syncUserUploadFiles() {
        List<String> userIds = new ArrayList<String>();
        try {
            UserDAO userDao = new UserDAO();
            List<User> users = userDao.getAllUsers();
            for(User user : users) {
                userIds.add( user.getUserId() );
            }
        }
        catch (Throwable t) {
            log.error(t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        } 
        
        for(String userId : userIds) {
            DataManager.syncUploadFiles(userId);
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
     * TODO: delete this code for the 3.4 release
     * 
     */
    private static void migrateUserUploadDirs() {
        List<String> userIds = new ArrayList<String>();
        try {
            UserDAO userDao = new UserDAO();
            List<User> users = userDao.getAllUsers();
            for(User user : users) {
                userIds.add( user.getUserId() );
            }
        }
        catch (Throwable t) {
            log.error(t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
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
        Context userContext = ServerConfiguration.Context.getContextForUser(userId);
        File userDir = ServerConfiguration.instance().getUserDir(userContext);
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
    public static void deleteAllPartialUploads() {
        int numRecordsDeleted = 0;
        int numFilesDeleted = 0;
        
        log.info("deleting all partial uploads from GP 3.3.2 server ...");
        List<String> paths = getAllPartialUploads();
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
                boolean recordDeleted = deleteUploadFileRecord(path);
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
    private static List<String> getAllPartialUploads() {
        try {
            HibernateUtil.beginTransaction();
            final String sql = "select path from upload_file where status != 0";
            SQLQuery query = HibernateUtil.getSession().createSQLQuery(sql);
            List<String> paths = query.list();
            return paths;
        }
        catch (Throwable t) {
            log.error("Error getting all partial uploads from GP 3.3.2 UPLOAD_FILE table: "+t.getLocalizedMessage(), t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        return Collections.emptyList();
    }
    
    /**
     * Delete the record from the DB.
     * @param path
     */
    private static boolean deleteUploadFileRecord(final String path) {
        try {
            HibernateUtil.beginTransaction();
            final String sqlDel = "delete from upload_file where path = :path";
            SQLQuery delQuery = HibernateUtil.getSession().createSQLQuery(sqlDel);
            delQuery.setString("path", path);
            int numDeleted = delQuery.executeUpdate();
            if (numDeleted != 1) {
                log.error("Error deleting record from UPLOAD_FILE, path="+path+", numDeleted="+numDeleted);
            }
            else {
                HibernateUtil.commitTransaction();
                return true;
            }
        }
        catch (Throwable t) {
            log.error("Error deleting record from UPLOAD_FILE, path="+path+", Error: "+t.getLocalizedMessage(), t);
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
        return false;
    }


}

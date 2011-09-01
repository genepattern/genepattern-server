package org.genepattern.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
import org.genepattern.server.dm.userupload.dao.UserUpload;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webapp.jsf.UIBeanHelper;

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
    
    public static boolean createSubdirectory(File parent, String name, String userId) {
        Context context = UIBeanHelper.getUserContext();
        File absoluteFile = new File(parent, name);
        boolean success = absoluteFile.mkdir();
        if (success) {
            try {
                File relativeFile = new File(UserUploadManager.absoluteToRelativePath(context, absoluteFile.getAbsolutePath()));
                GpFilePath subdir = UserUploadManager.getUploadFileObj(context, relativeFile);
                UserUploadManager.createUploadFile(context, subdir, 1);
                UserUploadManager.updateUploadFile(context, subdir, 1, 1);
            }
            catch (Exception e) {
                success = false;
            }
        }
        
        return success;
    }

    /**
     * Delete the file from the server file system, checking permissions based on user context.
     * 
     * @param uploadedFile
     * 
     * @return true if the file was deleted
     */
    public static boolean deleteFile(GpFilePath fileObj) {
        Context context = UIBeanHelper.getUserContext();
        // Reselect GpFilePath from the database so the id is attached
        UserUpload uploadedFile = UserUpload.initFromGpFileObj(context, fileObj);
        
        //if we are in a transaction, don't commit and close
        boolean inTransaction = HibernateUtil.isInTransaction();
        
        //this begins a new transaction
        UserUploadDao dao = new UserUploadDao();
        if (uploadedFile == null) {
            log.error("FileObj is null");
            return false;
        }

        File file = fileObj.getServerFile();

        boolean deleted = false;
        boolean canDelete = canDelete(uploadedFile);
        if (!canDelete) {
            return false;
        }
            
        if (file.exists()) {
            deleted = file.delete(); 
            if (!deleted) {
                log.error("Error deleting file: "+file.getPath());
            }
        }
        // as currently implemented there is a small chance the db will not get updated
        // after deleting the file, therefore, we remove the record from the DB in all cases
        if (!file.exists()) {
            try {
                dao.delete(uploadedFile);
                if (!inTransaction) {
                    HibernateUtil.commitTransaction();
                }
            }
            catch  (Throwable t) {
                deleted = false;
                //possible error updating the DB
                log.error("Error deleting file record from db, path="+uploadedFile.getPath(), t);
            }
            finally {
                if (!inTransaction) {
                    HibernateUtil.rollbackTransaction();
                } 
            }
        } 
        return deleted;
    }
    
    public static boolean canDelete(UserUpload uf) {
        if (uf == null) {
            return false;
        }
        File toDel = new File(uf.getPath());
        if (toDel.isDirectory() && toDel.listFiles().length > 0) {
            log.info("Unable to delete non-empty directories: " + toDel.getPath());
            return false;
        }
        String userid = UIBeanHelper.getUserId();
        if (userid == null) {
            //require a userid
            return false;
        }

        //TODO: come up with an improved policy for ACL for admin users
        boolean isAdmin = false;
        isAdmin = AuthorizationHelper.adminJobs(userid);
        if (isAdmin) {
            return true;
        }
        
        if (!toDel.canWrite()) {
            //TODO: server error
            return false;
        }
        
        return userid.equals(uf.getUserId());
    }
    
    private static boolean isExcludedFile(File file) {
        for (String i : FILE_EXCLUDES) {
            if (file.getName().equalsIgnoreCase(i)) {
                return true;
            }
        }
        return false;
    }
    
    private static void handleFileSync(UserUploadDao dao, File file, String user) throws Exception {
        // Exclude file on exclude list (ex: .DS_Store)
        if (isExcludedFile(file)) {
            return;
        }
        
        UserUpload uploadFile = new UserUpload();
        uploadFile.setPath(UserUploadManager.absoluteToRelativePath(UIBeanHelper.getUserContext(), file.getCanonicalPath()));
        uploadFile.setUserId(user);
        uploadFile.setNumParts(1);
        uploadFile.setNumPartsRecd(1);
        uploadFile.init(file);
        dao.saveOrUpdate(uploadFile);
        
        if (file.isDirectory()) {
            for (File i : file.listFiles()) {
                handleFileSync(dao, i, user);
            }
        }
    }
    
    public static void syncUploadFiles(String user) {
        try {
            UserUploadDao dao = new UserUploadDao();
            List<UserUpload> userFiles = dao.selectAllUserUpload(user);
            File uploadDir = getUserUploadDirectory(user);
            if (uploadDir == null) {
                log.error("Unable to get the user's upload directory in syncUploadFiles()");
                return;
            }
            
            // Remove all the old database entries
            for (UserUpload i : userFiles) {
                dao.delete(i);
            }
            HibernateUtil.commitTransaction();
            dao = new UserUploadDao();
            // Add new entries to the database
            for (File i : uploadDir.listFiles()) {
                handleFileSync(dao, i, user);
            }
            
            // Commit
            HibernateUtil.commitTransaction();
        }
        catch (Exception e) {
            log.error("Error committing upload file sync to database");
            HibernateUtil.rollbackTransaction();
        }
    }

    public static File getUserUploadDirectory(String user) {
        return ServerConfiguration.instance().getUserUploadDir(Context.getContextForUser(user));
    }
}

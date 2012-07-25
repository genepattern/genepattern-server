package org.genepattern.server.process;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.ServerConfiguration.Context;
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

    private Context userContext;
    private ExecutorService exec;

    public UserUploadPurger(Context userContext, long dateCutoff, boolean purgeAll) {
        this.userContext = userContext;
        
        //current implementation ignores these settings
        //this.dateCutoff = dateCutoff;
        //this.purgeAll = purgeAll;
    }

    public void purge(ExecutorService exec) throws Exception { 
        if (exec == null || exec.isShutdown()) {
            exec = Executors.newSingleThreadExecutor();
        }
        this.exec = exec;

        log.debug("initializing purger for userId="+userContext.getUserId());
        init();
        purgePartialUploadsFromDb();
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
        UserUploadDao dao = new UserUploadDao();
        List<UserUpload> userUploads = dao.selectStalledPartialUploadsForUser(userContext.getUserId(), maxModDate);
        
        for(UserUpload userUpload : userUploads) {
            try {
                GpFilePath gpFilePath = initGpFilePath(userUpload);
                purgeUserUploadFile(gpFilePath); 
            }
            catch (Throwable t) {
                log.error("Error purging partial upload, userId="+userContext.getUserId()+", path="+userUpload.getPath(), t);
            }
        }
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
     * Initialize all required variables before purging.
     * 
     * @throws Exception - if we shouldn't walk the tree
     */
    private void init() throws Exception {
        if (userContext == null) {
            throw new IllegalArgumentException("userContext == null");
        }
        if (userContext.getUserId() == null) {
            throw new IllegalArgumentException("userContext.userId == null");
        }
    }

    /**
     * Schedule the file to be purged.
     * @param gpFilePath
     */
    private void purgeUserUploadFile(final GpFilePath gpFilePath) {
        Future<Boolean> task = exec.submit(new Callable<Boolean> () {
            public Boolean call() throws Exception {
                log.debug("deleting relativeUri='"+gpFilePath.getRelativeUri()+"' ...");
                boolean deleted = false;
                try {
                    HibernateUtil.beginTransaction();
                    deleted = DataManager.deleteUserUploadFile(userContext.getUserId(), gpFilePath);
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

/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.process;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.DataManager;
import org.genepattern.server.FileUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.userupload.UserUploadManager;
//import org.genepattern.server.dm.userupload.dao.UserUpload;
//import org.genepattern.server.dm.userupload.dao.UserUploadDao;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

/**
 * Periodically purge jobs that completed some number of days ago and input files.
 * 
 */
public class Purger extends TimerTask {
    private static Logger log = Logger.getLogger(Purger.class);

    /** number of days back to preserve completed jobs */
    private int purgeInterval = -1;

    public Purger(int purgeInterval) {
        this.purgeInterval = purgeInterval;
    }

    @Override
    public void run() {
        if (purgeInterval != -1) {
            try {
                GregorianCalendar purgeDate = new GregorianCalendar();
                purgeDate.add(GregorianCalendar.DATE, -purgeInterval);
                log.info("Purger: purging jobs completed before " + purgeDate.getTime());

                HibernateUtil.beginTransaction();
                AnalysisDAO ds = new AnalysisDAO();
                List<Integer> jobIds = ds.getAnalysisJobIds(purgeDate.getTime());
                for(Integer jobId : jobIds) {
                    // delete the job from the database and recursively delete the job directory
                    ds.deleteJob(jobId);
                }
                HibernateUtil.commitTransaction();
                
                purgeBatchJobs(purgeDate);               
                
                long dateCutoff = purgeDate.getTime().getTime();
                // remove input files uploaded using web form
                purgeWebUploads(dateCutoff);
                // Other code purging uploads directory is also called; this is called in addition
                //purgeDirectUploads(dateCutoff);
                purgeUserUploads(dateCutoff);

                File soapAttachmentDir = new File(System.getProperty("soap.attachment.dir"));
                File[] userDirs = soapAttachmentDir.listFiles();
                if (userDirs != null) {
                    for (File f : userDirs) {
                        purge(f, dateCutoff);
                        File[] files = f.listFiles();
                        if (files == null || files.length == 0) {
                            f.delete();
                        }
                    }
                }
            } 
            catch (Exception e) {
                HibernateUtil.rollbackTransaction();
                log.error("Error while purging jobs", e);
            } 
            finally {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    /**
     * Purge files from the system web upload directory.
     * @param dateCutoff
     */
    private void purgeWebUploads(long dateCutoff) {
        File webUploadDir = new File(System.getProperty("java.io.tmpdir"));
        purge(webUploadDir, dateCutoff);
    }

    private void purge(File dir, long dateCutoff) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].lastModified() < dateCutoff) {
                    if (files[i].isDirectory()) {
                        Delete del = new Delete();
                        del.setDir(files[i]);
                        del.setIncludeEmptyDirs(true);
                        del.setProject(new Project());
                        del.execute();
                    } 
                    else {
                        files[i].delete();
                    }
                }
            }
        }
    }
    
    /**
     * Purge files in the user upload directory for each user.
     * 
     * Note: added by pcarr as a replacement for purgeDirectUploads
     * 
     * @param dateCutoff
     */
    private void purgeUserUploads(long dateCutoff) {
        List<String> userIds = new ArrayList<String>();
        HibernateUtil.beginTransaction();
        UserDAO userDao = new UserDAO();
        List<User> users = userDao.getAllUsers();
        for(User user : users) {
            userIds.add( user.getUserId() );
        }
        HibernateUtil.closeCurrentSession();
        for(String userId : userIds) {
            Context userContext = Context.getContextForUser(userId);
            purgeUserUploadsForUser(userContext, dateCutoff);
        }
    }
    
    private void purgeUserUploadsForUser(Context userContext, long dateCutoff) {
        boolean purgeAll = ServerConfiguration.instance().getGPBooleanProperty(userContext, "upload.purge.all", false);
        
        GpFilePath rootDir = null;
        try {
            rootDir = UserUploadManager.getUserUploadDir(userContext);
        }
        catch (Exception e) {
            log.error("Error purging upload files for user: "+userContext.getUserId(), e);
            return;
        }
        purgeUserUploadsFromDir(userContext, rootDir, rootDir, dateCutoff, purgeAll);
    }

    /**
     * recursively purge each file from the given dir
     */
    private void purgeUserUploadsFromDir(Context userContext, GpFilePath rootDir, GpFilePath dir, long dateCutoff, boolean purgeAll) {
        File f = dir.getServerFile();
        //filter some files from the list of files and directories to be purged
        FilenameFilter filenameFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if ( DataManager.FILE_EXCLUDES.contains( name ) ) {
                    return false;
                }
                return true;
            }
        };
        File[] uploadFiles = f.listFiles(filenameFilter);
        for(File uploadFile : uploadFiles) {
            GpFilePath filePath = null;

            try {
                //get relative path
                File relativePath = FileUtil.relativizePath(rootDir.getServerFile(), uploadFile);
                boolean initMetaData = true;
                filePath = UserUploadManager.getUploadFileObj(userContext, relativePath, initMetaData);
            }
            catch (Throwable t) {
                String message = "Error getting GpFilePath for file, '"+uploadFile.getPath()+", :"+t.getLocalizedMessage();
                log.error(message, t);
            }
            if (filePath != null) {
                if (filePath.isDirectory()) {
                    //depth-first delete, just in case
                    //NOTE: as currently implemented, depth-first is not necessary, because
                    //    we are not deleting directories, just files in directories
                    purgeUserUploadsFromDir(userContext, rootDir, filePath, dateCutoff, purgeAll);
                }
                else {
                    //purge each individual file here, 
                    boolean purged = purgeUserUploadFile(userContext, filePath, dateCutoff, purgeAll);
                }
            }
        }
    }
    
    /**
     * Purge the given file, if and only if, it is supposed to be purged.
     * 
     * @param userContext, requires a valid userId
     * @param uploadFilePath, the file to purge
     * @param dateCutoff, don't purge if the file is newer than this date
     * @param purgeAll, when this is true, purge all files, when false, only purge partial uploads
     * 
     * @return true if the file was deleted
     */
    private boolean purgeUserUploadFile(Context userContext, GpFilePath uploadFilePath, long dateCutoff, boolean purgeAll) {
        //Note: operating on server files because optimization is not as important as consistency
        File serverFile = uploadFilePath.getServerFile();
        
        //double-check that it's not a directory
        if (serverFile.isDirectory()) {
            return false;
        }
        //check that it is older than the purge date
        if (serverFile.lastModified() >= dateCutoff) {
            return false;
        }
        if (!purgeAll) {
            //only delete partial uploads
            if (uploadFilePath.getNumPartsRecd() == uploadFilePath.getNumParts()) {
                return false;
            }
        }
        
        //if we are here, it means delete the file, whether we have a record in the DB or not
        //1) delete the file from the filesystem
        //2) remove the record from the db, single db transaction per file
        boolean deleted = false;
        try {
            HibernateUtil.beginTransaction();
            deleted = DataManager.deleteUserUploadFile(userContext.getUserId(), uploadFilePath);
            HibernateUtil.commitTransaction();
            return deleted;
        }
        catch (Throwable t) {
            log.error("Error in purgeUserUploadFile for file '"+ uploadFilePath.getRelativeUri()+"': "+t.getLocalizedMessage(), t);
            HibernateUtil.rollbackTransaction();
            return false;
        }
    }
    
    private void purgeBatchJobs(GregorianCalendar purgeDate){
    	  HibernateUtil.beginTransaction();
          BatchJobDAO batchJobDAO = new BatchJobDAO();
          List<BatchJob> possiblyEmpty = batchJobDAO.getOlderThanDate(purgeDate.getTime());
          for (BatchJob batchJob: possiblyEmpty){
          	if (batchJob.getBatchJobs().size() == 0){
          		HibernateUtil.getSession().delete(batchJob);
          	}
          }
          HibernateUtil.commitTransaction();
    }

    public static void main(String args[]) {
        Purger purger = new Purger(0);
        purger.run();
    }
}

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
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.DataManager;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.domain.UploadFile;
import org.genepattern.server.domain.UploadFileDAO;
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
                purge(new File(System.getProperty("java.io.tmpdir")), dateCutoff);
                // Other code purging uploads directory is also called; this is called in addition
                purgeDirectUploads(dateCutoff);

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
     * Purge the direct upload directories of all users
     * @param dateCutoff
     */
    private void purgeDirectUploads(long dateCutoff) {
        UploadFileDAO uploadDAO = new UploadFileDAO();
        List<User> users = (new UserDAO()).getAllUsers();
        for (User i : users) {
            Context context = Context.getContextForUser(i.getUserId());
            File userUploadDir = ServerConfiguration.instance().getUserUploadDir(context);
            boolean purgeAll = ServerConfiguration.instance().getGPBooleanProperty(context, "upload.purge.all", false);
            purgeUserUploads(userUploadDir, dateCutoff, purgeAll, uploadDAO);
        }
    }
    
    /**
     * Purge the direct upload directory of a given user
     * @param dir
     * @param dateCutoff
     * @param purgeAll
     */
    private void purgeUserUploads(File dir, long dateCutoff, boolean purgeAll, UploadFileDAO uploadDAO) {
        for (File i : dir.listFiles()) {
            UploadFile file = null;
            try {
                file = uploadDAO.findByPath(i.getCanonicalPath());
                if (file == null) {
                    log.warn("Unable to find file in database, deteting manually: " + i.getAbsolutePath());
                    i.delete();
                    break;
                }
            }
            catch (IOException e) {
                log.error("Unable to get cannonical path of file: " + i.getAbsolutePath());
                break;
            }

            if (!i.isDirectory() && i.lastModified() < dateCutoff && (purgeAll || file.isPartial())) {
                // Delete the file
                boolean success = DataManager.deleteFile(file);
                if (!success) {
                    log.error("Problems deleting file: " + i.getAbsolutePath());
                }
            }
            else if (i.isDirectory()) {
                // Recurse into that directory
                purgeUserUploads(i, dateCutoff, purgeAll, uploadDAO);
            }
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

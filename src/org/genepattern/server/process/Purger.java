/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
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
    private static final Logger log = Logger.getLogger(Purger.class);

    private final GpConfig gpConfig;

    /** number of days back to preserve completed jobs */
    private int purgeInterval = -1;

    public Purger(int purgeInterval) {
        this.gpConfig=ServerConfigurationFactory.instance();
        this.purgeInterval = purgeInterval;
    }

    @Override
    public void run() {
        log.debug("running Purger ...");
        if (purgeInterval != -1) {
            final HibernateSessionManager mgr=HibernateUtil.instance();
            try {
                GregorianCalendar purgeDate = new GregorianCalendar();
                purgeDate.add(GregorianCalendar.DATE, -purgeInterval);
                log.info("Purger: purging jobs completed before " + purgeDate.getTime());

                log.debug("purging job results ...");
                mgr.beginTransaction();
                AnalysisDAO ds = new AnalysisDAO(mgr);
                List<Integer> jobIds = ds.getAnalysisJobIds(purgeDate.getTime());
                for(Integer jobId : jobIds) {
                    // delete the job from the database and recursively delete the job directory
                    ds.deleteJob(jobId);
                }
                mgr.commitTransaction();
                log.debug("done purging job results.");

                log.debug("purging batch jobs ...");
                purgeBatchJobs(mgr, purgeDate);
                log.debug("done purging batch jobs.");

                log.debug("purging web upload files ...");
                //long dateCutoff = purgeDate.getTime().getTime();
                final Date dateCutoff = purgeDate.getTime();
                // remove input files uploaded using web form
                purgeWebUploads(dateCutoff);
                log.debug("done purging web upload files.");

                // Other code purging uploads directory is also called; this is called in addition
                log.debug("purging user upload files ...");
                purgeUserUploads(mgr, dateCutoff);
                log.debug("done purging user upload files.");

                log.debug("purging soap attachments ...");
                File soapAttachmentDir = gpConfig.getSoapAttDir(GpContext.getServerContext());
                log.debug("    soapAttachmentDir="+soapAttachmentDir);
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
                log.debug("done purging soap attachments.");
            } 
            catch (Exception e) {
                mgr.rollbackTransaction();
                log.error("Error while purging jobs", e);
            } 
            finally {
                mgr.closeCurrentSession();
            }
        }
        log.debug("Done running purger!");
    }

    /**
     * Purge files from the system web upload directory.
     * @param dateCutoff
     */
    private void purgeWebUploads(final Date dateCutoff) {
        File webUploadDir = gpConfig.getTempDir(GpContext.getServerContext());
        purge(webUploadDir, dateCutoff);
    }

    private void purge(File dir, final Date dateCutoff) {
        if (dir != null) {
            log.debug("purging files from directory: "+dir.getPath());
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].lastModified() < dateCutoff.getTime()) {
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
     * @param dateCutoff
     */
    private void purgeUserUploads(final HibernateSessionManager mgr, final Date dateCutoff) {
        log.debug("getting user ids from db ...");
        List<String> userIds = new ArrayList<String>();
        mgr.beginTransaction();
        UserDAO userDao = new UserDAO(mgr);
        List<User> users = userDao.getAllUsers();
        for(User user : users) {
            userIds.add( user.getUserId() );
        }
        mgr.closeCurrentSession();
        log.debug("done getting user ids from db.");
        log.debug("purging data for each user ...");
        
        ExecutorService exec = Executors.newSingleThreadExecutor();
        for(String userId : userIds) {
            @SuppressWarnings("deprecation")
            GpContext userContext = GpContext.getContextForUser(userId);
            purgeUserUploadsForUser(exec, mgr, userContext, dateCutoff);
        }
        exec.shutdown();
        log.debug("done purging data for each user.");
    }
    
    private void purgeUserUploadsForUser(ExecutorService exec, final HibernateSessionManager mgr, GpContext userContext, final Date dateCutoff) {
        log.debug("purgeUserUploadsForUser(userId='"+userContext.getUserId()+"') ...");
        try {
            final UserUploadPurger uup = new UserUploadPurger(exec, mgr, gpConfig, userContext, dateCutoff);
            uup.purge();
        }
        catch (Throwable t) {
            log.error(t);
        }
    }

    private void purgeBatchJobs(final HibernateSessionManager mgr, final GregorianCalendar purgeDate){
    	  mgr.beginTransaction();
          BatchJobDAO batchJobDAO = new BatchJobDAO(mgr);
          List<BatchJob> possiblyEmpty = batchJobDAO.getOlderThanDate(purgeDate.getTime());
          for (BatchJob batchJob: possiblyEmpty){
          	if (batchJob.getBatchJobs().size() == 0){
          		mgr.getSession().delete(batchJob);
          	}
          }
          mgr.commitTransaction();
    }

    public static void main(String args[]) {
        Purger purger = new Purger(0);
        purger.run();
    }
}

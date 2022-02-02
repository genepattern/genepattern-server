/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/

package org.genepattern.server.purger.impl02;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.process.JobPurgerUtil;
import org.genepattern.server.process.UserUploadPurger;
import org.genepattern.server.purger.Purger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

import javassist.tools.reflect.Loader;

/**
 * Periodically purge jobs that completed some number of days ago and input files.
 * The purgeJobsAfter time can optionally be configured on a per-user or per-group basis.
 * 
 * The default 'purgeTime' is used to determine that time each day that the purger will run.
 * The optional custom settings for 'purgeJobsAfter' and 'purgeTime' determine the cutoff date
 * for deleting old resources (upload files, jobs, and batch jobs).
 * 
 * TODO: enable cancellation of the existing purger when ...
 *     ... during runtime, the configuration changes
 *     ... at server shutdown
 * 
 */
public class Purger02 extends TimerTask {
    private static final Logger log = Logger.getLogger(Purger02.class);
    
    private final GpConfig gpConfig;

    public Purger02() {
        gpConfig=ServerConfigurationFactory.instance();
    }

    
   
    
    
    @Override
    public void run() {
        log.debug("running Purger ...");
        final Date now=new Date();
        final HibernateSessionManager mgr=HibernateUtil.instance();
        final GpContext serverContext=GpContext.getServerContext();
        final int purgeJobsAfter=gpConfig.getGPIntegerProperty(serverContext, Purger.PROP_PURGE_JOBS_AFTER, Purger.PURGE_JOBS_AFTER_DEFAULT);
        final String purgeTime=gpConfig.getGPProperty(serverContext, Purger.PROP_PURGE_TIME, Purger.PURGE_TIME_DEFAULT);
        final Date serverCutoff=JobPurgerUtil.getCutoff(now, purgeJobsAfter, purgeTime);
        try {
            if (serverCutoff != null) {
                log.debug("purging web upload files ...");
                // remove input files uploaded using web form
                purgeLegacyWebUploadsFromTmpdir(serverCutoff.getTime());
                log.debug("done purging web upload files.");
            }

            // Other code purging uploads directory is also called; this is called in addition
            log.debug("purging user upload files and jobs ...");
            purgeUserUploadsAndJobs(mgr, now);
            log.debug("done purging user upload files.");

            if (serverCutoff != null) {
                log.debug("purging soap attachments ...");
                final File soapAttachmentDir = gpConfig.getSoapAttDir(GpContext.getServerContext());
                log.debug("    soapAttachmentDir="+soapAttachmentDir);
                final File[] userDirs = soapAttachmentDir.listFiles();
                if (userDirs != null) {
                    for (final File f : userDirs) {
                        purge(f, serverCutoff.getTime());
                        final File[] files = f.listFiles();
                        if (files == null || files.length == 0) {
                            f.delete();
                        }
                    }
                }
                log.debug("done purging soap attachments.");
            }
        } 
        catch (Exception e) {
            mgr.rollbackTransaction();
            log.error("Error while purging jobs", e);
        } 
        finally {
            mgr.closeCurrentSession();
        }
        log.debug("Done running purger!");
    }

    /**
     * Purge files from the system 'java.io.tmpdir' (legacy) web upload directory.
     * Newer versions of GP store web uploads as hidden files in the user uploads directory.
     * They can be managed on a per-user basis.
     * @param dateCutoff
     */
    private void purgeLegacyWebUploadsFromTmpdir(final long dateCutoff) {
        File webUploadDir = gpConfig.getTempDir(GpContext.getServerContext());
        purge(webUploadDir, dateCutoff);
    }

    private void purge(final File dir, final long dateCutoff) {
        if (dir==null) {
            log.error("dir==null");
            return;
        }
        log.debug("purging files from directory: "+dir.getPath());
        final File[] files = dir.listFiles();
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
    private void purgeUserUploadsAndJobs(final HibernateSessionManager mgr, final Date now) {
        final List<String> userIds = new ArrayList<String>();

        log.debug("getting userIds from db ...");
        try {
            UserDAO userDao = new UserDAO(mgr);
            final List<User> users = userDao.getAllUsers();
            for(final User user : users) {
                userIds.add( user.getUserId() );
            }
            log.debug("done getting userIds from db.");
        }
        catch (Throwable t) {
            log.error("error getting userIds from db",t);
        }
        finally {
            mgr.closeCurrentSession();
        }
        log.debug("purging data for each user ...");
        
        final ExecutorService exec = Executors.newSingleThreadExecutor();
        for(final String userId : userIds) {
            @SuppressWarnings("deprecation")
            final GpContext userContext = GpContext.getContextForUser(userId);
            final Date cutoffDate=JobPurgerUtil.getCutoffForUser(gpConfig, userContext, now);
            Date publicCutoffDate = JobPurgerUtil.getPublicJobCutoffForUser(gpConfig, userContext, now);
            if (publicCutoffDate == null) publicCutoffDate = cutoffDate;
            
            if (cutoffDate != null) {
                try {
                    purgeJobsForUser(mgr, userContext, cutoffDate, publicCutoffDate);
                    purgeUserUploadsForUser(exec, mgr, userContext, cutoffDate);
                    purgeBatchJobsForUser(mgr, userContext, cutoffDate);
                }
                catch (Throwable t) {
                    log.error("Unexpected error purging resources for userId="+userId, t);
                }
            }
        }
        exec.shutdown();
        log.debug("done purging data for each user.");
    }
    
    public void printList(String message, List<Integer> aList){
        System.out.print (message);
        for (int i: aList){
            System.out.print("  "+ i);
        }
        System.out.println();
    }
    
    
    private void purgeJobsForUser(final HibernateSessionManager mgr, final GpContext userContext, final Date cutoffDate, final Date publicJobCutoffDate) {
        log.debug("purging jobs for user="+userContext.getUserId()+" ...");
        final List<Integer> jobIds=getNonPublicJobIdsForUser(mgr, userContext.getUserId(), cutoffDate);
        final List<Integer> publicJobIds=getPublicJobIdsForUser(mgr, userContext.getUserId(), publicJobCutoffDate);
       
       
        for(Integer jobId : jobIds) {
            deleteJob(mgr, jobId);
         }
        for(Integer jobId : publicJobIds) {
            deleteJob(mgr, jobId);
         }
        
        
        log.debug("done purging job results.");
    }

    // getPublicAnalysisJobIdsForUser
    
    private List<Integer> getPublicJobIdsForUser(final HibernateSessionManager mgr, final String userId, final Date cutoffDate) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO(mgr);
            List<Integer> jobIds = ds.getPublicAnalysisJobIdsForUser(userId, cutoffDate);
            return jobIds;
        }
        catch (Throwable t) {
            log.error("Unexpected error getting list of jobIds for user="+userId, t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
        return Collections.emptyList();
    }

    private List<Integer> getNonPublicJobIdsForUser(final HibernateSessionManager mgr, final String userId, final Date cutoffDate) {
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO(mgr);
            List<Integer> jobIds = ds.getNonPublicAnalysisJobIdsForUser(userId, cutoffDate);
            return jobIds;
        }
        catch (Throwable t) {
            log.error("Unexpected error getting list of jobIds for user="+userId, t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
        return Collections.emptyList();
    }
    
    
    private boolean deleteJob(final HibernateSessionManager mgr, final Integer jobId) {
        // delete the job from the database and recursively delete the job directory
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO(mgr);
            ds.deleteJob(jobId);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return true;
        }
        catch (Throwable t) {
            log.error("Unexpected error deleting jobId="+jobId, t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
        return false;        
    }
    
    
    private void purgeUserUploadsForUser(final ExecutorService exec, final HibernateSessionManager mgr, final GpContext userContext, final Date cutoffDate) {
        if (cutoffDate==null) {
            log.debug("skipping userId="+userContext.getUserId());
            return;
        }
        log.debug("purging user uploads for userId='"+userContext.getUserId()+"' ...");
        try {
            final UserUploadPurger uup = new UserUploadPurger(exec, mgr, gpConfig, userContext, cutoffDate);
            uup.purge();
        }
        catch (Throwable t) {
            log.error(t);
        }
    }
    
    private void purgeBatchJobsForUser(final HibernateSessionManager mgr, final GpContext userContext, final Date cutoffDate) {
        if (userContext==null) {
            log.error("userContext==null");
            return;
        }
        if (userContext.getUserId()==null) {
            log.error("userContext.userId==null");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("purging batch jobs for userId="+userContext.getUserId()+" ... ");
            log.debug("cutoffDate="+cutoffDate);
        }
        if (cutoffDate==null) {
            return;
        }
        
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final BatchJobDAO batchJobDAO = new BatchJobDAO(mgr);
            final List<BatchJob> batchJobsToPurge = batchJobDAO.getOlderThanDateForUser(userContext.getUserId(), cutoffDate);
            if (log.isDebugEnabled()) {
                log.debug("batchJobsToPurge.size="+batchJobsToPurge.size());
            }
            for(final BatchJob batchJob : batchJobsToPurge) {
                deleteBatchJob(mgr, batchJob);
                mgr.getSession().flush();
            }
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
        log.debug("done purging batch jobs for userId="+userContext.getUserId());
    }
    
    /**
     * delete the batch job entry from the DB if it no longer contains any jobs.
     * @param batchJob
     */
    private void deleteBatchJob(final HibernateSessionManager mgr, final BatchJob batchJob) {
        if (batchJob==null) {
            log.error("batchJob==null");
            return;
        }
        if (batchJob.getBatchJobs()==null) {
            log.error("batchJob.getBatchJobs()==null");
            return;
        }
        if (batchJob.getBatchJobs().size() != 0) {
            if (log.isDebugEnabled()) {
                log.debug("skipping batch job_no="+batchJob.getJobNo()+" for user="+batchJob.getUserId()
                        +", batchJobs.size="+batchJob.getBatchJobs().size());
            }
            return;
        }
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            mgr.getSession().delete(batchJob);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error deleting batch job", t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}

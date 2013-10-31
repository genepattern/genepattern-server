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

package org.genepattern.server.purger.impl02;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.process.JobPurgerUtil;
import org.genepattern.server.process.UserUploadPurger;
import org.genepattern.server.purger.Purger;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

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

    public Purger02() {
    }

    @Override
    public void run() {
        log.debug("running Purger ...");
        final Date now=new Date();
        final Context serverContext=ServerConfiguration.Context.getServerContext();
        final int purgeJobsAfter=ServerConfiguration.instance().getGPIntegerProperty(serverContext, Purger.PROP_PURGE_JOBS_AFTER, Purger.PURGE_JOBS_AFTER_DEFAULT);
        final String purgeTime=ServerConfiguration.instance().getGPProperty(serverContext, Purger.PROP_PURGE_TIME, Purger.PURGE_TIME_DEFAULT);
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
            purgeUserUploadsAndJobs(now);
            log.debug("done purging user upload files.");

            if (serverCutoff != null) {
                log.debug("purging soap attachments ...");
                final File soapAttachmentDir = new File(System.getProperty("soap.attachment.dir"));
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
            HibernateUtil.rollbackTransaction();
            log.error("Error while purging jobs", e);
        } 
        finally {
            HibernateUtil.closeCurrentSession();
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
        File webUploadDir = new File(System.getProperty("java.io.tmpdir"));
        purge(webUploadDir, dateCutoff);
    }

    private void purge(final File dir, final long dateCutoff) {
        if (dir != null) {
            log.debug("purging files from directory: "+dir.getPath());
        }
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
    private void purgeUserUploadsAndJobs(final Date now) {
        final List<String> userIds = new ArrayList<String>();

        log.debug("getting userIds from db ...");
        try {
            UserDAO userDao = new UserDAO();
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
            HibernateUtil.closeCurrentSession();
        }
        log.debug("purging data for each user ...");
        
        final ExecutorService exec = Executors.newSingleThreadExecutor();
        for(final String userId : userIds) {
            final Context userContext = Context.getContextForUser(userId);
            final Date cutoffDate=JobPurgerUtil.getCutoffForUser(userContext, now);
            if (cutoffDate != null) {
                try {
                    purgeJobsForUser(userContext, cutoffDate);
                    purgeUserUploadsForUser(exec, userContext, cutoffDate);
                    purgeBatchJobsForUser(userContext, cutoffDate);
                }
                catch (Throwable t) {
                    log.error("Unexpected error purging resources for userId="+userId, t);
                }
            }
        }
        exec.shutdown();
        log.debug("done purging data for each user.");
    }
    
    private void purgeJobsForUser(final Context userContext, final Date cutoffDate) {
        log.debug("purging jobs for user="+userContext.getUserId()+" ...");
        final List<Integer> jobIds=getJobIdsForUser(userContext.getUserId(), cutoffDate);
        for(Integer jobId : jobIds) {
            deleteJob(jobId);
         }
        log.debug("done purging job results.");
    }

    private List<Integer> getJobIdsForUser(final String userId, final Date cutoffDate) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO();
            List<Integer> jobIds = ds.getAnalysisJobIdsForUser(userId, cutoffDate);
            return jobIds;
        }
        catch (Throwable t) {
            log.error("Unexpected error getting list of jobIds for user="+userId, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return Collections.emptyList();
    }

    private boolean deleteJob(final Integer jobId) {
        // delete the job from the database and recursively delete the job directory
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            AnalysisDAO ds = new AnalysisDAO();
            ds.deleteJob(jobId);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            return true;
        }
        catch (Throwable t) {
            log.error("Unexpected error deleting jobId="+jobId, t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return false;        
    }
    
    
    private void purgeUserUploadsForUser(final ExecutorService exec, final Context userContext, final Date cutoffDate) {
        if (cutoffDate==null) {
            log.debug("skipping userId="+userContext.getUserId());
            return;
        }
        log.debug("purging user uploads for userId='"+userContext.getUserId()+"' ...");
        try {
            final UserUploadPurger uup = new UserUploadPurger(exec, userContext, cutoffDate);
            uup.purge();
        }
        catch (Throwable t) {
            log.error(t);
        }
    }
    
    private void purgeBatchJobsForUser(final Context userContext, final Date cutoffDate) {
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
        
        final List<BatchJob> batchJobsToPurge=getBatchJobsToPurge(userContext.getUserId(), cutoffDate);
        for(final BatchJob batchJob : batchJobsToPurge) {
            deleteBatchJob(batchJob);
        }
        log.debug("done purging batch jobs for userId="+userContext.getUserId());
    }

    private List<BatchJob> getBatchJobsToPurge(final String userId, final Date cutoffDate) {
        try {
            final BatchJobDAO batchJobDAO = new BatchJobDAO();
            final List<BatchJob> batchJobsToPurge = batchJobDAO.getOlderThanDateForUser(userId, cutoffDate);
            return batchJobsToPurge;
        }
        catch (Throwable t) {
            log.error("Unexpected error getting batch jobs to purge for userId="+userId, t);
            return Collections.emptyList();
        }
        finally {
            HibernateUtil.closeCurrentSession();
        }
    }
    
    /**
     * delete the batch job entry from the DB if it no longer contains any jobs.
     * @param batchJob
     */
    private void deleteBatchJob(final BatchJob batchJob) {
        if (batchJob==null || batchJob.getBatchJobs()==null || batchJob.getBatchJobs().size() != 0) {
            return;
        }
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().delete(batchJob);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error deleting batch job", t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}

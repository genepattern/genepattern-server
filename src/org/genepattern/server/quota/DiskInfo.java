/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.quota;

import java.util.HashMap;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.genepattern.drm.Memory;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;
import org.genepattern.server.user.User;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.util.MailSender;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;

/**
 * Created by nazaire on 7/10/14.
 */
@XmlRootElement
public class DiskInfo
{
    final static private Logger log = Logger.getLogger(DiskInfo.class);

    private final String userId;
    private Memory diskUsageTotal;
    private Memory diskUsageTmp;
    private Memory diskUsageFilesTab;
    private Memory diskQuota;
    private int numProcessingJobs;
    private int maxSimultaneousJobs;
    private String aboveMaxSimultaneousJobsNotificationEmail;
    private int directExternalUploadTriggerSize;
    private boolean externalDirectDownloadsEnabled;
    
    public boolean isExternalDirectDownloadsEnabled() {
        return externalDirectDownloadsEnabled;
    }

    public void setExternalDirectDownloadsEnabled(boolean externalDirectDownloadsEnabled) {
        this.externalDirectDownloadsEnabled = externalDirectDownloadsEnabled;
    }


    final static public HashMap<String, Long> userNotifications = new HashMap<String, Long>(); 

    public DiskInfo(final String userId) {
        this.userId=userId;
    }
    
    public String getUserId() {
        return userId;
    }

    public int getNumProcessingJobs() {
        return numProcessingJobs;
    }

    public void setNumProcessingJobs(int numProcessingJobs) {
        this.numProcessingJobs = numProcessingJobs;
    }

    public int getMaxSimultaneousJobs() {
        return maxSimultaneousJobs;
    }

    public void setMaxSimultaneousJobs(int maxSimultaneousJobs) {
        this.maxSimultaneousJobs = maxSimultaneousJobs;
    }

    public int getDirectExternalUploadTriggerSize() {
        return directExternalUploadTriggerSize;
    }

    public void setDirectExternalUploadTriggerSize(int directExternalUploadTriggerSize) {
        this.directExternalUploadTriggerSize = directExternalUploadTriggerSize;
    }
    
    public void setDiskUsageTotal(Memory diskUsageTotal)
    {
        this.diskUsageTotal = diskUsageTotal;
    }

    public Memory getDiskUsageTotal() { return diskUsageTotal;}

    public void setDiskUsageTmp(Memory diskUsageTmp) {
        this.diskUsageTmp = diskUsageTmp;
    }

    public Memory getDiskUsageTmp() { return diskUsageTmp;}

    public void setDiskUsageFilesTab(Memory diskUsageFilesTab) {
        this.diskUsageFilesTab = diskUsageFilesTab;
    }

    public Memory getDiskUsageFilesTab() { return diskUsageFilesTab;}

    public Memory getDiskQuota() {
        return diskQuota;
    }

    public void setDiskQuota(Memory diskQuota) {
        this.diskQuota = diskQuota;
    }
    
    public static DiskInfo createDiskInfo(final String userId, final long filesTab_NumBytes) {
        GpConfig gpConfig=ServerConfigurationFactory.instance();
        return createDiskInfo(gpConfig, userId, filesTab_NumBytes);
    }

    public static DiskInfo createDiskInfo(final GpConfig gpConfig, final String userId, final long filesTab_NumBytes) {
        GpContext userContext=GpContext.createContextForUser(userId, false);
        return createDiskInfo(gpConfig, userContext, filesTab_NumBytes);
    }

    public static DiskInfo createDiskInfo(final GpConfig gpConfig, final GpContext userContext, final long filesTab_NumBytes) {
        final DiskInfo diskInfo = new DiskInfo(userContext.getUserId());
        diskInfo.setDiskUsageFilesTab(Memory.fromSizeInBytes(filesTab_NumBytes));
        diskInfo.setDiskQuota(gpConfig.getGPMemoryProperty(userContext, "quota"));
        return diskInfo;
    } 

    /** @deprecated should pass in a Hibernate session */
    public static DiskInfo createDiskInfo(final GpConfig gpConfig, final GpContext gpContext) throws DbException {
        return createDiskInfo(org.genepattern.server.database.HibernateUtil.instance(),
                gpConfig, gpContext);
    }

    public static DiskInfo createDiskInfo(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext context) throws DbException {
        final String userId=context.getUserId();
        final Memory diskQuota=gpConfig.getGPMemoryProperty(context, "quota");
        // default to 100 simultaneous jobs per user
        final int maxSimultaneousJobs = gpConfig.getGPIntegerProperty(context, "max_simultaneous_jobs", 100);
        final int directExternalUploadTriggerSize = gpConfig.getGPIntegerProperty(context, "direct_external_upload_trigger_size", -1);
        final boolean directDownloadEnabled = (gpConfig.getGPProperty(context, "download.aws.s3.downloader.class", null) != null);
        final String maxJobNotificationEmail = gpConfig.getGPProperty(context, "max_simultaneous_jobs_notification_email");
        final DiskInfo diskInfo = new DiskInfo(userId);
        final boolean isInTransaction= mgr.isInTransaction();
        try
        {
            mgr.beginTransaction();
            UserUploadDao userUploadDao = new UserUploadDao(mgr);

            // bug fix, GP-5412, make sure to compute files tab usage before total usage
            Memory diskUsageFilesTab = userUploadDao.sizeOfAllUserUploads(userId, false);
            Memory diskUsageTotal = userUploadDao.sizeOfAllUserUploads(userId, true);

            Memory diskUsageTmp = null;
            if(diskUsageTotal != null && diskUsageFilesTab != null) {
                long diff=diskUsageTotal.getNumBytes() - diskUsageFilesTab.getNumBytes();
                if (diff<0L) {
                    log.error("Invalid diskUsageTmp for userId="+userId+", value="+diff+", setting to 0L");
                    diff=0L;
                }
                diskUsageTmp = Memory.fromSizeInBytes(diff);
            }

            diskInfo.setDiskUsageTotal(diskUsageTotal);
            diskInfo.setDiskUsageFilesTab(diskUsageFilesTab);
            diskInfo.setDiskUsageTmp(diskUsageTmp);
            diskInfo.setDiskQuota(diskQuota);
            
            AnalysisDAO dao = new AnalysisDAO(mgr);
            int numProcessingJobs=dao.getNumProcessingJobsByUser(context.getUserId());
            
            //if (numProcessingJobs > maxSimultaneousJobs){
            //    diskInfo.
            //    
            //}
            diskInfo.setNumProcessingJobs(numProcessingJobs);
            diskInfo.setMaxSimultaneousJobs(maxSimultaneousJobs);
            diskInfo.setDirectExternalUploadTriggerSize(directExternalUploadTriggerSize);
            diskInfo.setAboveMaxSimultaneousJobsNotificationEmail(maxJobNotificationEmail);
            diskInfo.setExternalDirectDownloadsEnabled(directDownloadEnabled);
        }
        catch (Throwable t)
        {
            log.error(t);
            throw new DbException(t);
        }
        finally
        {
            if (!isInTransaction)
            {
                mgr.closeCurrentSession();
            }
        }

        return diskInfo;
    }

    public boolean isAboveQuota()
    {
        return isAboveQuota(0);
    }

    public boolean isAboveQuota(long fileSizeInBytes)
    {
        if(diskQuota == null || diskUsageFilesTab == null)
        {
            return false;
        }

        long diskUsagePlus = diskUsageFilesTab.getNumBytes();

        if(fileSizeInBytes > 0)
        {
            diskUsagePlus += fileSizeInBytes;
        }

        return diskUsagePlus > diskQuota.getNumBytes();
    }
    
    
    public boolean isAboveMaxSimultaneousJobs(){
        return this.numProcessingJobs > this.maxSimultaneousJobs;
    }


    public String getAboveMaxSimultaneousJobsNotificationEmail() {
        return aboveMaxSimultaneousJobsNotificationEmail;
    }

    public void setAboveMaxSimultaneousJobsNotificationEmail(String aboveMaxSimultaneousJobsNotificationEmail) {
        this.aboveMaxSimultaneousJobsNotificationEmail = aboveMaxSimultaneousJobsNotificationEmail;
    }
    
    
    /**
     * Send an email to the designated address if a user has bumped into the throttle limit on this server.  Since this is 
     * normally going to be a result of a script that might keep going, we use a cool down to prevent too many messages being sent 
     * close together, defaulting at 2 minutes.
     * 
     * Return a boolean indicating if an exception should be thrown to prevent the job from queuing up for later
     * or not
     * 
     * @param gpContext
     * @param gpConfig
     */
    
    public boolean notifyMaxJobsExceeded(final GpContext gpContext, final GpConfig gpConfig, final String taskName){
        String username = gpContext.getUserId();
        Boolean throwException = gpConfig.getGPBooleanProperty(gpContext, "max_simultaneous_jobs_exceeded_throw_exception", false);
        Long notificationCoolDown = gpConfig.getGPLongProperty(gpContext, "max_simultaneous_jobs_notification_cooldown", 120000L);
        Long lastNotificationForUser = userNotifications.get(username);
        Long now = System.currentTimeMillis();
        userNotifications.put(username, now);
        
        if ((lastNotificationForUser == null) || ((now - lastNotificationForUser ) > notificationCoolDown)){

        
            if (this.getAboveMaxSimultaneousJobsNotificationEmail() != null) {               
                User badUser = getUser(username);
                final String notificationEmail = this.getAboveMaxSimultaneousJobsNotificationEmail();
                
                String email = "email unavailable";
                try {
                    email = badUser.getEmail();
                } catch (Exception e){
                    // swallow quietly
                }
                
                
                final MailSender m=new MailSender.Builder(gpConfig, gpContext)
                    // set from
                    .from("liefeld@broadinstitute.org")
                    // set to
                    .to(notificationEmail)
                    // set subject
                    .subject("MaxSimultaneousJobs exceeded on server " + gpConfig.getGenePatternURL() + "  by " + gpContext.getUserId())
                    // set message
                    .message("Max Simultaneous jobs (Throttle) exceeded on server " + gpConfig.getGenePatternURL() + "  by user " + username + " ("+ email + ") running " + taskName)
                .build();
                try {
                    m.sendMessage();
             
                }
                catch (Exception e) {
                    // write mail send error to log but don't bother the user about it
                    log.error(e);
                }          
            }
            
        }
        return throwException;
    }
    
    
    private User getUser(final String userId) {
        //this method requires active local DB, with valid users 
        HibernateSessionManager mgr = HibernateUtil.instance();
        final boolean inTransaction=mgr.isInTransaction();
        try {
            UserDAO dao=new UserDAO(mgr);
            User user=dao.findById(userId);
            return user;
        }
        catch (Throwable t) {
            log.error("Error getting User instance for userId="+userId, t);
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
        
        return null;
    }
 
    
}

/*******************************************************************************
 * Copyright (c) 2003-2018 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.jsf;

import org.genepattern.server.quota.DiskInfo;

/**
 * JSF bean for displaying DiskInfo on the admin page.
 * @author pcarr
 *
 */
public class DiskInfoBean {
    /** display value when the quota is not set. */
    public static final String QUOTA_NOT_SET="  -  ";
    /** display value when the disk usage is not known. */
    public static final String DISK_USAGE_UNKNOWN="  ?  ";
    
    private final String userId;
    private final String diskUsage;
    private final String quota;
    private final boolean isAboveQuota;
    private final boolean isAboveMaxSimultaneousJobs;
    private final String aboveMaxSimultaneousJobsNotificationEmail;
    
    public DiskInfoBean(final DiskInfo diskInfo) {
        this.userId=diskInfo.getUserId();
        if (diskInfo.getDiskUsageFilesTab() == null) {
            this.diskUsage=DISK_USAGE_UNKNOWN;
        }
        else {
            this.diskUsage=diskInfo.getDiskUsageFilesTab().getDisplayValue();
        }
        if (diskInfo.getDiskQuota() == null) {
            this.quota=QUOTA_NOT_SET;
        }
        else {
            this.quota=diskInfo.getDiskQuota().getDisplayValue();
        }
        this.isAboveQuota=diskInfo.isAboveQuota();
        this.isAboveMaxSimultaneousJobs=diskInfo.isAboveMaxSimultaneousJobs();
        this.aboveMaxSimultaneousJobsNotificationEmail=diskInfo.getAboveMaxSimultaneousJobsNotificationEmail();
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getDiskUsage() {
        return diskUsage;
    }
    
    public String getQuota() {
        return quota;
    }
    
    public boolean isAboveQuota() {
        return isAboveQuota;
    }
    
    public boolean isAboveMaxSimultaneousJobs(){
        return isAboveMaxSimultaneousJobs;
    }
    public String aboveMaxSimultaneousJobsNotificationEmail(){
        return aboveMaxSimultaneousJobsNotificationEmail;
    }
}

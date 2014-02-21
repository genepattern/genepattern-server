package org.genepattern.server.config;

import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;

public class GpContext {
    //hard-coded default value is true for compatibility with GP 3.2.4 and earlier
    private boolean checkSystemProperties = true;
    //hard-coded default value is true for compatibility with GP 3.2.4 and earlier
    private boolean checkPropertiesFiles = true;
    private String userId = null;
    private TaskInfo taskInfo = null;
    private JobInfo jobInfo = null;
    private boolean isAdmin=false;

    public static GpContext getServerContext() {
        GpContext context = new GpContext();
        return context;
    }

    public static GpContext getContextForUser(final String userId) {
        return getContextForUser(userId, false);
    }

    public static GpContext getContextForUser(final String userId, final boolean initIsAdmin) {
        if (userId==null) {
            return new GpContext();
        }
        GpContext context = new GpContext();
        context.setUserId(userId);
        if (initIsAdmin) { 
            final boolean isAdmin = AuthorizationHelper.adminServer(userId);
            context.setIsAdmin(isAdmin);
        }
        return context;
    }

    public static GpContext getContextForJob(JobInfo jobInfo) {
        GpContext context = new GpContext();
        if (jobInfo != null) {
            context.setJobInfo(jobInfo);
            if (jobInfo.getUserId() != null) {
                context.setUserId(jobInfo.getUserId());
            }
        }
        return context;
    }

    public static GpContext getContextForJob(JobInfo jobInfo, TaskInfo taskInfo) {
        GpContext context = getContextForJob(jobInfo);
        if (taskInfo != null) {
            context.setTaskInfo(taskInfo);
        }
        return context;
    }

    public void setCheckSystemProperties(boolean b) {
        this.checkSystemProperties = b;
    }

    public boolean getCheckSystemProperties() {
        return checkSystemProperties;
    }

    public void setCheckPropertiesFiles(boolean b) {
        this.checkPropertiesFiles = b;
    }

    public boolean getCheckPropertiesFiles() {
        return checkPropertiesFiles;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    public String getUserId() {
        return userId;
    }

    public void setTaskInfo(TaskInfo taskInfo) {
        this.taskInfo = taskInfo;
    }
    public TaskInfo getTaskInfo() {
        return this.taskInfo;
    } 

    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }
    public JobInfo getJobInfo() {
        return jobInfo;
    }

    public void setIsAdmin(final boolean b) {
        this.isAdmin=b;
    }
    public boolean isAdmin() {
        return isAdmin;
    }

    public String getLsid() {
        if (taskInfo != null) {
            return taskInfo.getLsid();
        }
        if (jobInfo != null) {
            return jobInfo.getTaskLSID();
        }
        return null;
    }
}

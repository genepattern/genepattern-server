package org.genepattern.server.config;

import java.io.File;

import org.genepattern.server.JobPermissions;
import org.genepattern.server.job.input.JobInput;
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
    private File taskLibDir = null;  // aka installation dir, the directory to which the task was installed
    private JobInfo jobInfo = null;
    private Integer jobNumber = null;
    private JobInput jobInput = null;
    private boolean isAdmin=false;
    private JobPermissions jobPermissions=null;

    /**
     * TODO: @deprecated
     * @return
     */
    public static GpContext getServerContext() {
        GpContext context = new GpContext();
        return context;
    }

    /**
     * @deprecated
     * @param userId
     * @return
     */
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
            context.jobNumber=jobInfo.getJobNumber();
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

    /**
     * @deprecated
     */
    public GpContext() {
    }

    void setCheckSystemProperties(boolean b) {
        this.checkSystemProperties = b;
    }

    public boolean getCheckSystemProperties() {
        return checkSystemProperties;
    }

    void setCheckPropertiesFiles(boolean b) {
        this.checkPropertiesFiles = b;
    }

    public boolean getCheckPropertiesFiles() {
        return checkPropertiesFiles;
    }

    void setUserId(String userId) {
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
    
    public void setTaskLibDir(final File taskLibDir) {
        this.taskLibDir=taskLibDir;
    }
    /**
     * Get the '<libdir>' for the current TaskInfo, can be null if it
     * has not been initialized for this particular context.
     * @return
     */
    public File getTaskLibDir() {
        return this.taskLibDir;
    }

    void setJobNumber(final Integer jobNumber) {
        this.jobNumber=jobNumber;
    }
    
    /**
     * can return null if not set.
     * @return
     */
    public Integer getJobNumber() {
        if (jobInfo!=null) {
            return jobInfo.getJobNumber();
        }
        return jobNumber;
    }

    void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }
    public JobInfo getJobInfo() {
        return jobInfo;
    }
    
    void setJobInput(final JobInput jobInput) {
        this.jobInput=jobInput;
    }
    public JobInput getJobInput() {
        return this.jobInput;
    }

    void setIsAdmin(final boolean b) {
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

    void setJobPermissions(final JobPermissions jobPermissions) {
        this.jobPermissions=jobPermissions;
    }
    public JobPermissions getJobPermissions() {
        return this.jobPermissions;
    }
    
    public boolean canReadJob() {
        if (jobPermissions==null) {
            return false;
        }
        return jobPermissions.canReadJob();
    }
    
    public boolean canWriteJob() {
        if (jobPermissions==null) {
            return false;
        }
        return jobPermissions.canWriteJob();
    }

}

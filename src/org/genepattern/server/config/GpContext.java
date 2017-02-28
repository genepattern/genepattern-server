/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import java.io.File;

import org.apache.log4j.Logger;
import org.genepattern.server.JobPermissions;
import org.genepattern.server.JobPermissionsFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.eula.LibdirLegacy;
import org.genepattern.server.eula.LibdirStrategy;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.dao.JobInputValueRecorder;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

public class GpContext {
    private static final Logger log = Logger.getLogger(GpContext.class);

    private String userId = null;
    private TaskInfo taskInfo = null;
    private File taskLibDir = null;  // aka installation dir, the directory to which the task was installed
    private JobInfo jobInfo = null;
    private Integer jobNumber = null;
    private JobInput jobInput = null;
    private boolean isAdmin=false;
    private JobPermissions jobPermissions=null;

    public static GpContext createContextForUser(final String userId) {
        return createContextForUser(userId, false);
    }

    public static GpContext createContextForUser(final String userId, final boolean initIsAdmin) {
        if (userId==null) {
            return new Builder()
                .build();
        } 
        Builder builder=new Builder()
            .userId(userId);
        if (initIsAdmin) { 
            final boolean isAdmin = AuthorizationHelper.adminServer(userId);
            builder = builder.isAdmin(isAdmin);
        }
        return builder.build();
    }

    /** @deprecated should pass in a valid HibernateSessionManager */
    public static GpContext createContextForJob(final Integer jobNumber) throws Exception, Throwable {
        return createContextForJob(org.genepattern.server.database.HibernateUtil.instance(), jobNumber);
    }
    
    public static GpContext createContextForJob(final HibernateSessionManager mgr, final Integer jobNumber) throws Exception, Throwable {
        //null arg for currentUser means 'use the owner of the job'
        return createContextForJob(mgr, null, jobNumber);
    }
    public static GpContext createContextForJob(final HibernateSessionManager mgr, final String currentUser, final Integer jobNumber) throws Exception, Throwable {
        LibdirStrategy libdirStrategy=new LibdirLegacy();
        return createContextForJob(mgr, currentUser, jobNumber, libdirStrategy);
    }
    public static GpContext createContextForJob(final HibernateSessionManager mgr, final String currentUser, final Integer jobNumber, final LibdirStrategy libdirStrategy) throws Exception, Throwable {
        if (jobNumber==null) {
            throw new IllegalArgumentException("jobNumber==null");
        }
        if (libdirStrategy==null) {
            throw new IllegalArgumentException("libdirStrategy==null");
        }
        final boolean initFromDb=true;
        final boolean isInTransaction=mgr.isInTransaction();
        final JobInfo jobInfo;
        final JobInput jobInput;
        final TaskInfo taskInfo;
        final String taskName;
        final File taskLibDir;
        try {
            AnalysisDAO dao = new AnalysisDAO(mgr);
            jobInfo = dao.getJobInfo(jobNumber);
            jobInput = new JobInputValueRecorder(mgr).fetchJobInput(jobNumber);
            jobInput.setLsid(jobInfo.getTaskLSID());
            taskInfo=TaskInfoCache.instance().getTask(mgr, jobInfo.getTaskLSID());
            taskName=taskInfo.getName();
            if (log.isDebugEnabled()) {
                log.debug("taskName=" + taskName);
            }
            taskLibDir=libdirStrategy.getLibdir(taskInfo.getLsid());
            return createContextForJob(currentUser, jobInfo, taskInfo, taskLibDir, jobInput, initFromDb);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    private static GpContext createContextForJob(final String currentUser, final JobInfo jobInfo, final TaskInfo taskInfo, final File taskLibDir, final JobInput jobInput, final boolean initFromDb) {        
        Builder builder=new Builder();
        if (jobInfo != null) {
            builder=builder.jobInfo(jobInfo);
        }
        if (taskInfo != null) {
            builder=builder.taskInfo(taskInfo);
        }
        if (taskLibDir != null) {
            builder.taskLibDir(taskLibDir);
        }
        if (jobInput != null) {
            builder=builder.jobInput(jobInput);
        }

        final String userId;
        if (currentUser==null && jobInfo!=null) {
            userId=jobInfo.getUserId();
        }
        else {
            userId=currentUser;
        }
        builder.userId(userId);
        if (initFromDb) {
            if (jobInfo != null && jobInfo.getUserId() != null) {
                final boolean isAdmin = AuthorizationHelper.adminServer(userId);
                builder = builder.isAdmin(isAdmin);
                JobPermissions jobPermissions=JobPermissionsFactory.createJobPermissionsFromDb(isAdmin, userId, jobInfo.getJobNumber());
                builder.jobPermissions(jobPermissions);
            }
        }
        
        return builder.build();
    }
    
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

    public static GpContext getContextForTask(String lsid) {
        TaskInfo taskInfo = TaskInfoCache.instance().getTask(lsid);
        return getContextForTask(taskInfo);
    }

    public static GpContext getContextForTask(TaskInfo taskInfo) {
        GpContext context = new GpContext();
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

//    void setCheckSystemProperties(boolean b) {
//        this.checkSystemProperties = b;
//    }
//
//    public boolean getCheckSystemProperties() {
//        return checkSystemProperties;
//    }

//    void setCheckPropertiesFiles(boolean b) {
//        this.checkPropertiesFiles = b;
//    }
//
//    public boolean getCheckPropertiesFiles() {
//        return checkPropertiesFiles;
//    }

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

    public String getTaskName() {
        if (taskInfo != null) {
            return taskInfo.getName();
        }
        if (jobInfo != null) {
            return jobInfo.getTaskName();
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
    
    /**
     * Helper method to get the baseGpHref associated with a job or web request,
     * Currently only implemented for a job context initialized with a jobinput field.
     * @return the value or null if not set, e.g. 'http://127.0.0.1:8080/gp'
     */
    public String getBaseGpHref() {
        if (this.jobInput != null) {
            return this.jobInput.getBaseGpHref();
        }
        return null;
    } 
    
    // Builder pattern
    public static final class Builder {
        private String userId=null;
        private boolean isAdmin=false;
        private Integer jobNumber=null;
        private JobInfo jobInfo=null;
        private TaskInfo taskInfo=null;
        private File taskLibDir=null;
        private JobInput jobInput=null;
        private JobPermissions jobPermissions=null;

        public Builder userId(final String userId) {
            this.userId=userId;
            return this;
        }
        public Builder isAdmin(final boolean isAdmin) {
            this.isAdmin=isAdmin;
            return this;
        }
        public Builder jobNumber(final Integer jobNumber) {
            this.jobNumber=jobNumber;
            return this;
        }
        public Builder jobInfo(final JobInfo jobInfo) {
            this.jobInfo=jobInfo;
            return this;
        }
        public Builder taskInfo(final TaskInfo taskInfo) {
            this.taskInfo=taskInfo;
            return this;
        }
        public Builder taskLibDir(final File taskLibDir) {
            this.taskLibDir=taskLibDir;
            return this;
        }
        public Builder jobInput(final JobInput jobInput) {
            this.jobInput=jobInput;
            return this;
        }
        public Builder jobPermissions(final JobPermissions jobPermissions) {
            this.jobPermissions=jobPermissions;
            return this;
        }

        public GpContext build() {
            GpContext gpContext=new GpContext();
            if (userId!=null) {
                gpContext.setUserId(userId);
            }
            gpContext.setIsAdmin(isAdmin);
            if (jobNumber != null) {
                gpContext.setJobNumber(jobNumber);
            }
            if (jobInfo!=null) {
                gpContext.setJobInfo(jobInfo);
            }
            if (taskInfo!=null) {
                gpContext.setTaskInfo(taskInfo);
            }
            if (taskLibDir!=null) {
                gpContext.setTaskLibDir(taskLibDir);
            }
            if (jobInput!=null) {
                gpContext.setJobInput(jobInput);
            }
            if (jobPermissions!=null) {
                gpContext.setJobPermissions(jobPermissions);
            }
            return gpContext;
        }
    }


}

package org.genepattern.server.config;

import java.io.File;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.LibdirLegacy;
import org.genepattern.server.eula.LibdirStrategy;
import org.genepattern.server.JobPermissions;
import org.genepattern.server.JobPermissionsFactory;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.dao.JobInputValueRecorder;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;


/**
 * Factory methods for creating GpContext instances.
 * @author pcarr
 *
 */
public class GpContextFactory {
    private static final Logger log = Logger.getLogger(GpContextFactory.class);
    
    public GpContext createServerContext() {
        return new Builder()
        .build();
    }

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
    
    public static GpContext createContextForJob(final Integer jobNumber) throws Exception, Throwable {
        //null arg for currentUser means 'use the owner of the job'
        return createContextForJob(null, jobNumber);
    }
    public static GpContext createContextForJob(final String currentUser, final Integer jobNumber) throws Exception, Throwable {
        LibdirStrategy libdirStrategy=new LibdirLegacy();
        return createContextForJob(currentUser, jobNumber, libdirStrategy);
    }
    public static GpContext createContextForJob(final String currentUser, final Integer jobNumber, final LibdirStrategy libdirStrategy) throws Exception, Throwable {
        if (jobNumber==null) {
            throw new IllegalArgumentException("jobNumber==null");
        }
        if (libdirStrategy==null) {
            throw new IllegalArgumentException("libdirStrategy==null");
        }
        final boolean initFromDb=true;
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        final JobInfo jobInfo;
        final JobInput jobInput;
        final TaskInfo taskInfo;
        final String taskName;
        final File taskLibDir;
        try {
            AnalysisDAO dao = new AnalysisDAO();
            jobInfo = dao.getJobInfo(jobNumber);
            jobInput = new JobInputValueRecorder().fetchJobInput(jobNumber);
            taskInfo=TaskInfoCache.instance().getTask(jobInfo.getTaskLSID());
            taskName=taskInfo.getName();
            if (log.isDebugEnabled()) {
                log.debug("taskName=" + taskName);
            }
            taskLibDir=libdirStrategy.getLibdir(taskInfo.getLsid());
            return GpContextFactory.createContextForJob(currentUser, jobInfo, taskInfo, taskLibDir, jobInput, initFromDb);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
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

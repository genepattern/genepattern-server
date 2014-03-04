package org.genepattern.server.config;

import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Factory methods for creating GpContext instances.
 * @author pcarr
 *
 */
public class GpContextFactory {
    
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
    
    public static GpContext createContextForJob(final JobInfo jobInfo) {
        TaskInfo taskInfo=null;
        return createContextForJob(jobInfo, taskInfo);
    }

    public static GpContext createContextForJob(final JobInfo jobInfo, final TaskInfo taskInfo) {
        Builder builder=new Builder();
        if (jobInfo != null) {
            builder=builder.jobInfo(jobInfo);
            if (jobInfo.getUserId() != null) {
                builder=builder.userId(jobInfo.getUserId());
            }
        }
        if (taskInfo != null) {
            builder=builder.taskInfo(taskInfo);
        }
        return builder.build();
    }
    
    public static GpContext createContextForJob(final JobInfo jobInfo, final TaskInfo taskInfo, final JobInput jobInput) {
        Builder builder=new Builder();
        if (jobInfo != null) {
            builder=builder.jobInfo(jobInfo);
            if (jobInfo.getUserId() != null) {
                builder=builder.userId(jobInfo.getUserId());
            }
        }
        if (taskInfo != null) {
            builder=builder.taskInfo(taskInfo);
        }
        if (jobInput != null) {
            builder=builder.jobInput(jobInput);
        }
        return builder.build();
    }

    public static final class Builder {
        private String userId=null;
        private boolean isAdmin=false;
        private JobInfo jobInfo=null;
        private TaskInfo taskInfo=null;
        private JobInput jobInput=null;

        public Builder() {
        }
        public Builder userId(final String userId) {
            this.userId=userId;
            return this;
        }
        public Builder isAdmin(final boolean isAdmin) {
            this.isAdmin=isAdmin;
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
        public Builder jobInput(final JobInput jobInput) {
            this.jobInput=jobInput;
            return this;
        }

        public GpContext build() {
            GpContext gpContext=new GpContext();
            if (userId!=null) {
                gpContext.setUserId(userId);
            }
            gpContext.setIsAdmin(isAdmin);
            if (jobInfo!=null) {
                gpContext.setJobInfo(jobInfo);
            }
            if (taskInfo!=null) {
                gpContext.setTaskInfo(taskInfo);
            }
            if (jobInput!=null) {
                gpContext.setJobInput(jobInput);
            }
            return gpContext;
        }
    }

}

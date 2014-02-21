package org.genepattern.server.job;

import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

public class JobInfoLoaderDefault implements JobInfoLoader {

    /**
     * Get a JobInfo from the DB, for the given jobId.
     * (Legacy code copied from the RunTaskBean#setTask method).
     * 
     * @param userContext, Must be non-null with a valid userId
     * @param jobId, Must be non-null
     * 
     * @return
     * 
     * @throws Exception for the following,
     *     1) if there is no job with jobId in the DB
     *     2) if the current user does not have permission to 'read' the job
     */
    @Override
    public JobInfo getJobInfo(GpContext userContext, String jobId) throws Exception {
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        if (userContext.getUserId()==null || userContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userContext.userId is not set");
        }
        if (jobId==null) {
            throw new IllegalArgumentException("jobId==null");
        }
        final int jobNumber;
        try {
            jobNumber=Integer.parseInt(jobId);
        }
        catch (Throwable t) {
            throw new Exception("Error parsing jobId="+jobId, t);
        }
        JobInfo jobInfo = new AnalysisDAO().getJobInfo(jobNumber);
        if (jobInfo==null) {
            throw new Exception("Can't load job, jobId="+jobId);
        }

        // check permissions
        final boolean isAdmin = AuthorizationHelper.adminJobs(userContext.getUserId());
        PermissionsHelper perm = new PermissionsHelper(isAdmin, userContext.getUserId(), jobNumber);
        if (!perm.canReadJob()) {
            throw new Exception("User does not have permission to load job");
        }
        return jobInfo;
    }


}

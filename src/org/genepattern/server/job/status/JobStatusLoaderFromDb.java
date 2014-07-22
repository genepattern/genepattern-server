package org.genepattern.server.job.status;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.executor.drm.DbLookup;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.webapp.rest.api.v1.job.JobsResource;

public class JobStatusLoaderFromDb implements JobStatusLoader {
    private final String jobsResourceHref;  // e.g. http://127.0.0.1:8080/gp/rest/v1/jobs/
    
    public JobStatusLoaderFromDb() {
        this("http://127.0.0.1:8080/gp/rest/"+JobsResource.URI_PATH);
    }
    
    public JobStatusLoaderFromDb(final String jobsResourceHref) {
        this.jobsResourceHref=jobsResourceHref;
    }

    @Override
    public Status loadJobStatus(GpContext jobContext) {
        int gpJobNo=jobContext.getJobNumber();
        JobRunnerJob jobStatusRecord=DbLookup.selectJobRunnerJob(gpJobNo);
        String stderrLocation=null;
        String executionLogLocation=null;
        
        String jobHref=jobsResourceHref+""+gpJobNo;

        Status status=new Status.Builder()
            .jobHref(jobHref)
            .jobInfo(jobContext.getJobInfo())
            .jobStatusRecord(jobStatusRecord)
            .stderrLocation(stderrLocation)
            .executionLogLocation(executionLogLocation)
        .build();
        return status;
    }

}

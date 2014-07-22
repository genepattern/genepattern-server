package org.genepattern.server.job.status;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.job.output.JobOutputFile;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.genepattern.server.webapp.rest.api.v1.job.JobsResource;

/**
 * Initialize the Status object by making DB queries of the newer tables:
 *     job_runner_job and job_output
 * @author pcarr
 *
 */
public class JobStatusLoaderFromDb implements JobStatusLoader {
    private static final Logger log = Logger.getLogger(JobStatusLoaderFromDb.class);

    private final String gpUrl;
    private JobRunnerJobDao jobRunnerJobDao;
    private JobOutputDao jobOutputDao;
    
    /**
     * Create default instance.
     * @param gpUrl, requires a valid gpUrl to initialize links to log files
     *     e.g.  "http://127.0.0.1:8080/gp/"
     */
    public JobStatusLoaderFromDb(final String gpUrl) {
        this(gpUrl, new JobRunnerJobDao(), new JobOutputDao());
    }

    public JobStatusLoaderFromDb(final String gpUrl, final JobRunnerJobDao jobRunnerJobDao, final JobOutputDao jobOutputDao) {
        this.gpUrl=gpUrl;
        this.jobRunnerJobDao=jobRunnerJobDao;
        this.jobOutputDao=jobOutputDao;
    }

    @Override
    public Status loadJobStatus(GpContext jobContext) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        int gpJobNo=jobContext.getJobNumber();
        try {
            JobRunnerJob jobStatusRecord=jobRunnerJobDao.selectJobRunnerJob(gpJobNo);
            String executionLogLocation=null;
            List<JobOutputFile> executionLogs=jobOutputDao.selectGpExecutionLogs(gpJobNo);
            if (executionLogs != null && executionLogs.size()>0) {
                executionLogLocation=executionLogs.get(0).getHref(gpUrl);
            }
            String stderrLocation=null;
            List<JobOutputFile> stderrFiles=jobOutputDao.selectStderrFiles(gpJobNo);
            if (stderrFiles != null && stderrFiles.size()>0) {
                stderrLocation=stderrFiles.get(0).getHref(gpUrl);
            }
            String jobHref=initJobHref(gpJobNo);

            Status status=new Status.Builder()
                .jobHref(jobHref)
                .jobInfo(jobContext.getJobInfo())
                .jobStatusRecord(jobStatusRecord)
                .stderrLocation(stderrLocation)
                .executionLogLocation(executionLogLocation)
            .build();
            return status;
        }
        catch (DbException e) {
            log.error("", e);
            return null;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    // http://gpdev.broadinstitute.org/gp/rest/v1/jobs/66724
    public String initJobHref(final int gpJobNo) {
        StringBuilder sb=new StringBuilder();
        sb.append(gpUrl);
        if (!gpUrl.endsWith("/")) {
            sb.append("/");
        }
        sb.append("rest/v1/jobs/");
        sb.append(gpJobNo);
        return sb.toString();
    }

}

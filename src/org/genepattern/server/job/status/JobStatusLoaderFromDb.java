/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.status;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.drm.dao.JobRunnerJobDao;
import org.genepattern.server.job.output.JobOutputFile;
import org.genepattern.server.job.output.dao.JobOutputDao;

/**
 * Initialize the Status object by making DB queries of the newer tables:
 *     job_runner_job and job_output
 * @author pcarr
 *
 */
public class JobStatusLoaderFromDb implements JobStatusLoader {
    private static final Logger log = Logger.getLogger(JobStatusLoaderFromDb.class);

    private final HibernateSessionManager mgr;
    private final String gpUrl;
    private final JobRunnerJobDao jobRunnerJobDao;
    private final JobOutputDao jobOutputDao;
    
    /** @deprecated */
    public JobStatusLoaderFromDb(final String gpUrl) {
        this(org.genepattern.server.database.HibernateUtil.instance(), gpUrl);
    }
    
    /**
     * Create default instance.
     * @param gpUrl, requires a valid gpUrl to initialize links to log files
     *     e.g.  "http://127.0.0.1:8080/gp/"
     */
    public JobStatusLoaderFromDb(final HibernateSessionManager mgr,final String gpUrl) {
        this(mgr, gpUrl, new JobRunnerJobDao(), new JobOutputDao(mgr));
    }

    /** @deprecated */
    public JobStatusLoaderFromDb(final String gpUrl, final JobRunnerJobDao jobRunnerJobDao, final JobOutputDao jobOutputDao) {
        this(org.genepattern.server.database.HibernateUtil.instance(), gpUrl, jobRunnerJobDao, jobOutputDao);
    }

    public JobStatusLoaderFromDb(final HibernateSessionManager mgr, final String gpUrl, final JobRunnerJobDao jobRunnerJobDao, final JobOutputDao jobOutputDao) {
        this.mgr=mgr;
        this.gpUrl=gpUrl;
        this.jobRunnerJobDao=jobRunnerJobDao;
        this.jobOutputDao=jobOutputDao;
    }

    @Override
    public Status loadJobStatus(GpContext jobContext) {
        final int gpJobNo=jobContext.getJobNumber();
        final String jobHref=initJobHref(gpJobNo);
        JobRunnerJob jobStatusRecord=null;
        String executionLogLocation=null;
        String stderrLocation=null;

        final boolean isInTransaction=mgr.isInTransaction();
        try {
            try {
                jobStatusRecord=jobRunnerJobDao.selectJobRunnerJob(mgr, gpJobNo);
            }
            catch (DbException e) {
                // error logged in calling method
            }
            try { 
                List<JobOutputFile> executionLogs=jobOutputDao.selectGpExecutionLogs(gpJobNo);
                if (executionLogs != null && executionLogs.size()>0) {
                    executionLogLocation=executionLogs.get(0).getHref(gpUrl);
                }
                List<JobOutputFile> stderrFiles=jobOutputDao.selectStderrFiles(gpJobNo);
                if (stderrFiles != null && stderrFiles.size()>0) {
                    stderrLocation=stderrFiles.get(0).getHref(gpUrl);
                }
            }
            catch (DbException e) {
                // error logged in calling method
                if (jobContext.getJobInfo() != null) {
                    // TODO initialize from jobInfo
                }
            }

            final Status.Builder b=new Status.Builder();
            b.jobHref(jobHref);
            b.jobInfo(jobContext.getJobInfo());
            if (jobStatusRecord != null) {
                b.jobStatusRecord(jobStatusRecord);
            }
            if (stderrLocation != null) {
                b.stderrLocation(stderrLocation);
            }
            if (executionLogLocation != null) {
                b.executionLogLocation(executionLogLocation);
            }
            final Status status=b.build();
            return status;
        }
        catch (Throwable t) {
            log.error("Unexpected exception in loadJobStatus for gpJobNo="+gpJobNo, t);
            return null;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
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

package org.genepattern.server.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.jobqueue.JobQueueUtil;
import org.genepattern.webservice.JobInfo;

/**
 * Helper class used by the AnalysisJobScheduler, this replaces the ProcessingJobsHandler class.
 * The run() method starts a single GP job, based on the given jobId.
 * It is up to the calling method to poll the DB for new pending jobs.
 * @author pcarr
 *
 */
public class JobSubmitter implements Runnable {
    private static final Logger log = Logger.getLogger(JobSubmitter.class);

    private final GenePatternAnalysisTask genePattern;
    private final Integer jobId;
    
    public JobSubmitter(final GenePatternAnalysisTask genePattern, final Integer jobId) {
        if (genePattern==null) {
            throw new IllegalArgumentException("job #"+jobId+" not run because genePattern instance is null!");
        }
        this.genePattern=genePattern;
        this.jobId=jobId;
    }
    
    public Integer getJobId() {
        return jobId;
    }
    
    @Override
    public void run() {
        JobInfo jobInfo=null;
        try {
            jobInfo=FileDownloader.initJobInfo(jobId);
        }
        catch (Throwable t) {
            //log below
            log.debug(t);
        }
        if (jobInfo == null || AnalysisJobScheduler.isFinished(jobInfo)) {
            //special-case, no record of the job the the DB, most likely because it was deleted
            if (jobInfo==null) {
                log.info("No record of job in the ANALYSIS_JOB table, job_no="+jobId+", removing from JOB_QUEUE");
            }
            //special-case, job has already completed
            else if (AnalysisJobScheduler.isFinished(jobInfo)) {
                log.info("job already completed, jobId="+jobId+", removing from JOB_QUEUE");
            }
        
            try {
                JobQueueUtil.deleteJobQueueStatusRecord(jobId);
            }
            catch (Exception ex) {
                log.error("Error removing record from JOB_QUEUE for job_no="+jobId, ex);
            }
            return;
        }

        boolean interrupted=false;
        try {
            log.debug("submitting job "+jobId);
            startDownloadAndWait(jobInfo);
            log.debug("calling genePattern.onJob("+jobId+")");
            genePattern.onJob(jobId);
        }
        catch (JobDispatchException e) {
            handleJobDispatchException(jobId, e);
        }
        catch (ExecutionException e) {
            handleJobDispatchException(jobId, e);
        }
        catch (InterruptedException e) {
            interrupted=true;
            Thread.currentThread().interrupt();
        }
        catch (Throwable t) {
            log.error("Unexpected error thrown by GenePatternAnalysisTask.onJob: "+t.getLocalizedMessage());
            handleJobDispatchException(jobId, t);
        }
        finally {
            if (!interrupted) {
                try {
                    JobQueueUtil.deleteJobQueueStatusRecord(jobId);
                }
                catch (Exception ex) {
                    log.error("Error removing record from JOB_QUEUE for job_no="+jobId, ex);
                }
            }
            else {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void startDownloadAndWait(final JobInfo jobInfo) throws JobDispatchException, ExecutionException, InterruptedException {
        FileDownloader downloader=FileDownloader.fromJobInfo(jobInfo);
        if (!downloader.hasSelectedChoices()) {
            log.debug("No selected choices");
            return;
        }
        log.debug("downloading files for jobId="+jobId+" ...");
        downloader.startDownloadAndWait();
    }

    //handle errors during job dispatch (moved from GPAT.onJob)
    private void handleJobDispatchException(int jobId, Throwable t) {
        if (t.getCause() != null) {
          t = t.getCause();
        }
        log.error("Error submitting job #"+jobId, t);
        try {
            String errorMessage = "GenePattern Server error preparing job "+jobId+" for execution.\n"+t.getMessage() + "\n\n";
            errorMessage += stackTraceToString(t);
            GenePatternAnalysisTask.handleJobCompletion(jobId, -1, errorMessage);
        }
        catch (Throwable t1) {
            log.error("Error handling job completion for job #"+jobId, t1);
        }
    }
    
    private static String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

}
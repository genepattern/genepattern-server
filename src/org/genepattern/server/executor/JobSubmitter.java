package org.genepattern.server.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.jobqueue.JobQueueUtil;

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
    private final FileDownloader downloader;
    
    public JobSubmitter(final GenePatternAnalysisTask genePattern, final Integer jobId) throws JobDispatchException {
        if (genePattern==null) {
            throw new IllegalArgumentException("job #"+jobId+" not run because genePattern instance is null!");
        }
        this.genePattern=genePattern;
        this.jobId=jobId;
        this.downloader=FileDownloader.fromJobId(jobId);
    }
    
    public boolean hasSelectedChoices() {
        return downloader != null && downloader.hasSelectedChoices();
    }
    
    @Override
    public void run() {
        try {
            log.debug("submitting job "+jobId);
            startDownloadAndWait();
            log.debug("calling genePattern.onJob("+jobId+")");
            genePattern.onJob(jobId);
            JobQueueUtil.deleteJobQueueStatusRecord(jobId);
        }
        catch (JobDispatchException e) {
            handleJobDispatchException(jobId, e);
        }
        catch (ExecutionException e) {
            handleJobDispatchException(jobId, e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (Throwable t) {
            log.error("Unexpected error thrown by GenePatternAnalysisTask.onJob: "+t.getLocalizedMessage());
            handleJobDispatchException(jobId, t);
        }
    }
    
    private void startDownloadAndWait() throws JobDispatchException, ExecutionException, InterruptedException {
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
package org.genepattern.server.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;

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
        this.genePattern=genePattern;
        this.jobId=jobId;
    }
    
    @Override
    public void run() {
        if (genePattern == null) {
            log.error("job not run, genePattern == null!");
            return;
        }
        try {
            startDownloadAndWait();
            genePattern.onJob(jobId);
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
        final FileDownloader downloader=new FileDownloader(jobId);
        if (!downloader.hasSelectedChoices()) {
            log.debug("No selected choices");
            return;
        }
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
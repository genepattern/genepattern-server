package org.genepattern.server.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;

/**
 * Helper class used by the AnalysisJobScheduler.
 * 
 * Wait for new pending jobs (by taking job ids from the pendingJobQueue)
 * and start them by calling {@link GenePatternAnalysisTask#onJob(Integer)}.
 * 
 * This is the default implementation circa GP <= 3.6.1, before adding a new
 * method for downloading cached data files from external url before starting the jobs.
 * 
 * @author pcarr
 *
 */
public class ProcessingJobsHandler implements Runnable {
    private static final Logger log = Logger.getLogger(ProcessingJobsHandler.class);

    private ExecutorService jobSubmissionService;
    private final BlockingQueue<Integer> pendingJobQueue;
    private final GenePatternAnalysisTask genePattern;
    
    public ProcessingJobsHandler(BlockingQueue<Integer> pendingJobQueue) {
        this.pendingJobQueue = pendingJobQueue;
        this.jobSubmissionService = Executors.newSingleThreadExecutor();
        this.genePattern = new GenePatternAnalysisTask(jobSubmissionService);
    }
    
    public void run() {
        try {
            while (true) {
                Integer jobId = pendingJobQueue.take();
                submitJob(jobId);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (jobSubmissionService != null) {
            jobSubmissionService.shutdown();
            try {
                if (!jobSubmissionService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("jobSubmissionService shutdown timed out after 30 seconds.");
                    jobSubmissionService.shutdownNow();
                }
            }
            catch (final InterruptedException e) {
                log.error("jobSubmissionService executor.shutdown was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void submitJob(Integer jobId) {
        if (genePattern == null) {
            log.error("job not run, genePattern == null!");
            return;
        }
        try {
            genePattern.onJob(jobId);
        }
        catch (JobDispatchException e) {
            handleJobDispatchException(jobId, e);
        }
        catch (Throwable t) {
            log.error("Unexpected error thrown by GenePatternAnalysisTask.onJob: "+t.getLocalizedMessage());
            handleJobDispatchException(jobId, t);
        }
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
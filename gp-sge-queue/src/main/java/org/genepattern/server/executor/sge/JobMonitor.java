package org.genepattern.server.executor.sge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.broadinstitute.zamboni.server.batchsystem.BatchJob;
import org.broadinstitute.zamboni.server.batchsystem.JobCompletionStatus;
import org.broadinstitute.zamboni.server.batchsystem.sge.SgeBatchSystem;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;

import scala.Option;

/**
 * Helper class which monitors SgeBatchSystem for job completion events and posts them to GP server.
 * @author pcarr
 */
public class JobMonitor {
    public static Logger log = Logger.getLogger(SgeCommandExecutor.class);

    private final SgeBatchSystem sgeBatchSystem;
    //executor for handling job completion events from the sgeBatchSystem
    private ExecutorService jobCompletionService;

    
    public JobMonitor(final SgeBatchSystem sgeBatchSystem) {
        this.sgeBatchSystem = sgeBatchSystem;
    }
    
    public void start() {
        startJobCompletionService();
    }
    
    public void stop() {
        stopJobCompletionService();
    }
    
    private void startJobCompletionService() {
        jobCompletionService = Executors.newSingleThreadExecutor(); 
        
        jobCompletionService.execute(new Runnable() {
            public void run() {
                log.info("starting jobCompletionService ... ");
                while(!jobCompletionService.isShutdown()) {
                    try {
                        // sgeBatchSystem maintains a LinkedBlockingQueue (see AbstractBatchSystem) of completedJobs
                        if (log.isDebugEnabled()) {
                            final int numCompletedJobs=sgeBatchSystem.numCompletedJobsInQueue();
                            log.debug("polling ended jobs queue ... (numCompletedJobsInQueue="+numCompletedJobs+")");
                        }
                        final int numSecondsToWait = 60;
                        final Option<BatchJob> jobOrNull = sgeBatchSystem.pollEndedJobsQueue(numSecondsToWait);
                        if (jobOrNull.isDefined()) { 
                            final BatchJob job = jobOrNull.get();
                            log.debug("jobId="+job.getJobId()+", jobName="+job.getJobName());
                            handleJobCompletion(job);
                        }
                        else {
                            log.debug("job is not defined");
                        }
                    }
                    catch (Throwable t) {
                        if (t instanceof InterruptedException) {
                            log.debug("caught InterruptedException, try again");
                            Thread.currentThread().interrupt();
                        }
                        else {
                            try {
                                log.error("Unexpected error while processing next entry from the completedJobsQueue", t);
                            }
                            catch (Throwable t2) {
                                System.err.println("Unexpected log error on shutdown");
                                t2.printStackTrace();
                                t.printStackTrace();
                            }
                        }
                    }
                }
                try {
                    log.info("jobCompletionService is shut down.");
                }
                catch (Throwable t) {
                    System.err.println("Unexpected log error on shutdown");
                    t.printStackTrace();
                }
            }
        });
    }

    private void stopJobCompletionService() {
        if (jobCompletionService != null) {
            log.info("shutting down jobCompletionService ...");
            jobCompletionService.shutdown();
        }
    }
    
    private void handleJobCompletion(BatchJob sgeJob) {
        final int gpJobId = BatchJobUtil.getGpJobId(sgeJob);
        log.debug("SGE handleJobCompletion: gpJobId="+gpJobId);
        if (gpJobId == -1) {
            log.error("Unable to handleJobCompletion for jobName="+ (sgeJob.getJobName().isDefined() ? sgeJob.getJobName().get() : ""));
            return;
        }
        
        String errorMessage = null;
        boolean success = false;
        int returnCode = -1;
        try {
            scala.Enumeration.Value succeeded = JobCompletionStatus.withName( "SUCCEEDED" ); 
            scala.Option<scala.Enumeration.Value> jobCompletionStatusWrapper = sgeJob.getCompletionStatus();
            if (jobCompletionStatusWrapper.isDefined()) {
                scala.Enumeration.Value jobCompletionStatus = jobCompletionStatusWrapper.get();
                if (succeeded.equals( jobCompletionStatus )) {
                    success = true;
                }
                else {
                    errorMessage = "SGE job completed with jobCompletionStatus: "+jobCompletionStatus.toString();
                }
            }
            else {
                log.error("sgeJob.completionStatus is not defined");
            }
            log.debug("success="+success);
            returnCode= success ? 0 : -1;
            final Option<Integer> returnCodeOpt = sgeJob.getReturnCode();
            if (returnCodeOpt.isDefined()) {
                returnCode=returnCodeOpt.get();
            }
            else {
                log.debug("returnCode not set.");
            }
            log.debug("returnCode="+returnCode);
            BatchJobUtil.updateJobRecord(gpJobId, sgeJob);
        }
        catch (Throwable t) {
            log.error("Unexpected error in SGE handleJobCompletion for gpJobId="+gpJobId, t);
        }
        
        if (success) {
            GenePatternAnalysisTask.handleJobCompletion(gpJobId, returnCode);
        }
        else {
            log.debug(errorMessage);
            GenePatternAnalysisTask.handleJobCompletion(gpJobId, returnCode, errorMessage);
        } 
    } 

}

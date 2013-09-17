package org.genepattern.server.executor.sge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.broadinstitute.zamboni.server.batchsystem.BatchJob;
import org.broadinstitute.zamboni.server.batchsystem.JobCompletionStatus;
import org.broadinstitute.zamboni.server.batchsystem.sge.SgeBatchSystem;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobStatus;

import scala.Option;

/**
 * Helper class which monitors SgeBatchSystem for job completion events and posts them to GP server.
 * @author pcarr
 */
class JobMonitor {
    public static Logger log = Logger.getLogger(SgeCommandExecutor.class);

    private SgeBatchSystem sgeBatchSystem;
    //executor for handling job completion events from the sgeBatchSystem
    private ExecutorService jobCompletionService;

    
    public JobMonitor(SgeBatchSystem sgeBatchSystem) {
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
        
        //TODO: this code is a bit convoluted; sgeBatchSystem maintains a LinkedBlockingQueue (see AbstractBatchSystem);
        //    there is probably a cleaner way to implement this
        final int numSecondsToWait = 1;
        jobCompletionService.execute(new Runnable() {
            public void run() {
                log.info("starting jobCompletionService ...");
                while(!jobCompletionService.isShutdown()) {
                    Option<BatchJob> jobOrNull = JobMonitor.this.sgeBatchSystem.pollEndedJobsQueue(numSecondsToWait);
                    if (jobOrNull.isDefined()) { 
                        BatchJob job = jobOrNull.get();
                        handleJobCompletion(job);
                    }
                }
                log.info("jobCompletionService is shut down.");
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
        int gpJobId = BatchJobUtil.getGpJobId(sgeJob);
        log.debug("SGE handleJobCompletion: gpJobId="+gpJobId);
        if (gpJobId == -1) {
            log.error("Unable to handleJobCompletion for jobName="+ (sgeJob.getJobName().isDefined() ? sgeJob.getJobName().get() : ""));
            return;
        }

        String errorMessage = null;
        boolean success = false;
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

        int returnCode= success ? 0 : -1;
        Object returnCodeObj=sgeJob.getReturnCode().get();
        if (returnCodeObj!=null && returnCodeObj instanceof Integer) {
            returnCode=(Integer) returnCodeObj;
        }
        else {
            log.debug("Invalid returnCodeObj="+returnCodeObj);
        }
        log.debug("returnCode="+returnCode);
        
        if (success) {
            GenePatternAnalysisTask.handleJobCompletion(gpJobId, returnCode);
        }
        else {
            log.debug(errorMessage);
            GenePatternAnalysisTask.handleJobCompletion(gpJobId, returnCode, errorMessage);
        } 
        BatchJobUtil.updateJobRecord(gpJobId, sgeJob);
    } 

}

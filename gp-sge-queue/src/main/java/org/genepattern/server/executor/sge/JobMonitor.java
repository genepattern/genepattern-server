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
            @Override
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
        //TODO: improve shutdown process
        if (jobCompletionService != null) {
            log.info("shutting down jobCompletionService ...");
            jobCompletionService.shutdown();
        }
    }
    
    private void handleJobCompletion(BatchJob sgeJob) {
        String jobName = "";
        Option<String> opt = sgeJob.getJobName();
        if (opt.isDefined()) {
            jobName = opt.get();
        }
        log.debug("handleJobCompletion: sgeJob.jobName="+jobName);

        int gpJobId = -1;
        if (jobName.startsWith("GP_")) {
            String gpJobIdStr = jobName.substring( "GP_".length() );
            try {
                gpJobId = Integer.parseInt(gpJobIdStr);
            }
            catch (NumberFormatException e) {
                log.error("Invalid gpJobId, expecting an integer. gpJobId="+gpJobIdStr);
            }
        }
        if (gpJobId == -1) {
            log.error("Unable to handleJobCompletion for jobName="+jobName);
            return;
        }

        //TODO: handle stdout and stderr
        int exitCode = JobStatus.JOB_PROCESSING;
        String errorMessage = null;

        boolean success = false;
        scala.Enumeration.Value succeeded = JobCompletionStatus.withName( "SUCCEEDED" ); 
        scala.Option<scala.Enumeration.Value> jobCompletionStatusWrapper = sgeJob.getCompletionStatus();
        if (jobCompletionStatusWrapper.isDefined()) {
            scala.Enumeration.Value jobCompletionStatus = jobCompletionStatusWrapper.get();
            if (succeeded.equals( jobCompletionStatus )) {
                success = true;
            }
        }
            
        //TODO: clean up code, single invocation to GPAT.handleJobCompletion, better messages on error
        if (success) {
            exitCode = JobStatus.JOB_FINISHED;
            GenePatternAnalysisTask.handleJobCompletion(gpJobId, exitCode);
        }
        else {
            exitCode = JobStatus.JOB_ERROR;
            errorMessage = JobCompletionStatus.SUCCEEDED().toString();
            GenePatternAnalysisTask.handleJobCompletion(gpJobId, exitCode, errorMessage);
        }
        new JobRecorder().updateSgeJobRecord(gpJobId, sgeJob);
    } 

}

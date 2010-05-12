package org.genepattern.server.executor.lsf;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;

import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;

/**
 * Handle job completion events from the BroadCore LSF handler.
 * @author pcarr
 */
public class LsfJobCompletionListener implements JobCompletionListener {
    private static Logger log = Logger.getLogger(LsfJobCompletionListener.class);
    private static ExecutorService executor = Executors.newFixedThreadPool(3);
    
    /**
     * Note: using the JOB_LSF.NAME column for storing the GP_JOB_ID
     * @param job
     * @return
     * @throws Exception
     */
    private static int getGpJobId(LsfJob job) throws Exception {
        if (job == null) throw new Exception("Null arg");
        if (job.getName() == null) throw new Exception("Null job.name");
        String jobIdStr = job.getName();
        return Integer.parseInt(jobIdStr);
    }

    public void jobCompleted(LsfJob job) throws Exception {
        final int gpJobId = getGpJobId(job);
        String jobStatus = job.getStatus();
        //TODO: check for error or terminated status
        log.debug("job completed...lsf_id="+job.getLsfJobId()+", internal_job_id="+job.getInternalJobId()+", gp_job_id="+gpJobId);
        final String stdoutFilename = job.getOutputFilename();
        final String stderrFilename = job.getErrorFileName();
        final int exitCode = 0;

        //must run this in a new thread because this callback is run from within a hibernate transaction
        //and the GenePatternAnalyisTask.handleJobCompletion closes that transaction
        //See the comments in GenePatternAnalysisTask to see why the transaction must be closed, it is
        //    related to Oracle CLOBs.
        FutureTask<Integer> future =
            new FutureTask<Integer>(new Callable<Integer>() {
              public Integer call() throws Exception {
                  int rVal = 0;
                  GenePatternAnalysisTask.handleJobCompletion(gpJobId, stdoutFilename, stderrFilename, exitCode, JobStatus.JOB_FINISHED);
                  return rVal;
            }});
          executor.execute(future);

          //wait for the thread to complete before exiting
          try {
              int statusCode = future.get();
              log.debug("job #"+gpJobId+" saved to GP database, statusCode="+statusCode);
          }
          catch (Throwable t) {
              String message = "Error handling job completion for job #"+gpJobId;
              log.error(message,t);
              throw new Exception(message, t);
          }
    }
}

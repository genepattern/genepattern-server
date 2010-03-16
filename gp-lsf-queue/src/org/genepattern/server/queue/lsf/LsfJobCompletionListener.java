package org.genepattern.server.queue.lsf;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;
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

    public void jobCompleted(LsfJob job) throws Exception {
        log.debug("job completed...lsf_id="+job.getLsfJobId()+", gp_job_id="+job.getInternalJobId());
        
        final int jobId = job.getInternalJobId().intValue();
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
                  GenePatternAnalysisTask.handleJobCompletion(jobId, stdoutFilename, stderrFilename, exitCode);
                  return rVal;
            }});
          executor.execute(future);

          //wait for the thread to complete before exiting
          try {
              int statusCode = future.get();
              log.debug("job info saved to GP database, statusCode="+statusCode);
          }
          catch (Exception e) {
              log.error("Error handling job completion for job #"+jobId);
              throw e;
          }
    }
}

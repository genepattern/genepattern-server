package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;

import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;

/**
 * Handle job completion events from the BroadCore LSF handler.
 * @author pcarr
 */
public class LsfJobCompletionListener implements JobCompletionListener {
    private static Logger log = Logger.getLogger(LsfJobCompletionListener.class);
    
    /**
     * Note: using the JOB_LSF.NAME column for storing the GP_JOB_ID
     * @param job
     * @return
     * @throws Exception
     */
    public static int getGpJobId(LsfJob job) throws Exception {
        if (job == null) throw new Exception("Null arg");
        if (job.getName() == null) throw new Exception("Null job.name");
        String jobIdStr = job.getName();
        return Integer.parseInt(jobIdStr);
    }

    private File getStderrFile(final LsfJob lsfJob) {
        String stderrFilename = lsfJob.getErrorFileName();
        if (stderrFilename == null) {
            stderrFilename = GPConstants.STDERR;
        }
        File stderrFile = new File(stderrFilename);
        if (stderrFile.getParent() == null) {
            stderrFile = new File(lsfJob.getWorkingDirectory(), stderrFilename);
        }
        return stderrFile;
    }

    public void jobCompleted(final LsfJob job) throws Exception {
        final int gpJobId = getGpJobId(job);
        log.debug("job completed...lsf_id="+job.getLsfJobId()+", internal_job_id="+job.getInternalJobId()+", gp_job_id="+gpJobId);
        final File jobDir = new File(job.getWorkingDirectory());
        //TODO: this is hard-coded, must update this to handle modules which override the default setting
        final File stdoutFile = new File(jobDir, GPConstants.STDOUT);
        final File stderrFile = getStderrFile(job);
        final String errorMessage = null;

        //must run this in a new thread because this callback is run from within a hibernate transaction
        //and the GenePatternAnalyisTask.handleJobCompletion closes that transaction
        //See the comments in GenePatternAnalysisTask to see why the transaction must be closed, it is
        //    related to Oracle CLOBs.
        FutureTask<Integer> future =
            new FutureTask<Integer>(new Callable<Integer>() {
              public Integer call() throws Exception {
                  //TODO: get the exit code from the lsf job and send it along to genepattern
                  int exitValue = 0;
                  String lsfJobStatus = job.getStatus();
                  if (!("DONE".equalsIgnoreCase(lsfJobStatus))) {
                      //job did not complete as expected, flag as error
                      exitValue = -1;
                  }
                  GenePatternAnalysisTask.handleJobCompletion(gpJobId, exitValue, errorMessage, jobDir, stdoutFile, stderrFile);
                  return exitValue;
            }});
        ExecutorService jobCompletionService = LsfCommandExecutor.getJobCompletionService();
        if (jobCompletionService == null) {
            String message = "Error handling job completion for job #"+gpJobId+". LsfCommandExecutor returned null jobCompletionService.";
            log.error(message);
            throw new Exception(message);
        }
        jobCompletionService.execute(future);

        //wait for the thread to complete before exiting
        try {
            int exitValue = future.get();
            log.debug("job #"+gpJobId+" saved to GP database, exitValue="+exitValue);
        }
        catch (Throwable t) {
            String message = "Error handling job completion for job #"+gpJobId;
            log.error(message,t);
            throw new Exception(message, t);
        }
    }
}

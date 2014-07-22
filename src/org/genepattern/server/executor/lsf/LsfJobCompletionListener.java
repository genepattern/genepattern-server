package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;

import edu.mit.broad.core.Main;
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
    
    /**
     * Get the stdout file for the GP job. By convention this is the last arg of the lsf command line, e.g.
     *     'echo' '/path/to/input.txt' >> 'stdout.txt'
     * When stdout is redirected to a custom file the command is:
     *     'echo' '/path/to/input.txt' >> 'custom.txt' 
     *     
     * @param lsfJob
     * @return the path to the stdout file, or null if it can't be determined. Null means, use the default value.
     */
    private File getStdoutFile(final LsfJob lsfJob) {
        try {
            String stdout = "";
            final String command = lsfJob.getCommand();
            int idx = -1;
            if (command == null) {
                log.error("lsfJob.command == null");
            }
            else {
                idx = command.lastIndexOf(">>");
            }
            if (idx < 0) {
                log.error("command does not contain '>>', command="+command);
            }
            else {
                stdout = command.substring(idx+2).trim();
            }
            if (stdout == null || stdout.length() == 0) {
                log.error("command does not declare a stdout file, command="+command);
            }
            else {
                if (stdout.startsWith("'") && stdout.endsWith("'")) {
                    log.debug("trimming quote characters: "+stdout);
                    stdout = stdout.substring(1, stdout.length()-1);
                }
                File stdoutFile = new File(stdout);
                if (!stdoutFile.isAbsolute()) {
                    log.debug("stdout file is in job directory");
                    stdoutFile = new File(lsfJob.getWorkingDirectory(), stdout);
                }
                if (stdoutFile.exists()) {
                    log.debug("stdoutFile="+stdoutFile);
                    return stdoutFile;
                }
            }
        }
        catch (Throwable t) {
            log.error(t);
        }
        
        log.warn("Unable to determine stdout file for lsf job, lsfJob.name="+lsfJob.getName());        
        final File stdoutFile = new File(lsfJob.getWorkingDirectory(), GPConstants.STDOUT);
        return stdoutFile;
    }

    public void jobCompleted(final LsfJob job) throws Exception {
        final int gpJobId = getGpJobId(job);
        log.debug("job completed...lsf_id="+job.getLsfJobId()+", internal_job_id="+job.getInternalJobId()+", gp_job_id="+gpJobId);
        final File jobDir = new File(job.getWorkingDirectory());
        final File stdoutFile = getStdoutFile(job);            
        final File stderrFile = getStderrFile(job);

        //must run this in a new thread because this callback is run from within a hibernate transaction
        //and the GenePatternAnalyisTask.handleJobCompletion closes that transaction
        //See the comments in GenePatternAnalysisTask to see why the transaction must be closed, it is
        //related to Oracle CLOBs.
        FutureTask<Integer> future =
            new FutureTask<Integer>(new Callable<Integer>() {
              public Integer call() throws Exception {
                  //TODO: get the exit code from the lsf job and send it along to genepattern
                  int exitValue = 0;
                  String errorMessage = null;

                  String lsfJobStatus = job.getStatus();                  
                  if (!("DONE".equalsIgnoreCase(lsfJobStatus))) {
                      //job did not complete as expected, flag as error
                      exitValue = -1;

                      try
                      {
                            File lsfJobOutputFile = new File(jobDir, job.getOutputFilename());
                            LsfErrorCheckerImpl errorCheck = new LsfErrorCheckerImpl(lsfJobOutputFile);
                            LsfErrorStatus status = errorCheck.getStatus();
                            if(status != null)
                            {
                                errorMessage = status.getErrorMessage();
                            }
                      }
                      catch(Exception e)
                      {
                            log.error("Error writing lsf error to stderr:\n"); 
                            log.error(e);
                            //log and ignore any errors in getting info about the Lsf error and continue
                      }
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
        finally {
            try {
                //delete the entry from the JOB_LSF table, see GP-3336
                Main.getInstance().getHibernateSession().delete(job);
            }
            catch (Throwable t) {
                String message = "Error deleting entry from JOB_LSF table for gpJobId="+gpJobId;
                log.error(message, t);
            }
        }
    }
}

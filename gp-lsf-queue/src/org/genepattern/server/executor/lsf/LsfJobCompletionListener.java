package org.genepattern.server.executor.lsf;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;

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
    
    //split the given command line into tokens delimited by space char, or enclosed within quotes
    //make sure to handle escaped quote characters
    private static List<String> splitCommandLine(String commandLine) {
        List<String> rval = new ArrayList<String>();
        
        int idx = 0;
        while(true) {
            int startIdx = nextNonWsIdx(idx, commandLine);
            if (startIdx >= commandLine.length()) {
                //no more tokens
                break;
            }
            char delim = ' ';
            if (commandLine.charAt(startIdx) == '\"') {
                delim = '\"';
            }
            //jump to the end, ignoring escape ('\') characters
            int endIdx = nextIdx(1+idx, delim, commandLine);
            String token = commandLine.substring(startIdx, endIdx);
            rval.add(token);
            idx = endIdx + 1;
        }
        
        return rval;
    }
    
    private static String getNextToken(int idx, String commandLine) {
        int startIdx = nextNonWsIdx(idx, commandLine);
        if (startIdx >= commandLine.length()) {
            //no more tokens
            return null;
        }
        char delim = ' ';
        if (commandLine.charAt(startIdx) == '\"') {
            delim = '\"';
        }
        //jump to the end, ignoring escape ('\') characters
        int endIdx = nextIdx(1+idx, delim, commandLine);
        return commandLine.substring(startIdx, endIdx);
    }

    //get the next non whitespace character from the string
    private static int nextNonWsIdx(int idx, String commandLine) {
        while(idx < commandLine.length()) {
            char c = commandLine.charAt(idx);
            if (!Character.isWhitespace(c)) {
                break;
            }
            ++idx;
        }
        return idx;
    }
    
    //get the end index of the current token
    private static int nextIdx(int idx, char delim, String commandLine) {
        while(idx < commandLine.length()) {
            char c = commandLine.charAt(idx);
            if (c == '\\') {
                //escape char
                ++idx;
            }
            else if (c == delim) {
                return idx;
            }
            ++idx;
        }
        return idx;
    }

    private static String getOutputFilename(int gpJobId, LsfJob job) {
        String stdoutFilename = job.getOutputFilename();

        AnalysisDAO dao = new AnalysisDAO();
        JobInfo jobInfo = dao.getJobInfo(gpJobId);
        Properties lsfProperties = CommandManagerFactory.getCommandManager().getCommandProperties(jobInfo);
        String wrapperScript = lsfProperties.getProperty(LsfProperties.Key.WRAPPER_SCRIPT.getKey());
        if (wrapperScript != null) {
            String commandLine = job.getCommand();
            List<String> commandLineArgs = StringUtil.splitCommandLine(commandLine);
            if (commandLineArgs.size() >= 2) {
                String arg0 = commandLineArgs.get(0);
                String arg1 = commandLineArgs.get(1);
                if (wrapperScript.equals(arg0)) {
                    return arg1;
                }
            }
        }
        return stdoutFilename;        
    }

    public void jobCompleted(final LsfJob job) throws Exception {
        final int gpJobId = getGpJobId(job);
        //TODO: check for error or terminated status
        log.debug("job completed...lsf_id="+job.getLsfJobId()+", internal_job_id="+job.getInternalJobId()+", gp_job_id="+gpJobId);
        
        final String stderrFilename = job.getErrorFileName();
        final int exitCode = 0;

        //must run this in a new thread because this callback is run from within a hibernate transaction
        //and the GenePatternAnalyisTask.handleJobCompletion closes that transaction
        //See the comments in GenePatternAnalysisTask to see why the transaction must be closed, it is
        //    related to Oracle CLOBs.
        FutureTask<Integer> future =
            new FutureTask<Integer>(new Callable<Integer>() {
              public Integer call() throws Exception {
                  //special handling for stdoutFilename as it could be the case the the bsub -o arg is a different file than the stdout from the gp command
                  String stdoutFilename = job.getOutputFilename();
                  try {
                      //Note: this method opens a db connection, which is closed in the call to handleJobCompletion
                      stdoutFilename = getOutputFilename(gpJobId, job);
                  }
                  catch (Throwable t) {
                      log.error("Error getting stdout filename for LSF job, using the lsf output filename instead: "+stdoutFilename, t);
                  }
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

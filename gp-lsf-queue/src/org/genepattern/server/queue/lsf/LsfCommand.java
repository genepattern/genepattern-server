package org.genepattern.server.queue.lsf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.queue.CommandExecutor;
import org.genepattern.webservice.JobInfo;

import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;

/**
 * Run the given command line on the LSF queue. This class depends on another thread which monitors the LSF queue for completed jobs.
 * 
 * @author pcarr
 */
public class LsfCommand implements CommandExecutor {
    private static Logger log = Logger.getLogger(LsfCommand.class);
    
    private int jobId;
    private File runDir;
    
    //TODO: these properties should be configurable
    private String project="genepattern";
    private String queue="broad";
    private String maxMemory="2"; //2G
    //private String hosts = "hassium others+1";
    
    private LsfJob lsfJob = null;
    
    //example LSF command from the GP production server,
    //bsub -P $project -q "$queue" -R "rusage[mem=$max_memory]" -M $max_memory -m "$hosts" -K -o .lsf_%J.out -e $lsf_err $"$@" \>\> $cmd_out
    
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) {
        this.jobId = jobInfo != null ? jobInfo.getJobNumber() : -1;
        this.runDir = runDir;

        lsfJob = new LsfJob();
        
        String commandLineStr = getCommandLineStr(commandLine);
        log.debug("lsf job commandLine: "+commandLineStr);
        lsfJob.setCommand(commandLineStr);
        lsfJob.setWorkingDirectory(this.runDir.getAbsolutePath());
        lsfJob.setInternalJobId((long)jobId);
        
        lsfJob.setProject(project);
        lsfJob.setQueue(queue);
        
        List<String> extraBsubArgs = new ArrayList<String>();
        extraBsubArgs.add("-R");
        extraBsubArgs.add("rusage[mem="+maxMemory+"]");
        extraBsubArgs.add("-M");
        extraBsubArgs.add(maxMemory);
        //extraBsubArgs.add("-m");
        //extraBsubArgs.add(hosts);
        
        List<String> preExecArgs = getPreExecCommandArgs(commandLine);
        extraBsubArgs.addAll(preExecArgs);
        
        lsfJob.setCompletionListenerName(MyJobCompletionListener.class.getCanonicalName());
    }
    
    public LsfJob getLsfJob() {
        return lsfJob;
    }
    
    /**
     * helper method which converts a list of String args into a single command line string for LSF submission.
     * @param commandLine
     * @return
     */
    private String getCommandLineStr(String[] commandLine) {
        String rval = "";
        boolean first = true;
        for(String arg : commandLine) {
            if (first) {
                first = false;
            }
            else {
                rval += " ";
            }
            //wrap args with space characters in quotes
            if (arg.contains(" ")) {
                arg = "\""+arg+"\"";
            }
            rval += arg;
        }
        return rval;
    }
    
    /**
     * Get the pre_exec_command arguments, including the '-E'.
     * For example,
     *     { "-E",  "cd /xchip/gpint/d2 && cd /xchip/gpint/d2" }
     * @param commandLine
     * 
     * @return a List of extra args to include with the bsub command, an empty list if no pre_exec_command is required.
     */
    private List<String> getPreExecCommandArgs(String[] commandLine) {
        List<String> rval = new ArrayList<String>();
        return rval;
    }
    
    public static class MyJobCompletionListener implements JobCompletionListener {

        public void jobCompleted(LsfJob job) throws Exception {
            log.debug("job completed...lsf_id="+job.getLsfJobId()+", gp_job_id="+job.getInternalJobId());
            
            int jobId = job.getInternalJobId().intValue();
            String stdoutFilename = job.getOutputFilename();
            String stderrFilename = job.getErrorFileName();
                        
            //TODO: figure out the exit code
            int exitCode = 0;
            
            GenePatternAnalysisTask.handleJobCompletion(jobId, stdoutFilename, stderrFilename, exitCode);
        }
        
    }

}

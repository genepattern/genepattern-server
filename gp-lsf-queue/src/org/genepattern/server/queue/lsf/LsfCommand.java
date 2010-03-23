package org.genepattern.server.queue.lsf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.webservice.JobInfo;

import edu.mit.broad.core.lsf.LsfJob;

/**
 * Run the given command line on the LSF queue. This class depends on another thread which monitors the LSF queue for completed jobs.
 * 
 * @author pcarr
 */
class LsfCommand {
    private static Logger log = Logger.getLogger(LsfCommand.class);
    
    private int jobId;
    private File runDir;
    
    private static String project="gp_3_2_3_dev";
    private static String queue="genepattern";
    private static String maxMemory="2"; //2G
    
    private static void initProperties() {
        project=System.getProperty("LsfCommandExecSvc.project", "gp_dev");
        queue=System.getProperty("LsfCommandExecSvc.queue", "genepattern");
        maxMemory=System.getProperty("LsfCommandExecSvc.max.memory", "2");
        
        //validate maxMemory
        if (maxMemory == null) {
            maxMemory = "2";
        }
        try {
            Integer.parseInt(maxMemory);
        }
        catch (NumberFormatException e) {
            log.error("Invalid setting for 'LsfCommandExecSvc.max.memory="+maxMemory+"': "+e.getLocalizedMessage(), e);
            maxMemory="2";
        }
    }
    
    private LsfJob lsfJob = null;
    
    //example LSF command from the GP production server,
    //bsub -P $project -q "$queue" -R "rusage[mem=$max_memory]" -M $max_memory -m "$hosts" -K -o .lsf_%J.out -e $lsf_err $"$@" \>\> $cmd_out
    
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) {
        //TODO: move this to global startup
        initProperties();
        
        this.jobId = jobInfo != null ? jobInfo.getJobNumber() : -1;
        this.runDir = runDir;

        lsfJob = new LsfJob();
        //note: use the name of the job (the bsub -J arg) to map the GP JOB ID to the JOB_LSF table
        //    the internalJobId is (by default) configured as a primary key with a sequence
        lsfJob.setName(""+jobId);
        
        String commandLineStr = getCommandLineStr(commandLine);
        log.debug("lsf job commandLine: "+commandLineStr);
        lsfJob.setCommand(commandLineStr);
        lsfJob.setWorkingDirectory(this.runDir.getAbsolutePath());
        //TODO: handle stdin, currently it is ignored
        //lsfJob.setInputFilename(inputFilename);
        lsfJob.setOutputFilename(".lsf_%J.out");
        //lsfJob.setErrorFileName(".lsf.err");
        //lsfJob.setOutputFilename(stdoutFile.getAbsolutePath());
        lsfJob.setErrorFileName(stderrFile.getAbsolutePath());
        
        lsfJob.setProject(project);
        lsfJob.setQueue(queue);
        
        List<String> extraBsubArgs = new ArrayList<String>();
        extraBsubArgs.add("-R");
        extraBsubArgs.add("rusage[mem="+maxMemory+"]");
        extraBsubArgs.add("-M");
        extraBsubArgs.add(maxMemory);
        
        List<String> preExecArgs = getPreExecCommandArgs(commandLine);
        extraBsubArgs.addAll(preExecArgs);
        
        //HACK: special case for keeping the LSF output separate from the stdout of the process
        extraBsubArgs.add(">>");
        extraBsubArgs.add(stdoutFile.getAbsolutePath());
        
        lsfJob.setCompletionListenerName(LsfJobCompletionListener.class.getName());
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
}

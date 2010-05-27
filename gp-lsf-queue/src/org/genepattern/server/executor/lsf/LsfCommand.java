package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;

import edu.mit.broad.core.lsf.LsfJob;

/**
 * Run the given command line on the LSF queue. This class depends on another thread which monitors the LSF queue for completed jobs.
 * 
 * @author pcarr
 */
class LsfCommand {
    private static Logger log = Logger.getLogger(LsfCommand.class);
    
    private Properties lsfProperties = null;
    private LsfJob lsfJob = null;
    
    public void setLsfProperties(Properties p) {
        this.lsfProperties = p;
    }
    
    //example LSF command from the GP production server,
    //bsub -P $project -q "$queue" -R "rusage[mem=$max_memory]" -M $max_memory -m "$hosts" -K -o .lsf_%J.out -e $lsf_err $"$@" \>\> $cmd_out
    
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin) { 
        long jobId = jobInfo != null ? jobInfo.getJobNumber() : -1L;

        lsfJob = new LsfJob();
        //note: use the name of the job (the bsub -J arg) to map the GP JOB ID to the JOB_LSF table
        //    the internalJobId is (by default) configured as a primary key with a sequence
        lsfJob.setName(""+jobId);
        lsfJob.setInternalJobId(jobId);
        lsfJob.setWorkingDirectory(runDir.getAbsolutePath());
        //TODO: handle stdin, currently it is ignored
        //lsfJob.setInputFilename(inputFilename);
        //Note: BroadCore does not handle the %J idiom for the output file
        lsfJob.setOutputFilename(lsfProperties.getProperty(LsfProperties.Key.OUTPUT_FILENAME.getKey()));
        lsfJob.setErrorFileName(stderrFile.getAbsolutePath());
        
        lsfJob.setProject(lsfProperties.getProperty(LsfProperties.Key.PROJECT.getKey()));
        lsfJob.setQueue(lsfProperties.getProperty(LsfProperties.Key.QUEUE.getKey()));
        
        List<String> extraBsubArgs = new ArrayList<String>();
        String maxMemory = lsfProperties.getProperty(LsfProperties.Key.MAX_MEMORY.getKey());
        extraBsubArgs.add("-R");
        extraBsubArgs.add("rusage[mem="+maxMemory+"]");
        extraBsubArgs.add("-M");
        extraBsubArgs.add(maxMemory);
        
        List<String> preExecArgs = getPreExecCommand(jobInfo);
        extraBsubArgs.addAll(preExecArgs);
        
        //HACK: append a shell script to the command, whose only purpose is to separate stdout of the command from the LSF header information
        //      LSF does not have a bsub option for this
        String wrapperScript = lsfProperties.getProperty(LsfProperties.Key.WRAPPER_SCRIPT.getKey());
        if (wrapperScript != null) {
            extraBsubArgs.add(wrapperScript);
            String stdoutPath = stdoutFile.getAbsolutePath();
            if (stdoutPath.contains(" ")) {
                stdoutPath = "\""+stdoutPath+"\"";
            }
            extraBsubArgs.add(stdoutPath);
        }

        //workaround for current implementation of Broad Core lsfJob
        // it only accepts a single String for the command line, but it would be better if it were treated as a List<String>        
        String commandLineStr = getCommandLineStr(commandLine);
        lsfJob.setCommand(commandLineStr);
        // use extraBsubArgs instead
        //int lastIdx = commandLine.length - 1;
        //if (commandLine.length > 1) {
        //    for(int i=0; i<(commandLine.length-1); ++i) {
        //        extraBsubArgs.add(commandLine[i]);
        //    }
        //}
        //if (lastIdx >= 0) {
        //    lsfJob.setCommand(commandLine[lastIdx]);
        //}

        //TODO: make this a configuration option
        lsfJob.setCompletionListenerName(LsfJobCompletionListener.class.getName());
    }
    
    public void prepareToTerminate(JobInfo jobInfo) {
        int jobId = jobInfo != null ? jobInfo.getJobNumber() : -1;
        lsfJob = new LsfJob();
        //note: use the name of the job (the bsub -J arg) to map the GP JOB ID to the JOB_LSF table
        //    the internalJobId is (by default) configured as a primary key with a sequence
        lsfJob.setName(""+jobId);
        
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
     *     { "-E",  "cd /xchip/gpint/d1 && cd /xchip/gpint/d2" }
     * @param commandLine
     * 
     * @return a List of extra args to include with the bsub command, an empty list if no pre_exec_command is required.
     */
    private List<String> getPreExecCommand(JobInfo jobInfo) { 
        List<String> rval = new ArrayList<String>();
        
        if (!Boolean.valueOf(lsfProperties.getProperty(LsfProperties.Key.USE_PRE_EXEC_COMMAND.getKey()))) {
            return rval;
        }

        Set<String> filePaths = new HashSet<String>();

        //add the working directory for the job
        String jobDirName = GenePatternAnalysisTask.getJobDir(""+jobInfo.getJobNumber());
        File jobDir = new File(jobDirName);
        if (jobDir.exists()) {
            String path = jobDir.getAbsolutePath();
            filePaths.add(path);
        }

        //for each input parameter, if it is a file which exists, add its parent to the list
        for(ParameterInfo param : jobInfo.getParameterInfoArray()) {
            String val = param.getValue();
            File file = new File(val);
            File parentFile = file.getParentFile();
            if (parentFile != null && parentFile.exists()) {
                String path = parentFile.getAbsolutePath();
                filePaths.add(path);
            }
        }
        
        if (filePaths.isEmpty()) {
            return rval;
        }
        
        String preExecCommand="";
        boolean first = true;
        for(String path : filePaths) {
            if (!first) {
                preExecCommand += " && ";
            }
            else {
                first = false;
            }
            preExecCommand += "cd \""+path+"\"";
        }

        log.debug("setting pre_exec_command to: -E \""+preExecCommand+"\"");
        rval.add("-E");
        rval.add(preExecCommand);
        return rval;
    }
}

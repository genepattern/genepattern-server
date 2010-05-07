package org.genepattern.server.executor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;

public class RuntimeCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(RuntimeCommandExecutor.class);

    public void setConfigurationFilename(String filename) {
        log.error("ignoring: setCofigurationFilename("+filename+")");
    }

    public void setConfigurationProperties(Properties properties) {
        log.error("ignoring setConfigurationProperties");
    }
    
    //----- keep references to all currently running jobs which were started by this executor
    private Map<String,RuntimeExecCommand> runningJobs = new HashMap<String,RuntimeExecCommand>();


    public void start() {
    }

    public void stop() {
        terminateAll("--> Shutting down server");
    }
    
    private void terminateAll(String message) {
        //TODO: globally terminate all running pipelines
        //    pipelines used to be terminated here
        log.debug(message);
        
        for(Entry<String,RuntimeExecCommand> entry : runningJobs.entrySet()) {
            //String jobID = entry.getKey();
            RuntimeExecCommand cmd = entry.getValue();
            cmd.terminateProcess();
            Thread.yield();
        }
    }

    public void runCommand(String[] commandLine,
            Map<String, String> environmentVariables, File runDir,
            File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin,
            StringBuffer stderrBuffer) {
        
        RuntimeExecCommand cmd = new RuntimeExecCommand();
        String jobId = ""+jobInfo.getJobNumber();
        runningJobs.put(jobId, cmd);
        cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdin, stderrBuffer);
        runningJobs.remove(jobId);
        int exitValue = cmd.getExitValue();
        int jobStatus = JobStatus.JOB_FINISHED;
        if (RuntimeExecCommand.Status.TERMINATED.equals( cmd.getInternalJobStatus() )) {
            jobStatus = JobStatus.JOB_ERROR;
        }
        if (exitValue != 0) {
            jobStatus = JobStatus.JOB_ERROR;
        }
        try {
            GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), stdoutFile.getName(), stderrFile.getName(), exitValue, jobStatus);
        }
        catch (Exception e) {
            log.error("Error handling job completion for job "+jobInfo.getJobNumber(), e);
        }
    }

    public void terminateJob(JobInfo jobInfo) {
        if (jobInfo == null) {
            log.error("null jobInfo");
            return;
        }
        String jobId = ""+jobInfo.getJobNumber();
        
        RuntimeExecCommand cmd = runningJobs.get(jobId);
        if (cmd == null) {
            //terminateJob is called from deleteJob, so quite often terminateJob should have no effect
            log.debug("terminateJob("+jobInfo.getJobNumber()+"): job not running");
            return;
        }
        cmd.terminateProcess();
    }

}

package org.genepattern.server.queue;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;

public class RuntimeExecCmdExecSvc implements CommandExecutorService {
    private static Logger log = Logger.getLogger(RuntimeExecCmdExecSvc.class);

    public void start() {
        // TODO Auto-generated method stub
        
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }

    public void runCommand(String[] commandLine,
            Map<String, String> environmentVariables, File runDir,
            File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin,
            StringBuffer stderrBuffer) {
        
        RuntimeExecCommand cmd = new RuntimeExecCommand();
        cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdin, stderrBuffer);
        int exitValue = cmd.getExitValue();
        try {
            GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), stdoutFile.getName(), stderrFile.getName(), exitValue);
        }
        catch (Exception e) {
            log.error("Error handling job completion for job "+jobInfo.getJobNumber(), e);
        }
    }

    public void terminateJob(JobInfo jobInfo) {
        // TODO Auto-generated method stub
        
    }

}

package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
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

    public void start() {
        // TODO Auto-generated method stub
        
    }

    public void stop() {
        RuntimeExecCommand.terminateAll("--> Shutting down server");
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
        log.error("method not implemented: terminatJob("+jobInfo.getJobNumber()+")");
    }
}

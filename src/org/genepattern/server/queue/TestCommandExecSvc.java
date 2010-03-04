package org.genepattern.server.queue;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;

public class TestCommandExecSvc implements CommandExecutorService {
    private static Logger log = Logger.getLogger(TestCommandExecSvc.class);

    public void start() {
        log.debug("starting CommandExecutorService ...");
    }

    public void stop() {
        log.debug("stopping CommandExecutorService ...");
    }

    public void terminateJob(JobInfo jobInfo) {
        log.debug("terminating job: "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
    }

    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) {
        int exitCode = 0;
        try {
        String cmdLine = "";
        for(String arg : commandLine) {
            if (arg.contains("\"")) {
                //escape quote characters
                arg = arg.replaceAll("\"", "\\\"");
            }
            if (arg.contains(" ")) {
                arg = "\""+arg+"\"";
            }
            cmdLine += arg + " ";
        }
        log.debug("Running command: "+cmdLine);
        }
        catch (RuntimeException e) {
            exitCode = -1;
        }
        try { 
            GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), stdoutFile.getName(), stderrFile.getName(), exitCode);
        }
        catch (Exception e) {
            log.error("Error handling job completion for job "+jobInfo.getJobNumber(), e);
        }
    }
}

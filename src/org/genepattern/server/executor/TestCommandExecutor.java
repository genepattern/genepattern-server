package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;

public class TestCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(TestCommandExecutor.class);
    
    public void setConfigurationFilename(String filename) {
        log.info("setting configuration filename: "+filename);
    }
    
    public void setConfigurationProperties(Properties properties) {
        log.info("setting configuration properties: "+properties.toString());
    }

    public void start() {
        log.info("starting CommandExecutor ...");
    }

    public void stop() {
        log.info("stopping CommandExecutor ...");
    }

    public void terminateJob(JobInfo jobInfo) {
        log.info("terminating job: "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
    }

    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer) {
        int exitCode = 0;
        int jobStatus = JobStatus.JOB_PROCESSING;
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
            jobStatus = JobStatus.JOB_FINISHED;
        }
        catch (RuntimeException e) {
            exitCode = -1;
            jobStatus = JobStatus.JOB_ERROR;
        }
        try { 
            GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), stdoutFile.getName(), stderrFile.getName(), exitCode, jobStatus);
        }
        catch (Exception e) {
            log.error("Error handling job completion for job "+jobInfo.getJobNumber(), e);
        }
    }
    
    public void reloadConfiguration() {
        //no-op
    }
}

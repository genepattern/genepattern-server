/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;

public class TestCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(TestCommandExecutor.class);
    
    public void setConfigurationFilename(String filename) {
        log.info("setting configuration filename: "+filename);
    }
    
    public void setConfigurationProperties(CommandProperties properties) {
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

    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) {
        int exitCode = 0;
        String errorMessage = null;
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
            GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), exitCode, null, runDir, stdoutFile, stderrFile);
        }
        catch (Exception e) {
            log.error("Error handling job completion for job "+jobInfo.getJobNumber(), e);
        }
    }

    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        log.info("handle running job #"+jobInfo.getJobNumber()+" on startup");
        return JobStatus.JOB_PENDING;
    }

}

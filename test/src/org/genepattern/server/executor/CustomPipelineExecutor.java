/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;

import org.genepattern.webservice.JobInfo;
import org.junit.Ignore;

@Ignore
public class CustomPipelineExecutor implements CommandExecutor {

    @Override
    public void setConfigurationFilename(String filename) {
    }

    @Override
    public void setConfigurationProperties(CommandProperties properties) {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        throw new CommandExecutorException("Not implemented!");
    }

    @Override
    public void terminateJob(JobInfo jobInfo) throws Exception {
        throw new Exception("Not implemented!");
    }

    @Override
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        throw new Exception("Not implemented!");
    }

}

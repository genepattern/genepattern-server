package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;

import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.JobInfo;

public interface CommandExecutor2 extends CommandExecutor {
    void runCommand(
            String commandLine[], 
            Map<String, String> environmentVariables, 
            File runDir, 
            File stdoutFile, 
            File stderrFile, 
            JobInput jobInput,
            JobInfo jobInfo, 
            File stdinFile) 
    throws CommandExecutorException;
}

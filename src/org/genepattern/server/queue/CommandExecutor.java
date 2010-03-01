package org.genepattern.server.queue;

import java.io.File;
import java.util.Map;

import org.genepattern.webservice.JobInfo;

/**
 * Interface for executing a command line.
 * 
 * @author pcarr
 */
public interface CommandExecutor {
    void runCommand(String commandLine[], Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, String stdin, StringBuffer stderrBuffer);
}

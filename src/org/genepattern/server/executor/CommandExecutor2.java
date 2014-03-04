package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;

import org.genepattern.server.config.GpContext;

/**
 * Update to the CommandExecutor API so that an initialized GpContext
 * can be used to pass in the job details to the executor.
 *  
 * @author pcarr
 *
 */
public interface CommandExecutor2 extends CommandExecutor {

    /**
     * Run the job.
     * 
     * @param gpContext, requires a valid JobInfo and userId.
     * @param commandLine, the command line args
     * @param environmentVariables
     * @param runDir, the working directory for the job.
     * @param stdoutFile, stream stdout from the job to this file.
     * @param stderrFile, stream stderr from the job to this file.
     * @param stdinFile, stream stdin to the job from this file.
     * 
     * @throws CommandExecutorException
     */
    void runCommand(
            GpContext gpContext,
            String[] commandLine,
            Map<String, String> environmentVariables,
            File runDir,
            File stdoutFile,
            File stderrFile,
            File stdinFile)
    throws CommandExecutorException;
}

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;

import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.JobInfo;

/**
 * Helper class to transition from the pre 3.8.1 CommandExecutor API to the new CommandExecutor2 API,
 * which takes a GpContext instance as an arg.
 * 
 * @author pcarr
 *
 */
public class CommandExecutor2Wrapper implements CommandExecutor2 {
    private final CommandExecutor cmdExec;
    private final CommandExecutor2 cmdExec2;

    public static final CommandExecutor2 createCmdExecutor(final CommandExecutor cmdExecIn) {
        if (cmdExecIn == null) {
            throw new IllegalArgumentException("cmdExecIn==null");
        }
        if (cmdExecIn instanceof CommandExecutor2) {
            return (CommandExecutor2) cmdExecIn;
        }
        return new CommandExecutor2Wrapper(cmdExecIn);
    }
    
    private CommandExecutor2Wrapper(final CommandExecutor cmdExec) {
        this.cmdExec=cmdExec;
        this.cmdExec2=null;
    }
    
    private CommandExecutor2Wrapper(final CommandExecutor2 cmdExec2) {
        this.cmdExec=cmdExec2;
        this.cmdExec2=cmdExec2;
    }

    @Override
    public void setConfigurationFilename(final String filename) {
        cmdExec.setConfigurationFilename(filename);
    }

    @Override
    public void setConfigurationProperties(final CommandProperties properties) {
        cmdExec.setConfigurationProperties(properties);
    }

    @Override
    public void start() {
        cmdExec.start();
    }

    @Override
    public void stop() {
        cmdExec.stop();
    }

    @Override
    public void terminateJob(final JobInfo jobInfo) throws Exception {
        if (cmdExec2 != null) {
            cmdExec2.terminateJob(jobInfo);
            return;
        }
        if (cmdExec != null) {
            cmdExec.terminateJob(jobInfo);
        }
    }

    @Override
    public int handleRunningJob(final JobInfo jobInfo) throws Exception {
        if (cmdExec2 != null) {
            return cmdExec2.handleRunningJob(jobInfo);
        }
        if (cmdExec != null) {
            return cmdExec.handleRunningJob(jobInfo);
        }
        return -1;
    }

    @Override
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        cmdExec.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile);
    }

    @Override
    public void runCommand(
            final GpContext gpContext, 
            final String[] commandLine, 
            final Map<String, String> environmentVariables, 
            final File runDir, 
            final File stdoutFile,
            final File stderrFile, 
            final File stdinFile) 
    throws CommandExecutorException {
        if (cmdExec2 != null) {
            cmdExec2.runCommand(gpContext, commandLine, environmentVariables, runDir, stdoutFile, stderrFile, stdinFile);
            return;
        }
        
        //legacy CommandExecutor
        if (gpContext==null) {
            throw new CommandExecutorException("gpContext==null, Can't start job without a valid gpContext");
        }
        if (gpContext.getJobInfo()==null) {
            throw new CommandExecutorException("gpContext.jobInfo==null, Can't start job without a valid jobInfo");
        }
        if (cmdExec != null) {
            cmdExec.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, gpContext.getJobInfo(), stdinFile);
        }
    }
}

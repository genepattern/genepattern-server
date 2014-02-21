package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;

import org.genepattern.server.job.input.JobInput;
import org.genepattern.webservice.JobInfo;

public class CommandExecutor2Wrapper implements CommandExecutor2 {
    private final CommandExecutor cmdExec;
    private final CommandExecutor2 cmdExec2;

    public CommandExecutor2 createCmdExecutor(final CommandExecutor cmdExecIn) {
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
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        final JobInput jobInput=null;
        runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInput, jobInfo, stdinFile);
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
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInput jobInput, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        if (cmdExec2 != null) {
            cmdExec2.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInput, jobInfo, stdinFile);
            return;
        }
        if (cmdExec != null) {
            cmdExec.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile);
        }
    }
}

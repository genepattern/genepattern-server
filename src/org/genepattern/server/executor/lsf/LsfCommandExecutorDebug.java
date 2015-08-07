/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.lsf;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandManagerFactory;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;

import edu.mit.broad.core.lsf.LocalLsfJob;
import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;

public class LsfCommandExecutorDebug extends LsfCommandExecutor {
    private static Logger log = Logger.getLogger(LsfCommandExecutor.class);

    @Override
    public void setConfigurationFilename(final String filename) {
        super.setConfigurationFilename(filename);
    }

    @Override
    public void setConfigurationProperties(final CommandProperties properties) {
        super.setConfigurationProperties(properties);
    }

    @Override
    public void start() {
        log.debug("start (no-op)");
        //don't initialize
    }

    @Override
    public void stop() {
        log.debug("stop (no-op)");
    }

    @Override
    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) throws CommandExecutorException {
        log.debug("Running command for job "+jobInfo.getJobNumber()+". "+jobInfo.getTaskName());
        final CommandProperties lsfProperties = CommandManagerFactory.getCommandManager().getCommandProperties(jobInfo);
        final LsfCommand cmd = new LsfCommand();
        cmd.setLsfProperties(lsfProperties);
        cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile, LsfJobCompletionListener.class);
        String cmdLine=cmd.getLsfJob().getCommand();
        log.debug(""+cmdLine); 
        
        LsfJob lsfJob = cmd.getLsfJob();
        LocalLsfJob localJob = convert(lsfJob);
        
        StringBuffer sb=new StringBuffer();
        final String NL="\n";
        sb.append("Debug mode, didn't actually run the job."); sb.append(NL);
        sb.append("    executor: "+LsfCommandExecutorDebug.class.getName()); sb.append(NL);
        sb.append(NL);
        sb.append("command="); sb.append(cmdLine); sb.append(NL);
        boolean first=true;
        for(String arg : localJob.getBsubCommand()) {
            //join hack
            if (!first) { sb.append("    "); }
            if (first) { first=false; }
            sb.append(arg); sb.append(NL);
        }
        sb.trimToSize();
        GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), 0, sb.toString());
    }

    @Override
    public void terminateJob(JobInfo jobInfo) {
        log.debug("terminated "+jobInfo.getJobNumber());
    }

    @Override
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        log.debug("handleRunningJob "+jobInfo.getJobNumber()); 
        return 0;
    }
    
    @Override
    public void runCommand(final String[] commandLine,
            final Map<String, String> environmentVariables, final File runDir,
            final File stdoutFile, final File stderrFile,
            final JobInfo jobInfo, final File stdinFile,
            final Class<? extends JobCompletionListener> completionListenerClass)
            throws CommandExecutorException {
        log.error("ignoring runCommand");
    }

    //
    // Copied the following methods from the BroadCore LSF library, LsfWrapper.java.
    // 
    /**
     * Creates an LocalLsfJob instance from an LsfJob instance. Checks to see what
     * fields are null on the incoming LsfJob, and only sets non-null properties
     * on the LocalLsfJob.
     */
    private LocalLsfJob convert(LsfJob job) {
        LocalLsfJob localJob = new LocalLsfJob();
        localJob.setCommand(job.getCommand());
        localJob.setName(job.getName());
        localJob.setQueue(job.getQueue());
        localJob.setProject(job.getProject());
        localJob.setExtraBsubArgs(job.getExtraBsubArgs());

        if (job.getWorkingDirectory() != null) {
            localJob.setWorkingDir(new File(job.getWorkingDirectory()));
        }

        if (job.getInputFilename() != null) {
            localJob.setInputFile(getAbsoluteFile(job.getWorkingDirectory(),
                    job.getInputFilename()));
        }

        if (job.getOutputFilename() != null) {
            localJob.setOutputFile(getAbsoluteFile(job.getWorkingDirectory(),
                    job.getOutputFilename()));
        }

        if (job.getErrorFileName() != null) {
            localJob.setErrFile(getAbsoluteFile(job.getWorkingDirectory(), job
                    .getErrorFileName()));
        }

        if (job.getLsfJobId() != null) {
            localJob.setBsubJobId(job.getLsfJobId());
        }

        if (job.getStatus() != null) {
            localJob.setLsfStatusCode(job.getStatus());
        }

        return localJob;
    }

    private File getAbsoluteFile(String dir, String filename) {
        if (filename.startsWith(File.separator)) {
            return new File(filename);
        } else {
            return new File(dir, filename);
        }
    }

}

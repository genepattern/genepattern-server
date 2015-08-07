/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.serverthread;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;

/**
 * A command executor which runs each job as a separate thread within the server's runtime.
 * 
 * @author pcarr
 *
 */
public class ServerThreadExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(ServerThreadExecutor.class);

    private ExecutorService executor;
    private Map<Integer,Future<Integer>> jobs = new HashMap<Integer,Future<Integer>>();

    public void setConfigurationFilename(String filename) {
    }

    public void setConfigurationProperties(CommandProperties properties) {
    }
    
    private void terminateRunningJobs() {
        for(Entry<Integer,Future<Integer>> entry : jobs.entrySet()) {
            final int jobId = entry.getKey();
            final Future<Integer> task = entry.getValue();
            final boolean mayInterruptIfRunning = true;
            final boolean cancelled = task.cancel(mayInterruptIfRunning);
        }
    }

    public void start() {
        if (executor != null) {
            terminateRunningJobs();
            executor.shutdown();
        }
        
        executor = Executors.newSingleThreadExecutor();
    }

    public void stop() {
        if (executor != null) {
            log.debug("stopping executor...");
            terminateRunningJobs();
            
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("executor shutdown timed out after 30 seconds.");
                    executor.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                log.error("executor.shutdown was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    public void runCommand(final String[] commandLine, Map<String, String> environmentVariables, final File runDir, final File stdoutFile, final File stderrFile, final JobInfo jobInfo, final File stdinFile) throws CommandExecutorException {
        //first arg must be a classname which implements the ServerThreadJob interface 
        final ServerTask serverTask;
        try {
            serverTask = initServerTask(commandLine);
        }
        catch (Exception e) {
            throw new CommandExecutorException(e.getLocalizedMessage());
        }
        serverTask.setUserId(jobInfo.getUserId());
        serverTask.setJobId(jobInfo.getJobNumber());        
        String[] args = new String[commandLine.length - 1];
        for(int i=1; i<commandLine.length; ++i) {
            args[i-1] = commandLine[i];
        }
        serverTask.setArgs(args);
        
        //wrap the serverThreadJob in a callable in order to call handleJobCompletion
        Callable<String> wrapper = new Callable<String>() {
            public String call() throws Exception {
                String status = JobStatus.ERROR;
                String errorMessage = "Error running server task";
                try {
                    status = serverTask.call();
                }
                catch (Exception e) {
                    status = JobStatus.ERROR;
                    errorMessage = e.getLocalizedMessage();
                }
                if (JobStatus.ERROR.equals(status)) {
                    int exitCode = -1;
                    GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), exitCode, errorMessage);
                }
                else if (JobStatus.FINISHED.equals(status)) {
                    GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), 0);
                }
                return status;
            }
        };
        Future<String> future = executor.submit(wrapper);
    }
    
    /**
     * Use reflection to create a new ServerTask instance.
     * 
     * The first arg of the commandLine must be a fully qualified classname to a class
     * which implements the ServerTask interface.
     * 
     * @param commandLine
     * @return
     * @throws Exception
     */
    private ServerTask initServerTask(String[] commandLine) throws Exception {
        String classname = commandLine[0];
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Class<?> svcClass = Class.forName(classname, false, classLoader);
        boolean isValid = ServerTask.class.isAssignableFrom(svcClass);
        if (!isValid) {
            throw new Exception("Can't cast "+classname+" to "+ServerTask.class.getName());
        }
        
        ServerTask serverTask = (ServerTask) svcClass.newInstance();
        return serverTask;
    }

    public void terminateJob(JobInfo jobInfo) throws Exception {
        log.error("terminateJob not implemented for "+ServerThreadExecutor.class.getName());
    }

    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        log.error("handleRunningJob not implemented for "+ServerThreadExecutor.class.getName());
        return JobStatus.JOB_ERROR;
    }

}

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import java.io.File;
import java.util.Map;

import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;

/**
 * Interface for managing job execution via runtime exec or an external queuing system. This interface is responsible for both initialization and shutdown of external services,
 * as well as the management of job submission, getting job status, and killing, pausing, and resuming jobs.
 * 
 * @author pcarr
 */
public interface CommandExecutor {
    //configuration support
    /**
     * [optionally] set a path to a configuration file.
     */
    void setConfigurationFilename(String filename);

    /**
     * [optionally] provide properties.
     * @param properties
     */
    void setConfigurationProperties(CommandProperties properties);
    
    /**
     * Start the service, typically called at application startup.
     */
    public void start();
    
    /**
     * Stop the service, typically called just before application shutdown.
     */
    public void stop();

    /**
     * Request the service to run a GenePattern job. It is up to the service to monitor for job completion and callback to GenePattern when the job is completed.
     * 
     * @see GenePatternAnalysisTask#handleJobCompletion(int, String, String, int)
     * 
     * @param commandLine
     * @param environmentVariables
     * @param runDir
     * @param stdoutFile
     * @param stderrFile
     * @param jobInfo
     * @param stdin
     * 
     * @throws CommandExecutorException when errors occur attempting to submit the job
     */
    void runCommand(
            String commandLine[], 
            Map<String, String> environmentVariables, 
            File runDir, 
            File stdoutFile, 
            File stderrFile, 
            JobInfo jobInfo, 
            File stdinFile) 
    throws CommandExecutorException;
    
    /**
     * Request the service to terminate a GenePattern job which is running via this service.
     * @param jobInfo
     * @throws Exception indicating that the job was not properly terminated.
     */
    void terminateJob(JobInfo jobInfo) throws Exception;

    //TODO: add these methods to the interface
    //void pauseJob(JobInfo jobInfo);
    //public void resumeJob(JobInfo jobInfo);
    //public List<JobStatus> getJobs();

    /**
     * This method is called on server startup for each RUNNING job for this queue.
     * 
     * For RuntimeExec, tell the GP server to delete the job results directory and requeue the job.
     * For other executors, (such as LSF), you may want to ignore this message.
     * For PipelineExec, you may need to determine the last successfully completed step before resuming the pipeline.
     * 
     * @return an optional int flag to update the JOB_STATUS_ID in the GP database, ignore if it is less than zero
     */
    int handleRunningJob(JobInfo jobInfo) throws Exception;
}


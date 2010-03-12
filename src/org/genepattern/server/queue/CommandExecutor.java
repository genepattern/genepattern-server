package org.genepattern.server.queue;

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
     * @param stderrBuffer
     * @throws Exception
     */
    void runCommand(
            String commandLine[], 
            Map<String, String> environmentVariables, 
            File runDir, 
            File stdoutFile, 
            File stderrFile, 
            JobInfo jobInfo, 
            String stdin, 
            StringBuffer stderrBuffer) 
    throws Exception;
    
    /**
     * Request the service to terminate a GenePattern job which is running via this service.
     * @param jobInfo
     */
    void terminateJob(JobInfo jobInfo);

    //TODO: add these methods to the interface
    //void pauseJob(JobInfo jobInfo);
    //public void resumeJob(JobInfo jobInfo);
    //public List<JobStatus> getJobs();
}

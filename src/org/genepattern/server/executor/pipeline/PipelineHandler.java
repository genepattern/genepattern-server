package org.genepattern.server.executor.pipeline;

import org.genepattern.webservice.JobInfo;

/**
 * The pipeline handler listens for job completion events.
 * 
 * @author pcarr
 */
public interface PipelineHandler {   
    /**
     * Initialize the pipeline and add the first job to the queue.
     */
    public void startPipeline(String[] commandLine, JobInfo pipelineJobInfo, int stopAfterTask) throws Exception;

    /**
     * Called when a step in a pipeline job has completed, put the next job on the queue.
     */
    public void handleJobCompletion(int jobId) throws Exception;
    
    /**
     * Called when the last step of the pipeline job has completed.
     */
    public void completePipeline(int rootJobId, String stdoutFilename, String stderrFilename, int exitCode, StringBuffer stderrBuffer) throws Exception;
}

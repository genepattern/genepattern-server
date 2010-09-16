package org.genepattern.server.executor.pipeline;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.webservice.JobInfo;

/**
 * Run all pipelines with this CommandExecutor.
 * 
 * This (initial) implementation uses a single thread for each pipeline using code ported from when each pipeline 
 * was run in a new JVM processes.
 * 
 * Manage submission of pipeline jobs, so that they can be terminated by jobId 
 * in response to user request or server shutdown.
 * 
 * @author pcarr
 */
public class PipelineExecutor2 implements CommandExecutor {
    private static Logger log = Logger.getLogger(PipelineExecutor2.class);

    //'pipeline.num.threads' from genepattern.properties
    //private int numPipelines = 20;   
    
    public void setConfigurationFilename(String filename) {
        // TODO Auto-generated method stub
    }

    public void setConfigurationProperties(Properties properties) {
        // TODO Auto-generated method stub
    }

    public void start() {
        // TODO Auto-generated method stub
    }

    public void stop() {
        // TODO Auto-generated method stub
    }

    public void runCommand(final String[] commandLine,
            final Map<String, String> environmentVariables, 
            final File runDir,
            final File stdoutFile, 
            final File stderrFile, 
            final JobInfo jobInfo, 
            final File stdinFile) 
    throws CommandExecutorException {
        
        if (jobInfo == null) {
            throw new CommandExecutorException("null jobInfo");
        }
        
        try {
            LegacyPipelineHandler.startPipeline(commandLine, jobInfo, Integer.MAX_VALUE);
        }
        catch (Exception e) {
            throw new CommandExecutorException("Server error starting pipeline job #"+jobInfo.getJobNumber(), e);
        }
    }
    
    public void terminateJob(JobInfo jobInfo) throws Exception {
        //TODO: implement terminate for the pipeline
        String jobId = ""+jobInfo.getJobNumber();
        log.error("terminateJob("+jobId+")  Not Implemented!");
    }
    
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        log.error("handleRunningJob("+jobInfo.getJobNumber()+")  Not Implemented!");
        //TODO: handle corner cases:
        // a) all of the child jobs are 'FINISHED' but the pipeline's status is still 'PROCESSING'
        // b) one of the child jobs is 'ERROR', but the pipeline's status is still 'PROCESSING'
        return JobStatus.JOB_PROCESSING;
    }
}

package org.genepattern.server.executor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.webservice.JobInfo;

/**
 * Run all GP pipelines via with this executor service.
 * 
 * @author pcarr
 */
public class PipelineExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(PipelineExecutor.class);

    //'pipeline.num.threads' from genepattern.properties
    private int numPipelines = 20;
    
    private ExecutorService executor = null;
   
    //initial implementation uses a single thread for each pipeline ... 
    //    ... using code ported from when pipelines were exectuing in separate JVM processes
    private Map<String,PipelineObj> runningPipelines = new HashMap<String,PipelineObj>();
    
    public void setConfigurationFilename(String filename) {
        // TODO Auto-generated method stub
    }

    public void setConfigurationProperties(Properties properties) {
        // TODO Auto-generated method stub
    }

    public void start() {
        String numThreadsProp = System.getProperty("num.threads", "20");
        String pipelineNumThreadsProp = System.getProperty("pipeline.num.threads", numThreadsProp);
        try {
            this.numPipelines = Integer.parseInt(pipelineNumThreadsProp);
        }
        catch (NumberFormatException e) {
            log.error("Configuration error in 'genepattern.properties': 'pipeline.num.threads="+pipelineNumThreadsProp+"'");
            this.numPipelines = 20;
        }
        log.info("Initializing pipeline executor with newFixedThreadPool("+numPipelines+") ");
        executor = Executors.newFixedThreadPool(numPipelines);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
        }
        terminateAll("stopping service");
        if (executor != null) {
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

    public void runCommand(final String[] commandLine,
            final Map<String, String> environmentVariables, 
            final File runDir,
            final File stdoutFile, 
            final File stderrFile, 
            final JobInfo jobInfo, 
            final String stdin,
            final StringBuffer stderrBuffer) 
    throws Exception {
        
        if (jobInfo == null) {
            stderrBuffer.append("null jobInfo");
            return;
        }
        final String jobId = ""+jobInfo.getJobNumber();
        PipelineCommand cmd = new PipelineCommand();
        cmd.setCommandTokens(commandLine);
        cmd.setStdoutFile(stdoutFile);
        cmd.setStderrFile(stderrFile);
        cmd.setJobInfo(jobInfo);
        cmd.setStderrBuffer(stderrBuffer);
        
        PipelineObj p = new PipelineObj(jobId, cmd);
        runningPipelines.put(jobId, p);
        p.start();
    }

    private synchronized void terminateAll(String message) {
        log.debug(message);        
        for(Entry<String, PipelineObj> entry : runningPipelines.entrySet()) {
            PipelineObj cmd = entry.getValue();
            if (cmd != null) {
                cmd.handleServerShutdown();
            }
            else {
                log.error("Unexpected null entry in runningPipelines");
            }
        }
    }

    public void terminateJob(JobInfo jobInfo) throws Exception {
        PipelineObj cmd = runningPipelines.get(""+jobInfo.getJobNumber());
        if (cmd != null) {
            cmd.terminateJob();
        }
    }
    
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        //what do do with a running pipeline on server startup ...
        //... terminate all child steps, then delete each child step
        return resetRunningPipeline(jobInfo);
    }
    
    private int resetRunningPipeline(JobInfo jobInfo) {
        if (jobInfo == null) {
            log.error("null jobInfo arg");
            return JobStatus.JOB_ERROR;
        }
        boolean isPipeline = JobInfoManager.isPipeline(jobInfo);
        if (!isPipeline) {
            log.error("job #"+jobInfo.getJobNumber()+" is not a pipeline");
            return JobStatus.JOB_ERROR;
        }
        
        //set its status to ERROR
        log.info("handling pipeline job on server restart, set status to "+JobStatus.ERROR);
        return JobStatus.JOB_ERROR;
    }
    
    //helper class to keep the PipelineCommand and its Future instance in the same hashmap
    private class PipelineObj implements Callable<Integer> {
        String jobId = null;
        private PipelineCommand cmd = null;
        private Future<Integer> future = null;
        
        public PipelineObj(String jobId, PipelineCommand cmd) {
            this.jobId = jobId;
            this.cmd = cmd;
        }
        
        public void start() throws Exception {
            try {
                future = executor.submit(this);
            }
            catch (RejectedExecutionException e) {
                log.error("pipeline #"+jobId+" was not scheduled for execution");
                runningPipelines.remove(jobId);
                throw(e);
            }
            catch (Throwable t) {
                log.error("unexpected error starting job #"+jobId, t);
                runningPipelines.remove(jobId);
            }
        }

        //Callable implementation. This runs the pipeline.
        public Integer call() throws Exception { 
            cmd.runPipeline();
            runningPipelines.remove(jobId);
            return cmd.getJobStatus();
        }
        
        /**
         * response to server shutdown request.
         */
        public void handleServerShutdown() {
            log.error("handleServerShutdown not implemented!");
        }

        /**
         * response to user request to cancel execution of the pipeline.
         * updates the job status accordingly.
         */
        public void terminateJob() {
            //immediately terminate the runpipeline thread ...
            //... clean up any running jobs ...
            //... update the job status
            
            if (future != null) {
                future.cancel(true);
            }
            if (cmd != null) {
                cmd.terminate();
            }
            runningPipelines.remove(jobId);
        }
        
    }
}

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
        String pipelineNumThreadsProp = System.getProperty("pipeline.num.threads", "20");
        try {
            this.numPipelines = Integer.parseInt(pipelineNumThreadsProp);
        }
        catch (NumberFormatException e) {
            log.error("Configuration error in 'genepattern.properties': 'pipeline.num.threads="+pipelineNumThreadsProp+"'");
            this.numPipelines = 20;
        }
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
            cmd.cancel();
        }
    }

    public void terminateJob(JobInfo jobInfo) throws Exception {
        PipelineObj cmd = runningPipelines.get(""+jobInfo.getJobNumber());
        if (cmd != null) {
            cmd.cancel();
        }
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

        //Callable imeplementation. This runs the pipeline.
        public Integer call() throws Exception { 
            cmd.runPipeline();
            runningPipelines.remove(jobId);
            return cmd.getJobStatus();
        }
  
        public void cancel() {
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

package org.genepattern.server.executor;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.domain.JobStatus;
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
public class PipelineExecutor_3_2 implements CommandExecutor {
    private static Logger log = Logger.getLogger(PipelineExecutor_3_2.class);

    //'pipeline.num.threads' from genepattern.properties
    private int numPipelines = 20;
    
    ExecutorService pipelineExecutorService = null;
    private PipelineCompletionService completionService = null;
    private ExecutorService monitorCompletionService = null;
   
    
    public void setConfigurationFilename(String filename) {
        // TODO Auto-generated method stub
    }

    public void setConfigurationProperties(Properties properties) {
        // TODO Auto-generated method stub
    }
    
    final static class PipelineCompletionService  {
        //store running pipelines in a map from jobId to future
        private Map<String,Future<PipelineCommand_3_2>> futuresByJobId = new HashMap<String,Future<PipelineCommand_3_2>>();
        private Map<Future<PipelineCommand_3_2>,String> jobIdsByFuture = new HashMap<Future<PipelineCommand_3_2>,String>();
        private Map<String,PipelineCommand_3_2> pipelineCmdsByJobId = new HashMap<String,PipelineCommand_3_2>();
        //handle special-case where a job is terminated before it is submitted
        private Set<String> prematurelyTerminatedJobs = new HashSet<String>();
        
        private ExecutorService executor = null;
        private ExecutorCompletionService<PipelineCommand_3_2> completionService = null;
        
        PipelineCompletionService(ExecutorService executor) {
            this.executor = executor;
            completionService = new ExecutorCompletionService<PipelineCommand_3_2>(this.executor);
        }
        
        public void submit(PipelineCommand_3_2 pipelineCmd) {
            String jobId = ""+pipelineCmd.getJobNumber();

            synchronized (this) {
                if (prematurelyTerminatedJobs.contains(jobId)) {
                    prematurelyTerminatedJobs.remove(jobId);
                    pipelineCmd.terminatePipelineBeforeStart();
                    return;
                }
            
                Future<PipelineCommand_3_2> future = completionService.submit(pipelineCmd);
                jobIdsByFuture.put(future, jobId);
                futuresByJobId.put(jobId, future);
                pipelineCmdsByJobId.put(jobId, pipelineCmd);
            }
        }
        
        public Wrapper wrapTake() throws InterruptedException {
            //wait for the next task to complete ...
            Future<PipelineCommand_3_2> future = completionService.take();
            
            //... remove items from hash maps ...
            String jobId = null;
            PipelineCommand_3_2 cmd = null;
            synchronized (this) {
                jobId = jobIdsByFuture.remove(future);
                if (jobId == null) {
                    jobId = "";
                    log.error("Unable to map future to jobId for running pipeline");
                }
                Future<PipelineCommand_3_2> removed = futuresByJobId.remove(jobId);
                if (removed == null || !removed.equals(future)) {
                    log.error("Unable to remove job from list of running pipelines for job #"+jobId);
                }
                cmd = pipelineCmdsByJobId.remove(jobId);
            }

            //... wrap results 
            return new Wrapper(jobId, future, cmd);
        }
        
        public void terminateJob(String jobId) {
            final Future<PipelineCommand_3_2> future;
            synchronized (this) {
                future = futuresByJobId.remove(jobId);
                if (future == null) {
                    this.prematurelyTerminatedJobs.add(jobId);
                    return;
                }
            }
            boolean cancelled = future.cancel(true);
            log.debug("cancelled job #"+jobId+":  "+cancelled);
        }

        public void terminateAllJobs(String message) {
            log.debug(message);
            for(Entry<String, Future<PipelineCommand_3_2>> entry : futuresByJobId.entrySet()) {
                terminateJob(entry.getKey());
            }
        }
        
        static class Wrapper {
            String jobId;
            Future<PipelineCommand_3_2> future;
            PipelineCommand_3_2 cmd;
            
            Wrapper(String jobId, Future<PipelineCommand_3_2> future, PipelineCommand_3_2 cmd) {
                this.jobId = jobId;
                this.future = future;
                this.cmd = cmd;
            }
            
            public String getJobId() {
                return jobId;
            }
            public Future<PipelineCommand_3_2> getFuture() {
                return future;
            }
            public PipelineCommand_3_2 getPipelineCommand() {
                return cmd;
            }
        }
    }

    /* This code requires Java 6; but makes it a lot easier to handle cancellation 
    private interface CancellableTask<T> extends Callable<T> {
        void cancel();
        RunnableFuture<T> newTask();
    }

    private static class CancellingExecutor extends ThreadPoolExecutor {
        public CancellingExecutor(int corePoolSize,
        int maximumPoolSize,
        long keepAliveTime,
        TimeUnit unit,
        BlockingQueue<Runnable> workQueue) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }
        
        protected<T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
            if (callable instanceof CancellableTask<?>) {
                return ((CancellableTask<T>) callable).newTask();
            }
            else {
                return super.newTaskFor(callable);
            }
        }
    }
    
    private abstract class PipelineTask<T> implements CancellableTask<T> {
        public synchronized void cancel() {
            
        }
        
        public RunnableFuture<T> newTask() {
            return new FutureTask<T>(this) {
                public boolean cancel(boolean mayInterruptIfRunning) {
                    try {
                        PipelineTask.this.cancel();
                    }
                    finally {
                        return super.cancel(mayInterruptIfRunning);
                    }
                }
            };
        }
    }
    */

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
        pipelineExecutorService = Executors.newFixedThreadPool(numPipelines);
        completionService = new PipelineCompletionService(pipelineExecutorService);
        
        //TODO: include this code in the PipelineCompletionService class
        // ... e.g. PipelineCompletionServer#startHandlingJobCompletion()
        monitorCompletionService = Executors.newSingleThreadExecutor(); 
        monitorCompletionService.execute(new Runnable() {
            public void run() {
                try {
                    while(true) {
                        PipelineCompletionService.Wrapper wrapper = completionService.wrapTake();
                        String jobId = wrapper.getJobId();
                        Future<PipelineCommand_3_2> future = wrapper.getFuture();
                        PipelineCommand_3_2 cmd = wrapper.getPipelineCommand();
                        try {
                            PipelineCommand_3_2 result = future.get();
                            log.debug("job #"+jobId+" completed!"+
                                    " wrapper.cmd.jobNumber="+cmd.getJobNumber()+
                                    " result.jobNumber="+result.getJobNumber());
                        }
                        catch (ExecutionException e) {
                            //here is where we call GenePatternAnalysisTask.handleJobCompletion
                            log.debug("job #"+jobId+" threw unhandled ExecutionException", e);
                        }
                        catch (CancellationException e) {
                            log.debug("job #"+jobId+" was cancelled");
                        }
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void stop() {
        if (pipelineExecutorService != null) {
            pipelineExecutorService.shutdown();
        }
        terminateAll("stopping service");
        if (monitorCompletionService != null) {
            monitorCompletionService.shutdown();
        }
        if (pipelineExecutorService != null) {
            pipelineExecutorService.shutdown();
            try {
                if (!pipelineExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("executor shutdown timed out after 30 seconds.");
                    pipelineExecutorService.shutdownNow();
                }
            }
            catch (InterruptedException e) {
                log.error("executor.shutdown was interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        if (monitorCompletionService != null) {
            monitorCompletionService.shutdown();
            monitorCompletionService.shutdownNow();
        }
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
        
        PipelineCommand_3_2 cmd = new PipelineCommand_3_2();
        cmd.setCommandTokens(commandLine);
        cmd.setStdoutFile(stdoutFile);
        cmd.setStderrFile(stderrFile);
        cmd.setJobInfo(jobInfo);
        completionService.submit(cmd);
    }
    
    private synchronized void terminateAll(String message) {
        completionService.terminateAllJobs(message);
    }
    
    public void terminateJob(JobInfo jobInfo) throws Exception {
        String jobId = ""+jobInfo.getJobNumber();
        completionService.terminateJob(jobId);
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
        log.info("terminating pipeline job #"+jobInfo.getJobNumber()+" on server restart");
        return JobStatus.JOB_ERROR;
    }
    
}

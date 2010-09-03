package org.genepattern.server.executor.pipeline;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.CommandExecutor;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.GenePatternAnalysisTask.JOB_TYPE;
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
    private int numPipelines = 20;
    
    //ExecutorService pipelineExecutorService = null;
    //private PipelineCompletionService completionService = null;
    //private ExecutorService monitorCompletionService = null;
   
    
    public void setConfigurationFilename(String filename) {
        // TODO Auto-generated method stub
    }

    public void setConfigurationProperties(Properties properties) {
        // TODO Auto-generated method stub
    }
    
//    final static class PipelineCompletionService  {
//        //store running pipelines in a map from jobId to future
//        private Map<String,Future<PipelineCommand2>> futuresByJobId = new HashMap<String,Future<PipelineCommand2>>();
//        private Map<Future<PipelineCommand2>,String> jobIdsByFuture = new HashMap<Future<PipelineCommand2>,String>();
//        private Map<String,PipelineCommand2> pipelineCmdsByJobId = new HashMap<String,PipelineCommand2>();
//        //handle special-case where a job is terminated before it is submitted
//        private Set<String> prematurelyTerminatedJobs = new HashSet<String>();
//        
//        private ExecutorService executor = null;
//        private ExecutorCompletionService<PipelineCommand2> completionService = null;
//        
//        PipelineCompletionService(ExecutorService executor) {
//            this.executor = executor;
//            completionService = new ExecutorCompletionService<PipelineCommand2>(this.executor);
//        }
//        
//        public void submit(PipelineCommand2 pipelineCmd) {
//            String jobId = ""+pipelineCmd.getJobNumber();
//
//            synchronized (this) {
//                if (prematurelyTerminatedJobs.contains(jobId)) {
//                    prematurelyTerminatedJobs.remove(jobId);
//                    pipelineCmd.terminatePipelineBeforeStart();
//                    return;
//                }
//            
//                Future<PipelineCommand2> future = completionService.submit(pipelineCmd);
//                jobIdsByFuture.put(future, jobId);
//                futuresByJobId.put(jobId, future);
//                pipelineCmdsByJobId.put(jobId, pipelineCmd);
//            }
//        }
//        
//        public Wrapper wrapTake() throws InterruptedException {
//            //wait for the next task to complete ...
//            Future<PipelineCommand2> future = completionService.take();
//            
//            //... remove items from hash maps ...
//            String jobId = null;
//            PipelineCommand2 cmd = null;
//            synchronized (this) {
//                jobId = jobIdsByFuture.remove(future);
//                if (jobId == null) {
//                    jobId = "";
//                    log.error("Unable to map future to jobId for running pipeline");
//                }
//                Future<PipelineCommand2> removed = futuresByJobId.remove(jobId);
//                if (removed == null || !removed.equals(future)) {
//                    log.error("Unable to remove job from list of running pipelines for job #"+jobId);
//                }
//                cmd = pipelineCmdsByJobId.remove(jobId);
//            }
//
//            //... wrap results 
//            return new Wrapper(jobId, future, cmd);
//        }
//        
//        public void terminateJob(String jobId) {
//            final Future<PipelineCommand2> future;
//            synchronized (this) {
//                future = futuresByJobId.remove(jobId);
//                if (future == null) {
//                    this.prematurelyTerminatedJobs.add(jobId);
//                    return;
//                }
//            }
//            boolean cancelled = future.cancel(true);
//            log.debug("cancelled job #"+jobId+":  "+cancelled);
//        }
//
//        public void terminateAllJobs(String message) {
//            log.debug(message);
//            for(Entry<String, Future<PipelineCommand2>> entry : futuresByJobId.entrySet()) {
//                terminateJob(entry.getKey());
//            }
//        }
//        
//        static class Wrapper {
//            String jobId;
//            Future<PipelineCommand2> future;
//            PipelineCommand2 cmd;
//            
//            Wrapper(String jobId, Future<PipelineCommand2> future, PipelineCommand2 cmd) {
//                this.jobId = jobId;
//                this.future = future;
//                this.cmd = cmd;
//            }
//            
//            public String getJobId() {
//                return jobId;
//            }
//            public Future<PipelineCommand2> getFuture() {
//                return future;
//            }
//            public PipelineCommand2 getPipelineCommand() {
//                return cmd;
//            }
//        }
//    }

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
//        String numThreadsProp = System.getProperty("num.threads", "20");
//        String pipelineNumThreadsProp = System.getProperty("pipeline.num.threads", numThreadsProp);
//        try {
//            this.numPipelines = Integer.parseInt(pipelineNumThreadsProp);
//        }
//        catch (NumberFormatException e) {
//            log.error("Configuration error in 'genepattern.properties': 'pipeline.num.threads="+pipelineNumThreadsProp+"'");
//            this.numPipelines = 20;
//        }
//        
//        log.info("Initializing pipeline executor with newFixedThreadPool("+numPipelines+") ");
//        pipelineExecutorService = Executors.newFixedThreadPool(numPipelines);
//        completionService = new PipelineCompletionService(pipelineExecutorService);
//        
//        //TODO: include this code in the PipelineCompletionService class
//        // ... e.g. PipelineCompletionServer#startHandlingJobCompletion()
//        monitorCompletionService = Executors.newSingleThreadExecutor(); 
//        monitorCompletionService.execute(new Runnable() {
//            public void run() {
//                try {
//                    while(true) {
//                        PipelineCompletionService.Wrapper wrapper = completionService.wrapTake();
//                        String jobId = wrapper.getJobId();
//                        Future<PipelineCommand2> future = wrapper.getFuture();
//                        PipelineCommand2 cmd = wrapper.getPipelineCommand();
//                        try {
//                            PipelineCommand2 result = future.get();
//                            log.debug("job #"+jobId+" completed!"+
//                                    " wrapper.cmd.jobNumber="+cmd.getJobNumber()+
//                                    " result.jobNumber="+result.getJobNumber());
//                        }
//                        catch (ExecutionException e) {
//                            //here is where we call GenePatternAnalysisTask.handleJobCompletion
//                            log.debug("job #"+jobId+" threw unhandled ExecutionException", e);
//                        }
//                        catch (CancellationException e) {
//                            log.debug("job #"+jobId+" was cancelled");
//                        }
//                    }
//                }
//                catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        });
    }

    public void stop() {
//        if (pipelineExecutorService != null) {
//            pipelineExecutorService.shutdown();
//        }
//        terminateAll("stopping service");
//        if (monitorCompletionService != null) {
//            monitorCompletionService.shutdown();
//        }
//        if (pipelineExecutorService != null) {
//            pipelineExecutorService.shutdown();
//            try {
//                if (!pipelineExecutorService.awaitTermination(30, TimeUnit.SECONDS)) {
//                    log.error("executor shutdown timed out after 30 seconds.");
//                    pipelineExecutorService.shutdownNow();
//                }
//            }
//            catch (InterruptedException e) {
//                log.error("executor.shutdown was interrupted", e);
//                Thread.currentThread().interrupt();
//            }
//        }
//        if (monitorCompletionService != null) {
//            monitorCompletionService.shutdown();
//            monitorCompletionService.shutdownNow();
//        }
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
            PipelineHandler handler = new LegacyPipelineHandler();
            handler.startPipeline(commandLine, jobInfo, Integer.MAX_VALUE);
        }
        catch (Exception e) {
            throw new CommandExecutorException("Server error starting pipeline job #"+jobInfo.getJobNumber(), e);
        }
        //PipelineCommand2 cmd = new PipelineCommand2();
        //cmd.setCommandTokens(commandLine);
        //cmd.setStdoutFile(stdoutFile);
        //cmd.setStderrFile(stderrFile);
        //cmd.setJobInfo(jobInfo);
        //completionService.submit(cmd);
    }
    
    private synchronized void terminateAll(String message) {
        //completionService.terminateAllJobs(message);
    }
    
    public void terminateJob(JobInfo jobInfo) throws Exception {
        //TODO: implement terminate for the pipeline
        String jobId = ""+jobInfo.getJobNumber();
        //completionService.terminateJob(jobId);
    }
    
    public int handleRunningJob(JobInfo jobInfo) throws Exception {
        //TODO: implement startup for pipeline
        return JobStatus.JOB_PROCESSING;
        
        //what do do with a running pipeline on server startup ...
        //... terminate all child steps, then delete each child step
        //return resetRunningPipeline(jobInfo);
    }
    
//    private int resetRunningPipeline(JobInfo jobInfo) {
//        if (jobInfo == null) {
//            log.error("null jobInfo arg");
//            return JobStatus.JOB_ERROR;
//        }
//        boolean isPipeline = JobInfoManager.isPipeline(jobInfo);
//        if (!isPipeline) {
//            log.error("job #"+jobInfo.getJobNumber()+" is not a pipeline");
//            return JobStatus.JOB_ERROR;
//        }
//        
//        //set its status to ERROR
//        log.info("terminating pipeline job #"+jobInfo.getJobNumber()+" on server restart");
//        return JobStatus.JOB_ERROR;
//    }
    
}

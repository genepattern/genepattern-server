package org.genepattern.server.executor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.RuntimeExecCommand.Status;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.webservice.JobInfo;

public class RuntimeCommandExecutor implements CommandExecutor {
    private static Logger log = Logger.getLogger(RuntimeCommandExecutor.class);
    
    private ExecutorService executor = null;

    //the total number of jobs which should be executing concurrently
    //TODO: enable setting this without requiring a server restart;
    //    at the moment you must restart your GP server to modify this setting
    final int defaultNumThreads = 20;
    private int numThreads = defaultNumThreads;
    private String numThreadsProp = null; //set in configuration.properties
    
    public void setConfigurationFilename(String filename) {
        log.error("ignoring: setConfigurationFilename("+filename+"): must set configuration.properties directly in the job configuration file!");
    }

    public void setConfigurationProperties(CommandProperties properties) {
        numThreadsProp = properties.getProperty("num.threads");
    }
    
    private void initNumThreads() {  
        //1) load 'num.threads' from the 'configuration.properties' section of the job configuration file
        if (numThreadsProp != null) {
            log.info("reading 'num.threads="+numThreadsProp+"' from configuration.properties");
            try {
                numThreadsProp = numThreadsProp.trim();
                numThreads = Integer.parseInt(numThreadsProp);
                return;
            }
            catch (NumberFormatException e) {
                log.error("Error in 'configuration.properties', num.threads="+numThreadsProp, e);
            }
            numThreads = defaultNumThreads;
            return;
        }

        //2) if not set in configuration.properties, load from the 'genepattern.properties' file (via System.properties)
        numThreadsProp = System.getProperty("num.threads");
        if (numThreadsProp != null) {
            log.info("reading 'num.threads="+numThreadsProp+"' from genepattern.properties");
            try {
                numThreadsProp = numThreadsProp.trim();
                numThreads = Integer.parseInt(numThreadsProp);
                return;
            }
            catch (NumberFormatException e) {
                log.error("Error in 'genepattern.properties', num.threads="+numThreadsProp, e);
            }
            numThreads = defaultNumThreads;
            return;
        }
        
        //3) otherwise use the hard-coded default
        numThreads = defaultNumThreads;
    }
    
    //----- keep references to all currently running jobs which were started by this executor
    private Map<String,CallableRuntimeExecCommand> runningJobs = new ConcurrentHashMap<String,CallableRuntimeExecCommand>();

    public void start() {
        initNumThreads();
        log.info("Initializing runtime command executor with newFixedThreadPool("+numThreads+") ");
        executor = Executors.newFixedThreadPool(numThreads);
    }

    public void stop() {
        executor.shutdown();
        terminateAll("--> Shutting down server");
        
        if (executor != null) {
            log.debug("stopping executor...");
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
    
    private void terminateAll(String message) {
        log.debug(message);
        for(Entry<String, CallableRuntimeExecCommand> entry : runningJobs.entrySet()) {
            CallableRuntimeExecCommand cmd = entry.getValue();
            cmd.cancel(Status.SERVER_SHUTDOWN);
            Thread.yield();
        }
    }

    public void runCommand(String[] commandLine, Map<String, String> environmentVariables, 
            File runDir, File stdoutFile, File stderrFile, JobInfo jobInfo, File stdinFile) 
    throws CommandExecutorException 
    { 
        String jobId = ""+jobInfo.getJobNumber();
        try {
            logCommandLine(commandLine, runDir, jobInfo);
        }
        catch (Throwable t) {
            log.error("server configuration error, logging command line: "+t.getLocalizedMessage(), t);
            throw new CommandExecutorException("job #"+jobId+" was not started", t);
        }
        
        CallableRuntimeExecCommand task = new CallableRuntimeExecCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile);
        runningJobs.put(jobId, task);
        try {
            Future<?> f = executor.submit(task);
            task.setFuture(f);
        }
        catch (RejectedExecutionException e) {
            //TODO: when the queue is full, reset the job status back to PENDING
            runningJobs.remove(jobId);
            throw new CommandExecutorException("job #"+jobId+" was not started", e);
        }
        catch (Throwable t) {
            log.error("unexpected error starting job #"+jobId, t);
            runningJobs.remove(jobId);
        }
    }

    public void terminateJob(JobInfo jobInfo) {
        if (jobInfo == null) {
            log.error("null jobInfo");
            return;
        }
        String jobId = ""+jobInfo.getJobNumber();
        CallableRuntimeExecCommand cmd = runningJobs.get(jobId);
        if (cmd == null) {
            //terminateJob is called from deleteJob, so quite often terminateJob should have no effect
            log.debug("terminateJob("+jobInfo.getJobNumber()+"): job not running");
            return;
        }
        cmd.cancel(Status.TERMINATED);
    }
    
    public int handleRunningJob(JobInfo jobInfo) {
        if (JobStatus.PROCESSING.equals(jobInfo.getStatus())) {
            return JobStatus.JOB_PENDING;
        }
        return -1;
    }
    
    /**
     * If configured by the server admin, write the command line into a log file in the working directory for the job.
     * <pre>
     *     # flag, if true save the command line into a log file in the working directory for each job
           rte.save.logfile: false
           # the name of the command line log file
           rte.logfile: .rte.out
     * </pre>
     * 
     * @author pcarr
     */
    private void logCommandLine(String[] commandLine, File runDir, JobInfo jobInfo) {
        GpContext jobContext = GpContext.getContextForJob(jobInfo);

        boolean saveLogFile = ServerConfigurationFactory.instance().getGPBooleanProperty(jobContext, "rte.save.logfile", false);
        if (!saveLogFile) {
            return;
        }
        log.debug("saving command line to log file ...");
        String commandLineStr = "";
        boolean first = true;
        for(String arg : commandLine) {
            if (first) {
                commandLineStr = arg;
                first = false;
            }
            else {
                commandLineStr += (" "+arg);
            }
        }

        String filename = ServerConfigurationFactory.instance().getGPProperty(jobContext, "rte.logfile", ".rte.out");
        File commandLogFile = new File(runDir, filename);
        if (commandLogFile.exists()) {
            log.error("log file already exists: "+commandLogFile.getAbsolutePath());
            return;
        }

        BufferedWriter bw = null;
        try {
            FileWriter fw = new FileWriter(commandLogFile);
            bw = new BufferedWriter(fw);
            bw.write(commandLineStr);
            bw.newLine();
            for(int i=0; i<commandLine.length; ++i) {
                bw.write("    arg["+i+"]: '"+commandLine[i]+"'");
                bw.newLine();
            }
            bw.close();
        }
        catch (IOException e) {
            log.error("error writing log file: "+commandLogFile.getAbsolutePath(), e);
            return;
        }
        catch (Throwable t) {
            log.error("error writing log file: "+commandLogFile.getAbsolutePath(), t);
            log.error(t);
        }
        finally {
            if (bw != null) {
                try {
                    bw.close();
                }
                catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }
    
    private class CallableRuntimeExecCommand implements Callable<RuntimeExecCommand> 
    {
        private String[] commandLine = null;
        private Map<String, String> environmentVariables = null; 
        private File runDir = null;
        private File stdoutFile = null;
        private File stderrFile = null;
        private JobInfo jobInfo = null;
        private File stdinFile = null;
        
        private RuntimeExecCommand cmd = null;
        
        private Future<?> future = null;
        public void setFuture(Future<?> f) {
            this.future = f;
        }

        public CallableRuntimeExecCommand(
                String[] commandLine,
                Map<String, String> environmentVariables, 
                File runDir,
                File stdoutFile, 
                File stderrFile, 
                JobInfo jobInfo, 
                File stdinFile) {
            this.commandLine = commandLine;
            this.environmentVariables = environmentVariables;
            this.runDir = runDir;
            this.stdoutFile = stdoutFile;
            this.stderrFile = stderrFile;
            this.jobInfo = jobInfo;
            this.stdinFile = stdinFile;
        }

        public RuntimeExecCommand call() throws Exception {
            cmd = new RuntimeExecCommand();
            cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile);
            String jobId = ""+jobInfo.getJobNumber();
            runningJobs.remove(jobId);
            int exitValue = cmd.getExitValue();
            if (RuntimeExecCommand.Status.TERMINATED.equals( cmd.getInternalJobStatus() )) {
                if (exitValue == 0) {
                    exitValue = -1;
                }
            }
            
            if (RuntimeExecCommand.Status.SERVER_SHUTDOWN.equals( cmd.getInternalJobStatus() )) {
                //don't handle job completion
            }
            else {
                //handle stderr messages stored in the cmd object
                String errorMessage = cmd.getStderr();
                int exitCode = cmd.getExitValue();
                try {
                    GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), exitCode, errorMessage, runDir, stdoutFile, stderrFile);
                }
                catch (Throwable t) {
                    log.error("Error handling job completion for job "+jobInfo.getJobNumber(), t);
                }
            }
            return cmd;
        }
        
        //if updateJobStatus 
        public void cancel(Status status) {
            if (cmd != null) {
                cmd.terminateProcess(status);
            }
            else {
                //special-case: job has not started
                if (future != null) {
                    future.cancel(false);
                }
                
                if (status == Status.TERMINATED) {
                    try {
                        String errorMessage = "Job #"+jobInfo.getJobNumber()+" terminated by user";
                        int exitCode = -1;
                        GenePatternAnalysisTask.handleJobCompletion(jobInfo.getJobNumber(), exitCode, errorMessage, runDir, stdoutFile, stderrFile);
                    }
                    catch (Throwable t) {
                        log.error("Error terminating job "+jobInfo.getJobNumber(), t);
                    }
                }
            }
        }
    }
}

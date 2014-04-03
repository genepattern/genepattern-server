package org.genepattern.drm.impl.local;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.RuntimeExecCommand;

/**
 * An implementation of the JobRunner interface for local execution.
 * 
 * To use this, add the following to the config_.yaml file for your server.
 * 
 * <pre>
 * LocalJobRunner:
        classname: org.genepattern.server.executor.drm.JobExecutor
        configuration.properties:
            jobRunnerClassname: org.genepattern.drm.impl.local.LocalJobRunner
            jobRunnerName: LocalJobRunner
            lookupType: DB
            #lookupType: HASHMAP
            # when 'logFilename' is set the command line args will be logged to the file
            logFilename: .rte.out
 * </pre>
 * 
 * @author pcarr
 *
 */
public class LocalJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(LocalJobRunner.class);

    //map of jobId to task
    private final int numThreads=20;
    private Map<String, Future<DrmJobStatus>> runningTasks;
    private ExecutorService executor;
    
    public LocalJobRunner() {
        runningTasks=new HashMap<String, Future<DrmJobStatus>>();
        executor = Executors.newFixedThreadPool(numThreads);
    }
    
    @Override
    public void stop() {
        executor.shutdownNow();
    }

    @Override
    public String startJob(final DrmJobSubmission drmJobSubmission) throws CommandExecutorException {
        final String drmJobId=""+drmJobSubmission.getGpJobNo();
        final Future<DrmJobStatus> future=executor.submit(new Callable<DrmJobStatus>() {
            @Override
            public DrmJobStatus call() throws Exception {
                RuntimeExecCommand cmd = new RuntimeExecCommand();
                final String[] commandLine=drmJobSubmission.getCommandLine().toArray(new String[0]);
                cmd.runCommand(commandLine, 
                        drmJobSubmission.getEnvironmentVariables(), 
                        drmJobSubmission.getWorkingDir(), 
                        drmJobSubmission.getStdoutFile(), 
                        drmJobSubmission.getStderrFile(), 
                        drmJobSubmission.getJobInfo(), 
                        drmJobSubmission.getStdinFile());
                final DrmJobStatus drmJobStatus;
                
                //after the job completes, if the logfile param was set, write the command line to the logfile
                logCommandLine(drmJobSubmission);
                    
                if (cmd.getExitValue()==0) {
                    drmJobStatus=new DrmJobStatus.Builder(drmJobId, DrmJobState.DONE).exitCode(cmd.getExitValue()).build();
                }
                else {
                    drmJobStatus=new DrmJobStatus.Builder(drmJobId, DrmJobState.FAILED).exitCode(cmd.getExitValue()).build();
                }
                return drmJobStatus;                
            }
        });
        runningTasks.put(drmJobId, future);
        return drmJobId;
    }

    @Override
    public DrmJobStatus getStatus(final DrmJobRecord drmJobRecord) {
        final String drmJobId=drmJobRecord.getExtJobId();
        final Future<DrmJobStatus> task=runningTasks.get(drmJobId);
        if (task==null) {
            //TODO: could look at the file system for evidence of job completion
            DrmJobStatus drmJobStatus=new DrmJobStatus.Builder(drmJobId, DrmJobState.UNDETERMINED).build();
            return drmJobStatus;
        }
        
        if (!task.isDone()) {
            DrmJobStatus drmJobStatus=new DrmJobStatus.Builder(drmJobId, DrmJobState.RUNNING).build();
            return drmJobStatus;
        }

        if (task.isCancelled()) {
            return new DrmJobStatus.Builder(drmJobId, DrmJobState.CANCELLED)
                .jobStatusMessage("Job was terminated by user")
                .exitCode(-1).build();
        }
        try {
            return task.get();
        }
        catch (InterruptedException e) {
            log.error(e);
            Thread.currentThread().interrupt();
            return new DrmJobStatus.Builder(drmJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
        catch (Exception e) {
            log.error(e);
            return new DrmJobStatus.Builder(drmJobId, DrmJobState.UNDETERMINED).build();
        }
    }

    @Override
    public boolean cancelJob(final DrmJobRecord jobRecord) throws Exception {
        final Future<DrmJobStatus> task=runningTasks.get(jobRecord.getExtJobId());
        if (task != null) {
            final boolean mayInterruptIfRunning=true;
            final boolean success=task.cancel(mayInterruptIfRunning);
            log.debug("success="+success);
            return success;
        }
        log.debug("No entry in 'runningTasks' map for drmJobId="+jobRecord.getExtJobId());
        //TODO: job may have already completed ... successfully or already cancelled successfully,
        return false;
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
    private void logCommandLine(final DrmJobSubmission drmJobSubmission) {
        if (drmJobSubmission.getLogFile()==null) {
            // a null logfile means "don't write the log file"
            return;
        }
        
        final File commandLogFile;
        if (!drmJobSubmission.getLogFile().isAbsolute()) {
            //relative path is relative to the working directory for the job
            commandLogFile=new File(drmJobSubmission.getWorkingDir(), drmJobSubmission.getLogFile().getPath());
        }
        else {
            commandLogFile=drmJobSubmission.getLogFile();
        }
        
        log.debug("saving command line to log file ...");
        String commandLineStr = "";
        boolean first = true;
        for(final String arg : drmJobSubmission.getCommandLine()) {
            if (first) {
                commandLineStr = arg;
                first = false;
            }
            else {
                commandLineStr += (" "+arg);
            }
        }

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
            int i=0;
            for(final String arg : drmJobSubmission.getCommandLine()) {
                bw.write("    arg["+i+"]: '"+arg+"'");
                bw.newLine();
                ++i;
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

}


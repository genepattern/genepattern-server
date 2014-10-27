package org.genepattern.drm.impl.local.commons_exec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.CommandProperties;

/**
 * An implementation of a local job runner using the Apache Commons Exec package.
 * Example config_yaml entry:
 * <pre>
    LocalJobRunner:
        classname: org.genepattern.server.executor.drm.JobExecutor
        configuration.properties:
            jobRunnerClassname: org.genepattern.drm.impl.local.commons_exec.LocalCommonsExecJobRunner
            jobRunnerName: CommonsExecJobRunner
            logFilename: ".rte.out"
 * </pre>
 * @author pcarr
 *
 */
public class LocalCommonsExecJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(LocalCommonsExecJobRunner.class);

    private long getPendingInterval() {
        return 0L;
        // for debugging, keep jobs in the pending state for a little while
        //return 5L+ (Long) Math.round((60.0*1000.0*Math.random()));
    }

    private int numThreads=-1;
    
    public void setCommandProperties(CommandProperties properties) {
        log.debug("setCommandProperties");
        String numThreadsProp=null;
        if (properties != null) {
            numThreadsProp=properties.getProperty("num.threads");
        }
        if (numThreadsProp != null) {
            try {
                this.numThreads = Integer.parseInt(numThreadsProp);
                log.debug("numThreads="+numThreads);
            }
            catch (Throwable t) {
                log.error("Error parsing num.threads="+numThreadsProp);
            }
        }
    }

    public void start() {
        log.debug("started JobRunner, classname="+this.getClass());
    }

    private ConcurrentMap<Integer,DrmJobStatus> statusMap=new ConcurrentHashMap<Integer, DrmJobStatus>();
    private ConcurrentMap<Integer,Executor> execMap=new ConcurrentHashMap<Integer, Executor>();
    
    private ExecutorService pendingExec=null;
    private ConcurrentMap<Integer,Future<?>> pendingMap=new ConcurrentHashMap<Integer, Future<?>>();
    
    // more accurate reporting a user-cancelled tasks
    private Set<Integer> cancelledJobs = new HashSet<Integer>();
    
    private void initStatus(DrmJobSubmission gpJob) {
        DrmJobStatus status = new DrmJobStatus.Builder(""+gpJob.getGpJobNo(), DrmJobState.QUEUED)
            .submitTime(new Date())
        .build();
        statusMap.put(gpJob.getGpJobNo(), status);
    }
    
    private void updateStatus_startJob(DrmJobSubmission gpJob) {
        DrmJobStatus updated = new DrmJobStatus.Builder(statusMap.get(gpJob.getGpJobNo()))
            .startTime(new Date())
            .jobState(DrmJobState.RUNNING)
        .build();
        statusMap.put(gpJob.getGpJobNo(), updated);
    }
    
    private void updateStatus_complete(int gpJobNo, int exitCode, ExecuteException exception) {
        DrmJobStatus status = statusMap.get(gpJobNo);
        DrmJobStatus.Builder b;
        if (status == null) {
            log.error("Unexpected null status for gpJobNo="+gpJobNo);
            b = new DrmJobStatus.Builder().extJobId(""+gpJobNo);
        }
        else {
            b = new DrmJobStatus.Builder(status);
        }
        b.exitCode(exitCode);
        if (exitCode==0 && exception==null) {
            b.jobState(DrmJobState.DONE);
        }
        else {
            b.jobState(DrmJobState.FAILED);
        }
        b.endTime(new Date());
        statusMap.put(gpJobNo, b.build());
    }
    
    private DrmJobStatus updateStatus_cancel(int gpJobNo, boolean isPending) {
        cancelledJobs.add(gpJobNo);
        log.debug("updateStatus_cancel, gpJobNo="+gpJobNo+", isPending="+isPending);
        DrmJobStatus status = statusMap.get(gpJobNo);
        DrmJobStatus.Builder b;
        if (status==null) {
            log.error("Unexpected null status for gpJobNo="+gpJobNo);
            b = new DrmJobStatus.Builder().extJobId(""+gpJobNo);
            b.jobState(DrmJobState.UNDETERMINED);
        }
        else {
            b = new DrmJobStatus.Builder(status);
            if (status.getJobState().is(DrmJobState.IS_QUEUED)) {
                b.jobState( DrmJobState.ABORTED );
            }
            else {
                b.jobState( DrmJobState.CANCELLED );
            }
        }
        b.exitCode(-1); // hard-code exitCode for user-cancelled task
        b.endTime(new Date());
        b.jobStatusMessage("Job cancelled by user");
        return statusMap.put(gpJobNo, b.build());
    }

    boolean shuttingDown=false;
    @Override
    public void stop() {
        log.debug("shutting down ...");
        shuttingDown=true;
        if (pendingExec != null) {
            pendingExec.shutdownNow();
        }
        for(final Executor exec : execMap.values()) {
            exec.getWatchdog().destroyProcess();
            exec.getWatchdog().stop();
        }
    }

    @Override
    public String startJob(DrmJobSubmission gpJob) throws CommandExecutorException {
        try {
            initStatus(gpJob);
            final long pending_interval_ms=getPendingInterval();
            if (pending_interval_ms > 0L) {
                Future<?> f=sleepThenStart(pending_interval_ms, gpJob);
                pendingMap.put(gpJob.getGpJobNo(), f);
            }
            else {
                Executor exec=runJobNoWait(gpJob);
                execMap.put(gpJob.getGpJobNo(), exec);
                updateStatus_startJob(gpJob);
            }
        }
        catch (Throwable t) {
            throw new CommandExecutorException("Error starting job: "+gpJob.getGpJobNo(), t);
        }
        return ""+gpJob.getGpJobNo();
    }
    
    private Future<?> sleepThenStart(final long pending_interval_ms, final DrmJobSubmission gpJob) {
        if (pendingExec==null) {
            pendingExec=Executors.newSingleThreadExecutor();
        }
        Future<?> f = pendingExec.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(pending_interval_ms);
                }
                catch (InterruptedException e) {
                    // Restore the interrupted status
                    Thread.currentThread().interrupt();
                    return;
                }
                try {
                    Executor exec = runJobNoWait(gpJob);
                    execMap.put(gpJob.getGpJobNo(), exec);
                    updateStatus_startJob(gpJob);
                }
                catch (ExecutionException e) {
                    updateStatus_complete(gpJob.getGpJobNo(), -1, null);
                }
                catch (IOException e) {
                    updateStatus_complete(gpJob.getGpJobNo(), -1, null);
                }
            }
        });
        return f;
    }

    @Override
    public DrmJobStatus getStatus(DrmJobRecord jobRecord) {
        DrmJobStatus jobStatus=statusMap.get(jobRecord.getGpJobNo());
        if (jobStatus != null) {
            return jobStatus;
        }
        // special-case: job was terminated in a previous GP instance
        return new DrmJobStatus.Builder()
                .extJobId(jobRecord.getExtJobId())
                .jobState(DrmJobState.UNDETERMINED)
                .jobStatusMessage("No record for job, assuming it was terminated at shutdown of GP server")
            .build();
    }

    @Override
    public boolean cancelJob(DrmJobRecord drmJobRecord) throws Exception {
        log.debug("cancelJob, gpJobNo="+drmJobRecord.getGpJobNo());
        boolean isPending=false;
        Future<?> f=pendingMap.remove(drmJobRecord.getGpJobNo());
        if (f != null) {
            //assume it's a pending job
            isPending=true;
            boolean mayInterruptIfRunning=true;
            f.cancel(mayInterruptIfRunning);
        }
        updateStatus_cancel(drmJobRecord.getGpJobNo(), isPending);
        Executor exec=execMap.remove(drmJobRecord.getGpJobNo());
        if (exec != null) {
            exec.getWatchdog().destroyProcess();
        }
        return true;
    }
    
    private class CmdResultHandler extends DefaultExecuteResultHandler {
        private int gpJobNo;
        public CmdResultHandler(final int gpJobNo) {
            this.gpJobNo=gpJobNo;
        }
        
        @Override
        public void onProcessComplete(final int exitValue) {
            if (!shuttingDown) {
                super.onProcessComplete(exitValue);
                updateStatus_complete(gpJobNo, exitValue, null);
            }
        }

        @Override
        public void onProcessFailed(final ExecuteException e) {
            if (!shuttingDown) {
                super.onProcessFailed(e);
                if (cancelledJobs.contains(gpJobNo)) {
                    boolean isPending=false;
                    updateStatus_cancel(gpJobNo, isPending);
                }
                else {
                    updateStatus_complete(gpJobNo, e.getExitValue(), e);
                }
            }
        }
    }

    private CommandLine initCommand(final DrmJobSubmission gpJob) {
        boolean handleQuoting=false;
        List<String> gpCommand = gpJob.getCommandLine();
        CommandLine cl=new CommandLine(gpCommand.get(0));
        for(int i=1; i<gpCommand.size(); ++i) {
            cl.addArgument(gpCommand.get(i), handleQuoting);
        }
        return cl;
    }

    private Executor initExecutorForJob(final DrmJobSubmission gpJob) throws ExecutionException, IOException {
        File outfile = gpJob.getRelativeFile(gpJob.getStdoutFile());
        File errfile = gpJob.getRelativeFile(gpJob.getStderrFile());
        File infile = gpJob.getRelativeFile(gpJob.getStdinFile());
        final PumpStreamHandler pumpStreamHandler;
        if (infile != null) {
            pumpStreamHandler = new PumpStreamHandler( 
                new FileOutputStream(outfile),
                new FileOutputStream(errfile),
                new FileInputStream(infile));
        }
        else {
            pumpStreamHandler = new PumpStreamHandler( 
                new FileOutputStream(outfile),
                new FileOutputStream(errfile));
        }
        
        final ExecuteWatchdog watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        final ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
        
        DefaultExecutor exec=new DefaultExecutor();
        exec.setWorkingDirectory(gpJob.getWorkingDir());
        exec.setStreamHandler( pumpStreamHandler );
        exec.setWatchdog(watchDog);
        exec.setProcessDestroyer(processDestroyer);
        return exec;
    }

    private Executor runJobNoWait(final DrmJobSubmission gpJob) throws ExecutionException, IOException {
        Executor exec=initExecutorForJob(gpJob);
        CommandLine cl = initCommand(gpJob);
        final CmdResultHandler resultHandler=new CmdResultHandler(gpJob.getGpJobNo());
        exec.execute(cl, resultHandler);
        
        return exec;
    }
    
    private void runJobAndWait(final DrmJobSubmission gpJob) throws InterruptedException, ExecutionException, IOException {
        Executor exec=initExecutorForJob(gpJob);
        CommandLine cl = initCommand(gpJob);
        final CmdResultHandler resultHandler=new CmdResultHandler(gpJob.getGpJobNo());
        exec.execute(cl, resultHandler);
        resultHandler.waitFor();
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

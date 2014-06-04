package org.genepattern.drm.impl.local.commons_exec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
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

    private ConcurrentMap<Integer,DrmJobStatus> statusMap=new ConcurrentHashMap<Integer, DrmJobStatus>();
    private ConcurrentMap<Integer,Executor> execMap=new ConcurrentHashMap<Integer, Executor>();
    
    //for debugging, keep jobs in the pending state for a little while
    private static final long pending_interval_ms=0L;
    //private static final long pending_interval_ms=30L*1000L;
    private ExecutorService pendingExec=null;
    
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

    @Override
    public void stop() {
        log.debug("shutting down ...");
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
            if (pending_interval_ms > 0L) {
                sleepThenStart(gpJob);
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
    
    private void sleepThenStart(final DrmJobSubmission gpJob) {
        if (pendingExec==null) {
            pendingExec=Executors.newSingleThreadExecutor();
        }
        pendingExec.submit(new Runnable() {
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
    }

    @Override
    public DrmJobStatus getStatus(DrmJobRecord drmJobRecord) {
        return statusMap.get(drmJobRecord.getGpJobNo());
    }

    @Override
    public boolean cancelJob(DrmJobRecord drmJobRecord) throws Exception {
        Executor exec=execMap.remove(drmJobRecord.getGpJobNo());
        if (exec != null) {
            exec.getWatchdog().destroyProcess();
            return true;
        }
        return false;
    }
    
    private class CmdResultHandler extends DefaultExecuteResultHandler {
        private int gpJobNo;
        public CmdResultHandler(final int gpJobNo) {
            this.gpJobNo=gpJobNo;
        }
        
        @Override
        public void onProcessComplete(final int exitValue) {
            super.onProcessComplete(exitValue);
            updateStatus_complete(gpJobNo, exitValue, null);
        }

        @Override
        public void onProcessFailed(final ExecuteException e) {
            super.onProcessFailed(e);
            updateStatus_complete(gpJobNo, e.getExitValue(), e);
        }
    }
    
    private Executor runJobNoWait(final DrmJobSubmission gpJob) throws ExecutionException, IOException {
        CommandLine cl = initCommand(gpJob);
        
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
        
        final ExecuteResultHandler resultHandler=new CmdResultHandler(gpJob.getGpJobNo());
        final ExecuteWatchdog watchDog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
        final ShutdownHookProcessDestroyer processDestroyer = new ShutdownHookProcessDestroyer();
        
        DefaultExecutor exec=new DefaultExecutor();
        exec.setWorkingDirectory(gpJob.getWorkingDir());
        exec.setStreamHandler( pumpStreamHandler );
        exec.setWatchdog(watchDog);
        exec.setProcessDestroyer(processDestroyer);
        exec.execute(cl, resultHandler);
        return exec;
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

}

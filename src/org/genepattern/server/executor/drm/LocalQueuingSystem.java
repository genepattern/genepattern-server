package org.genepattern.server.executor.drm;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.RuntimeExecCommand;
import org.genepattern.webservice.JobInfo;

/**
 * Proof of concept implementation of the QueuingSystem interface for local execution.
 * 
 * To use this, add the following to the config_.yaml file for your server.
 * 
 * <pre>
    DRM:
        classname: org.genepattern.server.executor.drm.DrmExecutor
        configuration.properties:
            classname: org.genepattern.server.executor.drm.LocalQueuingSystem
 * </pre>
 * 
 * @author pcarr
 *
 */
public class LocalQueuingSystem implements QueuingSystem {
    private static final Logger log = Logger.getLogger(LocalQueuingSystem.class);

    //map of jobId to task
    private final int numThreads=20;
    private Map<String, Future<DrmJobStatus>> runningTasks;
    private ExecutorService executor;
    
    public LocalQueuingSystem() {
        runningTasks=new HashMap<String, Future<DrmJobStatus>>();
        executor = Executors.newFixedThreadPool(numThreads);
    }
    
    @Override
    public void stop() {
        executor.shutdownNow();
    }

    @Override
    public String startJob(final String[] commandLine, final Map<String, String> environmentVariables, final File runDir, final File stdoutFile, final File stderrFile, final JobInfo jobInfo, final File stdinFile) throws CommandExecutorException {
        final String drmJobId=""+jobInfo.getJobNumber();
        final Future<DrmJobStatus> future=executor.submit(new Callable<DrmJobStatus>() {
            @Override
            public DrmJobStatus call() throws Exception {
                RuntimeExecCommand cmd = new RuntimeExecCommand();
                cmd.runCommand(commandLine, environmentVariables, runDir, stdoutFile, stderrFile, jobInfo, stdinFile);
                final DrmJobStatus drmJobStatus;
                if (cmd.getExitValue()==0) {
                    drmJobStatus=new DrmJobStatus(drmJobId, JobState.DONE, cmd.getExitValue());
                }
                else {
                    drmJobStatus=new DrmJobStatus(drmJobId, JobState.FAILED, cmd.getExitValue());
                }
                return drmJobStatus;                
            }
        });
        runningTasks.put(drmJobId, future);
        return drmJobId;
    }

    @Override
    public DrmJobStatus getStatus(String drmJobId) {
        final Future<DrmJobStatus> task=runningTasks.get(drmJobId);
        if (task==null) {
            //TODO: could look at the file system for evidence of job completion
            DrmJobStatus drmJobStatus=new DrmJobStatus(drmJobId, JobState.UNDETERMINED);
            return drmJobStatus;
        }
        
        if (!task.isDone()) {
            DrmJobStatus drmJobStatus=new DrmJobStatus(drmJobId, JobState.RUNNING);
            return drmJobStatus;
        }

        if (task.isCancelled()) {
            //TODO: need to clearly indicate a user-cancelled job, from a job which failed because of a non-zero exit code
            return new DrmJobStatus(drmJobId, JobState.FAILED, -1);
        }
        try {
            return  task.get();
        }
        catch (InterruptedException e) {
            log.error(e);
            Thread.currentThread().interrupt();
            return new DrmJobStatus(drmJobId, JobState.FAILED, -1);
        }
        catch (Exception e) {
            log.error(e);
            return new DrmJobStatus(drmJobId, JobState.UNDETERMINED);
        }
    }

    @Override
    public void cancelJob(String drmJobId, JobInfo jobInfo) throws Exception {
        final Future<DrmJobStatus> task=runningTasks.get(drmJobId);
        if (task != null) {
            final boolean mayInterruptIfRunning=true;
            final boolean success=task.cancel(mayInterruptIfRunning);
            log.debug("success="+success);
        }
    }
}


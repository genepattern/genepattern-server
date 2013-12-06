package org.genepattern.drm.impl.local;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.JobRunner;
import org.genepattern.drm.JobSubmission;
import org.genepattern.server.executor.CommandExecutorException;
import org.genepattern.server.executor.RuntimeExecCommand;
import org.genepattern.webservice.JobInfo;

/**
 * An implementation of the JobRunner interface for local execution.
 * 
 * To use this, add the following to the config_.yaml file for your server.
 * 
 * <pre>
 * LocalJobRunner:
        classname: org.genepattern.server.executor.drm.DrmExecutor
        configuration.properties:
            jobRunnerClassname: org.genepattern.drm.impl.local.LocalJobRunner
            jobRunnerName: LocalJobRunner
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
    public String startJob(final JobSubmission drmJobSubmit) throws CommandExecutorException {
        final String drmJobId=""+drmJobSubmit.getGpJobNo();
        final Future<DrmJobStatus> future=executor.submit(new Callable<DrmJobStatus>() {
            @Override
            public DrmJobStatus call() throws Exception {
                RuntimeExecCommand cmd = new RuntimeExecCommand();
                final String[] commandLine=drmJobSubmit.getCommandLine().toArray(new String[0]);
                cmd.runCommand(commandLine, 
                        drmJobSubmit.getEnvironmentVariables(), 
                        drmJobSubmit.getWorkingDir(), 
                        drmJobSubmit.getStdoutFile(), 
                        drmJobSubmit.getStderrFile(), 
                        drmJobSubmit.getJobInfo(), 
                        drmJobSubmit.getStdinFile());
                final DrmJobStatus drmJobStatus;
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
    public DrmJobStatus getStatus(String drmJobId) {
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
            //TODO: need to clearly indicate a user-cancelled job, from a job which failed because of a non-zero exit code
            return new DrmJobStatus.Builder(drmJobId, DrmJobState.FAILED).exitCode(-1).build();
        }
        try {
            return  task.get();
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
    public void cancelJob(String drmJobId, JobInfo jobInfo) throws Exception {
        final Future<DrmJobStatus> task=runningTasks.get(drmJobId);
        if (task != null) {
            final boolean mayInterruptIfRunning=true;
            final boolean success=task.cancel(mayInterruptIfRunning);
            log.debug("success="+success);
        }
    }
}


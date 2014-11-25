package org.genepattern.drm.impl.debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.genepattern.drm.CpuTime;
import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;
import org.genepattern.drm.JobRunner;
import org.genepattern.server.executor.CommandExecutorException;

/**
 * For debugging the JobRunner integration with GenePattern.
 * 
 * <pre>
    DebugJobRunner:
        classname: org.genepattern.server.executor.drm.JobExecutor
        configuration.properties:
            jobRunnerClassname: org.genepattern.drm.impl.debug.DebugJobRunner
            jobRunnerName: DebugJobRunner
            # lookupType: DB, requires GP server >= 3.7.6 build 12709 (or customization of the internal database and hibernate mapping files)
            # the 'HASHMAP' option is a fallback for development (*but not production*). It does not save status of running jobs after a GP server restart)
            lookupType: DB
            #lookupType: HASHMAP
 * </pre>

 * @author pcarr
 *
 */
public class DebugJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(DebugJobRunner.class);

    /**
     * Utility method for creating a new file with the given message as it's content.
     * @param message
     * @param toFile
     */
    private static void writeToFile(final String message, final File toFile) {
        toFile.getParentFile().mkdirs();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(toFile));
            writer.write(message);
        } 
        catch (IOException e) {
            log.error("Error writing file="+toFile, e);
        } 
        finally {
            if (writer != null) {
                try {
                    writer.close();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void stop() {
        log.info("stopping DebugJobRunner");
    }

    @Override
    public String startJob(final DrmJobSubmission drmJobSubmission) throws CommandExecutorException {
        StringBuffer buf=new StringBuffer();
        buf.append("starting gpJob="+drmJobSubmission.getGpJobNo()+" in "+drmJobSubmission.getWorkingDir()+"\n");
        buf.append("    "+drmJobSubmission.getJobInfo().getTaskName()+" ( lsid='"+drmJobSubmission.getJobInfo().getTaskLSID()+"' ) \n");
        buf.append("commandLine="+drmJobSubmission.getCommandLine()+"\n");
        buf.append("====== resource requirements ======\n");
        if (drmJobSubmission.getQueue() != null) {
            buf.append("queue: "+drmJobSubmission.getQueue()+"\n");
        }
        if (drmJobSubmission.getMemory() != null) {
            buf.append("memory: "+drmJobSubmission.getMemory().toString()+"\n");
        }
        if (drmJobSubmission.getExtraArgs() != null && drmJobSubmission.getExtraArgs().size()>0) {
            buf.append("extraArgs: "+drmJobSubmission.getExtraArgs()+"\n");
        }
        buf.trimToSize();
        String message=buf.toString();
        File out=new File(drmJobSubmission.getWorkingDir(), "debug.txt");
        writeToFile(message, out);
        log.debug(message);
        return ""+drmJobSubmission.getGpJobNo();
    }

    @Override
    public DrmJobStatus getStatus(final DrmJobRecord drmJobRecord) {
        log.info("getStatus, drmJobId="+drmJobRecord.getExtJobId());
        //I work so hard, I'm always finished
        return new DrmJobStatus.Builder(drmJobRecord.getExtJobId(), DrmJobState.DONE)
            .exitCode(0)
            .endTime(new Date())
            .build();
    }
    
    /**
     * This example method shows how to initialize a DrmJobStatus instance. 
     * You don't need to set each value, we use reasonable defaults if they are not yet set (e.g. endTime()) 
     * or not known (e.g. maxThreads()). 
     * 
     * @return
     */
    protected DrmJobStatus demoGetStatus() {
        return new DrmJobStatus.Builder()
            // the external job id
            .extJobId("EXT_01")
            // the name of the job queue
            .queueId("job_queue_id")
            // the date the job was submitted to the queue (Don't set this if the job has not been submitted)
            .submitTime(new Date())
            // the date the job started running on the queue (Don't set this if the job has not yet started)
            .startTime(new Date())
            // the date the job completed running (Don't set this if the job has not yet completed)
            .endTime(new Date())
            // the status code
            .jobState(DrmJobState.DONE)
            // the exit code of the job, '0' indicates success
            .exitCode(0)
            // the max memory usage in bytes (can also pass in a string such as "2 Gb")
            .memory(2000L)
            // the max swap space used by the job, can accept numBytes or a String (see above)
            .maxSwap("2 Gb")
            // the CPU usage of the job
            .cpuTime(new CpuTime(1000L, TimeUnit.MILLISECONDS))
            // the max number of threads used by the job (Don't call this if not known)
            .maxThreads(1)
            // the max number of processes used by the job (Don't call this if not known)
            .maxProcesses(1)
        .build();
    }

    @Override
    public boolean cancelJob(final DrmJobRecord job) throws Exception {
        log.info("cancelJob, drmJobId="+job.getExtJobId());
        return true;
    }

}

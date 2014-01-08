package org.genepattern.drm.impl.debug;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
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
        buf.trimToSize();
        String message=buf.toString();
        File out=new File(drmJobSubmission.getWorkingDir(), "debug.txt");
        writeToFile(message, out);
        log.debug(message);
        return ""+drmJobSubmission.getGpJobNo();
    }

    @Override
    public DrmJobStatus getStatus(final String drmJobId) {
        log.info("getStatus, drmJobId="+drmJobId);
        //I work so hard, I'm always finished
        return new DrmJobStatus.Builder(drmJobId, DrmJobState.DONE)
            .exitCode(0)
            .endTime(new Date())
            .build();
    }

    @Override
    public boolean cancelJob(final String drmJobId, final DrmJobSubmission drmJobSubmission) throws Exception {
        log.info("cancelJob, drmJobId="+drmJobId);
        return true;
    }

}

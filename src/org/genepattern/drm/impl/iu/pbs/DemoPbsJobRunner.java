package org.genepattern.drm.impl.iu.pbs;

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
 * Example job runner with custom configuration options.
 * The following variables can be customized in the config_.yaml file.
 * <pre>
 * # these flags are part of the built-in DrmJobSubmission class, note the 'job.' prefix.
   job.queue: "defaultQueue"
   job.walltime: "02:00:00"
   job.nodeCount: "1"
   job.extraArgs: []                                                                                                                                                                           

   # these flags are customized for this particular implementation of the JobRunner. Note the 'pbs.' prefix.
   pbs.host: "example.edu"
   pbs.mem: "8gb"
   pbs.ppn: "8"
   pbs.cput: ""
   pbs.vmem: "64gb"
 * </pre>
 * 
 * To customize your configuration, define a map (YAML format) in the default.properties section
 * of the executor. Create one map for each worker type.
 * 
 * 
 * @author pcarr
 *
 */
public class DemoPbsJobRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(DemoPbsJobRunner.class);
    

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
        if (drmJobSubmission.getWalltime() != null) {
            buf.append("walltime: "+drmJobSubmission.getWalltime().toString()+"\n");
        }
        if (drmJobSubmission.getNodeCount() != null) {
            buf.append("nodes: "+drmJobSubmission.getNodeCount()+"\n");
        }
        if (drmJobSubmission.getExtraArgs() != null && drmJobSubmission.getExtraArgs().size()>0) {
            buf.append("extraArgs: "+drmJobSubmission.getExtraArgs()+"\n");
        }
        
        buf.append("host: "+drmJobSubmission.getProperty("pbs.host")+"\n");
        buf.append("mem: "+drmJobSubmission.getProperty("pbs.mem")+"\n");
        buf.append("ppn: "+drmJobSubmission.getProperty("pbs.ppn")+"\n");
        buf.append("cput: "+drmJobSubmission.getProperty("pbs.cput")+"\n");
        buf.append("vmem: "+drmJobSubmission.getProperty("pbs.vmem")+"\n");
        
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

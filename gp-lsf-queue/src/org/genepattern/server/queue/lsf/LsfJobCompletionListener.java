package org.genepattern.server.queue.lsf;

import org.apache.log4j.Logger;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;

import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;

/**
 * Handle job completion events from the BroadCore LSF handler.
 * @author pcarr
 */
public class LsfJobCompletionListener implements JobCompletionListener {
    private static Logger log = Logger.getLogger(LsfJobCompletionListener.class);

    public void jobCompleted(LsfJob job) throws Exception {
        log.debug("job completed...lsf_id="+job.getLsfJobId()+", gp_job_id="+job.getInternalJobId());

        int jobId = job.getInternalJobId().intValue();
        String stdoutFilename = job.getOutputFilename();
        String stderrFilename = job.getErrorFileName();
                        
        //TODO: figure out the exit code
        int exitCode = 0;
        GenePatternAnalysisTask.handleJobCompletion(jobId, stdoutFilename, stderrFilename, exitCode);
    }
}

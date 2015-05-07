/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.scattergather;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.genepattern.server.executor.lsf.LsfCommandExecutor;
import org.genepattern.server.executor.lsf.LsfJobCompletionListener;

import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfJob.JobCompletionListener;
import edu.mit.broad.core.lsf.LsfJobDAO;

/**
 * Special handler for scatter-gather jobs. 
 * Use the following configuration properties:<pre>
     lsf.firehose.scatter.gather: true
     lsf.firehose.scatter.gather.output.filename: scatter.gather.out
     lsf.firehose.scatter.gather.output.filename.lsf.0: scatter.gather.lsf.out.0
     lsf.firehose.scatter.gather.output.filename.lsf.1: scatter.gather.lsf.out.1
 * </pre>
 * 
 * @author pcarr
 */
public class LsfScatterGatherJobCompletionListener implements JobCompletionListener  {
    private static Logger log = Logger.getLogger(LsfScatterGatherJobCompletionListener.class);
    private final static Pattern JOB_ID_PATTERN = Pattern.compile("Job <(\\d+)> is submitted to queue <[^>]+>.");


    //the output of the initial scatter job, must contain a single line with the LSF ID of the LSF job to wait for
    private static final String outputFilename = "jobSubmissions.txt";
    
    private static final LsfJobDAO dao = new LsfJobDAO();
    
    //add another entry in the broad core database so that we are notified when the given job completes
    public void jobCompleted(final LsfJob job) throws Exception { 
    	if (job.getStatus() != LsfJob.LSF_STATUS_DONE) {
    		// job did not complete successfully, delegate to the regular completion handler to deal with the failure
    		new LsfJobCompletionListener().jobCompleted(job);
    	}
    	
        try {
        	final List<Integer> scatterGatherLsfIds = getScatterGatherLsfIds(job);
        	
        	if (scatterGatherLsfIds.isEmpty() && job.getStatus() == LsfJob.LSF_STATUS_DONE) {
        		throw new Exception("No scatter or gather jobs produced, either job submissions file is empty or it can't be read.");
        	}
        	
        	final int gatherJobIndex = scatterGatherLsfIds.size()-1;
        	final Collection<LsfJob> scatterGatherJobs = new ArrayList<LsfJob>(scatterGatherLsfIds.size());
        	for (int i = 0; i < gatherJobIndex; i++) {
        		scatterGatherJobs.add(addNewJobEntry(scatterGatherLsfIds.get(i), job, i));
			}
			scatterGatherJobs.add(addNewJobEntry(scatterGatherLsfIds.get(gatherJobIndex), job, null));

	    	if (job.getStatus() != LsfJob.LSF_STATUS_DONE) {
	    		// terminate any jobs that may have been queued before failure
	    		LsfCommandExecutor.terminateJobs(job.getName(), scatterGatherJobs);
	    	}
        }
        catch (final Throwable e) {
            //TODO: call handleJobCompletion with ERROR status
            log.error("TODO: call handleJobCompletion with ERROR status", e);
            return;
        }
    }

    /**
     * Expect to find an output file named 'jobSubmissions.txt' in the working directory for the job,
     * which contains line per job submitted in the form
     * Job <[0-9]+> is submitted to queue <[a-zA-Z]+>.
     * The last line contains the job id of the gather job and when that completes the regular
     * job complete listener should be called.
     * 
     * @param job
     * @return 
     * @throws Exception
     */
    private List<Integer> getScatterGatherLsfIds(final LsfJob job) throws Exception {
        if (job == null) {
            throw new Exception("null arg");
        }
        if (job.getWorkingDirectory() == null) {
            throw new Exception("null job.workingDirectory");
        }
        //TODO: use configuration parameter
        final File outputFile = new File(job.getWorkingDirectory(), outputFilename);
        if (!outputFile.canRead()) {
            return Collections.emptyList();
        }

        BufferedReader reader = null;
        try {
        	final List<Integer> lsfIds = new ArrayList<Integer>();
            reader = new BufferedReader(new FileReader(outputFile));
            String nextLine = null;
            while ((nextLine = reader.readLine()) != null) {
            	final Matcher matcher = JOB_ID_PATTERN.matcher(nextLine);
            	if (matcher.matches()) {
            		lsfIds.add(Integer.valueOf(matcher.group(1)));
            	}
            }
            return lsfIds;
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (final IOException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * Add a new entry to the broad core lsf job table.
     * 
     * @param lsfJobId, the lsf id of the new job
     * @param origJob, the originating job
     * @param scatterJobIndex if null, this is the gather job
     */
    private LsfJob addNewJobEntry(final int lsfJobId, final LsfJob origJob, final Integer scatterJobIndex) {
    	final LsfJob newJob = new LsfJob();
    	if (scatterJobIndex == null) {
    		newJob.setWorkingDirectory(origJob.getWorkingDirectory());
    		newJob.setOutputFilename("scatter-gather.out.txt");
    		newJob.setErrorFileName("scatter-gather.err.txt");
    		newJob.setCompletionListenerName(LsfJobCompletionListener.class.getName());
    		newJob.setCommand("gather");
        	newJob.setStatus(LsfJob.LSF_STATUS_PENDING);
    	} else {
    		newJob.setWorkingDirectory(String.format("%s%sscatter.%010d", origJob.getWorkingDirectory(), File.separator, scatterJobIndex));
    		newJob.setOutputFilename("scatter.out");
    		newJob.setErrorFileName("scatter.err");
    		newJob.setCommand("scatter_" + scatterJobIndex);
    		
    		// Set to unknown status because we don't care about job completion and unknown status jobs
    		// are not polled, however unknown status jobs are considered active and can be terminated.
    		// Because the gather job removes the outputs of the scatter jobs the scatter jobs will never
    		// come out of pending state if we use that.
    		newJob.setStatus(LsfJob.LSF_STATUS_UNKNOWN);
    	}

    	//note: use setName to hold onto the GP job id
    	newJob.setName(origJob.getName());
    	newJob.setQueue(origJob.getQueue());
    	newJob.setProject(origJob.getProject());
    	newJob.setLsfJobId(""+lsfJobId);

    	newJob.setGapServerId(origJob.getGapServerId());
    	newJob.setUpdatedDate(new Date());

    	dao.save(newJob);
    	return newJob;
    }

}

package org.genepattern.server.job.query;

import org.genepattern.server.job.input.JobInput;

/**
 * Interface for fetching user supplied input values for a given job, 
 * to be implemented by a DAO helper class.
 * 
 * @author pcarr
 *
 */
public interface JobInputQuery {
    JobInput getJobInput(int jobId) throws Exception;
}

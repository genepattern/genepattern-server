package org.genepattern.server.job;

import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.JobInfo;

/**
 * Interface for loading a JobInfo into the system, by default this is loaded from 
 * the database. 
 * 
 * @author pcarr
 *
 */
public interface JobInfoLoader {
    JobInfo getJobInfo(final Context userContext, final String jobId) throws Exception;
}

package org.genepattern.server.webapp.rest.api.v1.job;

import org.genepattern.server.config.ServerConfiguration;
import org.json.JSONObject;

/**
 * Interface for getting the JSON representation for a given job, based on jobId.
 * Implementation details are left to a particular instance of this interface.
 * @author pcarr
 *
 */
interface GetJob {
    JSONObject getJob(final ServerConfiguration.Context userContext, final String jobId)
    throws GetJobException;
}
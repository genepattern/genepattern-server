package org.genepattern.server.webapp.rest.api.v1.job;

import org.genepattern.server.config.ServerConfiguration;
import org.json.JSONObject;

interface GetJob {
    JSONObject getJob(final ServerConfiguration.Context userContext, final String jobId)
    throws GetJobException;
}
/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job;

import org.genepattern.server.config.GpContext;
import org.json.JSONObject;

/**
 * Interface for getting the JSON representation for a given job, based on jobId.
 * Implementation details are left to a particular instance of this interface.
 * @author pcarr
 *
 */
interface GetJob {
    JSONObject getJob(final GpContext userContext, final String jobId, boolean includeComments, boolean includeTags)
    throws GetJobException;
}
/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job;

import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.JobInfo;

/**
 * Interface for loading a JobInfo into the system, by default this is loaded from 
 * the database. 
 * 
 * @author pcarr
 *
 */
public interface JobInfoLoader {
    JobInfo getJobInfo(final GpContext userContext, final String jobId) throws Exception;
}

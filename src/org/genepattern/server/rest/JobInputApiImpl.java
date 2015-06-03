/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.rest;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.job.input.JobInput;

/**
 * this class accepts a job submit form from the end user and adds a job to the queue.
 * 
 * special cases:
 *     1) when a value is not supplied for an input parameter, the default value will be used
 *         if there is no default value, then assume that the value is not set.
 *     2) when a parameter is not optional, throw an exception if no value has been set
 *     3) when a list of values is supplied, automatically generate a file list file before adding the job to the queue
 *     4) when an external URL is supplied, GET the contents of the file before adding the job to the queue
 *     
 * TODO: 
 *     5) transferring data from an external URL as well as generating a file list can take a while, we should
 *         update this code so that it does not have to block the web client.
 *     
 * @author pcarr
 * 
 * @deprecated for 3.8.1 should use the newer version of the API which can handle optional job configuration parameters
 *
 */
public class JobInputApiImpl implements JobInputApi {
    final static private Logger log = Logger.getLogger(JobInputApiImpl.class);
    
    private final GpConfig gpConfig;
    private GetTaskStrategy getTaskStrategy;
    /**
     * special-case, as a convenience, for input parameters which have not been set, initialize them from their default value.
     * By default, this value is false.
     */
    private boolean initDefault=false;
    protected JobInputApiImpl(final GpConfig gpConfig, final boolean initDefault) {
        this.gpConfig=gpConfig;
        this.initDefault=initDefault;
    }

    @Override
    public String postJob(final GpContext jobContext, final JobInput jobInput) throws GpServerException {
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (jobContext.getUserId()==null) {
            throw new IllegalArgumentException("jobContext.userId==null");
        }
        if (jobInput==null) {
            throw new IllegalArgumentException("jobInput==null");
        }
        if (jobInput.getLsid()==null) {
            throw new IllegalArgumentException("jobInput.lsid==null");
        }
        try {
            JobInputApiLegacy jobInputHelper=new JobInputApiLegacy(gpConfig, jobContext, jobInput, getTaskStrategy, initDefault);
            final String jobId=jobInputHelper.submitJob();
            return jobId;
        }
        catch (Throwable t) {
            String message="Error adding job to queue, currentUser="+jobContext.getUserId()+", lsid="+jobInput.getLsid();
            log.error(message,t);
            throw new GpServerException(t.getLocalizedMessage(), t);
        }
    }
    
}

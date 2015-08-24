/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.batch.BatchInputFileHelper;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApi;
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for the job input form, includes methods for preparing both batch jobs and regular jobs.
 * 
 * For batch jobs, you can either explicitly add all batch input values with the #addBatchValue method.
 * Alternatively, you can declare a batch job with the #addBatchDirectory method.
 * This is based on the pre-existing 3.5.0 and earlier method. Which has the following limitations:
 *     1) can only batch on input files, not other types
 *     2) the value for a batch input parameter must be to one and only one directory on the file system:
 *        can be on the server file path or in the user upload tab.
 *     3) when there are multiple batch input parameters, the intersection of all matching parameters determines
 *        how many jobs to run. Matches are based on file basename.
 * 
 * @author pcarr
 *
 */
public class JobInputHelper {
    final static private Logger log = Logger.getLogger(JobInputHelper.class);
    
    /**
     * Is the input value an external URL?
     * 
     * @param value
     * 
     * @return the URL if it's an external url, otherwise return null.
     */
    public static URL initExternalUrl(final GpConfig gpConfig, final String value) {
        log.debug("intialize external URL for value="+value);
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }

        if (value.startsWith("<GenePatternURL>")) {
            log.debug("it's a substition for the gp url");
            return null;
        }
        if (value.startsWith(gpConfig.getGpUrl())) {
            log.debug("it's a gp url");
            return null;
        }

        URL url=null;
        try {
            url=new URL(value);
        }
        catch (MalformedURLException e) {
            log.debug("it's not a url", e);
            return null;
        }
        
        //special-case for file:/// urls
        if ("file".equalsIgnoreCase(url.getProtocol())) {
            log.debug("it's a file url, assume it's a local path: "+value);
            return null;
        }
        return url;
    }

    private final GpConfig gpConfig;
    private final BatchInputFileHelper batchInputFileHelper;

    public JobInputHelper(final GpConfig gpConfig, final GpContext userContext, final String lsid) {
        this(gpConfig, userContext, lsid, null);
    }
    public JobInputHelper(final GpConfig gpConfig, final GpContext userContext, final String lsid, final JobInputApi singleJobInputApi) {
        this(gpConfig, userContext, lsid, singleJobInputApi, new GetTaskStrategyDefault());
    }
    public JobInputHelper(final GpConfig gpConfig, final GpContext userContext, final String lsid, final JobInputApi jobInputApi, GetTaskStrategy getTaskStrategyIn) {
        if (getTaskStrategyIn == null) {
            getTaskStrategyIn=new GetTaskStrategyDefault();
        }
        final TaskInfo taskInfo = getTaskStrategyIn.getTaskInfo(lsid);
        this.gpConfig=gpConfig;
        this.batchInputFileHelper=new BatchInputFileHelper(gpConfig, userContext, taskInfo, jobInputApi);
    }
    public JobInputHelper(final GpConfig gpConfig, final GpContext userContext, final String lsid, final JobInputApi jobInputApi, final TaskInfo taskInfo) {
        this.gpConfig=gpConfig;
        this.batchInputFileHelper=new BatchInputFileHelper(gpConfig, userContext, taskInfo, jobInputApi);
    }
    
    /**
     * Add a value for a non-batch parameter. 
     * 
     * @param name
     * @param value
     */
    public void addValue(final String name, final String value) {
        addValue(name, value, GroupId.EMPTY);
    }
    
    /**
     * Add a value for a non-batch parameter, including optional groupId.
     * @param name
     * @param value
     * @param groupId
     */
    public void addValue(final String name, final String value, final GroupId groupId) {
        this.batchInputFileHelper.addValue(new ParamId(name), value, groupId);
    }

    /**
     * Add a value for a non-batch parameter. 
     * 
     * @param paramId
     * @param value
     */
    public void addValue(final ParamId paramId, final String value) {
        addValue(paramId, value, GroupId.EMPTY);
    }

    public void addValue(final ParamId paramId, final String value, final GroupId groupId) {
        this.batchInputFileHelper.addValue(paramId, value, groupId);
    }
    
    /**
     * Add a value for a batch parameter.
     * @param name
     * @param value
     */
    public void addBatchValue(final String name, final String value) throws GpServerException {
        addBatchValue(new ParamId(name), value);
    }
    
    /**
     * Add a value for a batch parameter.
     * @param paramId
     * @param value
     */
    public void addBatchValue(final ParamId paramId, final String value) throws GpServerException {
        this.batchInputFileHelper.addBatchValue(gpConfig, paramId, value);
    }

    /**
     * When you assign an input parameter (e.g. 'input.file') to a batch directory, you are telling the server to
     * automatically generate a batch of jobs, one for each matching file in the batch directory.
     * 
     * A file matches based on the file extension of the file and the list of accepted fileFormats for the parameter,
     * as declared in the module manifest.
     * 
     *
     */
    public void addBatchDirectory(final String name, final String value) throws GpServerException {
        addBatchDirectory(new ParamId(name), value);
    }
    
    public void addBatchDirectory(final ParamId id, final String value) throws GpServerException {
        this.batchInputFileHelper.addBatchValue(gpConfig, id, value);
    }
    
    /**
     * After you initialize all of the values, call this method to create the
     * list of JobInputs, one for each new job to run.
     * 
     * @return
     * @throws GpServerException
     */
    public List<JobInput> prepareBatch() throws GpServerException {
        return batchInputFileHelper.prepareBatch();
    }

    /**
     * Submit your jobs to the GP server.
     * Use the list of JobInput from the prepareBatch() method as input to this method.
     * 
     * @param batchInputs
     * @return
     * @throws GpServerException
     */
    public JobReceipt submitBatch(final List<JobInput> batchInputs) throws GpServerException {
        return batchInputFileHelper.submitBatch(batchInputs);
    }

}

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.batch.BatchGenerator;
import org.genepattern.server.job.input.batch.BatchInputFileHelper;
import org.genepattern.server.job.input.batch.FilenameBatchGenerator;
import org.genepattern.server.job.input.batch.SimpleBatchGenerator;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.server.rest.ParameterInfoRecord;

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
    private final GpContext taskContext;
    private BatchGenerator batchGenerator;
    private final JobInput jobInputTemplate;
    private final Map<String,ParameterInfoRecord> paramInfoMap;
    private boolean hasDirectory = false;

    /**
     * @param gpConfig
     * @param taskContext non-null, must have a non-null TaskInfo with a non-null lsid.
     */
    public JobInputHelper(final GpConfig gpConfig, final GpContext taskContext) {
        if (taskContext==null) {
            throw new IllegalArgumentException("taskContext==null");
        }
        if (taskContext.getTaskInfo()==null) {
            throw new IllegalArgumentException("taskContext.taskInfo==null");
        }
        if (taskContext.getTaskInfo().getLsid()==null) {
            throw new IllegalArgumentException("taskContext.taskInfo.lsid==null");
        }
        
        this.gpConfig=gpConfig;
        this.taskContext = taskContext;

        this.jobInputTemplate = new JobInput();
        this.jobInputTemplate.setLsid(taskContext.getTaskInfo().getLsid());
        this.paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskContext.getTaskInfo());
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
        jobInputTemplate.addValue(new ParamId(name), value, groupId);
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

    public void addValue(final ParamId paramId, final String value, final GroupId groupId)
    {
        jobInputTemplate.addValue(paramId, value, groupId);
    }
    
    /**
     * Add a value for a batch parameter.
     * @param name
     * @param value
     */
    public void addBatchValue(final String name, final String value) throws GpServerException {
        addBatchValue(new ParamId(name), value);
    }

    public boolean isFileParameter(ParamId paramId)
    {
        final ParameterInfoRecord record=paramInfoMap.get(paramId.getFqName());

        boolean isFile = false;
        boolean isDirectory = false;

        if(record != null) {
            isFile = record.getFormal().isInputFile();
            isDirectory = record.getFormal()._isDirectory();
        }
        return isFile || isDirectory;
    }

    /**
     * Add a value for a batch parameter.
     * @param paramId
     * @param value
     */
    public void addBatchValue(final ParamId paramId, final String value) throws GpServerException
    {
        final ParameterInfoRecord record = paramInfoMap.get(paramId.getFqName());

        if (record==null) {
            final String message="No matching parameter, '"+paramId.getFqName()+"', for task="+jobInputTemplate.getLsid();
            log.error(message);
            throw new GpServerException(message);
        }

        //check if this is a file parameter
        if(isFileParameter(paramId))
        {
            //check if the value for this file parameter is a directory
            final GpFilePath gpPath=BatchInputFileHelper.initGpFilePath(gpConfig, value, true);
            if(gpPath.isDirectory())
            {
                hasDirectory = true;
            }


            List<String> batchFileValues = BatchInputFileHelper.getBatchValues(gpConfig, taskContext, paramId, record, value);

            for(String file: batchFileValues)
            {
                jobInputTemplate.addValue(paramId, file, true);
            }
        }
        else
        {
            jobInputTemplate.addValue(paramId, value, true);
        }
    }

    private BatchGenerator initBatchGenerator() throws GpServerException
    {
        BatchGenerator generator;

        //return the simple generator if this contains batch params that are not files
        final Set<Param> params = jobInputTemplate.getBatchParams();

        boolean hasNonFileBatchParams = false;
        for(final Param param : params)
        {
            if(!isFileParameter(param.getParamId()))
            {
                hasNonFileBatchParams = true;
            }
        }

        if(!hasNonFileBatchParams)
        {
            generator = new FilenameBatchGenerator(gpConfig, taskContext);

        }
        else if(!hasDirectory)
        {
            generator = new SimpleBatchGenerator(gpConfig, taskContext);
        }
        else
        {
            throw new GpServerException("Batching of non file parameters and file parameters containing " +
                    "directories is not supported. Please provide a list of files instead of directories.");
        }

        return generator;
    }

    /**
     * After you initialize all of the values, call this method to create the
     * list of JobInputs, one for each new job to run.
     * 
     * @return
     * @throws GpServerException
     */
    public List<JobInput> prepareBatch() throws GpServerException
    {
        if(batchGenerator == null)
        {
           batchGenerator = initBatchGenerator();
        }

        return batchGenerator.prepareBatch(jobInputTemplate);
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
        return batchGenerator.submitBatch(batchInputs);
    }

}

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.batch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.GpServerException;

import com.google.common.collect.LinkedListMultimap;

/**
 * Generate batch jobs, match multiple batch parameters by basename and extension.
 * Sort by basename.
 * 
 * @author pcarr
 *
 */
public class FilenameBatchGenerator extends SimpleBatchGenerator {
    private static final Logger log = Logger.getLogger(FilenameBatchGenerator.class);

    private final boolean extractBatchValues;
    // Multimap pname -> GpFilePath, map.asMap() is a Map of pname -> list of GpFilePath
    private final LinkedListMultimap<String,GpFilePath> map;
    
    public FilenameBatchGenerator(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext userContext) {
        super(mgr, gpConfig, userContext);
        this.extractBatchValues=true;
        this.map = LinkedListMultimap.create();
    }
    
    @Override
    public List<JobInput> prepareBatch(final JobInput batchInputTemplate) throws GpServerException {
        if (extractBatchValues) {
            extractBatchValues(batchInputTemplate);
        }
        final JobInput jobInput=new JobInput(batchInputTemplate);
        jobInput.setBaseGpHref(batchInputTemplate.getBaseGpHref());

        if (map.asMap().size()==0) {
            //no batch params
            List<JobInput> rval=new ArrayList<JobInput>();
            rval.add( new JobInput(jobInput) );
            return rval;
        }
        else if (map.asMap().size()==1) {
            //one batch param
            final boolean isBatchParam=true; 
            for(final Entry<String,Collection<GpFilePath>> entry : map.asMap().entrySet()) {
                for(final GpFilePath inputFile : entry.getValue()) {
                    final String value = getJobInputValueFromGpFilePath(batchInputTemplate.getBaseGpHref(), inputFile);
                    jobInput.addValue(entry.getKey(), value, isBatchParam);
                }
            }
        }
        else {
            //multi batch params
            Set<String> commonBasenames=computeCommonBasenames();
            //special-case: no matching parameters, because commonBasenames is empty
            if (commonBasenames.isEmpty()) {
                throw new GpServerException("No matching input files for multi-batch job.");
            }
            appendBatchValuesToJobInputTemplate(jobInput, commonBasenames);
        }
        return doBatch(jobInput);
    }


    protected static List<JobInput> doBatch(final JobInput jobInput) throws GpServerException {
        List<JobInput> batchInputs=new ArrayList<JobInput>();
        int numJobs=jobInput.getNumBatchJobs();
        if (numJobs==1) {
            batchInputs.add(jobInput);
            return batchInputs;
        }

        //it is a batch of jobs
        for(int idx=0; idx<numJobs; ++idx) {
            JobInput nextJob=prepareJobInput(idx, jobInput);
            batchInputs.add(nextJob);
        }
        return batchInputs;
    }

    protected static JobInput prepareJobInput(final int idx, final JobInput template) {
        //start with a copy of the jobInput template
        final JobInput nextJobInput = new JobInput(template);
        //then replace batch parameters with the values for this particular (idx) batch job
        for(final Param batchParamIn : template.getBatchParams()) {
            final Param batchParam=new Param(batchParamIn.getParamId(), false);
            final ParamValue batchParamValue = template.getParam(batchParamIn.getParamId()).getValues().get(idx);
            batchParam.addValue(batchParamValue);
            nextJobInput.setValue(batchParamIn.getParamId(), batchParam);
        }
        return nextJobInput;
    }

    /**
     * After all user input values have been added from the web input form 
     * update the jobInput template by adding any matching batch values.
     */
    public void appendBatchValuesToJobInputTemplate(final JobInput jobInput, final Set<String> commonBasenames) throws GpServerException {
        //if there are any common basenames, only add the parameters which match
        //ensure the values are added in the correct order
        List<String> usedFiles = new ArrayList<String>();

        for(final Entry<String,Collection<GpFilePath>> entry : map.asMap().entrySet()) {
            SortedMap<String, GpFilePath> sortedValues=new TreeMap<String, GpFilePath>();
            //ignore basenames found more than once after a new value has been set
            List<String> ignoredBasenames = new ArrayList<String>();
            for(final GpFilePath inputFile : entry.getValue()) {
                final String basename=BatchInputFileHelper.getBaseFilename(inputFile);
                if (commonBasenames.contains(basename)) {
                    //if this basename was not found before then just save the value
                    if (!sortedValues.containsKey(basename)) {
                        sortedValues.put(basename,inputFile);
                        String fileUrlOrPath = getFileUrlOrPath(inputFile);
                        if (!usedFiles.contains(fileUrlOrPath)) {
                            usedFiles.add(fileUrlOrPath);
                            ignoredBasenames.add(basename);
                        }
                    }
                    else {
                        //if this basename was found before then set this new value as the current
                        //only if the current value was not unique
                        if(!ignoredBasenames.contains(basename) && !usedFiles.contains(inputFile.getServerFile().getAbsolutePath())) {
                            sortedValues.put(basename,inputFile);
                        }
                    }
                }
            }
            for(final Entry<String,GpFilePath> next : sortedValues.entrySet()) {
                final GpFilePath inputFile = next.getValue();
                final String value=getJobInputValueFromGpFilePath(jobInput.getBaseGpHref(), inputFile);
                final boolean isBatchParam=true;
                jobInput.addValue(entry.getKey(), value, isBatchParam);
            }
        }
    }

    /**
     * Convenience method to get the value from a given GpFilePath; To be called when building a new JobInput to submit to the 
     * queue from a list of GpFilePath input values. For local files use the baseGpHref, for external files use the url.
     * 
     * @param baseGpHref
     * @param inputFile, can be a local or external url
     * @return
     * @throws GpServerException
     */
    protected String getJobInputValueFromGpFilePath(final String baseGpHref, final GpFilePath inputFile) throws GpServerException {
        final String value;
        if (inputFile.isLocal()) {
            value=UrlUtil.getHref(baseGpHref, inputFile);
        }
        else {
            try {
                value=inputFile.getUrl(gpConfig).toExternalForm();
            }
            catch (Throwable t) {
                throw new GpServerException(t.getLocalizedMessage(),t);
            }
        }
        return value;
    }

    /**
     * Get the relativeUri path (for local files) or the URL path (for external files) for the
     * given input file.
     * 
     * This is done to prevent using duplicate values for different batch 
     * params when there are multiple files with the same basename
     * 
     * Note: using same rule as in GP 3.9.5; for local files changed from using serverFile.absolutePath
     * to using serverFile.relativeUri.path.
     * 
     * Not sure how this is being used, pcarr. 
     * 
     * @param inputFile
     * @return
     */
    protected String getFileUrlOrPath(final GpFilePath inputFile) {
        if (inputFile.isLocal()) {
            return inputFile.getRelativeUri().getPath();
        }
        else {
            try {
                return inputFile.getUrl(gpConfig).getPath();
            }
            catch (Exception e) {
                log.error("Error getting file url for " + inputFile.getName());
                return inputFile.getName();
            }
        }
    }

    protected void extractBatchValues(final JobInput batchInputTemplate) {        
        for(final Param batchParam : batchInputTemplate.getBatchParams()) {
            //remove the param from the template
            batchInputTemplate.removeValue(batchParam.getParamId());
            final String key=batchParam.getParamId().getFqName();
            for(final ParamValue batchParamValue : batchParam.getValues()) {
                final GpFilePath gpFilePath = BatchInputFileHelper.initGpFilePath(gpConfig, batchInputTemplate, batchParamValue.getValue(), true);
                if (gpFilePath != null) {
                    map.put(key, gpFilePath);
                }
            }
        } 
    }
    
    /**
     * For multi batch parameters ... first get the intersection of common basenames.
     * @return
     */
    protected Set<String> computeCommonBasenames() {
        boolean first=true;
        final Set<String> commonBasenames = new LinkedHashSet<String>();
        for(final Entry<String,Collection<GpFilePath>> entry : map.asMap().entrySet()) {
            final Set<String> basenames=new LinkedHashSet<String>();
            for(GpFilePath inputFile : entry.getValue()) {
                final String basename = BatchInputFileHelper.getBaseFilename(inputFile);
                basenames.add(basename);
            }
            if (first) {
                first=false;
                commonBasenames.addAll(basenames);
            }
            else {
                commonBasenames.retainAll(basenames);
                if (commonBasenames.isEmpty()) {
                    //no matching basenames!
                    break;
                }
            }
        }
        return commonBasenames;
    }
    
    /// for debugging
    protected LinkedListMultimap<String,GpFilePath> getBatchValues() {
        return map;
    }

}

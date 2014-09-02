package org.genepattern.server.job.input.batch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.rest.GpServerException;

/**
 * Generate batch jobs, match multiple batch parameters by basename and extension.
 * Sort by basename.
 * 
 * @author pcarr
 *
 */
public class FilenameBatchGenerator implements BatchGenerator {

    private static Logger log = Logger.getLogger(FilenameBatchGenerator.class);

    private final boolean extractBatchValues;
    private final Map<String,List<GpFilePath>> batchValues;
    
    public FilenameBatchGenerator() {
        this.extractBatchValues=true;
        this.batchValues=new LinkedHashMap<String,List<GpFilePath>>();
    }
    
    public FilenameBatchGenerator(final Map<String,List<GpFilePath>> initializedBatchValues) {
        this.extractBatchValues=false;
        this.batchValues=initializedBatchValues;
    }

    @Override
    public List<JobInput> prepareBatch(final JobInput batchInputTemplate) throws GpServerException {
        if (extractBatchValues) {
            extractBatchValues(batchInputTemplate);
        }
        JobInput jobInput=new JobInput(batchInputTemplate);

        if (batchValues.size()==0) {
            //no batch params
            List<JobInput> rval=new ArrayList<JobInput>();
            rval.add( new JobInput(jobInput) );
            return rval;
        }
        else if (batchValues.size()==1) {
            //one batch param
            final boolean isBatchParam=true;
            for(final Entry<String,List<GpFilePath>> entry : batchValues.entrySet()) {
                for(final GpFilePath inputFile : entry.getValue()) {
                    final String value;
                    try {
                        value=inputFile.getUrl().toExternalForm();
                    }
                    catch (Throwable t) {
                        throw new GpServerException(t.getLocalizedMessage(),t);
                    }
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

    public static List<JobInput> doBatch(final JobInput jobInput) throws GpServerException {
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

    private static JobInput prepareJobInput(final int idx, final JobInput template) {
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
        List<String> usedFiles = new ArrayList();

        for(final Entry<String,List<GpFilePath>> entry : batchValues.entrySet()) {
            SortedMap<String, GpFilePath> sortedValues=new TreeMap<String, GpFilePath>();
            //ignore basenames found more than once after a new value has been set
            List<String> ignoredBasenames = new ArrayList();
            for(final GpFilePath inputFile : entry.getValue()) {
                final String basename=BatchInputFileHelper.getBaseFilename(inputFile);
                if (commonBasenames.contains(basename)) {

                    //if this basename was not found before then just save the value
                    if(!sortedValues.containsKey(basename))
                    {
                        sortedValues.put(basename,inputFile);
                        //this is done to prevent using duplicate values for different batch
                        //params when there are multiple files with the same basename
                        String fileUrlOrPath = null;
                        if(inputFile.getServerFile() == null)
                        {
                            //the file path is not available so use the file url instead
                            //to check for duplicates
                            try {
                                fileUrlOrPath = inputFile.getUrl().getPath();
                            }
                            catch(Exception e)
                            {
                                log.error("Error getting file url for " + inputFile.getName());

                                //file url is not available so just use the file name
                                fileUrlOrPath = inputFile.getName();
                            }
                        }
                        else
                        {
                            fileUrlOrPath = inputFile.getServerFile().getAbsolutePath();
                        }

                        if(!usedFiles.contains(fileUrlOrPath))
                        {
                            usedFiles.add(fileUrlOrPath);
                            ignoredBasenames.add(basename);
                        }
                    }
                    else
                    {
                        //if this basename was found before then set this new value as the current
                        //only if the current value was not unique
                        if(!ignoredBasenames.contains(basename) && !usedFiles.contains(inputFile.getServerFile().getAbsolutePath()))
                        {
                            sortedValues.put(basename,inputFile);
                        }
                    }
                }
            }
            for(final Entry<String,GpFilePath> next : sortedValues.entrySet()) {
                final GpFilePath inputFile = next.getValue();
                final String value;
                try {
                    value=inputFile.getUrl().toExternalForm();
                }
                catch (Throwable t) {
                    throw new GpServerException(t.getLocalizedMessage(),t);
                }
                final boolean isBatchParam=true;
                jobInput.addValue(entry.getKey(), value, isBatchParam);
            }
        }
    }

    private void extractBatchValues(final JobInput batchInputTemplate) {        
        for(final Param batchParam : batchInputTemplate.getBatchParams()) {
            final String key=batchParam.getParamId().getFqName();
            for(final ParamValue batchParamValue : batchParam.getValues()) {
                final GpFilePath gpFilePath = BatchInputFileHelper.initGpFilePath(batchParamValue.getValue(), true);
                if (gpFilePath != null) {
                    List<GpFilePath> gpFilePaths;
                    if (!batchValues.containsKey(key)) {
                        gpFilePaths=new ArrayList<GpFilePath>();
                        batchValues.put(key, gpFilePaths);
                    }
                    else {
                        gpFilePaths=batchValues.get(key);
                    }
                    gpFilePaths.add(gpFilePath);
                }
            }
        } 
    }
    
    /**
     * For multi batch parameters ... first get the intersection of common basenames.
     * @return
     */
    public Set<String> computeCommonBasenames() {
        boolean first=true;
        final Set<String> commonBasenames = new LinkedHashSet<String>();
        for(final Entry<String,List<GpFilePath>> entry : batchValues.entrySet()) {
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

}

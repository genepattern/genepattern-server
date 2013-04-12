package org.genepattern.server.job.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

//import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.server.job.input.JobInput.ParamValue;
import org.genepattern.server.rest.JobInputApiLegacy.ParameterInfoRecord;
import org.genepattern.server.webapp.BatchSubmit;
import org.genepattern.server.webapp.BatchSubmit.MultiFileParameter;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

    /**
     * 
     * @param batchTemplate, the jobInput values entered in the job input form, 
     *     including any flags for batch parameters.
     * @return a list JobInput, each item in the list is a new job which should be run.
     * 
     * @throws Exception
     */

/**
 * Helper class for batch job input.
 * 
 * @author pcarr
 *
 */
public class BatchInputHelper {
    //final static private Logger log = Logger.getLogger(BatchInputHelper.class);

    /**
     * Helper method which is used to create a List of JobInput instances, one for each batch job to run.
     * Use the given jobInputTemplate to ...
     *     1) identify which parameters are batch parameters
     *        Can be inferred or declared.
     *     2) set non-default values for the remaining non-batch parameters
     * 
     * This is based on the pre-existing 3.5.0 and earlier method. Which has the following limitations:
     *     1) can only batch on input files, not other types
     *     2) the value for a batch input parameter must be to a directory on the file system.
     *     3) when there are multiple batch input parameters, the intersection of all matching parameters determines
     *        how many jobs to run. Matches are based on file basename.
     *        
     * See {@link #initValuesFromBatchDirectories()} for how this is used.
     * 
     * @param jobContext
     * @param getTaskStrategy
     * @param jobInputTemplate
     * @return
     * @throws Exception
     */
    public static BatchInputHelper initBatchJobInput(final Context jobContext, final GetTaskStrategy getTaskStrategy, final JobInput jobInputTemplate) 
    throws Exception
    {
        final BatchInputHelper batch = new BatchInputHelper();
        batch.setLsid(jobInputTemplate.getLsid());

        final  TaskInfo taskInfo=getTaskStrategy.getTaskInfo(jobInputTemplate.getLsid());
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);

        for(Entry<ParamId,Param> entry : jobInputTemplate.getParams().entrySet()) {
            final Param param=entry.getValue();
            final ParameterInfoRecord record=paramInfoMap.get(param.getParamId().getFqName());
            final GpFilePath batchInputDir=getBatchInputDir(jobContext, record, param);
            if (batchInputDir != null) {
                batch.setBatchDirectory(jobContext, record.getFormal(), param.getParamId(), batchInputDir);
            }
            else {
                //it's not a batch parameter
                for(ParamValue paramValue : param.getValues()) {
                    batch.addValue(param.getParamId(), paramValue.getValue());
                }
            }
        }        
        return batch;
    }
    
    private JobInput inputTemplate=new JobInput();
    private Map<ParamId,Integer> numBatchValuesMap=new HashMap<ParamId,Integer>();
    private Map<ParamId, GpFilePath> batchInputDirs=new LinkedHashMap<ParamId, GpFilePath>();
    private Map<ParamId,List<GpFilePath>> inferredBatchValues=new LinkedHashMap<ParamId,List<GpFilePath>>();
    
    public void setLsid(final String lsid) {
        inputTemplate.setLsid(lsid);
    }

    /**
     * Add a value for a non-batch parameter. 
     * 
     * @param paramId
     * @param value
     */
    public void addValue(final ParamId paramId, final String value) {
        inputTemplate.addValue(paramId, value, false);
    }
    
    /**
     * Add a value for a batch parameter.
     * @param paramId
     * @param value
     */
    public void addBatchValue(final ParamId paramId, final String value) {
        inputTemplate.addValue(paramId, value, true);
        if (!numBatchValuesMap.containsKey(paramId)) {
            numBatchValuesMap.put(paramId, 1);
        }
        else {
            int n=numBatchValuesMap.get(paramId);
            ++n;
            numBatchValuesMap.put(paramId, n);
        }
    }
    
    /**
     * When you assign an input parameter (e.g. 'input.file') to a batch directory, you are telling the server to
     * automatically generate a batch of jobs, one for each matching file in the batch directory.
     * 
     * A file matches based on the file extension of the file and the list of accepted fileFormats for the parameter,
     * as declared in the module manifest.
     * 
     * Extra steps are required if you have more than one batch directory.
     * 
     * @param jobContext
     * @param formalParam
     * @param paramId
     * @param batchDir
     * @throws Exception
     */
    private void setBatchDirectory(final Context jobContext, final ParameterInfo formalParam, final ParamId paramId, final GpFilePath batchDir) 
    throws Exception
    {
        batchInputDirs.put(paramId, batchDir);
        //infer batch parameters
        final List<GpFilePath> batchInputFiles=getBatchInputFiles(jobContext, formalParam, batchDir);
        inferredBatchValues.put(paramId, batchInputFiles);
    }
    
    private int getNumBatchJobs() {
        int numJobs=0;
        for(Param param : inputTemplate.getBatchParams()) {
            numJobs=param.getNumValues();
            //TODO: this assumes all batch params have the same number of values
            return numJobs;
        }
        return numJobs;
    }

    private ParamValue getValue(ParamId paramId, int idx) {
        return inputTemplate.getParam(paramId).getValues().get(idx);
    }

    public JobInput prepareJobInput(final int idx, final JobInput template) throws Exception {
        //start with a copy of the jobInput template
        JobInput nextJobInput = new JobInput(template);
        //then replace batch parameters with the values for this particular (idx) batch job
        for(Param batchParamIn : template.getBatchParams()) {
            Param batchParam=new Param(batchParamIn.getParamId(), false);
            ParamValue batchParamValue=getValue(batchParamIn.getParamId(), idx);
            batchParam.addValue(batchParamValue);
            nextJobInput.setValue(batchParamIn.getParamId(), batchParam);
        }
        return nextJobInput;
    }
    
    /**
     * If necessary, automatically add batch parameters for any declared batch directories.
     * Also, compute the intersection of basenames when there are multiple batch directories.
     * 
     * @throws Exception
     */
    private void initValuesFromBatchDirectories() throws Exception {
        if (inferredBatchValues.size()==0) {
            return;
        }
        //if there is only one batch parameter ...
        if (inferredBatchValues.size()==1) {
            for(final Entry<ParamId,List<GpFilePath>> entry : inferredBatchValues.entrySet()) {
                for(GpFilePath inputFile : entry.getValue()) {
                    final String value=inputFile.getUrl().toExternalForm();
                    addBatchValue(entry.getKey(), value);
                }
            }
            return;
        }

        //for multi batch parameters ... first get the intersection of common basenames
        boolean first=true;
        Set<String> commonBasenames = new LinkedHashSet<String>();
        for(final Entry<ParamId,List<GpFilePath>> entry : inferredBatchValues.entrySet()) {
            Set<String> basenames=new LinkedHashSet<String>();
            for(GpFilePath inputFile : entry.getValue()) {
                final String basename = getBaseFilename(inputFile);
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
        
        //if there are any common basenames, only add the parameters which match
        //ensure the values are added in the correct order
        for(final Entry<ParamId,List<GpFilePath>> entry : inferredBatchValues.entrySet()) {
            SortedMap<String, GpFilePath> sortedValues=new TreeMap<String, GpFilePath>();
            for(final GpFilePath inputFile : entry.getValue()) {
                final String basename=getBaseFilename(inputFile);
                if (commonBasenames.contains(basename)) {
                    sortedValues.put(basename,inputFile);
                }
            }
            for(final Entry<String,GpFilePath> next : sortedValues.entrySet()) {
                final GpFilePath inputFile = next.getValue();
                final String url=inputFile.getUrl().toExternalForm();
                addBatchValue(entry.getKey(), url);
            }
        }
        
    }

    protected String getBaseFilename(GpFilePath file) {
        int periodIndex = file.getName().lastIndexOf('.');
        if (periodIndex > 0) {
            return file.getName().substring(0, periodIndex);
        }
        else {
            return file.getName();
        }
    }
    
    public List<JobInput> prepareBatch() throws Exception {
        initValuesFromBatchDirectories();

        List<JobInput> batchInputs=new ArrayList<JobInput>();
        int numJobs=getNumBatchJobs();
        if (numJobs==0) {
            batchInputs.add(inputTemplate);
            return batchInputs;
        }

        //it is a batch job
        for(int idx=0; idx<numJobs; ++idx) {
            JobInput nextJob=prepareJobInput(idx, inputTemplate);
            batchInputs.add(nextJob);
        }
        return batchInputs;
    }
    
    /**
     * Any input parameter which accepts one and only one file, but which is given a directory input parameter
     * should be flagged as a batch input parameter.
     * 
     * @return
     */
    private static GpFilePath getBatchInputDir(final Context jobContext, final ParameterInfoRecord record, final Param param) {
        if (record.getFormal().isInputFile()) {
            ParamListHelper plh=new ParamListHelper(jobContext, record, param);
            GpFilePath batchInputDir=plh.getBatchInputDirectory();
            return batchInputDir;
        }
        return null;
    }

    private static List<GpFilePath> getBatchInputFiles(final Context jobContext, final ParameterInfo pinfo, final GpFilePath batchDir) throws Exception { 
        //assume that the value is a directory on the server  
        List<GpFilePath> out=new ArrayList<GpFilePath>();
        
        boolean isAdmin=jobContext.isAdmin();
        MultiFileParameter mfp = BatchSubmit.getMultiFileParameter(isAdmin, jobContext, batchDir);
        for(GpFilePath file : mfp.getFiles()) {
            boolean accepted=accept(pinfo, file);
            if (accepted) {
                //final String value=file.getUrl().toExternalForm();
                out.add(file);
            }
        }
        return out;
    }

    /**
     * Is the given input value a valid batch input parameter for the given pinfo?
     * 
     * @param pinfo
     * @param in
     * @return
     */
    private static boolean accept(final ParameterInfo pinfo, final GpFilePath inputValue) {
        if (inputValue.isDirectory()) {
            //special-case for directory inputs
            if (pinfo._isDirectory()) {
                return true;
            }
            else {
                return false;
            }
        }
        List<String> fileFormats = SemanticUtil.getFileFormats(pinfo);
        if (fileFormats.size()==0) {
            //no declared fileFormats, acceptAll
            return true;
        }
        final String kind=inputValue.getKind();
        if (fileFormats.contains(kind)) {
            return true;
        }
        final String ext = inputValue.getExtension();
        if (fileFormats.contains(ext)) {
            return true;
        }
        return false;
    }
}

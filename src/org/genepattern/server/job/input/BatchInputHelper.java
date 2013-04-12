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

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.domain.AnalysisJobDAO;
import org.genepattern.server.domain.BatchJob;
import org.genepattern.server.domain.BatchJobDAO;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.server.job.input.JobInput.ParamValue;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApi;
import org.genepattern.server.rest.JobInputApiFactory;
import org.genepattern.server.rest.JobInputApiLegacy.ParameterInfoRecord;
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.server.webapp.BatchSubmit;
import org.genepattern.server.webapp.BatchSubmit.MultiFileParameter;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for batch job input.
 * 
 * @author pcarr
 *
 */
public class BatchInputHelper {
    final static private Logger log = Logger.getLogger(BatchInputHelper.class);

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
    private static BatchInputHelper initBatchJobInput(final Context userContext, final GetTaskStrategy getTaskStrategy, final JobInput jobInputTemplate) 
    throws GpServerException
    {
        final BatchInputHelper batch = new BatchInputHelper(userContext, jobInputTemplate.getLsid());

        final  TaskInfo taskInfo=getTaskStrategy.getTaskInfo(jobInputTemplate.getLsid());
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);

        for(Entry<ParamId,Param> entry : jobInputTemplate.getParams().entrySet()) {
            final Param param=entry.getValue();
            final ParameterInfoRecord record=paramInfoMap.get(param.getParamId().getFqName());
            final GpFilePath batchInputDir=getBatchInputDir(userContext, record, param);
            if (batchInputDir != null) {
                batch.setBatchDirectory(record.getFormal(), param.getParamId(), batchInputDir);
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
    
    private Context userContext=null;
    final JobInputApi singleJobInputApi;
    final GetTaskStrategy getTaskStrategy;
    
    private JobInput inputTemplate=new JobInput();
    private boolean inferBatchParams=false;
    private Map<ParamId,Integer> numBatchValuesMap=new HashMap<ParamId,Integer>();
    private Map<ParamId, String> batchInputDirStrs=new LinkedHashMap<ParamId, String>();
    private Map<ParamId, GpFilePath> batchInputDirs=new LinkedHashMap<ParamId, GpFilePath>();
    private Map<ParamId,List<GpFilePath>> inferredBatchValues=new LinkedHashMap<ParamId,List<GpFilePath>>();
    
    public BatchInputHelper(final Context userContext, final String lsid) {
        this(userContext, lsid, null);
    }
    public BatchInputHelper(final Context userContext, final String lsid, final JobInputApi singleJobInputApi) {
        this(userContext, lsid, singleJobInputApi, null);
    }
    public BatchInputHelper(final Context userContext, final String lsid, final JobInputApi singleJobInputApiIn, final GetTaskStrategy getTaskStrategyIn) {
        this.userContext=userContext;
        inputTemplate.setLsid(lsid);
        if (getTaskStrategyIn == null) {
            getTaskStrategy=new GetTaskStrategyDefault();
        }
        else {
            getTaskStrategy=getTaskStrategyIn;
        }
        if (singleJobInputApiIn == null) {
            this.singleJobInputApi=JobInputApiFactory.createJobInputApi(userContext);
        }
        else {
            this.singleJobInputApi=singleJobInputApiIn;
        }
    }
    
    public void setInferBatchParams(final boolean inferBatchParams) {
        this.inferBatchParams=inferBatchParams;
    }
    
    /**
     * Add a value for a non-batch parameter. 
     * 
     * @param paramId
     * @param value
     */
    public void addValue(final String name, final String value) {
        addValue(new ParamId(name), value);
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
    public void addBatchValue(final String name, final String value) {
        addBatchValue(new ParamId(name), value);
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
    
    public void addBatchDirectory(final String name, final String value) {
        addBatchDirectory(new ParamId(name), value);
    }
    
    public void addBatchDirectory(final ParamId id, final String value) {
        batchInputDirStrs.put(id, value);
    }
    
    public List<JobInput> inferBatch() throws GpServerException {
        BatchInputHelper bih=BatchInputHelper.initBatchJobInput(userContext, getTaskStrategy, inputTemplate);
        return bih.prepareBatch();
    }
    
    /**
     * After you initialize all of the values, call this method to create the
     * list of JobInput, one for each new job to run.
     * 
     * @return
     * @throws GpServerException
     */
    public List<JobInput> prepareBatch() throws GpServerException {
        if (inputTemplate==null) {
            throw new IllegalArgumentException("inputTemplate==null");
        }
        if (inputTemplate.getLsid()==null || inputTemplate.getLsid().length()==0) {
            throw new IllegalArgumentException("lsid not set");
        }
        //inferBatchParams();
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
     * Submit your jobs to the GP server.
     * Use the list of JobInput from the prepareBatch() method as input to this method.
     * 
     * @param batchInputs
     * @return
     * @throws GpServerException
     */
    public JobReceipt submitBatch(final List<JobInput> batchInputs) throws GpServerException {
        JobReceipt receipt=new JobReceipt();
        for(JobInput batchInput : batchInputs) {
            String jobId = singleJobInputApi.postJob(userContext, batchInput);
            receipt.addJobId(jobId);
        }
        
        if (receipt.getJobIds().size()>1) {
            //record batch job to DB, optionally assign custom batch id
            final String batchId=recordBatchJob(userContext, receipt);
            receipt.setBatchId(batchId);
        }
        return receipt;
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
     * @throws GpServerException
     */
    private void setBatchDirectory(final ParameterInfo formalParam, final ParamId paramId, final GpFilePath batchDir) 
    throws GpServerException
    {
        batchInputDirs.put(paramId, batchDir);
        //infer batch parameters
        final List<GpFilePath> batchInputFiles=getBatchInputFiles(userContext, formalParam, batchDir);
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

    private JobInput prepareJobInput(final int idx, final JobInput template) {
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
     * Helper method which is used to create a List of JobInput instances, one for each batch job to run.
     * Use the inputTemplate to infer which parameters are batch parameters
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
    private void inferBatchParams() throws GpServerException {
        if (!inferBatchParams) {
            return;
        }
        
        //if necessary go through the list of input and infer batch input parameters
        final  TaskInfo taskInfo=getTaskStrategy.getTaskInfo(inputTemplate.getLsid());
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);

        for(Entry<ParamId,Param> entry : inputTemplate.getParams().entrySet()) {
            final ParamId paramId=entry.getKey();
            final Param param=entry.getValue();
            if (param.isBatchParam()) {
                //ignore explicitly declared batch params
            }
            else {
                final ParameterInfoRecord record=paramInfoMap.get(param.getParamId().getFqName());
                final GpFilePath batchInputDir=getBatchInputDir(userContext, record, param);
                if (batchInputDir != null) {
                    final String value; 
                    try {
                        value=batchInputDir.getUrl().toExternalForm();
                    }
                    catch (Throwable t) {
                        throw new GpServerException("Error initializing batch input parameter", t);
                    }
                    batchInputDirStrs.put(paramId, value);
                    param.clear();
                }
            }
        }
    }
    
    /**
     * If necessary, automatically add batch parameters for any declared batch directories.
     * Also, compute the intersection of basenames when there are multiple batch directories.
     * 
     * @throws Exception
     */
    private void initValuesFromBatchDirectories() throws GpServerException 
    {
        final TaskInfo taskInfo;
        Map<String,ParameterInfoRecord> paramInfoMap=null;
        if (batchInputDirStrs.size()>0) {
            taskInfo=getTaskStrategy.getTaskInfo(inputTemplate.getLsid());
            paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        }
        for(final Entry<ParamId,String> entry : batchInputDirStrs.entrySet()) {
            final ParamId paramId=entry.getKey();
            final String value=entry.getValue();
            final GpFilePath batchInputDir=ParamListHelper.getBatchInputDir(value);
            if (batchInputDir != null) {
                final ParameterInfoRecord record=paramInfoMap.get(paramId.getFqName());
                setBatchDirectory(record.getFormal(), paramId, batchInputDir);
            }
            else {
                //TODO: this is an error
                log.error("Invalid batch input directory: "+value);
            }
        }
        
        if (inferredBatchValues.size()==0) {
            return;
        }
        //if there is only one batch parameter ...
        if (inferredBatchValues.size()==1) {
            for(final Entry<ParamId,List<GpFilePath>> entry : inferredBatchValues.entrySet()) {
                for(GpFilePath inputFile : entry.getValue()) {
                    final String value;
                    try {
                        value=inputFile.getUrl().toExternalForm();
                    }
                    catch (Throwable t) {
                        throw new GpServerException(t.getLocalizedMessage(),t);
                    }
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
                final String value;
                try {
                    value=inputFile.getUrl().toExternalForm();
                }
                catch (Throwable t) {
                    throw new GpServerException(t.getLocalizedMessage(),t);
                }
                addBatchValue(entry.getKey(), value);
            }
        }
        
    }

    private String getBaseFilename(GpFilePath file) {
        int periodIndex = file.getName().lastIndexOf('.');
        if (periodIndex > 0) {
            return file.getName().substring(0, periodIndex);
        }
        else {
            return file.getName();
        }
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

    private static List<GpFilePath> getBatchInputFiles(final Context jobContext, final ParameterInfo pinfo, final GpFilePath batchDir) 
    throws GpServerException 
            { 
        //assume that the value is a directory on the server  
        List<GpFilePath> out=new ArrayList<GpFilePath>();
        
        boolean isAdmin=jobContext.isAdmin();
        MultiFileParameter mfp=null;
        try {
            mfp = BatchSubmit.getMultiFileParameter(isAdmin, jobContext, batchDir);
        }
        catch (Throwable t) {
            throw new GpServerException(t.getLocalizedMessage(), t);
        }
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
    
    private String recordBatchJob(final Context userContext, final JobReceipt jobReceipt) throws GpServerException {
        //legacy implementation, based on code in SubmitJobServlet
        String batchId="";
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            BatchJob batchJob = new BatchJob(userContext.getUserId());

            for(final String jobId : jobReceipt.getJobIds()) {
                int jobNumber = Integer.parseInt(jobId);
                batchJob.getBatchJobs().add(new AnalysisJobDAO().findById(jobNumber));
            }
            new BatchJobDAO().save(batchJob);
            batchId = ""+batchJob.getJobNo();
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new GpServerException("Error recording batch jobs to DB", t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return batchId;
    }

}

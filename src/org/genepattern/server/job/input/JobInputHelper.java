package org.genepattern.server.job.input;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
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
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
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
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
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
    public static URL initExternalUrl(final String value) {
        log.debug("intialize external URL for value="+value);
        if (value==null) {
            throw new IllegalArgumentException("value==null");
        }

        if (value.startsWith("<GenePatternURL>")) {
            log.debug("it's a substition for the gp url");
            return null;
        }
        if (value.startsWith(GpFilePath.getGenePatternUrl().toExternalForm())) {
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

    //added these static methods to support batch jobs
    /**
     * Get the GpFilePath for a batch input directory, if and only if, the given value
     * is a valid batch input directory. Otherwise, return null.
     * @param value
     * @return
     */
    public static GpFilePath initGpFilePath(final String value) {
        GpFilePath gpPath=null;
        URL externalUrl=initExternalUrl(value);
        if (externalUrl!=null) {
            //it's an externalURL
            return null;
        }

        try {
            gpPath = GpFileObjFactory.getRequestedGpFileObj(value);
            return gpPath;
        }
        catch (Exception e) {
            log.debug("getRequestedGpFileObj("+value+") threw an exception: "+e.getLocalizedMessage(), e);
            //ignore
        }
        
        //if we are here, it could be a server file path
        File serverFile=new File(value);
        gpPath = ServerFileObjFactory.getServerFile(serverFile);
        return gpPath;
    }
    
    /**
     * Get the GpFilePath for a batch input directory, if and only if, the given value
     * is a valid batch input directory. Otherwise, return null.
     * @param value
     * @return
     */
    public static GpFilePath getBatchInputDir(final String value) {
        final GpFilePath gpPath=initGpFilePath(value);
        if (gpPath != null && gpPath.isDirectory()) {
            return gpPath;
        }
        return null;
    }

    private static FilenameFilter listFilesFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            if (".svn".equals(name)) {
                return false;
            }
            return true;
        }
    };

    private Context userContext=null;
    final JobInputApi singleJobInputApi;
    final GetTaskStrategy getTaskStrategy;
    
    private JobInput inputTemplate=new JobInput();
    private boolean deduceBatchValues=false;
    private Map<ParamId,Integer> numBatchValuesMap=new HashMap<ParamId,Integer>();
    private Map<ParamId, List<String>> batchInputDirStrs=new LinkedHashMap<ParamId, List<String>>();
    private Map<ParamId,List<GpFilePath>> batchValues=new LinkedHashMap<ParamId,List<GpFilePath>>();
    
    public JobInputHelper(final Context userContext, final String lsid) {
        this(userContext, lsid, null);
    }
    public JobInputHelper(final Context userContext, final String lsid, final JobInputApi singleJobInputApi) {
        this(userContext, lsid, singleJobInputApi, null);
    }
    public JobInputHelper(final Context userContext, final String lsid, final JobInputApi singleJobInputApiIn, final GetTaskStrategy getTaskStrategyIn) {
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
    
    /**
     * When this is set to true, it means, automatically initialize batch input parameters,
     * based on use supplied values. 
     * 
     * The rule is as follows:
     *     When an input parameter value is a directory, and the parameter does not accept directory as an input value,
     *     it must be a batch input directory.
     * @param deduceBatchValues
     */
    public void setDeduceBatchValues(final boolean deduceBatchValues) {
        this.deduceBatchValues=deduceBatchValues;
    }
    
    /**
     * Add a value for a non-batch parameter. 
     * 
     * @param name
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
     * @param name
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

    /*
     * Add a value for a parameter which could be either a single or batch parameter
     * @param pInfo - A ParameterInfo object
     * @param value - The value to set the parameter identified by pInfo
     * @param isBatch  - Whether the parameter is a batch parameter
     */
    public void addSingleOrBatchValue(final ParameterInfo pInfo, String value, boolean isBatch)
    {
        //No pInfo or value found so do nothing
        if(pInfo == null || value == null)
        {
            return;
        }

        String pName = pInfo.getName();

        //No parameter name found so do nothing
        if(pName == null)
        {
            return;
        }
        //determine if this is a directory
        GpFilePath filepath = getBatchInputDir(value);
        if(isBatch)
        {
            if(filepath != null && filepath.isDirectory())
            {
                addBatchDirectory(pName, value);
            }
            else
            {
                addBatchValue(pName, value);
            }
        }
        else
        {
            addValue(pName, value);
        }
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
    public void addBatchDirectory(final String name, final String value) {
        addBatchDirectory(new ParamId(name), value);
    }
    
    public void addBatchDirectory(final ParamId id, final String value) {
        //if the value is empty then do not add it
        if(value == null || value.length() < 1)
        {
            return;
        }

        List values = batchInputDirStrs.get(id);
        if(values == null)
        {
            values = new ArrayList();
            batchInputDirStrs.put(id, values);
        }

        values.add(value);
    }
    
    /**
     * After you initialize all of the values, call this method to create the
     * list of JobInputs, one for each new job to run.
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
        
        deduceBatchValues();
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
    
    private int getNumBatchJobs() throws GpServerException {
        int numJobs=0;
        for(Param param : inputTemplate.getBatchParams()) {
            if (numJobs==0) {
                numJobs=param.getNumValues();
            }
            else {
                //validate
                if (numJobs != param.getNumValues()) {
                    //error
                    throw new GpServerException("Number of batch parameters doesn't match, ");
                    
                }
            }
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
     * Helper method which automatically converts the initial input value into batch input directories, 
     * based on the type of the input parameter and the value. If the type expects a file and the value
     * is for a directory, automatically convert to a batch input parameter.
     */
    private void deduceBatchValues() throws GpServerException {
        if (!deduceBatchValues) {
            return;
        }
        
        //go through the list of inputs and deduce batch input parameters
        List<ParamId> addedBatchDirectories=new ArrayList<ParamId>();

        for(Entry<ParamId,Param> entry : inputTemplate.getParams().entrySet()) {
            final ParamId paramId=entry.getKey();
            final Param param=entry.getValue();
            if (param.isBatchParam()) {
                //ignore explicitly declared batch params
            }
            else if (param.getNumValues()!=1) {
                //only check when numValues==1
                log.error("numValues=="+param.getNumValues()+": can't deduce batch values when there is more than one value for the parameter");
            }
            else {
                final String valueIn=param.getValues().get(0).getValue();
                final GpFilePath batchInputDir=getBatchInputDir(valueIn);
                if (batchInputDir!=null) {
                    //automatically convert to a batch param
                    try {
                        this.addBatchDirectory(paramId, batchInputDir.getUrl().toExternalForm());
                        addedBatchDirectories.add(paramId);
                    }
                    catch (Throwable t) {
                        log.error(t);
                        throw new GpServerException("Error deducing batch directory for: "+valueIn);
                    }
                }
            }
        }
        for(ParamId paramId : addedBatchDirectories) {
            inputTemplate.removeValue(paramId);
        }
    }

    /**
     * 
     * @param formalParam
     * @param initialValue
     * @return
     */
    private List<GpFilePath> listBatchDir(final ParameterInfo formalParam, final String initialValue) throws GpServerException {
        if (initialValue==null) {
            throw new IllegalArgumentException("initialValue==null");
        }
        if (formalParam==null) {
            throw new IllegalArgumentException("formalParam==null");
        }
        final GpFilePath batchInputDir=getBatchInputDir(initialValue);
        if (batchInputDir==null) {
            //error: initialValue is not a valid batch input directory
            final String errorMessage=""+initialValue+" is not a valid batch input directory";
            throw new GpServerException(errorMessage);
        }
        if (!batchInputDir.canRead(userContext.isAdmin(), userContext)) {
            throw new GpServerException("The current user ("+userContext.getUserId()+") doesn't have permission to read the batch input directory: "+initialValue);
        }
        if (!batchInputDir.getServerFile().exists()) {
            //another error
            throw new GpServerException("Can't read batch input directory: "+batchInputDir.getRelativeUri().toString());
        }
        if (!batchInputDir.getServerFile().isDirectory()) {
            final String errorMessage=""+initialValue+" is not a valid batch input directory";
            throw new GpServerException(errorMessage);
        }
        
        final List<GpFilePath> batchInputFiles=getBatchInputFiles(formalParam, batchInputDir);
        if (batchInputFiles.size()==0) {
            throw new GpServerException("No matching input files in batch input directory: "+initialValue);
        }
        return batchInputFiles;
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
        for(final Entry<ParamId,List<String>> entry : batchInputDirStrs.entrySet()) {
            final ParamId paramId=entry.getKey();
            final List<String> initialValues=entry.getValue();
            final ParameterInfoRecord record=paramInfoMap.get(paramId.getFqName());
            if (record==null) {
                //invalid parameter name, the module does not have a parameter with this name
                throw new GpServerException("Invalid parameter name for batch, "+paramId.getFqName());
            }
            final ParameterInfo formalParam=record.getFormal();

            List<GpFilePath> fileList = new ArrayList();
            for(String batchDir : initialValues)
            {
                fileList.addAll(this.listBatchDir(formalParam, batchDir));
            }
            //TODO: get rid of the batchValues map
            batchValues.put(paramId, fileList);
        }
        
        if (batchValues.size()==0) {
            return;
        }
        //if there is only one batch parameter ...
        if (batchValues.size()==1) {
            for(final Entry<ParamId,List<GpFilePath>> entry : batchValues.entrySet()) {
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
        for(final Entry<ParamId,List<GpFilePath>> entry : batchValues.entrySet()) {
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
        
        //special-case: no matching parameters, because commonBasenames is empty
        if (commonBasenames.isEmpty()) {
            throw new GpServerException("No matching input files for multi-batch job.");
        }
        
        //if there are any common basenames, only add the parameters which match
        //ensure the values are added in the correct order
        for(final Entry<ParamId,List<GpFilePath>> entry : batchValues.entrySet()) {
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

    public static String getBaseFilename(GpFilePath file) {
        int periodIndex = file.getName().lastIndexOf('.');
        if (periodIndex > 0) {
            return file.getName().substring(0, periodIndex);
        }
        else {
            return file.getName();
        }
    }
    
    /**
     * Get the list of matching input files in the given batch directory,
     * only include files which are valid input types for the given pinfo.
     * 
     * @param pinfo, the ParameterInfo instance for the batch input parameter.
     * @param batchDir, the value of the batch input directory.
     * @return
     * @throws GpServerException
     */
    public static List<GpFilePath> getBatchInputFiles(final ParameterInfo pinfo, final GpFilePath batchDir) throws GpServerException {
        final String parentUrl;
        try {
            parentUrl=batchDir.getUrl().toExternalForm();
        }
        catch (Exception e) {
            throw new GpServerException("Error initializing parentUrl: "+batchDir.getRelativeUri().toString());
        }
        List<GpFilePath> filePaths = new ArrayList<GpFilePath>();
        File[] files = batchDir.getServerFile().listFiles(listFilesFilter);
        for(File file : files) {
            final String fileUrl = parentUrl + UrlUtil.encodeURIcomponent( file.getName() );
            try {
                GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj(fileUrl);
                filePath.initMetadata();
                if (accept(pinfo,filePath)) {
                    filePaths.add(filePath);
                }
            }
            catch (Throwable t) {
                log.error("Server error preparing batch input fileUrl="+fileUrl, t);
            }
        }
        return filePaths;
    }

    /**
     * Is the given input value a valid batch input parameter for the given parameter?
     * 
     * Policy (circa GP <=3.6.0):
     *    if the pinfo is of type DIRECTORY, accept only input values which are directories
     *    if the pinfo is of type FILE,
     *        if it has fileFormats, accept any file which matches one of the file formats
     *        if it has not fileFormats, match any file which is not a directory
     *    otherwise, it's not a match
     * 
     * @param pinfo
     * @param inputValue
     * @return
     */
    private static boolean accept(final ParameterInfo pinfo, final GpFilePath inputValue) {
        //special-case for DIRECTORY input parameter
        if (pinfo._isDirectory()) {
            if (inputValue.isDirectory()) {
                return true;
            }
            else {
                return false;
            }
        }
        
        if (inputValue.isDirectory()) {
            //the value is a directory, but the parameter type is not a directory
            log.debug("Not implemented!");
            return false;
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

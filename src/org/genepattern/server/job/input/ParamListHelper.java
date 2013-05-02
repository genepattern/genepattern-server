package org.genepattern.server.job.input;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.server.job.input.JobInput.ParamValue;
import org.genepattern.server.rest.JobInputApiLegacy.ParameterInfoRecord;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class, instantiated as part of processing user input, before adding a job to the queue.
 * This class, when needed, will generate a filelist file based on the list of values from the job input form.
 * 
 * @author pcarr
 *
 */
public class ParamListHelper {
    final static private Logger log = Logger.getLogger(ParamListHelper.class);

    public enum ListMode { 
        /**
         * When listMode=legacy and num input files is ...
         *     0, no cmd line arg
         *     1, the data file is the cmd line arg
         *     >1, the filelist is the cmd line arg
         *     
         * This is for compatibility with older versions of GP, send data files by value, when only one input value is submitted.
         */
        LEGACY, 
        /**
         * When listMode=list and num input files is ...
         *     0, no cmd line arg
         *     >0, the filelist is the cmd line arg
         *     
         * For newer (3.6+) versions of GP, always send filelist files, except for when the list is empty.
         */
        LIST,
        /**
         * When listMode=listIncludeEmpty, always create a filelist file on the cmd line, even for empty lists.
         */
        LIST_INCLUDE_EMPTY
    }

    /**
     * Helper method for getting the number of allowed values for a given input parameter.
     * 
     * @param pinfo
     * @return 
     * @throws IllegalArgumentException if it can't parse the numValues string
     */
    public static NumValues initNumValues(final ParameterInfo pinfo) {
        if (pinfo==null) {
            throw new IllegalArgumentException("pinfo==null");
        }
        
        final String numValuesStr;
        if (pinfo.getAttributes()==null) {
            numValuesStr=null;
        }
        else {
            numValuesStr = (String) pinfo.getAttributes().get("numValues");
        }
        NumValues numValues=null;
        
        if (numValuesStr!=null && numValuesStr.trim().length()>0) {
            NumValuesParser nvParser=new NumValuesParserImpl();
            try { 
                numValues=nvParser.parseNumValues(numValuesStr);
            }
            catch (Exception e) {
                String message="Error parsing numValues="+numValuesStr+" for "+pinfo.getName();
                log.error(message,e);
                throw new IllegalArgumentException(message);
            }
        }
        
        if (numValues==null) {
            //if numValues is null, initialize it based on optional
            boolean optional=pinfo.isOptional();
            int min=1;
            if (optional) {
                min=0;
            }
            numValues=new NumValues(min, 1);
        }
        return numValues;
    }

    /**
     * Utility method for getting the input values from a (presumably completed) job.
     * Use this to initialize the jQuery job input form for a reloaded job.
     * 
     * @param userContext
     * @param jobId
     * @return
     */
    static public JobInput getInputValues(final Context userContext, final String jobId) throws Exception {
        /*
         * Call this method if you don't already have a JobInfo initialized.
         * This method call is deliberately agnostic about how we load the info from the GP server.
         * At the moment (circa GP 3.5) it is getting the values from the ANALYSIS_JOB.PARAMETER_INFO CLOB.
         */
        JobInfo jobInfo = initJobInfo(userContext, jobId);
        return getInputValues(jobInfo);
    }

    /**
     * Helper method for initializing the 'fileFormat' from a given file name.
     * Usually we shouldn't need to call this method because both '_file=' and
     * '_format=' request parameters are set from the send-to menu.
     * 
     * @param _fileParam
     * @return
     */
    public static String getType(final String _fileParam) {
        int idx=_fileParam.lastIndexOf(".");
        if (idx<0) {
            log.debug("file has no extension: "+_fileParam);
            return "";
        }
        if (idx==_fileParam.length()-1) {
            log.debug("file ends with '.': "+_fileParam);
            return "";
        }
        return _fileParam.substring(idx+1);
    }

    public static JobInput getInitialValues(
            ParameterInfo[] parameterInfos, //the formal input parameters
            final JobInput reloadedValues, 
            final String _fileParam,
            String _formatParam,
            final Map<String,String[]> parameterMap
    )
    throws Exception
    {
        if (parameterInfos == null) {
            throw new IllegalArgumentException("parameterInfos==null");
        }
        JobInput initialValues = new JobInput();
        //TODO: initialValues.setLsid(lsid);
        for(ParameterInfo pinfo : parameterInfos) {
            final String pname=pinfo.getName();
            //1) initialize from default values
            final List<String> defaultValues=ParamListHelper.getDefaultValues(pinfo);
            if (defaultValues != null) {
                boolean first=true;
                for(final String defaultValue : defaultValues) {
                    if (first) {
                        initialValues.addOrReplaceValue(pname, defaultValue);
                        first=false;
                    }
                    else {
                        initialValues.addValue(pname, defaultValue);
                    }
                }
            }
            
            //2) if it's a reloaded job, use that
            if (reloadedValues != null) {
                boolean first=true;
                for(final ParamValue reloadedValue : reloadedValues.getParamValues(pname)) {
                    if (first) {
                        initialValues.addOrReplaceValue(pname, reloadedValue.getValue());
                        first=false;
                    }
                    else {
                        initialValues.addValue(pname, reloadedValue.getValue());
                    }
                }
            }

            //3) if there's a matching request parameter, use that
            if (parameterMap.containsKey(pname)) {
                //List<String> fromRequestParam=new ArrayList<String>();
                boolean first=true;
                for(String requestParam : parameterMap.get(pname)) {
                    if (first) {
                        initialValues.addOrReplaceValue(pname, requestParam);
                        first=false;
                    }
                    else {
                        initialValues.addValue(pname, requestParam);
                    }
                }
            }
            
            //validate numValues
            NumValues numValues=ParamListHelper.initNumValues(pinfo);
            if (numValues.getMax() != null) {
                final Param param=initialValues.getParam(pname);
                if (param!=null) {
                    if (param.getNumValues()>numValues.getMax()) {
                        //this is an error: more input values were specified than
                        //this parameter allows so throw an exception
                        throw new Exception(" Error: " + param.getNumValues() + " input values were specified for " +
                                pname + " but a maximum of " + numValues.getMax() + " is allowed. ");
                    }
                }
            }
        }
        
        //special-case for send-to module from file
        if (_fileParam != null && _fileParam.length() != 0) {
            if (_formatParam == null || _formatParam.length() == 0) {
                log.error("_format request parameter is not set, _file="+_fileParam);
                _formatParam=getType(_fileParam);
            }
            
            //find the first parameter which matches the type of the file
            for(ParameterInfo pinfo : parameterInfos) {
                List<String> fileFormats=ParamListHelper.getFileFormats(pinfo);
                if (pinfo != null) {
                    if (fileFormats.contains(_formatParam)) {
                        //we found the first match
                        initialValues.addOrReplaceValue(pinfo.getName(), _fileParam);
                    }
                }
            }
        }
        return initialValues;
    }
    
    /**
     * Helper method for initializing the values for the job input form.
     * Set initial values for the parameters for the following cases:
     * 
     * 1) a reloaded job
     * 2) values set in request parameters, when linking from the protocols page
     * 3) send to module, from the context menu for a file
     * 
     * @param parameterInfos, the list of formal parameters, from the TaskInfo object
     * @param reloadedValues, the values from the original job, if this is a job reload request
     * @param _fileParam, the input file value, if this is from a send-to module request
     * @param _formatParam, the input file type, if this is from a send-to module request
     * @param parameterMap, the HTTP request parameters, if this is from protocols page link
     * 
     * @return a JSON representation of the initial input values
     * @throws JSONException
     */
    public static JSONObject getInitialValuesJson(
            ParameterInfo[] parameterInfos, //the formal input parameters
            final JobInput reloadedValues, 
            final String _fileParam,
            String _formatParam,
            final Map<String,String[]> parameterMap
    )
    throws JSONException, Exception
    {
        JobInput initialValues=getInitialValues(
                parameterInfos,
                reloadedValues,
                _fileParam,
                _formatParam,
                parameterMap);
            
        JSONObject values=new JSONObject();
        for(Entry<ParamId, Param> entry : initialValues.getParams().entrySet()) {
            final String pname=entry.getKey().getFqName();
            JSONArray jsonArray = new JSONArray();
            for(final ParamValue val : entry.getValue().getValues()) {
                jsonArray.put(val.getValue());
            }
            values.put(pname, jsonArray);
        }
        return values;
    }

    /**
     * Get a JobInfo from the DB, for the given jobId.
     * (Legacy code copied from the RunTaskBean#setTask method).
     * 
     * @param userContext, Must be non-null with a valid userId
     * @param jobId, Must be non-null
     * 
     * @return
     * 
     * @throws Exception for the following,
     *     1) if there is no job with jobId in the DB
     *     2) if the current user does not have permission to 'read' the job
     */
    private static JobInfo initJobInfo(final Context userContext, final String jobId) throws Exception {
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        if (userContext.getUserId()==null || userContext.getUserId().length()==0) {
            throw new IllegalArgumentException("userContext.userId is not set");
        }
        if (jobId==null) {
            throw new IllegalArgumentException("jobId==null");
        }
        final int jobNumber;
        try {
            jobNumber=Integer.parseInt(jobId);
        }
        catch (Throwable t) {
            throw new Exception("Error parsing jobId="+jobId, t);
        }
        JobInfo jobInfo = new AnalysisDAO().getJobInfo(jobNumber);
        if (jobInfo==null) {
            throw new Exception("Can't load job, jobId="+jobId);
        }

        // check permissions
        final boolean isAdmin = AuthorizationHelper.adminJobs(userContext.getUserId());
        PermissionsHelper perm = new PermissionsHelper(isAdmin, userContext.getUserId(), jobNumber);
        if (!perm.canReadJob()) {
            throw new Exception("User does not have permission to load job");
        }
        return jobInfo;
    }
    
    /**
     * Utility method for getting the original input parameters from a 'reloaded' job, 
     * when you already have a JobInfo initialized from the DB.
     * 
     * @param reloadJob
     * @return
     */
    private static JobInput getInputValues(final JobInfo reloadJob) {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(reloadJob.getTaskLSID());
        Map<String, List<String>> orig=getOriginalInputValues(reloadJob);
        for(final Entry<String, List<String>> entry : orig.entrySet()) {
            final String pname=entry.getKey();
            for(String value : entry.getValue()) {
                jobInput.addValue(pname, value);
            }
        }
        return jobInput;
    }


    //inputs
    Context jobContext;
    ParameterInfoRecord record;
    Param actualValues;
    //outputs
    NumValues allowedNumValues;
    ListMode listMode=ListMode.LEGACY;

    public ParamListHelper(final Context jobContext, final ParameterInfoRecord record, final Param actualValues) {
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (record==null) {
            throw new IllegalArgumentException("record==null");
        }
        if (actualValues==null) {
            throw new IllegalArgumentException("actualValues==null");
        }
        this.jobContext=jobContext;
        this.record=record;
        this.actualValues=actualValues;

        initAllowedNumValues();

        //initialize list mode
        String listModeStr = (String) record.getFormal().getAttributes().get("listMode");
        if (listModeStr != null && listModeStr.length()>0) {
            listModeStr = listModeStr.toUpperCase().trim();
            try {
                listMode=ListMode.valueOf(listModeStr);
            }
            catch (Throwable t) {
                String message="Error initializing listMode from listMode="+listModeStr;
                log.error(message, t);
                throw new IllegalArgumentException(message);
            }
        }
    }

    private void initAllowedNumValues() {
        final String numValuesStr = (String) record.getFormal().getAttributes().get("numValues");
        //parse num values string
        NumValuesParser nvParser=new NumValuesParserImpl();
        try { 
            allowedNumValues=nvParser.parseNumValues(numValuesStr);
        }
        catch (Exception e) {
            String message="Error parsing numValues="+numValuesStr+" for "+record.getFormal().getName();
            log.error(message,e);
            throw new IllegalArgumentException(message);
        }
    }

    public static List<String> getDefaultValues(final ParameterInfo pinfo) {
        //parse default_values param ... 
        //TODO: implement support for default values as a list of values
        if (pinfo.getDefaultValue() != null) {
            List<String> defaultValues=new ArrayList<String>();
            defaultValues.add(pinfo.getDefaultValue());
            return defaultValues;
        }
        return null;
    }
    
    public static List<String> getFileFormats(final ParameterInfo pinfo) {
        if (pinfo.isInputFile()) {
            String fileFormatsString = (String) pinfo.getAttributes().get(GPConstants.FILE_FORMAT);
            if (fileFormatsString == null || fileFormatsString.equals("")) {
                return Collections.emptyList();
            }

            List<String> inputFileTypes=new ArrayList<String>();
            StringTokenizer st = new StringTokenizer(fileFormatsString, GPConstants.PARAM_INFO_CHOICE_DELIMITER);
            while (st.hasMoreTokens()) {
                String type = st.nextToken();
                inputFileTypes.add(type);
            }
            return inputFileTypes;
        }
        else if (pinfo._isDirectory()) {
            List<String> inputFileTypes=new ArrayList<String>();
            inputFileTypes.add("directory");
            return inputFileTypes;
        }
        return Collections.emptyList();
    }

    public NumValues getAllowedNumValues() {
        return allowedNumValues;
    }

    public boolean acceptsList() {
        if (allowedNumValues==null) {
            log.debug("allowedNumValues==null");
            return false;
        } 
        return allowedNumValues.acceptsList();
    }
    
    /**
     * @throws IllegalArgumentException if number of input values entered is not within
     *      the allowed range of values. For example,
     *          a missing required parameter, num input values is 0, for a required parameter
     *          not enough args, num input vals is 1 when numValues=2+
     *          too many args, num input vals is 5 when numValues=0..4
     */
    public void validateNumValues() {
        final int numValuesSet=actualValues.getNumValues();
        //when allowedNumValues is not set or if minNumValues is not set, it means there is no 'numValues' attribute for the parameter, assume it's not a filelist
        if (allowedNumValues==null || allowedNumValues.getMin()==null) {
            if (numValuesSet==0) {
                if (!record.getFormal().isOptional()) {
                    throw new IllegalArgumentException("Missing required parameter: "+record.getFormal().getName());
                }
            }
            //everything else is valid
            return;
        }

        //if we're here, it means numValues is set, need to check for filelists
        //are we in range?
        if (numValuesSet < allowedNumValues.getMin()) {
            throw new IllegalArgumentException("Not enough values for "+record.getFormal().getName()+
                    ", num="+numValuesSet+", min="+allowedNumValues.getMin());
        }
        if (allowedNumValues.getMax() != null) {
            //check upper bound
            if (numValuesSet > allowedNumValues.getMax()) {
                throw new IllegalArgumentException("Too many values for "+record.getFormal().getName()+
                        ", num="+numValuesSet+", max="+allowedNumValues.getMax());
            }
        }
    }
    
    /**
     * Do we need to create a filelist file for this parameter?
     * Based on the following rules.
     * 1) when the actual number of values is >1, always create a file list.
     * 2) when there are 0 values, depending on the listMode
     *     LEGACY, no
     *     LIST, no
     *     LIST_INCLUDE_EMPTY, yes
     * 3) when there is 1 value, depending on the listMode
     *     LEGACY, no
     *     LIST, yes
     *     LIST_INCLUDE_EMPTY, yes
     * @return
     */
    public boolean isCreateFilelist() {
        final int numValuesSet=actualValues.getNumValues();
        if (numValuesSet>1) {
            //always create a filelist when there are more than 1 values
            return true;
        }

        //special-case for 0 args, depending on the listMode  
        if (numValuesSet==0) {
            //special-case for empty list 
            if (ListMode.LEGACY.equals( listMode ) ||
                    ListMode.LIST.equals( listMode )) {
                return false;
            }
            return true;
        }

        //special-case for 1 arg
        if (ListMode.LEGACY.equals( listMode )) {
            return false;
        }
        return true;
    }

    private static String replaceGpUrl(final String in) {
        if (!in.startsWith("<GenePatternURL>")) {
            return in;
        }
        URL gpURL=ServerConfiguration.instance().getGenePatternURL();
        String prefix=gpURL.toExternalForm();
        String suffix=in.substring("<GenePatternURL>".length());
        if (prefix.endsWith("/") && suffix.startsWith("/")) {
            return prefix + suffix.substring(1);
        }
        if (!prefix.endsWith("/") && !suffix.startsWith("/")) {
            return prefix + "/" + suffix;
        }
        return prefix +suffix;
    }
    
    public void updatePinfoValue() throws Exception {
        final int numValues=actualValues.getNumValues();
        final boolean createFilelist=isCreateFilelist();
        
        if (record.getFormal()._isDirectory() || record.getFormal().isInputFile()) {
            HashMap attrs = record.getActual().getAttributes();
            attrs.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
            attrs.remove(ParameterInfo.TYPE);
        }

        if (createFilelist) {
            final boolean downloadExternalFiles=true;
            final List<GpFilePath> listOfValues=getListOfValues(downloadExternalFiles);
            final GpFilePath filelistFile=createFilelist(listOfValues);

            String filelist=filelistFile.getUrl().toExternalForm();
            record.getActual().setValue(filelist);
            
            //HACK: instead of storing the input values in the filelist or in the DB, store them in the parameter info CLOB
            int idx=0;
            for(GpFilePath inputValue : listOfValues) {
                final String key="values_"+idx;
                //String value=url.toExternalForm();
                final String value="<GenePatternURL>"+inputValue.getRelativeUri().toString();
                record.getActual().getAttributes().put(key, value);
                ++idx;
            } 
        }
        else if (numValues==0) {
            record.getActual().setValue("");
        }
        else if (numValues==1) {
            //special-case for DIRECTORY type, need to convert URL input into server file path
            if (record.getFormal()._isDirectory()) {
                final String valueIn=actualValues.getValues().get(0).getValue();
                try {
                    GpFilePath directory=GpFileObjFactory.getRequestedGpFileObj(valueIn);
                    final String valueOut=directory.getServerFile().getAbsolutePath();
                    record.getActual().setValue(valueOut);
                }
                catch (Throwable t) {
                    log.debug("Could not get a GP file path to the directory: "+valueIn);
                    record.getActual().setValue(valueIn);
                }
            }
            else {
                record.getActual().setValue(actualValues.getValues().get(0).getValue());
            }
        }
        else {
            log.error("It's not a filelist and numValues="+numValues);
        }
        
        //special-case: for a choice, if necessary, replace the UI value with the command line value
        // the key is the UI value
        // the value is the command line value
        Map<String,String> choices = record.getFormal().getChoices();
        if (choices != null && choices.size() > 0) {
            final String origValue=record.getActual().getValue();
            if (choices.containsValue(origValue)) {
                //the value is a valid command line value
            }
            else if (choices.containsKey(origValue)) {
                //TODO: log this?
                String newValue=choices.get(origValue);
                record.getActual().setValue(newValue);
            }
            //finally, validate
            if (!choices.containsValue(record.getActual().getValue())) {
                log.error("Invalid value for choice parameter");
            }
        }
    }
    
    //-----------------------------------------------------
    //helper methods for creating parameter list files ...
    //-----------------------------------------------------
    /**
     * Get the list of previous input values for the given parameter, this is to be called 
     * when reloading a job.
     * 
     * @param pinfo
     * 
     * @return the list of input values, or null if the paramater is not an input parameter.
     */
    public static List<String> getInputValues(ParameterInfo pinfo) {
        if (pinfo==null) {
            throw new IllegalArgumentException("pinfo == null");
        }
        HashMap<?,?> attrs=pinfo.getAttributes();
        if (attrs==null) {
            log.error("pinfo.attributes==null");
            return Collections.emptyList();
        }
        
        if (pinfo.isOutputFile()) {
            //ignore output files
            log.debug("pinfo.isOutputFile == true");
            return null;
        }
        
        //extract all 'values_' 
        SortedMap<Integer,String> valuesMap=new TreeMap<Integer,String>();
        //List<String> values=new ArrayList<String>();
        for(Entry<?,?> entry : attrs.entrySet()) {
            String key=entry.getKey().toString();
            if (key.startsWith("values_")) {
                try {
                    int idx=Integer.parseInt( key.split("_")[1] );
                    String value=entry.getValue().toString();
                    value=replaceGpUrl(value);
                    valuesMap.put(idx, value);
                }
                catch (Throwable t) {
                    log.error("Can't parse pinfo.attribute, key="+key, t);
                }
            }
        }
        if (valuesMap.size() > 0) {
            List<String> values=new ArrayList<String>(valuesMap.values());
            return values;
        }
        
        List<String> values=new ArrayList<String>();
        values.add(pinfo.getValue());
        return values;
    }

    private GpFilePath createFilelist(final List<GpFilePath> listOfValues) throws Exception {
        //now, create a new filelist file, add it into the user uploads directory for the given job
        JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
        final int index=-1;
        final String pname=record.getFormal().getName();
        final String filename=".list.txt";
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, pname, filename);

        //write the file list
        ParamListWriter writer=new ParamListWriter.Default();
        writer.writeParamList(gpFilePath, listOfValues);
        fileUtil.updateUploadsDb(gpFilePath);
        return gpFilePath;
    }
    
    private List<GpFilePath> getListOfValues(final boolean downloadExternalFiles) throws Exception {
        final List<Record> tmpList=new ArrayList<Record>();
        for(ParamValue pval : actualValues.getValues()) {
            final Record rec=initFromValue(pval);
            tmpList.add(rec);
        }
        
        //if necessary, download data from external sites
        if (downloadExternalFiles) {
            for(final Record rec : tmpList) {
                if (rec.type.equals(Record.Type.EXTERNAL_URL)) {
                    copyExternalUrlToUserUploads(rec.gpFilePath, rec.url);
                }
            }
        } 

        final List<GpFilePath> values=new ArrayList<GpFilePath>();
        for(final Record rec : tmpList) {
            values.add( rec.gpFilePath );
        }
        return values;
    }
    
    private Record initFromValue(final ParamValue pval) throws Exception {
        final String value=pval.getValue();
        URL externalUrl=JobInputHelper.initExternalUrl(value);
        if (externalUrl != null) {
            //this method does not download the file
            GpFilePath gpPath=JobInputFileUtil.getDistinctPathForExternalUrl(jobContext, externalUrl);
            return new Record(Record.Type.EXTERNAL_URL, gpPath, externalUrl);
        }

        try {
            GpFilePath gpPath = GpFileObjFactory.getRequestedGpFileObj(value);
            return new Record(Record.Type.SERVER_URL, gpPath, null);
        }
        catch (Exception e) {
            log.debug("getRequestedGpFileObj("+value+") threw an exception: "+e.getLocalizedMessage(), e);
            //ignore
        }
        
        //if we are here, it could be a server file path
        File serverFile=new File(value);
        GpFilePath gpPath = ServerFileObjFactory.getServerFile(serverFile);
        return new Record(Record.Type.SERVER_PATH, gpPath, null); 
    }

    private static class Record {
        enum Type {
            SERVER_PATH,
            EXTERNAL_URL,
            SERVER_URL
        }
        Type type;
        GpFilePath gpFilePath;
        URL url; //can be null
        
        public Record(final Type type, final GpFilePath gpFilePath, final URL url) {
            this.type=type;
            this.gpFilePath=gpFilePath;
            this.url=url;
        }
    }
    
    /**
     * Copy data from an external URL into a file in the GP user's uploads directory.
     * This method blocks until the data file has been transferred.
     * 
     * TODO: turn this into a task which can be cancelled.
     * TODO: limit the size of the file which can be transferred
     * TODO: implement a timeout
     * 
     * @param gpPath
     * @param url
     * @throws Exception
     */
    private void copyExternalUrlToUserUploads(final GpFilePath gpPath, final URL url) throws Exception {
        final File parentDir=gpPath.getServerFile().getParentFile();
        if (!parentDir.exists()) {
            boolean success=parentDir.mkdirs();
            if (!success) {
                String message="Error creating upload directory for external url: dir="+parentDir.getPath()+", url="+url.toExternalForm();
                log.error(message);
                throw new Exception(message);
            }
        }
        final File dataFile=gpPath.getServerFile();
        if (dataFile.exists()) {
            //do nothing, assume the file has already been transferred
            //TODO: should implement a more robust caching mechanism, using HTTP HEAD to see if we need to 
            //    download a new copy
            log.debug("dataFile already exists: "+dataFile.getPath());
        }
        else {
            //copy the external url into a new file in the user upload folder
            org.apache.commons.io.FileUtils.copyURLToFile(url, dataFile);

            //add a record of the file to the DB, so that a link will appear in the Uploads tab
            JobInputFileUtil jobInputFileUtil=new JobInputFileUtil(jobContext);
            jobInputFileUtil.updateUploadsDb(gpPath);
        }
    }

    static private Map<String, List<String>> getOriginalInputValues(final JobInfo reloadJob) {
        if (reloadJob==null) {
            log.error("reloadJob==null");
            return Collections.emptyMap();            
        }
        ParameterInfo[] params = reloadJob.getParameterInfoArray();
        if (params==null) {
            log.error("reloadJob.parameterInfoArray == null");
            return Collections.emptyMap();
        }
        if (params.length==0) {
            return Collections.emptyMap();
        }

        //use LinkedHashMap to preserve input order
        Map<String, List<String>> inputValues=new LinkedHashMap<String, List<String>>();
        for (ParameterInfo param : params) {
            final String pname=param.getName();
            final List<String> values=getInputValues(param);
            if (values==null) {
            }
            else if (values.size()==0) {
            }
            else {
                inputValues.put(pname, values);
            }            
        }
        return inputValues;
    }
    
}

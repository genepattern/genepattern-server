/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.genepattern.drm.Memory;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.job.JobInfoLoader;
import org.genepattern.server.job.JobInfoLoaderDefault;
import org.genepattern.server.job.input.configparam.JobConfigParams;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helper class for initializing the job input form, handles different scenarios for opening the job input form:
 * 
 * 1) from default values
 * 2) from a reloaded job
 * 3) from a send-to menu event from a previous job result
 * 4) from the Recent Jobs tab
 * 5) from the Uploads tab
 * 6) from the GenomeSpace tab
 * 7) from the GS landing page
 * 
 * @author pcarr
 *
 */
public class LoadModuleHelper {
    final static private Logger log = Logger.getLogger(LoadModuleHelper.class);
    
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

    /**
     * Helper method for initializing a GpFilePath object for a given JobInfo and ParameterInfo.
     * By default, don't include return the execution log as a GpFilePath.
     * 
     * @param jobInfo
     * @param pinfo
     * 
     * @return null if the pinfo is not an output file.
     */
    private static GpFilePath getOutputFile(final JobInfo jobInfo, final ParameterInfo pinfo) {
        final boolean ignoreExecutionLog=true;
        return getOutputFile(jobInfo, pinfo, ignoreExecutionLog);
    }

    /**
     * Helper method for initializing a GpFilePath object for a given JobInfo and ParameterInfo,
     * with the option to include the execution log.
     * 
     * @param jobInfo, the completed jobInfo
     * @param pinfo, a single ParameterInfo in the list of ParameterInfo parsed from the parameter_info clob in the database.
     * @param ignoreExecutionLog, when true, filter out the execution log.
     * 
     * @return null if the pinfo is not an output file.
     */
    private static GpFilePath getOutputFile(final JobInfo jobInfo, final ParameterInfo pinfo, final boolean ignoreExecutionLog) {
        if (pinfo.isOutputFile()) {
            if (ignoreExecutionLog && isExecutionLog(pinfo)) {
                return null;
            }
            String name=pinfo.getName();
            try {
                JobResultFile outputFile=new JobResultFile(jobInfo, new File(name));
                return outputFile;
            }
            catch (Throwable t) {
                log.error(t);
            }
        }
        return null;
    }
    
    /**
     * Helper method, is the given pinfo the execution log.
     * @param pinfo
     * @return
     */
    private static boolean isExecutionLog(final ParameterInfo pinfo) {
        boolean isExecutionLog = (
                pinfo.getName().equals(GPConstants.TASKLOG) || 
                pinfo.getName().endsWith(GPConstants.PIPELINE_TASKLOG_ENDING));
        return isExecutionLog;
    }

    
    final GpConfig gpConfig;
    final GpContext userContext;
    final GetTaskStrategy getTaskStrategy;
    final JobInfoLoader jobInfoLoader;
    
    /** @deprecated pass in a GpConfig */
    public LoadModuleHelper(final GpContext userContext) {
        this(userContext, null);
    }
    /** @deprecated pass in a GpConfig */
    public LoadModuleHelper(final GpContext userContext, final GetTaskStrategy getTaskStrategyIn) {
        this(userContext, null, null);
    }
    /** @deprecated pass in a GpConfig */
    public LoadModuleHelper(final GpContext userContext, final GetTaskStrategy getTaskStrategyIn, final JobInfoLoader jobInfoLoaderIn) { 
        this(ServerConfigurationFactory.instance(),
                userContext, getTaskStrategyIn, jobInfoLoaderIn);
    }
    public LoadModuleHelper(final GpConfig gpConfig, final GpContext userContext) {
        this(gpConfig, userContext, null);
    }
    public LoadModuleHelper(final GpConfig gpConfig, final GpContext userContext, final GetTaskStrategy getTaskStrategyIn) {
        this(gpConfig, userContext, getTaskStrategyIn, null);
    }
    public LoadModuleHelper(final GpConfig gpConfig, final GpContext userContext, final GetTaskStrategy getTaskStrategyIn, final JobInfoLoader jobInfoLoaderIn) { 
        this.gpConfig=gpConfig;
        if (userContext==null) {
            throw new IllegalArgumentException("userContext==null");
        }
        this.userContext=userContext;
        if (getTaskStrategyIn != null) {
            this.getTaskStrategy=getTaskStrategyIn;
        }
        else {
            this.getTaskStrategy=new GetTaskStrategyDefault();
        }
        if (jobInfoLoaderIn != null) {
            this.jobInfoLoader=jobInfoLoaderIn;
        }
        else {
            this.jobInfoLoader=new JobInfoLoaderDefault();
        }
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
    public JSONObject getInitialValuesJson(
            final String lsid,
            final ParameterInfo[] parameterInfos, //the formal input parameters
            final JobInput reloadedValues, 
            final String _fileParam,
            final String _formatParam,
            final Map<String,String[]> parameterMap
    )
    throws JSONException, Exception
    {
        JobInput initialValues=getInitialValues(
                lsid,
                parameterInfos,
                reloadedValues,
                _fileParam,
                _formatParam,
                parameterMap);
        
        JSONObject json = asJsonV1(initialValues);
        return json;
    }
    
    /**
     * Get the json representation of the given JobInput instance, 
     * the v1 format pre GP 3.7.6 does not include group information.
     * 
     * @param jobInput
     * @return
     * @throws JSONException
     */
    public static JSONObject asJsonV1(final JobInput jobInput) throws JSONException {
        final JSONObject values=new JSONObject();
        for(Entry<ParamId, Param> entry : jobInput.getParams().entrySet()) {
            final String pname=entry.getKey().getFqName();
            final JSONArray jsonArray = new JSONArray();
            for(final ParamValue val : entry.getValue().getValues()) {
                final String value=val.getValue();
                jsonArray.put(value);
            }
            values.put(pname, jsonArray);
        }
        return values;
    }
    
    /**
     * Get the v2 json representation of the given JobInput instance,
     * this version includes group information for optionally organizing lists of input values 
     * into groups.
     * 
     (a) groupid is not declared at all, e.g.
        "input.file": [ { "values": [ "<url1>" ] } ]
    (b) groupid is null, e.g.
        "input.file": [ { "groupid": null, "values": [ "<url1>" ] } ]
    (c) groupid is empty string, e.g.
        "input.file": [ {"groupid": "", "values": [ "<url1>" ] } ]

     * 
     * @param jobInput
     * @return
     * @throws JSONException
     */
    public static JSONObject asJsonV2(final JobInput jobInput) throws JSONException {
        final JSONObject jsonObj=new JSONObject();
        for(final Entry<ParamId, Param> paramEntry : jobInput.getParams().entrySet()) {            
            final Param param=paramEntry.getValue();
            final JSONArray groupArray=new JSONArray();
            //step through the groups in order
            for(final GroupId groupId : param.getGroups()) {
                final JSONObject groupObj = new JSONObject();
                final JSONArray values=new JSONArray();
                for(final ParamValue paramValue : param.getValuesInGroup(groupId)) {
                    values.put(paramValue.getValue());
                }
                groupObj.put("groupid", groupId.getGroupId());
                groupObj.put("values", values);
                groupArray.put(groupObj);
            }
            jsonObj.put(paramEntry.getKey().getFqName(), groupArray);
        }
        return jsonObj;
    }

    public JobInput getInitialValues(
            final String lsid, //the lsid for the module or pipeline
            final ParameterInfo[] parameterInfos, //the formal input parameters
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
        final JobInput initialValues = new JobInput();
        initialValues.setLsid(lsid);

        //check if there are any batch parameters in the request
        List<String> batchParamsList = new ArrayList<String>();
        if(parameterMap != null && parameterMap.get("_batchParam") != null)
        {
            for(final String requestParam : parameterMap.get("_batchParam"))
            {
                batchParamsList.add(requestParam);
            }
        }

        //check if there are any file groups in the request
        JSONObject fileGroupsJson = null;
        String[] fileGroups = parameterMap.get("_filegroup");
        if(parameterMap != null && fileGroups != null)
        {
            //only take the first file grouping
            if(fileGroups.length > 1)
            {
                log.warn(fileGroups.length + " file groups found. Only taking the first one.");
            }
            for(int i=0;i < 1;i++)
            {
                try
                {
                    fileGroupsJson = new JSONObject(fileGroups[i]);
                }
                catch(JSONException je)
                {
                    //just log any errors and continue
                    log.error(je);
                }
            }
        }

        for(final ParameterInfo pinfo : parameterInfos) {
            final String pname=pinfo.getName();

            boolean isBatch = false;

            //check if this is a batch request parameter
            if(batchParamsList.contains(pname))
            {
                isBatch = true;
            }
            //1) initialize from default values
            final List<String> defaultValues=ParamListHelper.getDefaultValues(pinfo);
            if (defaultValues != null) {
                boolean first=true;
                for(final String defaultValue : defaultValues) {
                    if (first) {
                        initialValues.removeValue(new ParamId(pname));
                        first=false;
                    }
                    initialValues.addValue(pname, defaultValue);
                }
            }
            
            //2) if it's a reloaded job, use that
            if (reloadedValues != null) {
                final Param param=reloadedValues.getParam(pname);
                if (param != null) {
                    boolean first=true;
                    for(final Entry<GroupId,ParamValue> entry : param.getValuesAsEntries()) {
                        String rvalue = entry.getValue().getValue();
                        if (first) {
                            initialValues.removeValue(new ParamId(pname));
                            first=false;
                        }
                        final GroupId groupId=entry.getKey();
                        initialValues.addValue(pname, rvalue, groupId);
                    }
                } 
                else {
                    if (log.isDebugEnabled()) {
                        log.debug("no values in previous job for, pname="+pname);
                    }
                }
            }

            //3) if there's a matching request parameter, use that
            if (parameterMap.containsKey(pname)) {
                JSONObject pFileGroupObjs = null;
                Map<Integer, String> fileGroupLookupTable = new HashMap<Integer, String>();
                if(fileGroupsJson != null && fileGroupsJson.has(pname))
                {
                    pFileGroupObjs = fileGroupsJson.getJSONObject(pname);
                    Iterator<String> pfIt = pFileGroupObjs.keys();
                    while(pfIt.hasNext())
                    {
                        String fileGroupName = pfIt.next();
                        JSONArray valueIndicesList = pFileGroupObjs.getJSONArray(fileGroupName);

                        for(int j=0;j<valueIndicesList.length();j++)
                        {
                            if(valueIndicesList.getString(j).contains(".."))
                            {
                                NumValuesParser numValuesParser = new NumValuesParserImpl();
                                NumValues numValues = numValuesParser.parseNumValues(valueIndicesList.getString(j));
                                if(numValues.getMin() != -1 && numValues.getMax() != -1)
                                {
                                    for(int t = numValues.getMin();t <= numValues.getMax(); t++)
                                    {
                                        fileGroupLookupTable.put(t, fileGroupName);
                                    }
                                }
                            }
                            else
                            {
                                try
                                {
                                    fileGroupLookupTable.put(Integer.parseInt(valueIndicesList.getString(j)), fileGroupName);
                                }
                                catch(NumberFormatException ne)
                                {
                                    String error = "Invalid file group index : " + valueIndicesList.getString(j) + " specified for parameter " + pname;
                                    log.error(error);
                                    throw new Exception(error);
                                }
                            }
                        }
                    }
                }
                boolean first=true;
                String[] parameterValues = parameterMap.get(pname);
                for(int i=0;i<parameterValues.length;i++)
                {
                    String requestParam = parameterValues[i];
                    if (first) {
                        initialValues.removeValue(new ParamId(pname));
                        first=false;
                    }

                    if(fileGroupLookupTable != null && fileGroupLookupTable.size() > 0)
                    {
                        //throw an error if no group was defined for the value at this index
                        if(fileGroupLookupTable.get(i) == null)
                        {
                            throw new Exception("No group id found for value at index " + i + " for parameter " + pname);
                        }
                        initialValues.addValue(pname, requestParam, new GroupId(fileGroupLookupTable.get(i))) ;
                    }
                    else if(isBatch)
                    {
                        initialValues.addValue(pname, requestParam, true);
                    }
                    else
                    {
                        initialValues.addValue(pname, requestParam);
                    }
                }
            }
            
            //validate numValues
            NumValues numValues=ParamListHelper.initNumValues(pinfo);
            if (numValues.getMax() != null) {
                final Param param=initialValues.getParam(pname);
                if (param!=null) {
                    if (!isBatch && param.getNumValues()>numValues.getMax()) {
                        //this is an error: more input values were specified than
                        //this parameter allows so throw an exception
                        throw new Exception(" Error: " + param.getNumValues() + " input values were specified for " +
                                pname + " but a maximum of " + numValues.getMax() + " is allowed. ");
                    }
                }
            }
        }
        
        //special-case for send-to module from file
        //    initialize the first matching input file parameter 
        //    if it's from a previous job, also set all the rest of the matching result files
        if (_fileParam != null && _fileParam.length() != 0) {
            if (_formatParam == null || _formatParam.length() == 0) {
                log.error("_format request parameter is not set, _file="+_fileParam);
                _formatParam=ParamListHelper.getType(_fileParam);
            }
            
            GpFilePath fromFile=null;
            try {
                fromFile = GpFileObjFactory.getRequestedGpFileObj(_fileParam);
            }
            catch (Throwable t) {
                //circa GP <= 3.6.1, GenePatternAnalysisTask#onJob depends on quirky behavior of GpFileObjFactory#getRequestedGpFileObj
                //     specifically, it throws an exception when given an external url file.
                // we rely on this exception being thrown
                try {
                    @SuppressWarnings("unused") // to throw exception
                    URL url = new URL(_fileParam);
                    fromFile = new ExternalFile(_fileParam);
                }
                catch (MalformedURLException e) {
                    ///assume it's not an external url
                }
            }
            if (fromFile==null) {
                log.error("_fileParam is not a valid GpFilePath nor a valid external url: "+_fileParam);
            }
            final Helper helper=new Helper(gpConfig, parameterInfos, fromFile, _formatParam);
            if (fromFile instanceof JobResultFile) {
                final String fromJobId=((JobResultFile) fromFile).getJobId();
                final JobInfo fromJobInfo=jobInfoLoader.getJobInfo(userContext, fromJobId);
                if (fromJobInfo == null) {
                    log.error("Error initializing jobInfo, fromJobId="+fromJobId);
                }
                else {
                    final boolean ignoreStderr=true;
                    final boolean ignoreStdout=true;
                    for(final ParameterInfo pinfo : fromJobInfo.getParameterInfoArray()) {
                        if (ignoreStderr && pinfo._isStderrFile()) {
                        }
                        else if (ignoreStdout && pinfo._isStdoutFile()) {
                        }
                        else {
                            final GpFilePath outputFile=getOutputFile(fromJobInfo, pinfo);
                            if (outputFile != null) {
                                if (fromFile.equals(outputFile)) {
                                    //only add the fromFile once
                                }
                                else {
                                    helper.addResultFile(outputFile);
                                }
                            }
                        }
                    }
                }
            }
            helper.addOrReplaceValues(initialValues);
        }
        return initialValues;
    }

    public static JSONArray getParameterGroupsJson(final TaskInfo taskInfo, final File paramGroupJsonFile) throws Exception
    {
        if (taskInfo == null) {
            throw new IllegalArgumentException("taskInfos==null");
        }

        if (paramGroupJsonFile == null) {
            throw new IllegalArgumentException("paramGroupJsonFile==null");
        }

        return getParameterGroupsJson(taskInfo.getParameterInfoArray(), paramGroupJsonFile);
    }

    public static JSONArray getParameterGroupsJson(final ParameterInfo[] pArray, final File paramGroupJsonFile) throws Exception
    {

        JSONArray paramGroupsJson = new JSONArray();

        //keep track of parameters without a group
        ArrayList<String> allParameters = new ArrayList<String>();

        if(paramGroupJsonFile != null)
        {
            if(paramGroupJsonFile.exists())
            {
                String pGroupsJsonString = FileUtils.readFileToString(paramGroupJsonFile);

                if(pGroupsJsonString != null && pGroupsJsonString.length() > 0)
                {
                    paramGroupsJson = new JSONArray(pGroupsJsonString);
                    for(int i=0;i<paramGroupsJson.length();i++)
                    {
                        JSONObject paramGroupObject = paramGroupsJson.getJSONObject(i);
                        if(!paramGroupObject.has("parameters"))
                        {
                            continue;
                        }

                        JSONArray parameters = paramGroupObject.getJSONArray("parameters");

                        //add all the parameters individually
                        for(int t=0;t<parameters.length();t++)
                        {
                            String paramName = parameters.getString(t);
                            if(!allParameters.contains(paramName))
                            {
                                allParameters.add(paramName);
                            }
                            else
                            {
                                throw new Exception("Parameter " + paramName +
                                        " found in multiple parameter groups");
                            }
                        }
                    }

                    validateParamGroupsJson(paramGroupsJson, pArray);
                }
            }
        }

        //if no groups were defined in the module then create one group containing all the parameters
        if(paramGroupsJson.length() == 0)
        {
            //create a default parameter group which contains all of the parameters
            JSONObject defaultParamJsonGroup = new JSONObject();

            JSONArray parameterNameList = new JSONArray();

            for(int i =0;i < pArray.length;i++)
            {
                parameterNameList.put(pArray[i].getName());
            }

            defaultParamJsonGroup.put("parameters", parameterNameList);
            paramGroupsJson.put(defaultParamJsonGroup);
        }
        else
        {
            //check if any parameters were not grouped
            for(int p=0;p<pArray.length;p++)
            {
                String paramName = pArray[p].getName();
                if(allParameters.contains(paramName))
                {
                    allParameters.remove(paramName);
                }
                else
                {
                    allParameters.add(paramName);
                }
            }

            if(allParameters.size() > 0)
            {
                //create a default parameter group which contains all of the parameters
                JSONObject defaultParamJsonGroup = new JSONObject();
                defaultParamJsonGroup.put("parameters", allParameters);
                paramGroupsJson.put(defaultParamJsonGroup);
            }
        }

        return paramGroupsJson;
    }

    private static void validateParamGroupsJson(final JSONArray paramsGroupsJson, final ParameterInfo[] pInfos) throws Exception 
    {
        //get the list of parameters
        ArrayList<String> parameters = new ArrayList<String>();
        for(int p=0;p<pInfos.length;p++)
        {
            parameters.add(pInfos[p].getName());
        }
        validateParamGroupsJson(paramsGroupsJson, parameters);
    }

    private static void validateParamGroupsJson(JSONArray paramsGroupsJson, final ArrayList<String> parameters) throws Exception
    {

        for(int i=0;i<paramsGroupsJson.length();i++)
        {
            if(!(paramsGroupsJson.get(i) instanceof JSONObject))
            {
                throw new Exception("Unexpected parameter group json object: " + paramsGroupsJson.get(i)
                        + " at index " + i);
            }

            JSONObject paramGroup = paramsGroupsJson.getJSONObject(i);
            if(!paramGroup.has("parameters"))
            {
                continue;
            }
            JSONArray params  = (JSONArray)paramGroup.get("parameters");
            for(int p=0;p<params.length();p++)
            {
                if(!(parameters.contains(params.get(p))))
                {
                    //specified parameter name in group does not exist
                    throw new Exception("Parameter " + params.get(p) + " in parameter group " + paramGroup.get("name")
                            + " does not exist");
                }
            }
        }
    }

    //helper class
    private static class Helper {
        private final GpConfig gpConfig;
        private final ParameterInfo[] parameterInfos;
        private final GpFilePath sendFromFile;
        private final String sendFromFormat;
        //one and only one type per file
        private final List<GpFilePath> resultFiles=new ArrayList<GpFilePath>();
        
        public Helper(final GpConfig gpConfig, final ParameterInfo[] parameterInfos, final GpFilePath sendFromFile, final String sendFromFormat) {
            this.gpConfig=gpConfig;
            this.parameterInfos=parameterInfos;
            this.sendFromFile=sendFromFile;
            if (sendFromFile != null) {
                if (sendFromFile.getKind()==null) {
                    sendFromFile.initMetadata();
                }
                resultFiles.add(sendFromFile);
            }
            this.sendFromFormat=sendFromFormat;
        }
        
        public void addResultFile(final GpFilePath resultFile) {
            if (resultFile.getKind()==null) {
                resultFile.initMetadata();
            }
            resultFiles.add(resultFile);
        }
        
        public GpFilePath getSendToFile(final ParameterInfo pinfo) {
            GpFilePath match=null;
            final List<String> fileFormats=LoadModuleHelper.getFileFormats(pinfo);
            //special-case: sendFromFormat is set
            if (sendFromFormat != null && sendFromFormat.length()>0 && resultFiles.contains(sendFromFile)) {
                //see if the sendFromType matchs
                if (fileFormats.contains(sendFromFormat)) {
                    match=sendFromFile;
                }
            }
            if (match == null) {
                for(GpFilePath resultFile : resultFiles) {
                    final String resultFormat=resultFile.getKind();
                    if (fileFormats.contains(resultFormat)) {
                        match=resultFile;
                        break;
                    }
                }
            }
            if (match != null) {
                resultFiles.remove(match);
            }
            return match;
        }
        
        public boolean hasUnmatchedFiles() {
            return resultFiles.size()>0;
        }
        
        public void addOrReplaceValues(final JobInput initialValues) {
            //find matching files from previous job
            for(ParameterInfo pinfo : parameterInfos) {
                if (hasUnmatchedFiles()) {
                    if (pinfo.isInputFile() || pinfo._isDirectory()) {
                        GpFilePath inputValue=getSendToFile(pinfo);
                        if (inputValue != null) {
                            try {
                                final URL inputUrl=inputValue.getUrl();
                                final String newInputValue=inputUrl.toExternalForm();
                                initialValues.removeValue(new ParamId(pinfo.getName()));
                                initialValues.addValue(pinfo.getName(), newInputValue);
                            }
                            catch (Exception e) {
                                log.error(e);
                            }
                        }
                    }
                }
                else {
                    break;
                }
            }
        }
    }
    
    private static  JSONArray _memJobOptionAsJson(Long val, JobConfigParams jobConfigParams) throws JSONException{
        JSONArray outer = new JSONArray();
        JSONObject jobOpt = new JSONObject();
        JSONArray memValues = new JSONArray();
        
        Memory memobj = Memory.fromSizeInBytes(val);
        
        if (jobConfigParams == null){
            memValues.put(memobj.getDisplayValue());
        } else {
            // try to make sure the formatting matches
            boolean isSet = false;
            ParameterInfo memParam = jobConfigParams.getParam("job.memory");
            Set<String> allowedVals =  memParam.getChoices().keySet();
            for (String aVal: allowedVals){
                try {
                    Memory okMem =  Memory.fromString(aVal);
                    if (okMem.equals(memobj)){
                        memValues.put(aVal);
                        isSet = true;
                        break;
                    }
                } catch (Exception e){
                    // do nothing
                    // e.printStackTrace();
                }
            }
            if (! isSet) memValues.put(memobj.getDisplayValue());  
        }
        
        jobOpt.put("groupid", "Job Options");
        jobOpt.put("values", memValues);
        outer.put(jobOpt);
        return outer;
    }
    
    private static  JSONArray _jobOptionAsJson(Integer val) throws JSONException{
        JSONArray outer = new JSONArray();
        JSONObject jobOpt = new JSONObject();
        JSONArray memValues = new JSONArray();
        
        memValues.put(val.toString());
       
        jobOpt.put("groupid", "Job Options");
        jobOpt.put("values", memValues);
        outer.put(jobOpt);
        return outer;
    }

    private static  JSONArray _jobOptionAsJson(String val) throws JSONException{
        JSONArray outer = new JSONArray();
        JSONObject jobOpt = new JSONObject();
        JSONArray memValues = new JSONArray();
        memValues.put(val.toString());
        jobOpt.put("groupid", "Job Options");
        jobOpt.put("values", memValues);
        outer.put(jobOpt);
        return outer;
    }
    
   
    
    public static JSONObject asJsonV2(JobRunnerJob reloadJobRunnerSettings, JSONObject holder,  JobConfigParams jobConfigParams) throws JSONException {
        final JSONObject jsonObj=new JSONObject();
        
        Long reqMem = reloadJobRunnerSettings.getRequestedMemory();
        if (reqMem != null)
            holder.put("job.memory", _memJobOptionAsJson(reqMem, jobConfigParams));
        
        Integer numCpu = reloadJobRunnerSettings.getRequestedCpuCount();
        if (numCpu != null)
            holder.put("job.cpuCount", _jobOptionAsJson(numCpu));
        
        String queue = reloadJobRunnerSettings.getRequestedQueue();
        if (queue != null)
            holder.put("job.queue", _jobOptionAsJson(queue));
        String walltime = reloadJobRunnerSettings.getRequestedWalltime();
        if (walltime != null)
            holder.put("job.walltime", _jobOptionAsJson(walltime));
        
        
        return jsonObj;
    }

}

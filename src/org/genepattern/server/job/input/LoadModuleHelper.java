package org.genepattern.server.job.input;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import org.genepattern.server.dm.ExternalFile;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.dm.tasklib.TasklibPath;
import org.genepattern.server.eula.GetTaskStrategy;
import org.genepattern.server.eula.GetTaskStrategyDefault;
import org.genepattern.server.job.JobInfoLoader;
import org.genepattern.server.job.JobInfoLoaderDefault;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.job.input.choice.Choice;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
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

    
    final Context userContext;
    final GetTaskStrategy getTaskStrategy;
    final JobInfoLoader jobInfoLoader;
    
    public LoadModuleHelper(final Context userContext) {
        this(userContext, null);
    }
    public LoadModuleHelper(final Context userContext, final GetTaskStrategy getTaskStrategyIn) {
        this(userContext, null, null);
    }
    public LoadModuleHelper(final Context userContext, final GetTaskStrategy getTaskStrategyIn, final JobInfoLoader jobInfoLoaderIn) { 
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
            
        final JSONObject values=new JSONObject();
        for(Entry<ParamId, Param> entry : initialValues.getParams().entrySet()) {
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
        for(final ParameterInfo pinfo : parameterInfos) {
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
                final List<ParamValue> paramValues=reloadedValues.getParamValues(pname);
                if (paramValues != null) {
                    for(final ParamValue reloadedValue : paramValues) {

                        String rvalue = reloadedValue.getValue();
                        //check if this a drop-down list and if any value is a file from the module taskLib
                        ChoiceInfo cInfo = ChoiceInfoHelper.initChoiceInfo(pinfo);
                        List<Choice> choices = null;

                        if(cInfo != null)
                        {
                            choices = cInfo.getChoices();
                        }
                        if(choices != null && choices.size() > 0)
                        {
                            for(Choice choice : choices)
                            {
                                String value = choice.getValue();
                                if(value.contains("<libdir>"))
                                {
                                    //Value is expected to refer to a file if we get here
                                    //so extract the name of the file
                                    String name = value.substring(value.indexOf("<libdir>")+8);
                                    if(reloadedValue.getValue().endsWith(name))
                                    {
                                        TaskInfo info = TaskInfoCache.instance().getTask(reloadedValues.getLsid());
                                        TasklibPath tp = new TasklibPath(info, name);
                                        if(reloadedValue.getValue().endsWith(tp.getUrl().toExternalForm()))
                                        {
                                            rvalue = value;
                                        }
                                    }
                                }
                            }

                        }
                        if (first) {
                            initialValues.addOrReplaceValue(pname, rvalue);
                            first=false;
                        }
                        else {
                            initialValues.addValue(pname, rvalue);
                        }
                    }
                }
                else {
                    log.error("no values in previous job for, pname="+pname);
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
            final Helper helper=new Helper(parameterInfos, fromFile, _formatParam);
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
    
    //helper class
    private static class Helper {
        private final ParameterInfo[] parameterInfos;
        private final GpFilePath sendFromFile;
        private final String sendFromFormat;
        //one and only one type per file
        private final List<GpFilePath> resultFiles=new ArrayList<GpFilePath>();
        
        public Helper(final ParameterInfo[] parameterInfos, final GpFilePath sendFromFile, final String sendFromFormat) {
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
                                initialValues.addOrReplaceValue(pinfo.getName(), newInputValue);
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

}

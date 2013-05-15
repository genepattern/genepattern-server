package org.genepattern.server.job.input;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamValue;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;

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

//    public static JobInput getInitialValues(
//            final String lsid, //the lsid for the module or pipeline
//            final ParameterInfo[] parameterInfos, //the formal input parameters
//            final JobInput reloadedValues, 
//            final String _fileParam,
//            String _formatParam,
//            final Map<String,String[]> parameterMap
//    )
//    throws Exception
//    {
//        if (parameterInfos == null) {
//            throw new IllegalArgumentException("parameterInfos==null");
//        }
//        JobInput initialValues = new JobInput();
//        initialValues.setLsid(lsid);
//        for(ParameterInfo pinfo : parameterInfos) {
//            final String pname=pinfo.getName();
//            //1) initialize from default values
//            final List<String> defaultValues=ParamListHelper.getDefaultValues(pinfo);
//            if (defaultValues != null) {
//                boolean first=true;
//                for(final String defaultValue : defaultValues) {
//                    if (first) {
//                        initialValues.addOrReplaceValue(pname, defaultValue);
//                        first=false;
//                    }
//                    else {
//                        initialValues.addValue(pname, defaultValue);
//                    }
//                }
//            }
//            
//            //2) if it's a reloaded job, use that
//            if (reloadedValues != null) {
//                boolean first=true;
//                for(final ParamValue reloadedValue : reloadedValues.getParamValues(pname)) {
//                    if (first) {
//                        initialValues.addOrReplaceValue(pname, reloadedValue.getValue());
//                        first=false;
//                    }
//                    else {
//                        initialValues.addValue(pname, reloadedValue.getValue());
//                    }
//                }
//            }
//
//            //3) if there's a matching request parameter, use that
//            if (parameterMap.containsKey(pname)) {
//                //List<String> fromRequestParam=new ArrayList<String>();
//                boolean first=true;
//                for(String requestParam : parameterMap.get(pname)) {
//                    if (first) {
//                        initialValues.addOrReplaceValue(pname, requestParam);
//                        first=false;
//                    }
//                    else {
//                        initialValues.addValue(pname, requestParam);
//                    }
//                }
//            }
//            
//            //validate numValues
//            NumValues numValues=ParamListHelper.initNumValues(pinfo);
//            if (numValues.getMax() != null) {
//                final Param param=initialValues.getParam(pname);
//                if (param!=null) {
//                    if (param.getNumValues()>numValues.getMax()) {
//                        //this is an error: more input values were specified than
//                        //this parameter allows so throw an exception
//                        throw new Exception(" Error: " + param.getNumValues() + " input values were specified for " +
//                                pname + " but a maximum of " + numValues.getMax() + " is allowed. ");
//                    }
//                }
//            }
//        }
//        
//        //special-case for send-to module from file
//        if (_fileParam != null && _fileParam.length() != 0) {
//            if (_formatParam == null || _formatParam.length() == 0) {
//                log.error("_format request parameter is not set, _file="+_fileParam);
//                _formatParam=getType(_fileParam);
//            }
//            
//            //find the first parameter which matches the type of the file
//            for(ParameterInfo pinfo : parameterInfos) {
//                List<String> fileFormats=ParamListHelper.getFileFormats(pinfo);
//                if (fileFormats != null) {
//                    if (fileFormats.contains(_formatParam)) {
//                        //we found the first match
//                        initialValues.addOrReplaceValue(pinfo.getName(), _fileParam);
//                        break;
//                    }
//                }
//            }
//        }
//        return initialValues;
//    }
//    
//    /**
//     * Helper method for initializing the values for the job input form.
//     * Set initial values for the parameters for the following cases:
//     * 
//     * 1) a reloaded job
//     * 2) values set in request parameters, when linking from the protocols page
//     * 3) send to module, from the context menu for a file
//     * 
//     * @param parameterInfos, the list of formal parameters, from the TaskInfo object
//     * @param reloadedValues, the values from the original job, if this is a job reload request
//     * @param _fileParam, the input file value, if this is from a send-to module request
//     * @param _formatParam, the input file type, if this is from a send-to module request
//     * @param parameterMap, the HTTP request parameters, if this is from protocols page link
//     * 
//     * @return a JSON representation of the initial input values
//     * @throws JSONException
//     */
//    public static JSONObject getInitialValuesJson(
//            final String lsid,
//            final ParameterInfo[] parameterInfos, //the formal input parameters
//            final JobInput reloadedValues, 
//            final String _fileParam,
//            final String _formatParam,
//            final Map<String,String[]> parameterMap
//    )
//    throws JSONException, Exception
//    {
//        JobInput initialValues=getInitialValues(
//                lsid,
//                parameterInfos,
//                reloadedValues,
//                _fileParam,
//                _formatParam,
//                parameterMap);
//            
//        JSONObject values=new JSONObject();
//        for(Entry<ParamId, Param> entry : initialValues.getParams().entrySet()) {
//            final String pname=entry.getKey().getFqName();
//            JSONArray jsonArray = new JSONArray();
//            for(final ParamValue val : entry.getValue().getValues()) {
//                jsonArray.put(val.getValue());
//            }
//            values.put(pname, jsonArray);
//        }
//        return values;
//    }

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

    /**
     * Replace the actual url with the '<GenePatternURL>' substitution variable.
     * @return
     */
    private static String insertGpUrlSubstitution(final String in) {
        URL gpURL=ServerConfiguration.instance().getGenePatternURL();
        String gpUrlStr=gpURL.toExternalForm();
        if (!gpUrlStr.endsWith("/")) {
            gpUrlStr += "/";
        }
        
        if (!in.startsWith(gpUrlStr)) {
            return in;
        }
        return in.replaceFirst(Pattern.quote(gpUrlStr), "<GenePatternURL>");
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
            
            //TODO: fix this HACK
            //    instead of storing the input values in the filelist or in the DB, store them in the parameter info CLOB
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
        
        //special-case for input files and directories, if necessary replace actual URL with '<GenePatternURL>'
        if (record.getFormal()._isDirectory() || record.getFormal().isInputFile()) {
            boolean replaceGpUrl=true;
            if (replaceGpUrl) {
                final String in=record.getActual().getValue();
                final String out=ParamListHelper.insertGpUrlSubstitution(in);
                record.getActual().setValue(out);
            }
            
        }
    }
    
    //-----------------------------------------------------
    //helper methods for creating parameter list files ...
    //-----------------------------------------------------
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
    
}

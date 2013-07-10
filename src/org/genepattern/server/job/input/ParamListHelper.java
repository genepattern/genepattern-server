package org.genepattern.server.job.input;

import java.io.File;
import java.net.MalformedURLException;
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
import org.genepattern.server.job.input.JobInput.ParamId;
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
    
    //inputs
    final Context jobContext;
    final ParameterInfoRecord record;
    final Param actualValues;
    //outputs
    final NumValues allowedNumValues;
    final ListMode listMode;

    public ParamListHelper(final Context jobContext, final ParameterInfoRecord record, final Param inputValues) {
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (record==null) {
            throw new IllegalArgumentException("record==null");
        }
        this.jobContext=jobContext;
        this.record=record;

        //initialize allowedNumValues
        this.allowedNumValues=initAllowedNumValues();

        //initialize list mode
        this.listMode=initListMode(record);
        
        //initialize from default value
        if (inputValues == null) {
            actualValues=initFromDefault();
        }
        else {
            actualValues=inputValues;
        }
    }
    
    private Param initFromDefault() {
        final List<String> defaultValues=ParamListHelper.getDefaultValues(record.getFormal());
        if (defaultValues==null) {
            //return a param with no value
            Param noValue=new Param(new ParamId(record.getFormal().getName()), false);
            return noValue;
        }
        else if (defaultValues.size()==0) {
            //special-case, an empty list is the 'default_value'
            Param noValue=new Param(new ParamId(record.getFormal().getName()), false);
            return noValue;
        }
        else if (defaultValues.size()==1 && "".equals(defaultValues.get(0))) {
            //special-case, an empty string is the 'default_value'
            if (record.getFormal().isInputFile()) {
                //special-case, an empty string for a file param is the 'default_value'
                Param noValue=new Param(new ParamId(record.getFormal().getName()), false);
                return noValue;
            }
            Param emptyStringValue=new Param(new ParamId(record.getFormal().getName()), false);
            emptyStringValue.addValue(new ParamValue(""));
            return emptyStringValue;
        }
        else {
            Param listValue=new Param(new ParamId(record.getFormal().getName()), false);
            for(final String value : defaultValues) {
                listValue.addValue(new ParamValue(value));
            }
            return listValue;
        }
    }
    
    private ListMode initListMode(final ParameterInfoRecord record) {
        //initialize list mode
        String listModeStr = (String) record.getFormal().getAttributes().get("listMode");
        if (listModeStr != null && listModeStr.length()>0) {
            listModeStr = listModeStr.toUpperCase().trim();
            try {
                return ListMode.valueOf(listModeStr);
            }
            catch (Throwable t) {
                String message="Error initializing listMode from listMode="+listModeStr;
                log.error(message, t);
                throw new IllegalArgumentException(message);
            }
        }
        //default value
        return ListMode.LIST;
    }

    private NumValues initAllowedNumValues() {
        final String numValuesStr = (String) record.getFormal().getAttributes().get("numValues");
        //parse num values string
        NumValuesParser nvParser=new NumValuesParserImpl();
        try { 
            return nvParser.parseNumValues(numValuesStr);
        }
        catch (Exception e) {
            String message="Error parsing numValues="+numValuesStr+" for "+record.getFormal().getName();
            log.error(message,e);
            throw new IllegalArgumentException(message);
        }
    }

    public static List<String> getDefaultValues(final ParameterInfo pinfo) {
        final String defaultValue=pinfo.getDefaultValue();
        if (defaultValue==null) {
            log.debug(pinfo.getName()+": default_value is not set");
            return null;
        }
        
        if (defaultValue.length()==0) {
            log.debug(pinfo.getName()+": default_value is empty string");
            if (pinfo.isInputFile()) {
                log.debug(pinfo.getName()+" input file and default_value is empty string");
                return null;
                //return Collections.emptyList();
            }
        }
        
        //parse default_values param ... 
        //TODO: implement support for default values as a list of values
        if (defaultValue != null) {
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
        if (this.allowedNumValues == null) {
            return false;
        }
        if (!this.allowedNumValues.acceptsList()) {
            return false;
        }
        
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
    
    public boolean isFileInputParam() {
        return record.getFormal().isInputFile();
    }
    public boolean isDirectoryInputParam() {
        return record.getFormal()._isDirectory();
    }

    /**
     * Convert the user-supplied value for a directory input parameter into a GpFilePath instance.
     * Example inputs include ...
     * a) literal server path, e.g. /xchip/shared_data/all_aml_test.cls
     * b) http url to server file, e.g. http://127.0.0.1:8080/gp/data//xchip/shared_data/all_aml_test.cls
     * c) file url, e.g. file:///xchip/shared_data/all_aml_test.cls
     * 
     * Invalid inputs include any external urls.
     *     
     * @param valueIn
     * @return
     */
    public GpFilePath initDirectoryInputValue(final ParamValue paramValueIn) throws Exception {
        if (!isDirectoryInputParam()) {
            throw new Exception("Input parameter is not DIRECTORY type: "+record.getFormal().getName()+"="+paramValueIn.getValue());
        } 
        if (paramValueIn==null) {
            log.error("paramValueIn==null"+record.getFormal().getName());
            return null;
        } 
        //ignore empty input
        if (paramValueIn.getValue()==null || paramValueIn.getValue().length()==0) {
            log.debug("value not set for DIRECTORY: "+record.getFormal().getName());
            return null;            
        }
        
        GpFilePath directory=null;
        final Record inputRecord=initFromValue(paramValueIn);
        //special-case: external urls are not allowed
        if (inputRecord.type==Record.Type.EXTERNAL_URL) {
            throw new Exception("External url not allowed for DIRECTORY: "+record.getFormal().getName()+"="+paramValueIn.getValue());
        }
        //special-case: it's not a directory
        if (!inputRecord.gpFilePath.isDirectory()) {
            throw new Exception("Value is not a directory: "+record.getFormal().getName()+"="+paramValueIn.getValue());
        }
        directory=inputRecord.gpFilePath;
        return directory;
    }
    
    /**
     * Convert user-supplied value for a file input paramter into a GpFilePath instance.
     * 
     * @param paramValueIn
     * @return
     * @throws Exception
     */
    public GpFilePath initFileInputValue(final ParamValue paramValueIn) throws Exception {
        if (!isFileInputParam()) {
            throw new Exception("Input parameter is not a FILE type: "+record.getFormal().getName()+"="+paramValueIn.getValue());
        }
        if (paramValueIn==null) {
            log.error("paramValueIn==null"+record.getFormal().getName());
            return null;
        } 
        //ignore empty input
        if (paramValueIn.getValue()==null || paramValueIn.getValue().length()==0) {
            log.debug("value not set for FILE: "+record.getFormal().getName());
            return null;            
        }
        
        GpFilePath file=null;
        final Record inputRecord=initFromValue(paramValueIn);
        //special-case: it's not a file
        if (!inputRecord.gpFilePath.isFile()) {
            throw new Exception("Value is not a file: "+record.getFormal().getName()+"="+paramValueIn.getValue());
        }
        file=inputRecord.gpFilePath;
        return file;
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
                final ParamValue paramValueIn=actualValues.getValues().get(0);
                final GpFilePath directory=initDirectoryInputValue(paramValueIn);
                if (directory != null) {                    
                    //TODO: check canRead
                    //boolean canRead=directory.canRead(jobContext.isAdmin(), jobContext);
                    //if (!canRead) {
                    //    throw new Exception("You are not permitted to access the file: "+paramValueIn.getValue());
                    //}
                    record.getActual().setValue(directory.getUrl().toExternalForm());
                }
                else {
                    record.getActual().setValue(paramValueIn.getValue());
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
    
    private Record initFromValue(final ParamValue pval) throws Exception 
    {
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
        
        //if we are here, it could be a server file path in one of two forms,
        //    a) literal path /
        //    b) uri path file:///
        GpFilePath gpPath=null;
        String pathIn=null;
        try {
            URL urlIn=new URL(value);
            if ("file".equalsIgnoreCase(urlIn.getProtocol())) {
                if (urlIn.getHost() != null && urlIn.getHost().length() > 0) {
                    log.error("Ignoring host part of file url: "+value);
                }
                //special-case, strip 'file' from url protocol
                pathIn=urlIn.getPath();
            }
        }
        catch (MalformedURLException e) {
            //it's not a URL, assume a literal file path
            pathIn=value;
        }
        if (pathIn != null) {
            try {
                //hint: need to append a '/' to the value, e.g. "/data//xchip/shared_data/all_aml_test.gct"
                gpPath=GpFileObjFactory.getRequestedGpFileObj("/data", "/"+pathIn);
            }
            catch (Throwable tx) {
                log.error("Error initializing gpFilePath for directory input: "+pathIn, tx);
            }
        }
        else {
            try {
                gpPath=GpFileObjFactory.getRequestedGpFileObj(value);
            }
            catch (Throwable t) {
                log.error("Error initializing gpFilePath for directory input: "+value, t);
                final File serverFile=new File(value);
                gpPath = ServerFileObjFactory.getServerFile(serverFile);
            }
        }
        if (gpPath != null) {
            return new Record(Record.Type.SERVER_PATH, gpPath, null); 
        }
        throw new Exception("Error initializing gpFilePath for value="+value);
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

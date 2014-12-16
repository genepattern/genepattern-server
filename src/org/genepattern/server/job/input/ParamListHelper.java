package org.genepattern.server.job.input;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.genomespace.GenomeSpaceClient;
import org.genepattern.server.genomespace.GenomeSpaceClientFactory;
import org.genepattern.server.genomespace.GenomeSpaceFileHelper;
import org.genepattern.server.job.input.collection.ParamGroupHelper;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.util.LSID;
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
            numValuesStr = (String) pinfo.getAttributes().get(NumValues.PROP_NUM_VALUES);
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
        URL gpURL=ServerConfigurationFactory.instance().getGenePatternURL();
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
    final GpContext jobContext;
    final ParameterInfoRecord parameterInfoRecord;
    final Param actualValues;
    //outputs
    final NumValues allowedNumValues;
    final GroupInfo groupInfo;
    final ListMode listMode;

    public ParamListHelper(final GpContext jobContext, final ParameterInfoRecord parameterInfoRecord, final Param inputValues) {
        this(jobContext, parameterInfoRecord, inputValues, false);
    }
    public ParamListHelper(final GpContext jobContext, final ParameterInfoRecord parameterInfoRecord, final Param inputValues, final boolean initDefault) {
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (parameterInfoRecord==null) {
            throw new IllegalArgumentException("parameterInfoRecord==null");
        }
        this.jobContext=jobContext;
        this.parameterInfoRecord=parameterInfoRecord;

        //initialize allowedNumValues
        this.allowedNumValues=initAllowedNumValues();

        //initialize list mode
        this.listMode=initListMode(parameterInfoRecord);
        
        //initialize group info
        this.groupInfo=initGroupInfo();
        
        //if necessary create a 'null' value for the param
        if (inputValues == null && !initDefault) {
            if (log.isDebugEnabled()) { log.debug("null value for param: "+parameterInfoRecord.getFormal().getName()); }
            actualValues=new Param(new ParamId(parameterInfoRecord.getFormal().getName()), false);
        }
        else if (inputValues == null && initDefault) {
            actualValues=initFromDefault(jobContext.getLsid());
        }
        else {
            actualValues=inputValues;
        }
    }
    
    private Param initFromDefault(final String lsid) {
        final List<String> defaultValues=ParamListHelper.getDefaultValues(parameterInfoRecord.getFormal());
        if (defaultValues==null) {
            //return a param with no value
            Param noValue=new Param(new ParamId(parameterInfoRecord.getFormal().getName()), false);
            return noValue;
        }
        else if (defaultValues.size()==0) {
            //special-case, an empty list is the 'default_value'
            Param noValue=new Param(new ParamId(parameterInfoRecord.getFormal().getName()), false);
            return noValue;
        }
        else if (defaultValues.size()==1 && "".equals(defaultValues.get(0))) {
            //special-case, an empty string is the 'default_value'
            if (parameterInfoRecord.getFormal().isInputFile()) {
                //special-case, an empty string for a file param is the 'default_value'
                Param noValue=new Param(new ParamId(parameterInfoRecord.getFormal().getName()), false);
                return noValue;
            }
            Param emptyStringValue=new Param(new ParamId(parameterInfoRecord.getFormal().getName()), false);
            emptyStringValue.addValue(new ParamValue(""));
            return emptyStringValue;
        }
        else {
            Param listValue=new Param(new ParamId(parameterInfoRecord.getFormal().getName()), false);
            for(final String value : defaultValues) {
                listValue.addValue(new ParamValue(value));
            }
            return listValue;
        }
    }
    
    private static ListMode initListMode(final ParameterInfoRecord parameterInfoRecord) {
        //initialize list mode
        String listModeStr = (String) parameterInfoRecord.getFormal().getAttributes().get(NumValues.PROP_LIST_MODE);
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
        final String numValuesStr = (String) parameterInfoRecord.getFormal().getAttributes().get(NumValues.PROP_NUM_VALUES);
        //parse num values string
        NumValuesParser nvParser=new NumValuesParserImpl();
        try { 
            return nvParser.parseNumValues(numValuesStr);
        }
        catch (Exception e) {
            String message="Error parsing numValues="+numValuesStr+" for "+parameterInfoRecord.getFormal().getName();
            log.error(message,e);
            throw new IllegalArgumentException(message);
        }
    }

    private GroupInfo initGroupInfo() {
        final GroupInfo groupInfo=new GroupInfo.Builder().fromParameterInfo(parameterInfoRecord.getFormal()).build();
        return groupInfo;
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
            }
        }
        
        //parse default_values param ... 
        //TODO: implement support for default values as a list of values
        List<String> defaultValues=new ArrayList<String>();
        defaultValues.add(pinfo.getDefaultValue());
        return defaultValues;
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
                if (!parameterInfoRecord.getFormal().isOptional()) {
                    throw new IllegalArgumentException("Missing required parameter: "+parameterInfoRecord.getFormal().getName());
                }
            }
            //everything else is valid
            return;
        }

        //if we're here, it means numValues is set, need to check for filelists
        //are we in range?
        if (numValuesSet < allowedNumValues.getMin()) {
            throw new IllegalArgumentException("Not enough values for "+parameterInfoRecord.getFormal().getName()+
                    ", num="+numValuesSet+", min="+allowedNumValues.getMin());
        }
        if (allowedNumValues.getMax() != null) {
            //check upper bound
            if (numValuesSet > allowedNumValues.getMax()) {
                throw new IllegalArgumentException("Too many values for "+parameterInfoRecord.getFormal().getName()+
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

    /**
     * Do we need to create a group file for this parameter? 
     * Based on the following rules.
     * 1) must be a normal fileList as returned by isCreateFilelist. Hint: numValues must be declared in the manifest.
     * AND
     * 2) groupInfo must be declared in the manifest.
     * 
     * By definition, if there is a groupInfo for the param, create a group file, regardless of the number of groups 
     * for a particular run of the module. 
     * Values without a groupId will be assigned to the empty group ("").
     * 
     * @return
     */
    public boolean isCreateGroupFile() {
        if (!isCreateFilelist()) {
            return false;
        }
        if (groupInfo==null) {
            return false;
        }
        //by definition, if there is a groupInfo for the param, create a group file, regardless of the number of groups defined
        //for a particular run of the modules. Args without a groupId will be assigned to the empty group ("").
        return true;
    }
    
    public boolean isFileInputParam() {
        return parameterInfoRecord.getFormal().isInputFile();
    }
    public boolean isDirectoryInputParam() {
        return parameterInfoRecord.getFormal()._isDirectory();
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
     * @param paramValueIn
     * @return
     */
    public GpFilePath initDirectoryInputValue(final ParamValue paramValueIn) throws Exception {
        if (!isDirectoryInputParam()) {
            throw new Exception("Input parameter is not DIRECTORY type: "+parameterInfoRecord.getFormal().getName()+"="+paramValueIn.getValue());
        } 
        if (paramValueIn==null) {
            log.error("paramValueIn==null"+parameterInfoRecord.getFormal().getName());
            return null;
        } 
        //ignore empty input
        if (paramValueIn.getValue()==null || paramValueIn.getValue().length()==0) {
            log.debug("value not set for DIRECTORY: "+parameterInfoRecord.getFormal().getName());
            return null;            
        }
        
        GpFilePath directory=null;
        final Record inputRecord=initFromValue(paramValueIn);
        //special-case: external urls are not allowed
        if (inputRecord.type==Record.Type.EXTERNAL_URL) {
            throw new Exception("External url not allowed for DIRECTORY: "+parameterInfoRecord.getFormal().getName()+"="+paramValueIn.getValue());
        }
        //special-case: it's not a directory
        if (!inputRecord.gpFilePath.isDirectory()) {
            throw new Exception("Value is not a directory: "+parameterInfoRecord.getFormal().getName()+"="+paramValueIn.getValue());
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
    public GpFilePath initGpFilePath(final ParamValue paramValueIn) throws Exception {
        if (!isFileInputParam() & !isDirectoryInputParam()) {
            throw new Exception("Input parameter is not a FILE or DIRECTORY: "+parameterInfoRecord.getFormal().getName()+"="+paramValueIn.getValue());
        }
        if (paramValueIn==null) {
            log.error("paramValueIn==null"+parameterInfoRecord.getFormal().getName());
            return null;
        } 
        //ignore empty input
        if (paramValueIn.getValue()==null || paramValueIn.getValue().length()==0) {
            log.debug("value not set for FILE: "+parameterInfoRecord.getFormal().getName());
            return null;            
        } 
        GpFilePath file=null;
        final Record inputRecord=initFromValue(paramValueIn);
        file=inputRecord.gpFilePath;
        return file;
    }

    public boolean isPassByReference()
    {
        return parameterInfoRecord.getFormal()._isUrlMode();
    }

    public void updatePinfoValue() throws Exception {
        final int numValues=actualValues.getNumValues();
        final boolean createFilelist=isCreateFilelist();
        final boolean createGroupFile=isCreateGroupFile();
        final boolean passByReference = isPassByReference();
        
        if (parameterInfoRecord.getFormal()._isDirectory() || parameterInfoRecord.getFormal().isInputFile()) {
            HashMap attrs = parameterInfoRecord.getActual().getAttributes();
            attrs.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
            attrs.remove(ParameterInfo.TYPE);
        }

        //Note: createFilelist is true when createGroupFile is true, so check for createGroupFile first
        if (createGroupFile) { 
            ParamGroupHelper pgh=new ParamGroupHelper.Builder(actualValues)
                .jobContext(jobContext)
                .groupInfo(groupInfo)
                .build();
            final GpFilePath toFile=pgh.createFilelist();
            parameterInfoRecord.getActual().setValue(toFile.getUrl().toExternalForm());
            
            //save the filelist and groupids to the parameter info CLOB
            final List<GpFilePath> listOfValues=pgh.getGpFilePaths();
            int idx=0;
            for(final Entry<GroupId, ParamValue> entry : actualValues.getValuesAsEntries()) {
                final String groupId = entry.getKey().getGroupId();
                final GpFilePath gpFilePath=listOfValues.get(idx);
                
                final String key="values_"+idx;
                final String value="<GenePatternURL>"+gpFilePath.getRelativeUri().toString();
                parameterInfoRecord.getActual().getAttributes().put(key, value);
                final String groupKey="valuesGroup_"+idx;
                parameterInfoRecord.getActual().getAttributes().put(groupKey, groupId);
                ++idx;
            }
        }
        else if (createFilelist)
        {
            final boolean downloadExternalFiles = !passByReference;
            final List<GpFilePath> listOfValues=getListOfValues(downloadExternalFiles);
            final GpFilePath filelistFile=createFilelist(listOfValues, passByReference);

            String filelist=filelistFile.getUrl().toExternalForm();
            parameterInfoRecord.getActual().setValue(filelist);
            
            //TODO: fix this HACK
            //    instead of storing the input values in the filelist or in the DB, store them in the parameter info CLOB
            int idx=0;
            for(GpFilePath inputValue : listOfValues) {
                final String key="values_"+idx;
                //String value=url.toExternalForm();
                String value="";
                if(downloadExternalFiles)
                {
                    value = "<GenePatternURL>"+inputValue.getRelativeUri().toString();
                }
                else
                {
                    //provide the url directly since the file was not downloaded
                    value = inputValue.getUrl().toExternalForm();
                }
                parameterInfoRecord.getActual().getAttributes().put(key, value);
                ++idx;
            } 
        }
        else if (numValues==0) {
            parameterInfoRecord.getActual().setValue("");
        }
        else if (numValues==1) {
            final ParamValue paramValueIn=actualValues.getValues().get(0);
            //special-case for FILE type with server file paths, check file access permissions and if necessary convert value to URL form
            if (parameterInfoRecord.getFormal().isInputFile()) {
                final Record inputRecord=initFromValue(paramValueIn);
                if (inputRecord.type==Record.Type.SERVER_PATH || inputRecord.type==Record.Type.SERVER_URL) {
                    final GpFilePath file=inputRecord.gpFilePath;
                    boolean canRead=file.canRead(jobContext.isAdmin(), jobContext);
                    if (!canRead) {
                        String pname=parameterInfoRecord.getFormal().getName();
                        String value=paramValueIn.getValue();
                        if (value==null) {
                            value="null";
                        }
                        else if (value.length()==0) {
                            value="<empty string>";
                        }
                        throw new Exception("For the input parameter, "+pname+", You are not permitted to access the file: "+value);
                    }
                    parameterInfoRecord.getActual().setValue(file.getUrl().toExternalForm());
                }
                else {
                    parameterInfoRecord.getActual().setValue(actualValues.getValues().get(0).getValue());
                }
            }
            //special-case for DIRECTORY type, check file access permissions and if necessary convert value to URL form
            else if (parameterInfoRecord.getFormal()._isDirectory()) {
                final GpFilePath directory=initDirectoryInputValue(paramValueIn);
                if (directory != null) {                    
                    boolean canRead=directory.canRead(jobContext.isAdmin(), jobContext);
                    if (!canRead) {
                        throw new Exception("You are not permitted to access the directory: "+paramValueIn.getValue());
                    }
                    parameterInfoRecord.getActual().setValue(directory.getUrl().toExternalForm());
                }
                else {
                    parameterInfoRecord.getActual().setValue(paramValueIn.getValue());
                }
            }
            else {
                parameterInfoRecord.getActual().setValue(actualValues.getValues().get(0).getValue());
            }
        }
        else {
            log.error("It's not a filelist and numValues="+numValues);
        }
        
        //special-case: for a choice, if necessary, replace the UI value with the command line value
        // the key is the UI value
        // the value is the command line value
        Map<String,String> choices = parameterInfoRecord.getFormal().getChoices();
        if (choices != null && choices.size() > 0) {
            final String origValue=parameterInfoRecord.getActual().getValue();
            if (choices.containsValue(origValue)) {
                //the value is a valid command line value
            }
            else if (choices.containsKey(origValue)) {
                //TODO: log this?
                String newValue=choices.get(origValue);
                parameterInfoRecord.getActual().setValue(newValue);
            }
            //finally, validate
            if (!choices.containsValue(parameterInfoRecord.getActual().getValue())) {
                log.error("Invalid value for choice parameter");
            }
        }
        
        //special-case for input files and directories, if necessary replace actual URL with '<GenePatternURL>'
        if (parameterInfoRecord.getFormal()._isDirectory() || parameterInfoRecord.getFormal().isInputFile()) {
            boolean replaceGpUrl=true;
            if (replaceGpUrl) {
                final String in=parameterInfoRecord.getActual().getValue();
                final String out=ParamListHelper.insertGpUrlSubstitution(in);
                parameterInfoRecord.getActual().setValue(out);
            }
        }
    }

    //-----------------------------------------------------
    //helper methods for creating parameter list files ...
    //-----------------------------------------------------
    private GpFilePath createFilelist(final List<GpFilePath> listOfValues) throws Exception {
        return createFilelist(listOfValues, false);
    }
        //-----------------------------------------------------
    //helper methods for creating parameter list files ...
    //-----------------------------------------------------
    private GpFilePath createFilelist(final List<GpFilePath> listOfValues, boolean urlMode) throws Exception {
        //now, create a new filelist file, add it into the user uploads directory for the given job
        JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
        final int index=-1;
        final String pname=parameterInfoRecord.getFormal().getName();
        final String filename=".list.txt";
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, pname, filename);

        //write the file list
        ParamListWriter writer=new ParamListWriter.Default();
        writer.writeParamList(gpFilePath, listOfValues, urlMode);
        fileUtil.updateUploadsDb(gpFilePath);
        return gpFilePath;
    }
    
    private List<GpFilePath> getListOfValues(final boolean downloadExternalFiles) throws Exception {
        final List<Record> tmpList=new ArrayList<Record>();
        for(ParamValue pval : actualValues.getValues()) {
            final Record rec=initFromValue(pval, downloadExternalFiles);
            tmpList.add(rec);
        }
        
        // If necessary, download data from external sites
        if (downloadExternalFiles) {
            for(final Record rec : tmpList) {
                // Handle GenomeSpace URLs
                if (rec.type.equals(Record.Type.GENOMESPACE_URL)) {
                    fileListGenomeSpaceToUploads(jobContext, rec.gpFilePath, rec.url);
                }

                // Handle external URLs
                if (rec.type.equals(Record.Type.EXTERNAL_URL)) {
                    forFileListCopyExternalUrlToUserUploads(rec.gpFilePath, rec.url);
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
        return ParamListHelper.initFromValue(jobContext, pval, false);
    }

    private Record initFromValue(final ParamValue pval, boolean downloadExternalUrl) throws Exception {
        return ParamListHelper.initFromValue(jobContext, pval, downloadExternalUrl);
    }

    public static Record initFromValue(final GpContext jobContext, final ParamValue pval) throws Exception
    {
        return initFromValue(jobContext, pval, false);
    }

    public static Record initFromValue(final GpContext jobContext, final ParamValue pval, boolean downloadExternalUrl) throws Exception {
        final String value=pval.getValue();
        URL externalUrl = JobInputHelper.initExternalUrl(value);

        // Handle GenomeSpace URLs
        if (externalUrl != null && GenomeSpaceFileHelper.isGenomeSpaceFile(externalUrl)) {

            if (downloadExternalUrl) {
                GpFilePath gpPath = JobInputFileUtil.getDistinctPathForExternalUrl(jobContext, externalUrl);
                return new Record(Record.Type.GENOMESPACE_URL, gpPath, externalUrl);
            }
        }
        if (externalUrl != null) {

            if (downloadExternalUrl)
            { //this method does not do the file download
                GpFilePath gpPath = JobInputFileUtil.getDistinctPathForExternalUrl(jobContext, externalUrl);
                return new Record(Record.Type.EXTERNAL_URL, gpPath, externalUrl);
            }
            else
            {
                //this section is for if the external file will not be downloaded
                GpFilePath gpPath = new ExternalFile(externalUrl);
                return new Record(Record.Type.EXTERNAL_URL, gpPath, externalUrl);
            }
        }
        LSID lsid=null;
        try {
            lsid=new LSID(jobContext.getLsid());
        }
        catch (Throwable t) {
            log.debug("LSID not set", t);
            lsid=null;
        }
        
        try {
            final GpFilePath gpPath = GpFileObjFactory.getRequestedGpFileObj(value, lsid);
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

    public static class Record {
        public enum Type {
            SERVER_PATH,
            EXTERNAL_URL,
            SERVER_URL,
            GENOMESPACE_URL
        }
        Type type;
        GpFilePath gpFilePath;
        URL url; //can be null
        
        public Record(final Type type, final GpFilePath gpFilePath, final URL url) {
            this.type=type;
            this.gpFilePath=gpFilePath;
            this.url=url;
        }
        
        public Type getType() {
            return type;
        }
        public GpFilePath getGpFilePath() {
            return gpFilePath;
        }
        public URL getUrl() {
            return url;
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
     * 
     * @deprecated - should replace this with a static call
     */
    private void forFileListCopyExternalUrlToUserUploads(final GpFilePath gpPath, final URL url) throws Exception {
        forFileListCopyExternalUrlToUserUploads(jobContext, gpPath, url);
    }
    /**
     * Copy data from an external URL into a file in the GP user's uploads directory.
     * This method blocks until the data file has been transferred.
     * 
     * TODO: turn this into a task which can be cancelled.
     * TODO: limit the size of the file which can be transferred
     * TODO: implement a timeout
     * 
     * @param jobContext, must have a valid userId and should have a valid jobInfo
     * @param gpPath
     * @param url
     * @throws Exception
     * 
     * @deprecated - should replace this with a static call
     */
    public static void forFileListCopyExternalUrlToUserUploads(final GpContext jobContext, final GpFilePath gpPath, final URL url) throws Exception {
        // for GP-5153
        if (GenomeSpaceClientFactory.isGenomeSpaceEnabled(jobContext)) {
            if (GenomeSpaceFileHelper.isGenomeSpaceFile(url)) {
                final String message="File list not supported with GenomeSpace files; We are working on a fix (GP-5153).";
                log.debug(message+", url="+url);
                throw new Exception(message);
            }
        }
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

    /**
     * Copy a GenomeSpace file to be an upload file for file list processing
     *
     * @param jobContext
     * @param gpPath
     * @param url
     * @throws Exception
     */
    public static void fileListGenomeSpaceToUploads(GpContext jobContext, GpFilePath gpPath, URL url) throws Exception {
        if (GenomeSpaceClientFactory.isGenomeSpaceEnabled(jobContext)) {
            // Make sure the user is logged into GenomeSpace
            GenomeSpaceClient gsClient = GenomeSpaceClientFactory.instance();

            final File parentDir = gpPath.getServerFile().getParentFile();
            if (!parentDir.exists()) {
                boolean success = parentDir.mkdirs();
                if (!success) {
                    String message = "Error creating upload directory for GenomeSpace url: dir=" + parentDir.getPath() + ", url=" + url.toExternalForm();
                    log.error(message);
                    throw new Exception(message);
                }
            }
            final File dataFile = gpPath.getServerFile();
            if (dataFile.exists()) {
                // Do nothing, assume the file has already been transferred
                //TODO: should implement a more robust caching mechanism, using HTTP HEAD to see if we need to
                log.debug("Downloaded GenomeSpace already exists: " + dataFile.getPath());
            }
            else {
                InputStream is = gsClient.getInputStream(jobContext.getUserId(), url);
                OutputStream os = new FileOutputStream(dataFile);

                IOUtils.copy(is, os);

                //add a record of the file to the DB, so that a link will appear in the Uploads tab
                JobInputFileUtil jobInputFileUtil=new JobInputFileUtil(jobContext);
                jobInputFileUtil.updateUploadsDb(gpPath);
            }
        }
        else {
            log.warn("GenomeSpace file added when GenomeSpace is not enabled: " + url.toString());
            throw new Exception("GenomeSpace not enabled. Need to enable GenomeSpace to download GenomeSpace files:" + url.toString());
        }
    }
    
//    /**
//     * Compare last modified with cached versions for FTP files
//     * @param realPath
//     * @param url
//     * @return
//     * @throws Exception
//     */
//    private static boolean needToRedownloadFTP(GpFilePath realPath, URL url) throws Exception {
//        FTPClient ftp = new FTPClient();
//        ftp.connect(url.getHost(), url.getPort() > 0 ? url.getPort() : 21);
//        ftp.login("anonymous", "");
//        String filename = FilenameUtils.getName(url.getFile());
//        String filepath = FilenameUtils.getPath(url.getFile());
//        boolean success = ftp.changeWorkingDirectory(filepath);
//        String lastModifiedString = ftp.getModificationTime(filename);
//        
//        // Trouble changing directory or last modified not supported by FTP server, assume cache is good
//        if (!success || lastModifiedString == null) {
//            return false;
//        }
//        
//        Date lastModified = new SimpleDateFormat("yyyyMMddhhmmss", Locale.ENGLISH).parse(lastModifiedString.substring(lastModifiedString.indexOf(" ")));
//        return lastModified.after(realPath.getLastModified());
//    }
    
//    /**
//     * Compare last modified with cached versions for HTTP files
//     * @param realPath
//     * @param url
//     * @return
//     * @throws Exception
//     */
//    private static boolean needToRedownloadHTTP(GpFilePath realPath, URL url) throws Exception {
//        HttpClient client = new HttpClient();
//        HttpMethod method = new HeadMethod(url.toString());
//        client.executeMethod(method);
//        Header lastModifiedHeader = method.getResponseHeader("Last-Modified");
//        String lastModifiedString = lastModifiedHeader.getValue();
//        // Example format: Mon, 05 Aug 2013 18:02:28 GMT
//        Date lastModified = new SimpleDateFormat("EEEE, dd MMMM yyyy kk:mm:ss zzzz", Locale.ENGLISH).parse(lastModifiedString);
//        return lastModified.after(realPath.getLastModified());
//    }
    
//    /**
//     * Do an HTTP HEAD or FTP Modification Time on the URL and see if it is out of date
//     * @param realPath
//     * @param url
//     * @return
//     */
//    private static boolean needToRedownload(GpFilePath realPath, URL url) throws Exception {
//        // If the last modified isn't set for this path, assume it's not out of date
//        realPath.initMetadata();
//        if (realPath.getLastModified() == null) {
//            log.debug("Last modified not set for: " + realPath.getName());
//            return false;
//        }
//        
//        String protocol = url.getProtocol().toLowerCase();   
//        if (protocol.equals("http") || protocol.equals("https")) {
//            return needToRedownloadHTTP(realPath, url);
//        }
//        else if (protocol.equals("ftp")) {
//            return needToRedownloadFTP(realPath, url);
//        }
//        else {
//            // The protocol is unknown, assume it's not out of date 
//            log.debug("Unknown protocol in URL passed into needToRedownload(): " + protocol);
//            return false;
//        }
//    }
    
}

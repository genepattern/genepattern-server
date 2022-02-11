/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.DataManager;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.GpFilePathException;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.UserUploadFile;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.executor.JobDispatchException;
import org.genepattern.server.job.input.cache.CachedFile;
import org.genepattern.server.job.input.cache.FileCache;
import org.genepattern.server.job.input.collection.ParamGroupHelper;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.util.UrlPrefixFilter;
import org.genepattern.util.LSID;
import org.genepattern.webservice.ParameterInfo;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

/**
 * Helper class, instantiated as part of processing user input, before adding a job to the queue.
 * This class, when needed, will generate a filelist file based on the list of values from the job input form.
 * 
 * @author pcarr
 *
 */
public class ParamListHelper {
    final static private Logger log = Logger.getLogger(ParamListHelper.class);
    
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

    //inputs
    final HibernateSessionManager mgr;
    final GpConfig gpConfig;
    final GpContext jobContext;
    final JobInput jobInput;
    final String baseGpHref;
    final ParameterInfoRecord parameterInfoRecord;
    final Param actualValues;
    //outputs
    final NumValues allowedNumValues;
    final RangeValues<Double> allowedRanges;
    final GroupInfo groupInfo;
    final ListMode listMode;

    /**
     * 
     * @param mgr
     * @param gpConfig
     * @param jobContext
     * @param parameterInfoRecord
     * @param inputValues
     * @param initDefault, (default=false)
     */
    public ParamListHelper(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final ParameterInfoRecord parameterInfoRecord, final JobInput jobInput, final Param inputValues, final boolean initDefault) {
        if (mgr==null) {
            throw new IllegalArgumentException("mgr==null");
        }
        if (gpConfig==null) {
            throw new IllegalArgumentException("gpConfig==null");
        }
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (parameterInfoRecord==null) {
            throw new IllegalArgumentException("parameterInfoRecord==null");
        }
        this.mgr=mgr;
        this.gpConfig=gpConfig;
        this.jobContext=jobContext;
        this.parameterInfoRecord=parameterInfoRecord;
        this.jobInput=jobInput;
        this.baseGpHref=initBaseGpHref(gpConfig, jobInput);

        //initialize allowedNumValues
        this.allowedNumValues=initAllowedNumValues();

        //initialize allowedRanges
        this.allowedRanges=initAllowedRanges(parameterInfoRecord.getFormal());

        //initialize list mode
        this.listMode=ParamListHelper.initListMode(parameterInfoRecord);
        
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

    @SuppressWarnings("deprecation")
    public static String initBaseGpHref(final GpConfig gpConfig, final JobInput jobInput) {
        if (jobInput != null && !Strings.isNullOrEmpty(jobInput.getBaseGpHref())) {
            return jobInput.getBaseGpHref();
        }
        else {
            if (log.isDebugEnabled()) {
                log.debug("jobInput.baseGpHref not set, initializing baseGpHref from GpConfig instead");
            }
            return UrlUtil.getBaseGpHref(gpConfig);
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

    /**
     * Get the value of the 'listMode' attribute from the manifest file;
     * automatically trim and convert to all upper case if it is set.
     * @param formalParam
     * @return the formatted value or null if not present
     */
    protected static String getListModeSpec(final ParameterInfo formalParam) {
        if (formalParam==null) {
            log.warn("formalParam==null");
            return null;
        }
        if (formalParam.getAttributes()==null) {
            return null;
        }
        final String rval = (String) formalParam.getAttributes().get(NumValues.PROP_LIST_MODE);
        if (rval==null) {
            return null;
        }
        return rval.toUpperCase().trim();
    }
    
    /**
     * calls hasListMode on the formalParam.
     */
    public static boolean hasListMode(final ParameterInfoRecord parameterInfoRecord) {
        if (parameterInfoRecord==null) {
            return false;
        }
        return hasListMode(parameterInfoRecord.getFormal());
    }

    /**
     * Returns true if the manifest declares a listMode= property.
     * @param formalParam
     * @return
     */
    public static boolean hasListMode(final ParameterInfo formalParam) {
        String listModeSpec=getListModeSpec(formalParam);
        return ! Strings.isNullOrEmpty(listModeSpec);
    }
    
    /**
     * calls initListMode on the formalParam.
     * @param parameterInfoRecord
     * @return
     */
    public static ListMode initListMode(final ParameterInfoRecord parameterInfoRecord) {
        if (parameterInfoRecord==null) {
            final ParameterInfo nullPinfo=null;
            return initListMode(nullPinfo);
        }
        return initListMode(parameterInfoRecord.getFormal());
    }
    
    /**
     * Get the ListMode as declared in the manifest file. For example,
     *     listMode=CMD
     * Returns the default value of ListMode.LIST when there is no custom value.
     * @param formalParam
     * @return a ListMode
     * @throws IllegalArgumentException when listMode does not match one of the entries in the ListMode enum.
     */
    public static ListMode initListMode(final ParameterInfo formalParam) throws IllegalArgumentException {
        final String listModeSpec=getListModeSpec(formalParam);
        if (! Strings.isNullOrEmpty(listModeSpec)) {
            try {
                return ListMode.valueOf(listModeSpec);
            }
            catch (Throwable t) {
                String message="Error initializing listMode from listMode="+listModeSpec;
                log.error(message, t);
                throw new IllegalArgumentException(message);
            }
        }
        //default value
        return ListMode.LIST;
    }
    
    /**
     * Helper method for checking whether the given param 
     * is configured for command line list mode.
     * 
     * This requires that numValues.acceptsList is true and that
     * listMode is either CMD or CMD_OPT.
     * 
     * @param formalParam
     * @return
     */
    public static boolean isCmdLineList(final ParameterInfo formalParam) {
        final NumValues numValues=ParamListHelper.initNumValues(formalParam);
        if (!numValues.acceptsList()) {
            // must accept a list
            return false;
        }
        final ListMode listMode=initListMode(formalParam);
        if (listMode==ListMode.CMD || listMode==ListMode.CMD_OPT) {
            return true;
        }
        return false;
    }
    
    public static boolean isCmdLineList(final ParameterInfo formalParam, final ListMode listMode) {
        if (listMode!=ListMode.CMD && listMode!=ListMode.CMD_OPT) {
            return false;
        }
        final NumValues numValues=ParamListHelper.initNumValues(formalParam);
        if (numValues.acceptsList()) {
            // must accept a list
            return true;
        }
        return false;
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

    public static RangeValues<Double> initAllowedRanges(ParameterInfo pInfo)
    {
        if (pInfo == null) {
            throw new IllegalArgumentException("pInfo == null");
        }

        @SuppressWarnings("unchecked")
        HashMap<String, String> attr = pInfo.getAttributes();
        if(attr != null && attr.containsKey(RangeValues.PROP_RANGE)) {
            final String rangeValuesStr = attr.get(RangeValues.PROP_RANGE);
            //parse range string
            RangeValuesParser rvParser = new RangeValuesParser();
            try {
                return rvParser.parseRange(rangeValuesStr);
            } 
            catch (Exception e) {
                String message = "Error parsing range=" + rangeValuesStr + " for " + pInfo.getName();
                log.error(message, e);
                throw new IllegalArgumentException(message);
            }
        }

        return new RangeValues<Double>();
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
     * @throws IllegalArgumentException if actual value entered is not within
     *      the allowed range of values. For example,
     *          actual value is 1 and range = 2+
     *          actual value is 2 and range = 0..1
     *          actual value is 3 and range = 0- (i.e <= 0)
     */
    public void validateRange() {
        final Double minRange = allowedRanges.getMin();
        final Double maxRange = allowedRanges.getMax();

        //when both range min and range max is not set, it means there is no 'range' attribute for the parameter, assume it's not a filelist
        if (minRange == null && maxRange == null) {
            //everything else is valid
            return;
        }

        //if we're here, it means range is set
        //are we in range?
        if (minRange != null && actualValues.getRangeMin() < minRange ) {
            throw new IllegalArgumentException("Minimum value for "+parameterInfoRecord.getFormal().getName()+
                    "is out of the expected range, num="+actualValues.getRangeMin()+", min="+allowedRanges.getMin());
        }

        //if we're here, it means range is set
        //are we in range?
        if (maxRange != null && actualValues.getRangeMax() > minRange ) {
            throw new IllegalArgumentException("Maximum value for " + parameterInfoRecord.getFormal().getName()+
                    "is out of the expected range, num="+actualValues.getRangeMax()+", max="+allowedRanges.getMax());
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
     * 
     * @param formalParam, initialized from the manifest file
     * @param actualValues, the values for a particular run of the module
     * @return
     */
    public static boolean isCreateFilelist(final ParameterInfo formalParam, final Param actualValues) {
        final NumValues numValues=ParamListHelper.initNumValues(formalParam);
        final ListMode listMode=ParamListHelper.initListMode(formalParam);
        return isCreateFilelist(numValues, listMode, actualValues);
    }

    protected static boolean isCreateFilelist(final NumValues numValues, final ListMode listMode, final Param actualValues) {
        if (numValues == null || !numValues.acceptsList()) {
            return false;
        }

        if(ListMode.CMD.equals(listMode) || (ListMode.CMD_OPT.equals(listMode))) {
            return false;
        }
        
        if (actualValues==null) {
            log.warn("actualValues==null");
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
     * Do we need to create a filelist file for this parameter?
     */
    public boolean isCreateFilelist() {
        return ParamListHelper.isCreateFilelist(this.allowedNumValues, this.listMode, this.actualValues);
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
    public GpFilePath initDirectoryInputValue(final ParamValue paramValueIn) throws GpFilePathException {
        if (!isDirectoryInputParam()) {
            throw new GpFilePathException("Input parameter is not DIRECTORY type: "+parameterInfoRecord.getFormal().getName()+"="+paramValueIn.getValue());
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
        
        final ParamListValue inputRecord=initFromValue(paramValueIn);
        //special-case: external urls are not allowed
        if (inputRecord.type==ParamListValue.Type.EXTERNAL_URL) {
            throw new GpFilePathException("External url not allowed for DIRECTORY: "+parameterInfoRecord.getFormal().getName()+"="+paramValueIn.getValue());
        }
        //special-case: it's not a directory
        if (!inputRecord.getGpFilePath().isDirectory()) {
            throw new GpFilePathException("Value is not a directory: "+parameterInfoRecord.getFormal().getName()+"="+paramValueIn.getValue());
        }
        return inputRecord.getGpFilePath();
    }
    
    /**
     * Convert user-supplied value for a file input parameter into a GpFilePath instance.
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
        final ParamListValue inputRecord=initFromValue(paramValueIn);
        return inputRecord.getGpFilePath();
    }

    public boolean isPassByReference() {
        return isPassByReference(parameterInfoRecord.getFormal());
    }

    public static boolean isPassByReference(ParameterInfo formalParam) {
        if (formalParam==null) {
            return false;
        }
        return formalParam._isUrlMode();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void updatePinfoValue() throws ParamListException, GpFilePathException, IOException, DbException, JobDispatchException { //throws Exception {
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
                .mgr(mgr)
                .gpConfig(gpConfig)
                .jobContext(jobContext)
                .jobInput(jobInput)
                .parameterInfoRecord(parameterInfoRecord)
                .groupInfo(groupInfo)
                .downloadExternalFiles(!passByReference)
                .build();
            final GpFilePath toFile=pgh.createFilelist();
            final String toFileHref=UrlUtil.getHref(baseGpHref, toFile);
            parameterInfoRecord.getActual().setValue(toFileHref);
            saveGroupedValuesToClob(pgh.getGpFilePaths());
        }
        else if (createFilelist)
        {
            final boolean downloadExternalFiles = !passByReference;
            final List<GpFilePath> listOfValues=ParamListHelper.getListOfValues(
                    mgr, 
                    gpConfig, 
                    jobContext, 
                    jobInput, 
                    this.parameterInfoRecord.getFormal(), 
                    actualValues, 
                    downloadExternalFiles
            );
            final GpFilePath toFile=createFilelist(mgr, gpConfig, listOfValues, passByReference);
            final String toFileHref=UrlUtil.getHref(baseGpHref, toFile);
            parameterInfoRecord.getActual().setValue(toFileHref);
            saveListOfValuesToClob(downloadExternalFiles, listOfValues); 
        }
        else if (numValues > 0 && (ListMode.CMD.equals(listMode) || (ListMode.CMD_OPT.equals(listMode))))
        {
            // set the display value ...
            String valuesString="[" + Joiner.on(", ").join(actualValues.getValues()) + "]";
            parameterInfoRecord.getActual().setValue(valuesString);
        }
        else if (numValues==0) {
            parameterInfoRecord.getActual().setValue("");
        }
        else if (numValues==1) {
            final ParamValue paramValueIn=actualValues.getValues().get(0);
            //special-case for FILE type with server file paths, check file access permissions and if necessary convert value to URL form
            if (parameterInfoRecord.getFormal().isInputFile()) {
                final ParamListValue inputRecord=initFromValue(paramValueIn);
                if (inputRecord.type==ParamListValue.Type.SERVER_PATH || inputRecord.type==ParamListValue.Type.SERVER_URL) {
                    final GpFilePath file=inputRecord.getGpFilePath();
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
                        throw new GpFilePathException("For the input parameter, "+pname+", You are not permitted to access the file: "+value);
                    } 
                    final String toFileHref=UrlUtil.getHref(baseGpHref, file);
                    parameterInfoRecord.getActual().setValue(toFileHref);
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
                        throw new GpFilePathException("You are not permitted to access the directory: "+paramValueIn.getValue());
                    }
                    final String directoryHref=UrlUtil.getHref(baseGpHref, directory);
                    parameterInfoRecord.getActual().setValue(directoryHref);
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
                final String out=UrlUtil.replaceGpUrl(gpConfig, baseGpHref, in);
                parameterInfoRecord.getActual().setValue(out);
            }
        }
    }

    /**
     * Save the list of values to the parameter info CLOB
     * @param downloadExternalFiles
     * @param listOfValues
     * @throws MalformedURLException 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    protected void saveListOfValuesToClob(final boolean downloadExternalFiles, final List<GpFilePath> listOfValues) 
    throws MalformedURLException 
    {
        int idx=0;
        for(GpFilePath inputValue : listOfValues) {
            final String key="values_"+idx;
            //String value=url.toExternalForm();
            String value="";
            if(downloadExternalFiles) {
                value = "<GenePatternURL>"+inputValue.getRelativeUri().toString();
            }
            else {
                //provide the url directly since the file was not downloaded
                value = inputValue.getUrl().toExternalForm();
            }
            parameterInfoRecord.getActual().getAttributes().put(key, value);
            ++idx;
        }
    }

    /**
     * Save the filelist and groupids to the parameter info CLOB
     * @param listOfValues
     */
    @SuppressWarnings("unchecked")
    protected void saveGroupedValuesToClob(final List<GpFilePath> listOfValues) {
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

    //-----------------------------------------------------
    //helper methods for creating parameter list files ...
    //-----------------------------------------------------
    private GpFilePath createFilelist(final HibernateSessionManager mgr, final GpConfig gpConfig, final List<GpFilePath> listOfValues, boolean urlMode) 
    throws GpFilePathException, ParamListException, DbException  
    {
        //now, create a new filelist file, add it into the user uploads directory for the given job
        JobInputFileUtil fileUtil = new JobInputFileUtil(gpConfig, jobContext);
        final int index=-1;
        final String pname=parameterInfoRecord.getFormal().getName();
        final String filename=".list.txt";
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, pname, filename);

        //write the file list
        try {
            final ParamListWriter writer=new ParamListWriter();
            writer.writeParamList(gpFilePath, listOfValues, urlMode);
        }
        catch (Throwable t) {
            throw new ParamListException("Error saving parameter list to file="+filename, t);
        }
        fileUtil.updateUploadsDb(mgr, gpFilePath);
        return gpFilePath;
    }
    
    /**
     * Create a list of GpFilePath mapped, in the same order as the actualValues, optionally downloading external URLs to the 
     * server file system.
     * 
     * @param gpConfig
     * @param jobContext
     * @param formalParam, initialized from the module manifest
     * @param actualValues, the actual job input values
     * @param downloadExternalUrl, when true download files and wait.
     * @return
     * @throws JobDispatchException 
     * @throws DbException 
     * @throws IOException 
     * @throws GenomeSpaceException 
     * @throws Exception
     */
    public static List<GpFilePath> getListOfValues(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final JobInput jobInput, final ParameterInfo formalParam, final Param actualValues, final boolean downloadExternalUrl) 
    throws GpFilePathException, IOException, DbException, JobDispatchException 
    {
        final List<ParamListValue> tmpList=new ArrayList<ParamListValue>();
        for(ParamValue pval : actualValues.getValues()) {
            final ParamListValue rec=initFromValue(mgr, gpConfig, jobContext, jobInput.getBaseGpHref(), formalParam, pval);
            tmpList.add(rec);
        }
        
        // If necessary, download data from external sites
        if (downloadExternalUrl) {
            for(final ParamListValue rec : tmpList) {
                downloadFromRecord(mgr, gpConfig, jobContext, rec);
            }
        } 

        final List<GpFilePath> values=new ArrayList<GpFilePath>();
        for(final ParamListValue rec : tmpList) {
            values.add( rec.getGpFilePath() );
        }
        return values;
    }

    protected static void downloadFromRecord(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final ParamListValue rec) 
    throws GpFilePathException, IOException, DbException, JobDispatchException 
    {
        // Handle external URLs
        if (rec.type.equals(ParamListValue.Type.EXTERNAL_URL)) {
            if (rec.isCached) {
                final GpFilePath cached=FileCache.downloadCachedFile(mgr, gpConfig, jobContext, rec.url.toExternalForm());
                rec.setGpFilePath(cached);
            }
           
            else {
                forFileListCopyExternalUrlToUserUploads(mgr, gpConfig, jobContext, rec.getGpFilePath(), rec.url);
            }
        }
    }

    protected ParamListValue initFromValue(final ParamValue pval) throws GpFilePathException {
        return ParamListHelper.initFromValue(mgr, gpConfig, jobContext, baseGpHref, this.parameterInfoRecord.getFormal(), pval);
    }

    public static ParamListValue initFromValue(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, 
            final String baseGpHref, final ParameterInfo formalParam, final ParamValue pval) 
    throws GpFilePathException 
    {
        
        final String value=pval.getValue();
        URL externalUrl=JobInputHelper.initExternalUrl(gpConfig, baseGpHref, value);
      
        
        final boolean isPassByReference=isPassByReference(formalParam);
        
        if (externalUrl != null) {
            final boolean isCached=UrlPrefixFilter.isCachedValue(gpConfig, jobContext, formalParam, externalUrl);
            if (isPassByReference) {
                // special-case: pass-by-reference
                GpFilePath gpPath = new ExternalFile(externalUrl);
                ParamListValue record=new ParamListValue(ParamListValue.Type.EXTERNAL_URL, gpPath, externalUrl);
                record.isCached=isCached;
                record.isPassByReference=isPassByReference;
                return record;
            }
            else if (isCached) {
                // special-case: 'cache.externalUrlDirs'
                CachedFile cachedFile=FileCache.initCachedFileObj(mgr, gpConfig, jobContext, value);
                ParamListValue record=new ParamListValue(ParamListValue.Type.EXTERNAL_URL, cachedFile.getLocalPath(), externalUrl);
                record.isCached=isCached;
                record.isPassByReference=isPassByReference;
                return record;
            }
            else {
                // by default, external url inputs for file lists are cached on a per-user basis, in the user's tmp directory
                GpFilePath gpPath = JobInputFileUtil.getDistinctPathForExternalUrl(gpConfig, jobContext, externalUrl);
                ParamListValue record=new ParamListValue(ParamListValue.Type.EXTERNAL_URL, gpPath, externalUrl);
                return record;
            }
        } else {
            // check if its an external URI (like s3://) that our ExternalFileManager can handle (if there is one)
            URI externalUri = null;
            externalUri = JobInputHelper.initExternalUri(gpConfig, value);
            final boolean isCached=false; // TODO make the cache handle URIs as well
            // final boolean isCached=UrlPrefixFilter.isCachedValue(gpConfig, jobContext, formalParam, externalUri);
            
            if ((externalUri != null)){
                
                GpFilePath gpPath = JobInputFileUtil.getDistinctPathForExternalUrl(gpConfig, jobContext, externalUri);
                
                ParamListValue record=new ParamListValue(ParamListValue.Type.EXTERNAL_URL, gpPath, externalUri);
                record.isCached=false;
                record.isPassByReference=false;
                return record;   
                
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
            final GpFilePath gpPath = GpFileObjFactory.getRequestedGpFileObj(gpConfig, value, lsid);
            return new ParamListValue(ParamListValue.Type.SERVER_URL, gpPath, (URL)null);
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
                gpPath=GpFileObjFactory.getRequestedGpFileObj(gpConfig, "/data", "/"+pathIn);
            }
            catch (Throwable tx) {
                log.error("Error initializing gpFilePath for directory input: "+pathIn, tx);
            }
        }
        else {
            try {
                gpPath=GpFileObjFactory.getRequestedGpFileObj(gpConfig, value);
            }
            catch (Throwable t) {
                log.error("Error initializing gpFilePath for directory input: "+value, t);
                final File serverFile=new File(value);
                gpPath = ServerFileObjFactory.getServerFile(serverFile);
            }
        }
        if (gpPath != null) {
            return new ParamListValue(ParamListValue.Type.SERVER_PATH, gpPath, (URL)null); 
        }
        throw new GpFilePathException("Error initializing gpFilePath for value="+value);
    }

    /**
     * Copy data from an external URL into a file in the GP user's uploads directory.
     * This method blocks until the data file has been transferred.
     * 
     * @param jobContext, must have a valid userId and should have a valid jobInfo
     * @param gpPath
     * @param url
     * @throws Exception
     * 
     */
    protected static void forFileListCopyExternalUrlToUserUploads(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, final GpFilePath gpPath, final URL url) 
    throws GpFilePathException, IOException, DbException 
    {
        final File parentDir=gpPath.getServerFile().getParentFile();
        if (!parentDir.exists()) {
            boolean success=parentDir.mkdirs();
            if (!success) {
                String message="Error creating upload directory for external url: dir="+parentDir.getPath()+", url="+url.toExternalForm();
                log.error(message);
                throw new GpFilePathException(message);
            }
        }
        final File dataFile=gpPath.getServerFile();
        if (dataFile.exists()) {
            //do nothing, assume the file has already been transferred
            log.debug("dataFile already exists: "+dataFile.getPath());
        }
        else {
            //copy the external url into a new file in the user upload folder
            if (DataManager.getExternalFileManager(jobContext) == null){
                org.apache.commons.io.FileUtils.copyURLToFile(url, dataFile);
            }
            //add a record of the file to the DB, so that a link will appear in the Uploads tab
            JobInputFileUtil jobInputFileUtil=new JobInputFileUtil(gpConfig, jobContext);
            jobInputFileUtil.updateUploadsDb(mgr, gpPath);
        }
    }

   
    
}

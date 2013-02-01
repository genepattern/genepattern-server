package org.genepattern.server.job.input;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamValue;
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

    //inputs
    Context jobContext;
    ParameterInfo pinfo;
    Param actualValues;
    //outputs
    NumValues allowedNumValues;
    ListMode listMode=ListMode.LEGACY;

    public ParamListHelper(final Context jobContext, final ParameterInfo pinfo, final Param actualValues) {
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (pinfo==null) {
            throw new IllegalArgumentException("pinfo==null");
        }
        if (actualValues==null) {
            throw new IllegalArgumentException("actualValues==null");
        }
        this.jobContext=jobContext;
        this.pinfo=pinfo;
        this.actualValues=actualValues;

        initAllowedNumValues();

        //initialize list mode
        String listModeStr = (String) pinfo.getAttributes().get("listMode");
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
        final String numValuesStr = (String) pinfo.getAttributes().get("numValues");
        //parse num values string
        NumValuesParser nvParser=new NumValuesParserImpl();
        try { 
            allowedNumValues=nvParser.parseNumValues(numValuesStr);
        }
        catch (Exception e) {
            String message="Error parsing numValues="+numValuesStr+" for "+pinfo.getName();
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
                if (!pinfo.isOptional()) {
                    throw new IllegalArgumentException("Missing required parameter: "+pinfo.getName());
                }
            }
            //everything else is valid
            return;
        }

        //if we're here, it means numValues is set, need to check for filelists
        //are we in range?
        if (numValuesSet < allowedNumValues.getMin()) {
            throw new IllegalArgumentException("Not enough values for "+pinfo.getName()+
                    ", num="+numValuesSet+", min="+allowedNumValues.getMin());
        }
        if (allowedNumValues.getMax() != null) {
            //check upper bound
            if (numValuesSet > allowedNumValues.getMax()) {
                throw new IllegalArgumentException("Too many values for "+pinfo.getName()+
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

    public void updatePinfoValue() throws Exception {
        final int numValues=actualValues.getNumValues();
        final boolean createFilelist=isCreateFilelist();

        if (pinfo._isDirectory() || pinfo.isInputFile()) {
            HashMap attrs = pinfo.getAttributes();
            attrs.put(ParameterInfo.MODE, ParameterInfo.URL_INPUT_MODE);
            attrs.remove(ParameterInfo.TYPE);
        }

        if (createFilelist) {
            final GpFilePath filelistFile=createFilelist();
            String filelist=filelistFile.getUrl().toExternalForm();
            pinfo.setValue(filelist);
        }
        else if (numValues==0) {
            pinfo.setValue("");
        }
        else if (numValues==1) {
            pinfo.setValue(actualValues.getValues().get(0).getValue());
        }
        else {
            //TODO: error
            log.error("It's not a filelist and numValues="+numValues);
        }
    }
    
    //-----------------------------------------------------
    //helper methods for creating parameter list files ...
    //-----------------------------------------------------
    private GpFilePath createFilelist() throws Exception {
        boolean downloadExternalFiles=true;
        List<GpFilePath> filepaths=getListOfValues(downloadExternalFiles);
        //now, create a new filelist file, add it into the user uploads directory for the given job
        JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
        final int index=-1;
        final String pname=pinfo.getName();
        final String filename=".list.txt";
        GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, pname, filename);

        //write the file list
        ParamListWriter writer=new ParamListWriter.Default();
        writer.writeParamList(gpFilePath, filepaths);
        //writeFilelist(gpFilePath.getServerFile(), filepaths, false);
        fileUtil.updateUploadsDb(gpFilePath);

        //return gpFilePath.getUrl().toExternalForm();
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
        URL externalUrl=initExternalUrl(value);
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
     * This mehtod blocks intil the data file has been transferred.
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

    /**
     * Is the input value an external URL?
     * 
     * @param value
     * 
     * @return the URL if it's an external url, otherwise return null.
     */
    static public URL initExternalUrl(final String value) {
        log.debug("intialize external URL for value="+value);

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
            //url.getHost()
        }
        catch (MalformedURLException e) {
            log.debug("it's not a url", e);
            return null;
        }
        return url;
    }

    //util
    //static private final AtomicReference<JobInputFileUtil> jobInputFileUtilRef= new AtomicReference<JobInputFileUtil>();
//    private static JobInputFileUtil getJobInputFileUtil(final Context jobContext) throws Exception {
//        //thread-safe, lazy-init of jobInputFileUtil
//        JobInputFileUtil existingValue=jobInputFileUtilRef.get();
//        if (existingValue != null) {
//            return existingValue;
//        }
//        JobInputFileUtil newValue=new JobInputFileUtil(jobContext);
//        if (jobInputFileUtilRef.compareAndSet(null, newValue)) {
//            return newValue;
//        }
//        return jobInputFileUtilRef.get();
//    }

}

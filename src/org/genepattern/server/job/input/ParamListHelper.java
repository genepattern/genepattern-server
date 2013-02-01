package org.genepattern.server.job.input;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
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

    //util
    static private final AtomicReference<JobInputFileUtil> jobInputFileUtilRef= new AtomicReference<JobInputFileUtil>();


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
        //when minNumValues is not set, it means there is no 'numValues' attribute for the parameter, assume it's not a filelist
        if (allowedNumValues.getMin()==null) {
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
            final String filelist=initFilelist();
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

    private String initFilelist() throws Exception {
        //TODO: need to handle server file paths 
        List<GpFilePath> filepaths=extractFilelist(jobContext); 
        //now, create a new filelist file, add it into the user uploads directory for the given job
        GpFilePath filelist=getFilelist(jobContext, actualValues);
        writeFilelist(filelist.getServerFile(), filepaths, false);
        getJobInputFileUtil(jobContext).updateUploadsDb(filelist);

        return filelist.getUrl().toExternalForm();
    }

    private void writeFilelist(File output, List<GpFilePath> files, boolean writeTimestamp) throws IOException {
        final String SEP="\t";
        FileWriter writer = null;
        BufferedWriter out = null;
        try {
            writer = new FileWriter(output);
            out = new BufferedWriter(writer);
            for(GpFilePath filePath : files) {
                File file = filePath.getServerFile();
                out.write(file.getAbsolutePath());
                if (writeTimestamp) {
                    out.write(SEP); out.write("timestamp="+file.lastModified());
                    out.write(SEP); out.write(" date="+new Date(file.lastModified())+" ");
                }
                out.newLine();
            }
        }
        finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private static GpFilePath getFilelist(final Context jobContext, final Param param) throws Exception {
        final String paramName=param.getParamId().getFqName();
        GpFilePath filelist=getJobInputFileUtil(jobContext).getDistinctPathForFilelist(paramName);
        return filelist;
    }

    /**
     * If necessary, initialize the filelist data structure.
     */

    private List<GpFilePath> extractFilelist(final Context jobContext) throws Exception {
        List<GpFilePath> filepaths=new ArrayList<GpFilePath>();
        for(ParamValue pval : actualValues.getValues()) {
            String value=pval.getValue();
            try {
                GpFilePath filepath = GpFileObjFactory.getRequestedGpFileObj(value);
                filepaths.add(filepath);
            }
            catch (Exception e) {
                //could be an external url
                try {
                    GpFilePath filepath = getGpFilePathFromExternalUrl(jobContext, value);
                    if (filepath != null) {
                        filepaths.add(filepath);
                    }
                }
                catch (Exception ex) {
                    log.error(ex);
                    throw ex;
                }
            }
        }
        return filepaths;
    }
    //Note: this method blocks until the data file has been transferred
    //TODO: turn this into a task which can be cancelled
    private static GpFilePath getGpFilePathFromExternalUrl(final Context jobContext, final String value) throws Exception {
        URL url = initExternalUrl(value);
        if (url==null) {
            return null;
        }

        GpFilePath gpPath=JobInputFileUtil.getDistinctPathForExternalUrl(jobContext, url);
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
            log.debug("dataFile already exists: "+dataFile.getPath());
        }
        else {
            //copy the external url into a new file in the user upload folder
            org.apache.commons.io.FileUtils.copyURLToFile(url, dataFile);

            //add a record of the file to the DB, so that a link will appear in the Uploads tab
            getJobInputFileUtil(jobContext).updateUploadsDb(gpPath);
        }
        return gpPath;
    }

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
        }
        catch (MalformedURLException e) {
            log.debug("it's not a url", e);
            return null;
        }
        return url;
    }

    private static JobInputFileUtil getJobInputFileUtil(final Context jobContext) throws Exception {
        //thread-safe, lazy-init of jobInputFileUtil
        JobInputFileUtil existingValue=jobInputFileUtilRef.get();
        if (existingValue != null) {
            return existingValue;
        }
        JobInputFileUtil newValue=new JobInputFileUtil(jobContext);
        if (jobInputFileUtilRef.compareAndSet(null, newValue)) {
            return newValue;
        }
        return jobInputFileUtilRef.get();
    }

}

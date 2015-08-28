/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.*;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApi;
import org.genepattern.server.rest.JobInputApiFactory;
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for preparing a batch of jobs, when batching over input files and directories.
 * 
 * 
 * @author pcarr
 *
 */
public class BatchInputFileHelper {
    private static final Logger log = Logger.getLogger(BatchInputFileHelper.class);

    /** @deprecated should pass in a valid GpConfig */
    public static GpFilePath initGpFilePath(final String value, final boolean includeExternalUrl) {
        return initGpFilePath(ServerConfigurationFactory.instance(), value, includeExternalUrl);
    }
    
    /**
     * by default, includeExternalUrl is false.
     * @param gpConfig
     * @param value
     * @return
     */
    public static GpFilePath initGpFilePath(final GpConfig gpConfig, final String value) {
        final boolean includeExternalUrl=false;
        return initGpFilePath(gpConfig, value, includeExternalUrl);
    }

    /**
     * Get the GpFilePath for a batch input directory, if and only if, the given value
     * is a valid batch input directory. Otherwise, return null.
     * 
     * @param gpConfig
     * @param value
     * @param includeExternalUrl
     * @return
     */
    public static GpFilePath initGpFilePath(final GpConfig gpConfig, final String value, final boolean includeExternalUrl) {
        GpFilePath gpPath=null;
        URL externalUrl=JobInputHelper.initExternalUrl(gpConfig, value);
        if (externalUrl!=null) {
            //it's an externalURL
            if (!includeExternalUrl) {
                return null;
            }
            else {
                return new ExternalFile(externalUrl);
            }
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

    private static FilenameFilter listFilesFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            if (".svn".equals(name)) {
                return false;
            }
            return true;
        }
    };

    /**
     * Get the list of matching input files in the given batch directory,
     * only include files which are valid input types for the given pinfo.
     * 
     * @param pinfo, the ParameterInfo instance for the batch input parameter.
     * @param batchDir, the value of the batch input directory.
     * @return
     * @throws GpServerException
     */
    public static List<String> getBatchInputFiles(final ParameterInfo pinfo, final GpFilePath batchDir) throws GpServerException {
        final String parentUrl;
        try {
            parentUrl=batchDir.getUrl().toExternalForm();
        }
        catch (Exception e) {
            throw new GpServerException("Error initializing parentUrl: "+batchDir.getRelativeUri().toString());
        }
        List<String> filePaths = new ArrayList<String>();
        File[] files = batchDir.getServerFile().listFiles(listFilesFilter);

        //sort the files in ascending order
        Arrays.sort(files);
        for(File file : files) {
            final String fileUrl = parentUrl + UrlUtil.encodeURIcomponent( file.getName() );
            try {
                GpFilePath filePath = GpFileObjFactory.getRequestedGpFileObj(fileUrl);
                filePath.initMetadata();
                if (accept(pinfo,filePath)) {
                    filePaths.add(filePath.getUrl().toExternalForm());
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
     *        if it has no fileFormats, match any file which is not a directory
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

    public static String getBaseFilename(final GpFilePath file) {
        if (file==null) {
            log.error("file==null");
            return "";
        }
        String filename=null;       
        if (file.getName()!=null) {
            filename=file.getName();
        }
        else {
            try {
                URL url=file.getUrl();
                if (url != null) {
                    String path=url.getPath();
                    if (path != null) {
                        filename=new File(path).getName();
                    }
                }
            }
            catch (Exception e) {
                log.error(e);
            }
        }
        if (filename==null) {
            log.error("error getting filename for file="+file);
            return "";
        }
        int periodIndex = filename.lastIndexOf('.');
        if (periodIndex > 0) {
            String basename = filename.substring(0, periodIndex);
            //special-case for {basename}_1.{ext} or {basename}_2.{ext}
            int idx_1=basename.lastIndexOf("_1");
            int idx_2=basename.lastIndexOf("_2");
            if (idx_1>0) {
                return basename.substring(0, idx_1);
            }
            else if (idx_2>0) {
                return basename.substring(0, idx_2);
            }
            else {
                return basename;
            }
        }
        else {
            return filename;
        }
    }

    public BatchInputFileHelper() {}

    /**
     * Helper method, based on the value provided from the web upload form
     * or the REST API request,
     * figure out whether to add a single batch value as an external url,
     *    or as a listing of the contents of a local directory
     *    or as an individual file.
     * @param id
     * @param value
     */
    public static List<String> getBatchValues(final GpConfig gpConfig, final GpContext gpContext, final ParamId paramId, final ParameterInfoRecord record, final String value) throws GpServerException
    {
        List<String> fileValues = new ArrayList<String>();
        fileValues.add(value);

        URL externalUrl=JobInputHelper.initExternalUrl(gpConfig, value);
        if (externalUrl == null)
        {
            final GpFilePath gpPath=BatchInputFileHelper.initGpFilePath(gpConfig, value);
            if (gpPath == null) {
                throw new GpServerException("batch input not supported for param="+paramId.getFqName()+", value="+value);
            }
            if (!gpPath.getServerFile().exists()) {
                throw new GpServerException("batch input file does not exist for param="+paramId.getFqName()+", value="+value);
            }

            if (gpPath.isDirectory()) {
                fileValues = getBatchDirectory(gpContext, paramId, record, gpPath);
            }
        }

        return fileValues;
    }


    private static List<String> getBatchDirectory(final GpContext userContext, final ParamId paramId, ParameterInfoRecord record, final GpFilePath batchDir) throws GpServerException
    {
        final List<String> batchFileValues=listBatchDir(record.getFormal(), batchDir, userContext);
        if (batchFileValues==null || batchFileValues.size()==0) {
            log.debug("No matching batchValues for "+paramId.getFqName()+"="+batchDir);
        }

        return batchFileValues;
    }

    /**
     *
     * @param formalParam
     * @return
     */
    public static List<String> listBatchDir(final ParameterInfo formalParam, final GpFilePath batchInputDir, GpContext userContext) throws GpServerException {
        if (batchInputDir==null) {
            throw new IllegalArgumentException("batchInputDir==null");
        }
        if (formalParam==null) {
            throw new IllegalArgumentException("formalParam==null");
        }
        if (!batchInputDir.canRead(userContext.isAdmin(), userContext)) {
            throw new GpServerException("The current user ("+userContext.getUserId()+") doesn't have permission to read the batch input directory: "+batchInputDir);
        }
        if (!batchInputDir.getServerFile().exists()) {
            //another error
            throw new GpServerException("Can't read batch input directory: "+batchInputDir.getRelativeUri().toString());
        }
        if (!batchInputDir.getServerFile().isDirectory()) {
            final String errorMessage=""+batchInputDir+" is not a valid batch input directory";
            throw new GpServerException(errorMessage);
        }

        final List<String> batchInputFileValues=BatchInputFileHelper.getBatchInputFiles(formalParam, batchInputDir);
        if (batchInputFileValues.size()==0) {

            String externalUrlMsg = "";
            try
            {
                externalUrlMsg = "in directory " + batchInputDir.getUrl().toExternalForm();
            }
            catch(Exception e)
            {
                //do nothing here
            }

            throw new GpServerException("No matching input files for batch parameter " + formalParam.getName() + " " + externalUrlMsg);
        }
        return batchInputFileValues;
    }
}

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.batch;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.dm.serverfile.ServerFileObjFactory;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.util.SemanticUtil;
import org.genepattern.webservice.ParameterInfo;

import com.google.common.base.Strings;

/**
 * Helper class for preparing a batch of jobs, when batching over input files and directories.
 * 
 * 
 * @author pcarr
 *
 */
public class BatchInputFileHelper {
    private static final Logger log = Logger.getLogger(BatchInputFileHelper.class);

    /**
     * Get the GpFilePath for a batch input directory, if and only if, the given value
     * is a valid batch input directory. Otherwise, return null.
     * 
     * @param gpConfig
     * @param value
     * @param includeExternalUrl
     * @return
     */
    public static GpFilePath initGpFilePath(final GpConfig gpConfig, final JobInput jobInput, final String value, final boolean includeExternalUrl) {
        GpFilePath gpPath=null;
        URL externalUrl=JobInputHelper.initExternalUrl(gpConfig, jobInput, value);
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
    public static List<String> getBatchInputFiles(final String baseGpHref, final ParameterInfo pinfo, final GpFilePath batchDir) throws GpServerException {
        final String parentHref=UrlUtil.getHref(baseGpHref, batchDir);
        final List<String> filePaths = new ArrayList<String>();
        final File[] files = batchDir.getServerFile().listFiles(listFilesFilter);
        //sort the files in ascending order
        Arrays.sort(files);
        for(final File file : files) {
            if (accept(pinfo,file)) {
                final String value=UrlUtil.glue(parentHref, UrlUtil.encodeURIcomponent(file.getName()));
                filePaths.add(value);
            }
        } 
        return filePaths;
    }
    
    private static boolean accept(final ParameterInfo pinfo, final File serverFile) {
        final String extension=SemanticUtil.getExtension(serverFile);
        final String kind=SemanticUtil.getKind(serverFile, extension);
        return accept(pinfo, serverFile.isDirectory(), kind, extension);
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
     * @param pinfo the input parameter
     * @param isDirectory true if the input file is a directory
     * @param kind the 'kind' of input file
     * @param ext the input file extension
     * @return
     */
    private static boolean accept(final ParameterInfo pinfo, final boolean isDirectory, final String kind, final String ext) {
        //special-case for DIRECTORY input parameter
        if (pinfo._isDirectory()) {
            if (isDirectory) {
                return true;
            }
            else {
                return false;
            }
        }
        
        if (isDirectory) {
            //the value is a directory, but the parameter type is not a directory
            log.debug("Not implemented!");
            return false;
        }
        
        List<String> fileFormats = SemanticUtil.getFileFormats(pinfo);
        if (fileFormats.size()==0) {
            //no declared fileFormats, acceptAll
            return true;
        }
        if (fileFormats.contains(kind)) {
            return true;
        }
        if (fileFormats.contains(ext)) {
            return true;
        }
        return false;
    }

    /**
     * Extract the base filename for the given GpFilePath, with special-case for paired input files
     * which match the pattern:
     * 
     *     {basename}_1.{ext} or {basename}_2.{ext}
     * 
     * @param file
     * @return
     */
    public static String getBaseFilename(final GpFilePath file) {
        final String filename=getFilename(file);
        if (Strings.isNullOrEmpty(filename)) {
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

    /** @deprecated pass in GpConfig */
    protected static String getFilename(final GpFilePath file) {
        return getFilename(ServerConfigurationFactory.instance(), file);
    }

    /**
     * helper method to get the filename for a GpFilePath.
     * Note: could be moved into the GpFilePath class instead.
     * @param file
     * @return
     */
    protected static String getFilename(final GpConfig gpConfig, final GpFilePath file) {
        if (file==null) {
            log.error("file==null");
            return "";
        }
        else if (file.getName()!=null) {
            return file.getName();
        }
        //special-case: GpFilePath.name not initialized
        log.debug("GpFilePath.name not initialized ... ");
        if (file.getRelativeFile() != null) {
            log.debug("use relativeFile.name");
            return file.getRelativeFile().getName();
        }
        else if (file.getRelativeUri() != null) {
            final String uriPath=file.getRelativeUri().getPath();
            if (uriPath != null) {
                log.debug("use relativeUri.path");
                return new File(uriPath).getName();
            }
        }
        try {
            final String filename=UrlUtil.getFilenameFromUrl(file.getUrl(gpConfig));
            if (!Strings.isNullOrEmpty(filename)) {
                if (log.isDebugEnabled()) {
                    log.debug("use url.path, filename="+filename);
                }
                return filename;
            }
        }
        catch (Exception e) {
            log.debug("error in GpFilePath.getUrl", e);
        }
        log.debug("unable to getFilename from GpFilePath, return empty string");
        return "";
    }
    
    /**
     * Helper method, based on the value provided from the web upload form
     * or the REST API request,
     * figure out whether to add a single batch value as an external url,
     *    or as a listing of the contents of a local directory
     *    or as an individual file.
     * @param id
     * @param value
     */
    public static List<String> getBatchValues(final GpConfig gpConfig, final GpContext gpContext, final JobInput jobInput, final ParamId paramId, final ParameterInfoRecord record, final String value) throws GpServerException
    {
        List<String> fileValues = new ArrayList<String>();
        fileValues.add(value);

        URL externalUrl=JobInputHelper.initExternalUrl(gpConfig, jobInput, value);
        if (externalUrl == null)
        {
            // set includeExternalUrl is false, external url are not implemented
            final boolean includeExternalUrl=false;
            final GpFilePath gpPath=BatchInputFileHelper.initGpFilePath(gpConfig, jobInput, value, includeExternalUrl);
            if (gpPath == null) {
                throw new GpServerException("batch input not supported for param="+paramId.getFqName()+", value="+value);
            }
            if (!gpPath.getServerFile().exists()) {
                throw new GpServerException("batch input file does not exist for param="+paramId.getFqName()+", value="+value);
            }

            if (gpPath.isDirectory()) {
                fileValues = getBatchDirectory(gpContext, jobInput.getBaseGpHref(), paramId, record, gpPath);
            }
        }

        return fileValues;
    }


    private static List<String> getBatchDirectory(final GpContext userContext, final String baseGpHref, final ParamId paramId, ParameterInfoRecord record, final GpFilePath batchDir) throws GpServerException
    {
        final List<String> batchFileValues=listBatchDir(baseGpHref, record.getFormal(), batchDir, userContext);
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
    protected static List<String> listBatchDir(final String baseGpHref, final ParameterInfo formalParam, final GpFilePath batchInputDir, final GpContext userContext) throws GpServerException {
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
        final List<String> batchInputFileValues=BatchInputFileHelper.getBatchInputFiles(baseGpHref, formalParam, batchInputDir);
        if (batchInputFileValues.size()==0) {
            throw new GpServerException("No matching input files for batch parameter " + formalParam.getName() + "=" + UrlUtil.getHref(baseGpHref, batchInputDir));
        }
        return batchInputFileValues;
    }
}

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.job.input.ParamValue;
import org.genepattern.server.job.input.cache.CachedFile;
import org.genepattern.server.job.input.cache.FileCache;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.server.util.UrlPrefixFilter;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Helper class for the JobSubmitter, for downloading external file drop-down selections
 * to the cache before starting a job.
 * 
 * @author pcarr
 *
 */
public class FileDownloader {
    private static final Logger log = Logger.getLogger(FileDownloader.class);

    protected static final class FileValue {
        final String value;
        final boolean isDir;
        
        public FileValue(final String value) {
            this(value, value.endsWith("/"));
        }
        public FileValue(final String value, boolean isDir) {
            this.value=value;
            this.isDir=isDir;
        } 
        
        public String getValue() {
            return value;
        }
        public boolean isRemoteDir() {
            return isDir;
        }
        
        public int hashCode() {
            return Objects.hash(value, isDir);
        }
        
        public boolean equals(Object arg) {
            if (!(arg instanceof FileValue)) {
                return false;
            }
            FileValue fv = (FileValue) arg;
            return Objects.equals(value, fv.value) && isDir==fv.isDir;
        }
    }
    
    /**
     * Create a new downloader for the given job based on the jobId.
     * 
     * @param jobId
     * @return
     * @throws JobDispatchException
     */
    public static final FileDownloader fromJobContext(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext) throws JobDispatchException {
        return new FileDownloader(mgr, gpConfig, jobContext);
    }
    
    private final HibernateSessionManager mgr;
    private final GpConfig gpConfig;
    private final GpContext jobContext;
    private final Map<String,ParameterInfoRecord> paramInfoMap;
    private final TaskInfo taskInfo;
    private final JobInput jobInput;
    private final UrlPrefixFilter cacheFilter;
    private List<CachedFile> filesToCache=null;

    private FileDownloader(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext) {
        this(mgr, gpConfig, jobContext, null);
    }

    private FileDownloader(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext jobContext, Map<String,ParameterInfoRecord> paramInfoMapIn) {
        if (gpConfig==null) {
            throw new IllegalArgumentException("gpConfig==null");
        }
        if (jobContext==null) {
            throw new IllegalArgumentException("jobContext==null");
        }
        if (paramInfoMapIn!=null) {
            this.paramInfoMap=paramInfoMapIn;
        }
        else {
            this.paramInfoMap=ParameterInfoRecord.initParamInfoMap(jobContext.getTaskInfo());
        }
        this.mgr=mgr;
        this.gpConfig=gpConfig;
        this.jobContext=jobContext;
        this.jobInput=jobContext.getJobInput();
        this.taskInfo=jobContext.getTaskInfo();
        if (taskInfo==null) {
            log.error("taskInfo==null");
        }
        // 1) check for 'cache.externalUrlDirs' from config; can be null
        cacheFilter=UrlPrefixFilter.initCacheExternalUrlDirsFromConfig(gpConfig, jobContext);
    }
    
    /**
     * Initialize the list of external data files to be saved to the cache.
     * By default, include all values from dynamic drop-down parameters.
     * Optionally, include all values which match the server configured 'cache.externalUrlDirs' array.
     */ 
    private List<CachedFile> initFilesToCache() {
        if (jobInput==null) {
            return Collections.emptyList();
        }
        Set<FileValue> fileValues=null;
        for(final Entry<ParamId, Param> entry : jobInput.getParams().entrySet()) {
            Set<FileValue> fv=getFilesToCacheForParam(entry.getValue());
            if (fv != null && fv.size()>0) {
                //lazy-init
                if (fileValues==null) {
                    fileValues=new LinkedHashSet<FileValue>();
                }
                fileValues.addAll(fv);
            }
        }
        if (fileValues==null || fileValues.size()==0) {
            return Collections.emptyList();
        }
        List<CachedFile> cachedFiles=new ArrayList<CachedFile>();
        for(final FileValue fv : fileValues) {
            cachedFiles.add(FileCache.initCachedFileObj(mgr, gpConfig, jobContext, fv.getValue(), fv.isRemoteDir()));
        }
        return cachedFiles;
    }
    
    /**
     * For the given input parameter, get the set of unique files (by externalUrl) 
     * which should be cached on the local server.
     * 
     * @param param
     * @return
     */
    protected Set<FileValue> getFilesToCacheForParam(final Param param) {
        final String pname=param.getParamId().getFqName();
        ParameterInfoRecord record=paramInfoMap.get(pname);
        if (record==null) {
            if (log.isDebugEnabled()) {
                log.debug("record==null, param="+pname);
            }
            return Collections.emptySet();
        }
        return getFilesToCacheForParam(param, record.getFormal());
    }

    /**
     * similar to UrlPrefixFilter.isCachedValue ... call this when the dropDownFilter and cacheFilter are already initialized.
     * @param paramValue
     * @param dropDownFilter
     * @param cacheFilter
     * @return
     */
    protected boolean isCachedValue(final String paramValue, UrlPrefixFilter dropDownFilter, UrlPrefixFilter cacheFilter) {
        // only if it's an external url
        URL externalUrl=JobInputHelper.initExternalUrl(gpConfig, jobInput, paramValue);
        if (externalUrl==null) {
            return false;
        }
        return UrlPrefixFilter.accept(externalUrl.toExternalForm(), dropDownFilter, cacheFilter);
    }
    
    protected Set<FileValue> getFilesToCacheForParam(final Param param, final ParameterInfo formal) {
        boolean canBeCached=UrlPrefixFilter.isCachedParam(jobContext, param, formal);
        if (!canBeCached) {
            return Collections.emptySet();
        }
        
        // 2) check for selection from dynamic drop-down menu; match all values which are prefixed by the remote directory
        final UrlPrefixFilter dropDownFilter=UrlPrefixFilter.initDropDownFilter(formal);
        if (cacheFilter==null && dropDownFilter==null) {
            // no filter
            return Collections.emptySet();
        }

        // filter values by prefix
        Set<FileValue> rval=null;
        for(ParamValue value : param.getValues()) {
            final boolean accepted=isCachedValue(value.getValue(), dropDownFilter, cacheFilter);
            if (accepted) {
                if (rval==null) {
                    // lazy-init rval
                    rval=new LinkedHashSet<FileValue>();
                }
                rval.add(new FileValue(value.getValue()));
            }
        }
        if (rval==null) {
            return Collections.emptySet();
        }
        return rval;
    }

    /**
     * Does the job have at least one input file for the cache?
     * @return
     */
    protected boolean hasFilesToCache() {
        getFilesToCache();
        return filesToCache != null && filesToCache.size()>0;
    }
    
    protected List<CachedFile> getFilesToCache() {
        if (this.filesToCache==null) {
            this.filesToCache=initFilesToCache();
        }
        return this.filesToCache;
    }

    /**
     * Call this method before running the job, it takes care of downloading any input files selected from a 
     * drop-down menu. The main purpose of this method is to wait, if necessary, for each of the files to download 
     * into the cache before proceeding.
     * 
     * @see ChoiceInfoCache
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void startDownloadAndWait(final GpConfig gpConfig, final GpContext jobContext) throws InterruptedException, ExecutionException {
        getFilesToCache();
        if (filesToCache == null) {
            log.debug("filesToCache==null");
            return;
        }
        if (filesToCache.size()==0) {
            log.debug("filesToCache.size()==0");
            return;
        }
        if (log.isDebugEnabled()) {
            if (jobContext != null) {
                log.debug("downloading files for jobId="+jobContext.getJobNumber()+" ...");
            }
        }

        // loop through all the filesToDownload and start downloading ...
        for(final CachedFile fileToCache : filesToCache) {
            try {
                final Future<?> f = FileCache.instance().getFutureObj(fileToCache);
                f.get(100, TimeUnit.MILLISECONDS);
            }
            catch (TimeoutException e) {
                //skip, it means the file is still downloading
            }
            Thread.yield();
        }
        // now loop through all of the filesToDownload and wait for each download to complete
        for(final CachedFile fileToCache : filesToCache) {
            final Future<?> f = FileCache.instance().getFutureObj(fileToCache);
            f.get();
        }
    }
    
}

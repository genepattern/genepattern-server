/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.genepattern.junitutil.ParameterInfoUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.cache.CachedFile;
import org.genepattern.server.job.input.cache.CachedFtpDir;
import org.genepattern.server.util.UrlPrefixFilter;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * junit tests for the FileDownloader class.
 * @author pcarr
 *
 */
public class TestFileDownloader {
    private GpConfig gpConfig;
    private GpContext jobContext;
    private TaskInfo taskInfo = new TaskInfo();
    private JobInfo jobInfo;
    private JobInput jobInput;

    private String choiceDir="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/";
    private String selectedValue="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_1.txt";
    private String choiceDir_dirListing="ftp://gpftp.broadinstitute.org/demo/dir/";
    private String selectedDirValue="ftp://gpftp.broadinstitute.org/demo/dir/A/";

    // for testing external cache config
    private Value cacheExternalDirs=new Value(Arrays.asList(
            "ftp://gpftp.broadinstitute.org/",
            "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/"));

    @Before
    public void setUp() {
        gpConfig=mock(GpConfig.class);
        jobContext=mock(GpContext.class);
        jobInfo = mock(JobInfo.class);
        jobInput=new JobInput();
        when(jobContext.getTaskInfo()).thenReturn(taskInfo);
        when(jobContext.getJobInfo()).thenReturn(jobInfo);
        when(jobContext.getJobInput()).thenReturn(jobInput);
    }

    @Test
    public void selectionFromDropDown()  throws JobDispatchException {
        taskInfo.setParameterInfoArray(new ParameterInfo[] { 
                ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir) 
        });
        jobInput.addValue("input.file", selectedValue);
        
        FileDownloader downloader = FileDownloader.fromJobContext(gpConfig, jobContext);
        assertTrue("Expecting a choice selection", downloader.hasFilesToCache());
        
        CachedFile cachedFile=downloader.getFilesToCache().get(0);
        assertEquals("cachedFile.url", selectedValue, cachedFile.getUrl().toString());
        assertEquals("cachedFile.downloaded", false, cachedFile.isDownloaded());
        assertEquals("cachedFile instanceof CachedFtpDir", false, downloader.getFilesToCache().get(0) instanceof CachedFtpDir);
    }
    
    @Test
    public void selectionFromDropDownDir() throws JobDispatchException {
        taskInfo.setParameterInfoArray(new ParameterInfo[] { 
                ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir_dirListing) 
        });
        jobInput.addValue("input.file", selectedDirValue);

        FileDownloader downloader = FileDownloader.fromJobContext(gpConfig, jobContext); 
        assertTrue("Expecting a choice selection", downloader.hasFilesToCache());
        assertEquals("selectedChoices[0].value",
                selectedDirValue, 
                downloader.getFilesToCache().get(0).getUrl().toString());
        assertEquals("cachedFile instanceof CachedFtpDir", true, downloader.getFilesToCache().get(0) instanceof CachedFtpDir);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void passByReference_ignoreDropdownSelection() throws JobDispatchException {
        ParameterInfo pinfo=ParameterInfoUtil.initFileDropdownParam("input.file", choiceDir);
        // set to passByReference
        pinfo.getAttributes().put(GPConstants.PARAM_INFO_URL_MODE[0], "on");
        assertEquals("pinfo._isUrlMode()", true, pinfo._isUrlMode());
        
        taskInfo.setParameterInfoArray(new ParameterInfo[] { 
                pinfo 
        });
        jobInput.addValue("input.file", selectedValue);

        FileDownloader downloader = FileDownloader.fromJobContext(gpConfig, jobContext);
        assertFalse("ignore passByReference selection", downloader.hasFilesToCache());
    }
    
    @Test
    public void cacheExternalDirs_default() {
        UrlPrefixFilter filter=UrlPrefixFilter.initCacheExternalUrlDirsFromConfig(gpConfig, jobContext);
        assertNull("by default, expecting null filter", filter);
    }

    @Test
    public void cacheExternalDirs_custom_accept() {
        when(gpConfig.getValue(jobContext, UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL)).thenReturn(cacheExternalDirs);
        UrlPrefixFilter filter=UrlPrefixFilter.initCacheExternalUrlDirsFromConfig(gpConfig, jobContext);
        
        // run through a few tests
        assertEquals("accept from ftp.broadinstitute.org", true, 
                filter.accept("ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct"));
        assertEquals("accept from gpftp.broadinstitute.org", true, 
                filter.accept("ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct"));
    }

    @Test
    public void cacheExternalDirs_custom_ignore() {
        when(gpConfig.getValue(jobContext, UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL)).thenReturn(cacheExternalDirs);
        UrlPrefixFilter filter=UrlPrefixFilter.initCacheExternalUrlDirsFromConfig(gpConfig, jobContext);
        assertEquals("ignore from http://www.broadinstitute.org/cancer/software/genepattern/data/", false, 
                filter.accept("http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_test.gct"));
    }

    @Test
    public void selectionFromCachedExternalUrl() throws JobDispatchException {
        taskInfo.setParameterInfoArray(new ParameterInfo[] { 
                ParameterInfoUtil.initFileParam("input.file", "", "an input file") 
        });
        jobInput.addValue("input.file", "ftp://gpftp.broadinstitute.org/example/all_aml_test.gct"); 
        when(gpConfig.getValue(jobContext, UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL)).thenReturn(cacheExternalDirs);

        FileDownloader downloader = FileDownloader.fromJobContext(gpConfig, jobContext); 
        Assert.assertEquals("hasFilesToDownload", true, downloader.hasFilesToCache());
        Assert.assertEquals("filesToDownload[0].value",
                "ftp://gpftp.broadinstitute.org/example/all_aml_test.gct", 
                downloader.getFilesToCache().get(0).getUrl().toString());
    } 

}

/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.genepattern.junitutil.ParameterInfoUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.Value;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.cache.CachedFile;
import org.genepattern.server.job.input.cache.CachedFtpDir;
import org.genepattern.server.util.UrlPrefixFilter;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;

/**
 * junit tests for the FileDownloader class.
 * @author pcarr
 *
 */
public class TestFileDownloaderConfig {
    final String gpUrl="http://127.0.0.1:8080/gp/";
    
    private HibernateSessionManager mgr;
    private GpConfig gpConfig;
    private GpContext jobContext;
    private TaskInfo taskInfo = new TaskInfo();
    private JobInfo jobInfo;
    private JobInput jobInput;

    private String choiceDir="ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/";
    private String selectedValue="ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/all_aml_train.cls";
    private String choiceDir_dirListing="ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/";
    private String selectedDirValue="ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/all_aml_test.cls";

    
    /**
     * custom assertion, assert that the actual file contains the expected content.
     * @param message
     * @param expectedContent
     * @param actual
     */
    protected static void assertFileFirstLineContent(final String message, final String expectedContent, final File actual) {
        assertEquals(""+actual+" exists", true, actual.exists());
        try {
            String actualContent=Files.toString(actual, Charset.forName("UTF-8"));
            List<String> lines  = FileUtils.readLines(actual);
            assertEquals(message, expectedContent, lines.get(0));        
        }
        catch (Throwable t) {
            fail("error validating file contents: "+t.getLocalizedMessage());
        }
    }
    
    
    // for testing external cache config
    private Value cacheExternalDirs=new Value(Arrays.asList(
            "ftp://ftp.broadinstitute.org/",
            "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/"));

    @Before
    public void setUp() throws ExecutionException {
        mgr=null;
        gpConfig=mock(GpConfig.class);
        when(gpConfig.getGpUrl()).thenReturn(gpUrl);
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
        
        FileDownloader downloader = FileDownloader.fromJobContext(mgr, gpConfig, jobContext);
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

        FileDownloader downloader = FileDownloader.fromJobContext(mgr, gpConfig, jobContext); 
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

        FileDownloader downloader = FileDownloader.fromJobContext(mgr, gpConfig, jobContext);
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
        jobInput.addValue("input.file", "ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/all_aml_test.gct"); 
        when(gpConfig.getValue(jobContext, UrlPrefixFilter.PROP_CACHE_EXTERNAL_URL)).thenReturn(cacheExternalDirs);

        FileDownloader downloader = FileDownloader.fromJobContext(mgr, gpConfig, jobContext); 
        assertEquals("hasFilesToDownload", true, downloader.hasFilesToCache());
        assertEquals("filesToDownload[0].value",
                "ftp://ftp.broadinstitute.org/pub/genepattern/all_aml/all_aml_test.gct", 
                downloader.getFilesToCache().get(0).getUrl().toString());
    } 

}

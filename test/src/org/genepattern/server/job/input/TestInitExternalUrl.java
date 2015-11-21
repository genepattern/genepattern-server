/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import static org.genepattern.junitutil.Demo.dataFtpDir;
import static org.genepattern.junitutil.Demo.dataGsDir;
import static org.genepattern.junitutil.Demo.gpHref;
import static org.genepattern.junitutil.Demo.gpUrl;
import static org.genepattern.junitutil.Demo.jobResultFile;
import static org.genepattern.junitutil.Demo.localDataDir;
import static org.genepattern.junitutil.Demo.proxyHref;
import static org.genepattern.junitutil.Demo.proxyUrl;
import static org.genepattern.junitutil.Demo.serverFile;
import static org.genepattern.junitutil.Demo.uploadPath;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;

import org.genepattern.server.config.GpConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * jUnit tests for JobInputHelper.initExternalUrl.
 * 
 * @author pcarr
 */
public class TestInitExternalUrl {
    private GpConfig gpConfig;
    private JobInput jobInput;

    /*
     * default test case:
     *     gpConfig.gpUrl is set to gpHref (http://127.0.0.1:8080/gp)
     *     jobInput.baseGpHref is set to proxyHref (https://gpdev.broadinstitute.org/gp))
     *     
     * Expecting to ignore values prefixed with gpHref or proxyHref.
     * Hint: a null result means it's a local path and not an external url.
     */
    
    @Before
    public void before() throws MalformedURLException {
        gpConfig=mock(GpConfig.class);
        when(gpConfig.getGpUrl()).thenReturn(gpUrl);
        jobInput=new JobInput();
        jobInput.setBaseGpHref(proxyHref);
    }

    @Test
    public void gpHref() {
        final String urlSpec=gpHref;
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test
    public void gpUrl() {
        final String urlSpec=gpUrl;
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test
    public void proxyHref() {
        final String urlSpec=proxyUrl;
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test
    public void proxyUrl() {
        final String urlSpec=proxyUrl;
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test
    public void callbackToGpServer_gpUrlPrefix() {
        final String urlSpec=gpHref+jobResultFile();
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }    

    @Test
    public void callbackToGpServer_proxyUrlPrefix() {
        final String urlSpec=proxyHref+jobResultFile();
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    } 
    
    @Test
    public void callbackToGpServer_proxyUrlPrefix_serverFile() {
        // pass in an href to a server file
        final String urlSpec=proxyHref+serverFile();
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test  // not a URL
    public void callbackToGpServer_relativeUriPath() {
        final String urlSpec=jobResultFile();
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }
    
    @Test
    public void serverFile_literalPath() {
        // pass in a server file path, e.g. '/xchip/.../'
        final String urlSpec=localDataDir+"all_aml_test.cls";
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test
    public void localFile() {
        final String urlSpec="file:///xchip/sqa/TestFiles/all_aml_test.cls";
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }
    
    @Test
    public void localWindowsFile() {
        final String urlSpec="file:///C:/xchip/sqa/TestFiles/all_aml_test.cls";
        assertEquals("initExternalUrl('"+urlSpec+"')", null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test
    public void externalFtp() {
        final String urlSpec=dataFtpDir+"all_aml_test.cls";
        assertEquals("initExternalUrl('"+urlSpec+"')", 
                urlSpec, 
                ""+JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }
    
    @Test
    public void externalHttp() {
        final String urlSpec=dataFtpDir+"all_aml_test.cls";
        assertEquals("initExternalUrl('"+urlSpec+"')", 
                urlSpec, 
                ""+JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test
    public void externalGenomeSpace() {
        final String urlSpec=dataGsDir+"all_aml_test.cls";
        assertEquals("initExternalUrl('"+urlSpec+"')", 
                urlSpec, 
                ""+JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }
    
    /*
     * Legacy mode, some code paths may not initialize the JobInput or the JobInput#baseGpHref;
     * In these cases, the proxyUrl prefix is not recognized as a callback and will be
     * incorrectly treated as an external url.
     * 
     * verify that the gpConfig.gpUrl will still match in these cases.
     */
    
    @Test
    public void legacy_nullJobInput_callback_gpUrl() {
        final String urlSpec=gpHref+uploadPath();
        assertEquals("initExternalUrl('"+urlSpec+"')",
                null, // null means callback url
                JobInputHelper.initExternalUrl(gpConfig, null, urlSpec));
    }

    @Test
    public void legacy_nullJobInput_callback_proxyUrl() {
        final String urlSpec=proxyHref+uploadPath();
        assertEquals("initExternalUrl('"+urlSpec+"')",
                urlSpec, // non-null match means ExternalUrl
                ""+JobInputHelper.initExternalUrl(gpConfig, null, urlSpec));
    }

    @Test
    public void legacy_null_baseGpHref_callback_gpUrl() {
        jobInput.setBaseGpHref(null);
        final String urlSpec=gpHref+uploadPath();
        assertEquals("initExternalUrl('"+urlSpec+"')",
                null, // null means callback url
                JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

    @Test
    public void legacy_null_baseGpHref_callback_proxyUrl() {
        jobInput.setBaseGpHref(null);
        final String urlSpec=proxyHref+uploadPath();
        assertEquals("initExternalUrl('"+urlSpec+"')",
                urlSpec, // non-null match means ExternalUrl
                ""+JobInputHelper.initExternalUrl(gpConfig, jobInput, urlSpec));
    }

}

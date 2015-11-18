/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.genepattern.server.config.GpConfig;
import org.junit.Before;
import org.junit.Test;

/**
 * jUnit tests for the the external url methods in the *.job.input.* package.
 * 
 * @author pcarr
 *
 */
public class TestExternalUrl {
    private final String gpUrl="http://genepattern.broadinstitute.org/gp/";
    private JobInput jobInput = null;

    private GpConfig gpConfig;
    private String jobResultUrlFq;
    
    @Before
    public void before() throws MalformedURLException {
        gpConfig=mock(GpConfig.class);
        when(gpConfig.getGpUrl()).thenReturn(gpUrl);
        
        final String relPath="jobResults/8805/stdout.txt";
        jobResultUrlFq=gpUrl;
        if (!gpUrl.endsWith("/")) {
            jobResultUrlFq+="/";
        }
        jobResultUrlFq+=relPath;
    }

    @Test
    public void gpUrl() {
        URL url=JobInputHelper.initExternalUrl(gpConfig, jobInput, gpUrl);
        assertNull("Not an external URL: "+gpUrl, url);
    }
    
    @Test
    public void jobResultUrl() {
        URL url=JobInputHelper.initExternalUrl(gpConfig, jobInput, jobResultUrlFq);
        assertNull("Not an external URL: "+jobResultUrlFq, url);
    }
    
    @Test
    public void externalFtp() {
        final String value="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(gpConfig, jobInput, value);
        assertNotNull(url);
        assertEquals(value, url.toExternalForm());
    }
    
    @Test
    public void externalHttp() {
        final String value="http://www.broadinstitute.org/cancer/software/genepattern/tutorial/linkedFiles/sample.cdt";
        URL url=JobInputHelper.initExternalUrl(gpConfig, jobInput, value);
        assertNotNull(url);
        assertEquals(value, url.toExternalForm());

    }
    
    @Test
    public void serverFilePath() {
        final String value="/xchip/sqa/TestFiles/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(gpConfig, jobInput, value);
        assertNull("Not an external URL: ", url);
    }
    
    @Test
    public void localFile() {
        final String value="file:///xchip/sqa/TestFiles/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(gpConfig, jobInput, value);
        assertNull("Not an external URL: ", url);
    }
    
    @Test
    public void localWindowsFile() {
        final String value="file:///C:/xchip/sqa/TestFiles/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(gpConfig, jobInput, value);
        assertNull("Not an external URL: ", url);
    }

    /**
     * simulate a server deployed in a VM, with default GenePatternURL, accessed from a different host name.
     * @throws MalformedURLException
     */
    @Test
    public void defaultInstall_proxy() throws MalformedURLException {
        // default GenePatternURL
        final String defaultGpUrl="http://127.0.0.1:8080/gp/";
        when(gpConfig.getGpUrl()).thenReturn(defaultGpUrl);

        // accessed at different host and port
        final String proxyGpUrl="https://gp.myhost.com/gp";
        
        JobInput jobInput=new JobInput();
        jobInput.setBaseGpHref(proxyGpUrl);

        assertEquals("no HttpServletRequest, defaultGpUrl", 
                null, 
                JobInputHelper.initExternalUrl(gpConfig, jobInput, defaultGpUrl+"users/test/all_aml/all_aml_test.gct"));
        
        HttpServletRequest request=mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer().append(proxyGpUrl+"/rest/RunTask/addJob"));
        when(request.getRequestURI()).thenReturn("/gp/rest/RunTask/addJob");
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("gp.myhost.com");
        when(request.getServerPort()).thenReturn(443);
        when(request.getContextPath()).thenReturn("/gp");
        when(request.getServletPath()).thenReturn("/rest");
        
        assertEquals("with HttpServletRequest, proxyGpUrl",
                null,
                JobInputHelper.initExternalUrl(gpConfig, jobInput, proxyGpUrl+"/rest/RunTask/addJob"));
    }

}

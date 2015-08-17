/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.net.MalformedURLException;
import java.net.URL;

import org.genepattern.server.config.GpConfig;
import org.junit.Assert;
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
    private GpConfig gpConfig;
    private String jobResultUrlFq;
    
    @Before
    public void before() throws MalformedURLException {
        gpConfig=new GpConfig.Builder()
            .genePatternURL(new URL(gpUrl))
        .build();
        
        final String relPath="jobResults/8805/stdout.txt";
        jobResultUrlFq=gpUrl;
        if (!gpUrl.endsWith("/")) {
            jobResultUrlFq+="/";
        }
        jobResultUrlFq+=relPath;
    }

    @Test
    public void gpUrl() {
        URL url=JobInputHelper.initExternalUrl(gpConfig,gpUrl);
        Assert.assertNull("Not an external URL: "+gpUrl, url);
    }
    
    @Test
    public void jobResultUrl() {
        URL url=JobInputHelper.initExternalUrl(gpConfig,jobResultUrlFq);
        Assert.assertNull("Not an external URL: "+jobResultUrlFq, url);
    }
    
    @Test
    public void externalFtp() {
        final String value="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(gpConfig,value);
        Assert.assertNotNull(url);
        Assert.assertEquals(value, url.toExternalForm());
    }
    
    @Test
    public void externalHttp() {
        final String value="http://www.broadinstitute.org/cancer/software/genepattern/tutorial/linkedFiles/sample.cdt";
        URL url=JobInputHelper.initExternalUrl(gpConfig,value);
        Assert.assertNotNull(url);
        Assert.assertEquals(value, url.toExternalForm());

    }
    
    @Test
    public void serverFilePath() {
        final String value="/xchip/sqa/TestFiles/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(gpConfig,value);
        Assert.assertNull("Not an external URL: ", url);
    }
    
    @Test
    public void localFile() {
        final String value="file:///xchip/sqa/TestFiles/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(gpConfig,value);
        Assert.assertNull("Not an external URL: ", url);
    }
    
    @Test
    public void localWindowsFile() {
        final String value="file:///C:/xchip/sqa/TestFiles/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(gpConfig,value);
        Assert.assertNull("Not an external URL: ", url);
    }

}

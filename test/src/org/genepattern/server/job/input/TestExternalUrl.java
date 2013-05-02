package org.genepattern.server.job.input;

import java.net.URL;

import org.junit.After;
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
    private String gpUrl;    
    private String jobResultUrlFq;
    
    @Before
    public void before() {
        if (System.getProperty("GenePatternURL")==null) {
            System.setProperty("GenePatternURL", "http://genepattern.broadinstitute.org/gp/");
        }
        this.gpUrl = System.getProperty("GenePatternURL", "http://genepattern.broadinstitute.org/gp/");
        
        final String relPath="jobResults/8805/stdout.txt";
        jobResultUrlFq=gpUrl;
        if (!gpUrl.endsWith("/")) {
            jobResultUrlFq+="/";
        }
        jobResultUrlFq+=relPath;
    }

    @After
    public void after() {
    }
    
    @Test
    public void gpUrl() {
        URL url=JobInputHelper.initExternalUrl(gpUrl);
        Assert.assertNull("Not an external URL: "+gpUrl, url);
    }
    
    @Test
    public void jobResultUrl() {
        URL url=JobInputHelper.initExternalUrl(jobResultUrlFq);
        Assert.assertNull("Not an external URL: "+jobResultUrlFq, url);
    }
    
    @Test
    public void externalFtp() {
        final String value="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(value);
        Assert.assertNotNull(url);
        Assert.assertEquals(value, url.toExternalForm());
    }
    
    @Test
    public void externalHttp() {
        final String value="http://www.broadinstitute.org/cancer/software/genepattern/tutorial/linkedFiles/sample.cdt";
        URL url=JobInputHelper.initExternalUrl(value);
        Assert.assertNotNull(url);
        Assert.assertEquals(value, url.toExternalForm());

    }
    
    @Test
    public void serverFilePath() {
        final String value="/xchip/sqa/TestFiles/all_aml_test.cls";
        URL url=JobInputHelper.initExternalUrl(value);
        Assert.assertNull("Not an external URL: ", url);
    }

}

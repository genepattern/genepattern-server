package org.genepattern.server.dm;

import static org.genepattern.server.dm.TestUrlUtil_isLocalHost.assertIsLocalHost;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import org.genepattern.junitutil.Demo;
import org.genepattern.junitutil.MockInetUtil;
import org.genepattern.server.config.GpConfig;
import org.junit.Before;
import org.junit.Test;

public class TestUrlUtil_isLocalHost_corner_cases {
    private GpConfig gpConfig;
    private GpConfig proxyConfig;
    
    @Before
    public void setUp() throws UnknownHostException, MalformedURLException {
        gpConfig=Demo.gpConfig();
        proxyConfig=Demo.gpConfig();
        when(proxyConfig.getGenePatternURL()).thenReturn(new URL(Demo.proxyUrl));
    }
    
    @Test
    public void nullUrl() {
        final URI uri=null;
        assertEquals("url==null", false, 
                UrlUtil.isLocalHost(gpConfig, Demo.gpHref, MockInetUtil.instance(), uri));
    }

    @Test
    public void emptyUrlHost() {
        final URI uri=new File("Test.txt").toURI();
        assertEquals("url.host is empty", false,
                UrlUtil.isLocalHost(gpConfig, Demo.gpHref, MockInetUtil.instance(), uri));
    }
    
    @Test
    public void nullBaseHref_externalHttpFile() throws URISyntaxException {
        String baseGpHref=null;
        assertEquals("external http file", false, 
                UrlUtil.isLocalHost(gpConfig, baseGpHref, MockInetUtil.instance(), new URI(Demo.dataHttpDir+"all_aml_test.gct")));
    }

    @Test
    public void nullBaseHref_proxy() throws URISyntaxException {
        String baseGpHref=null;
        assertEquals("local file", true, 
                UrlUtil.isLocalHost(proxyConfig, baseGpHref, MockInetUtil.instance(), new URI(Demo.proxyHref+Demo.uploadPath())));
    }

    @Test
    public void nullBaseHref_proxy_upperCase() {
        assertIsLocalHost(true, proxyConfig, null, MockInetUtil.instance(), 
                "https://"+Demo.proxyHost.toUpperCase()+Demo.gpPath+ Demo.uploadPath());
    }

    @Test
    public void nullBaseHref_proxy_upperCase_gpUrl() throws MalformedURLException {
        GpConfig customConfig=Demo.gpConfig();
        when(customConfig.getGenePatternURL()).thenReturn(new URL("https://"+Demo.proxyHost.toUpperCase()+Demo.gpPath+"/"));
        assertIsLocalHost(true, proxyConfig, null, MockInetUtil.instance(), 
                Demo.proxyHref + Demo.uploadPath());
    }

}

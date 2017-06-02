package org.genepattern.server.dm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.genepattern.junitutil.Demo;
import org.genepattern.junitutil.HostnameUtil;
import org.genepattern.server.config.GpConfig;
import org.junit.Before;
import org.junit.Test;

public class TestUrlUtil_substitution {
    private GpConfig gpConfig;
    
    @Before
    public void setUp() {
        gpConfig=Demo.gpConfig();
    }

    protected void assertReplaceGpUrl(final String baseGpHref, final String value) {
        final String expected="<GenePatternURL>" + ( Demo.uploadPath().substring(1) );
        assertReplaceGpUrl(expected, baseGpHref, value);
    }
    
    protected void assertReplaceGpUrl(final String expected, final String baseGpHref, final String value) {
        assertEquals(expected,
                UrlUtil.replaceGpUrl(gpConfig, 
                        baseGpHref, 
                        value));
    }

    @Test
    public void local_hostnames_all() throws UnknownHostException {
        for(final String baseHostname : HostnameUtil.instance().localHostnames) {
            final String baseGpHref="http://"+baseHostname+"/gp";
            for(final String requestHostname : HostnameUtil.instance().localHostnames) {
                assertReplaceGpUrl(baseGpHref, "http://"+requestHostname+":8080/gp" + Demo.uploadPath());
            }
        }
    }
    
    //special-case: baseGpHref is null; 
    @Test
    public void local_hostnames_all_baseGpHrefNull() throws UnknownHostException {
        final String baseGpHref=null;//"http://"+baseHostname+"/gp";
        for(final String requestHostname : HostnameUtil.instance().localHostnames) {
            assertReplaceGpUrl(baseGpHref, "http://"+requestHostname+":8080/gp" + Demo.uploadPath());
        }
    }
    
    @Test
    public void proxyRequest() {
        assertReplaceGpUrl(Demo.proxyHref, Demo.proxyHref + Demo.uploadPath());
    }

    @Test
    public void proxyRequest_baseGpHrefNull() throws MalformedURLException {
        // requires custom genepattern.properties
        when(gpConfig.getGenePatternURL()).thenReturn(new URL(Demo.proxyUrl));
        assertReplaceGpUrl(null, Demo.proxyHref + Demo.uploadPath());
    }

    @Test
    public void externalHttpFile_proxyRequest() {
        final String external=Demo.dataHttpDir + "all_aml_test.gct";
        assertReplaceGpUrl(external, Demo.gpHref, external);
    }

    @Test
    public void externalFtpFile_proxyRequest() {
        final String external=Demo.dataFtpDir + "all_aml_test.gct";
        assertReplaceGpUrl(external, Demo.gpHref, external);
    }

    @Test
    public void loopback() { 
        assertReplaceGpUrl(Demo.gpHref, "http://127.0.0.1:8080/gp" + Demo.uploadPath());
    }

    @Test
    public void localhost() { 
        assertReplaceGpUrl(Demo.gpHref, "http://localhost:8080/gp" + Demo.uploadPath());
    } 

    @Test
    public void computerName() throws UnknownHostException { 
        final String computerName=HostnameUtil.getComputerName();
        assertReplaceGpUrl(Demo.gpHref, "http://"+computerName+":8080/gp" + Demo.uploadPath());
    } 

    @Test
    public void computerAddress() throws UnknownHostException { 
        final String computerAddress=HostnameUtil.getComputerAddress();
        assertReplaceGpUrl(Demo.gpHref, "http://"+computerAddress+":8080/gp" + Demo.uploadPath());
    } 

}

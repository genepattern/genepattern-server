package org.genepattern.server.dm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Set;

import org.genepattern.junitutil.Demo;
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
        final Set<String> hostnames=TestUrlUtil_isLocalHost.getLocalHostnames();
        for(final String baseHostname : hostnames) {
            final String baseGpHref="http://"+baseHostname+"/gp";
            for(final String requestHostname : hostnames) {
                assertReplaceGpUrl(baseGpHref, "http://"+requestHostname+":8080/gp" + Demo.uploadPath());
            }
        }
    }
    
    //special-case: baseGpHref is null; 
    @Test
    public void local_hostnames_all_baseGpHrefNull() throws UnknownHostException {
        final Set<String> hostnames=TestUrlUtil_isLocalHost.getLocalHostnames();
        final String baseGpHref=null;//"http://"+baseHostname+"/gp";
        for(final String requestHostname : hostnames) {
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
        final InetAddress localAddr=InetAddress.getLocalHost();
        final String hostname=localAddr.getHostName(); 
        assertReplaceGpUrl(Demo.gpHref, "http://"+hostname+":8080/gp" + Demo.uploadPath());
    } 

    @Test
    public void computerAddress() throws UnknownHostException { 
        assertReplaceGpUrl(Demo.gpHref, "http://"+InetAddress.getLocalHost().getHostAddress()+":8080/gp" + Demo.uploadPath());
    } 

}

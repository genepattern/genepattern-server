package org.genepattern.server.dm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.genepattern.junitutil.Demo;
import org.genepattern.junitutil.MockInetUtil;
import org.genepattern.server.config.GpConfig;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestUrlUtil_isLocalHost {
    private GpConfig gpConfig;
    
    @Before
    public void setUp() {
        gpConfig=Demo.gpConfig();
    }

    protected void assertIsLocalHost(final boolean expected, final String urlSpec) {
        assertIsLocalHost(expected, InetUtilDefault.instance(), urlSpec);
    }

    protected void assertIsLocalHost(final boolean expected, final InetUtil inetUtil, final String urlSpec) {
        URL url=null;
        try {
            url=new URL(urlSpec);
        }
        catch (MalformedURLException e) {
            fail(e.getLocalizedMessage());
        }
        assertEquals("isLocalHost('"+urlSpec+"')", 
                expected, 
                UrlUtil.isLocalHost(gpConfig, inetUtil, url));
    }
    
    @Test
    public void localhost() {
        assertIsLocalHost(true, "http://localhost:8080/gp" + Demo.uploadPath());
    }

    @Test
    public void localhost_noPort() {
        assertIsLocalHost(true, "http://localhost/gp" + Demo.uploadPath());
        
    }

    @Test
    public void localhost_upperCase() {
        assertIsLocalHost(true, "http://LOCALHOST/gp" + Demo.uploadPath());
        
    }

    /** aka 127.0.0.1 */
    @Test
    public void loopback() {
        assertIsLocalHost(true, "http://127.0.0.1:8080/gp" + Demo.uploadPath());
    }

    @Test
    public void loopback_noPort() {
        assertIsLocalHost(true, "http://127.0.0.1/gp" + Demo.uploadPath());
    }
    
    /** 
     * proxyUrl requires GenePatternURL to be set in genepattern.properties 
     */
    @Test
    public void proxyRequest() throws MalformedURLException {
        // mock config of Broad hosted GP server; InetUtil will not recognize the proxyUrl as a local.
        MockInetUtil mock=new MockInetUtil();
        mock.add(Demo.proxyHost, "34.173.100.22", false, false, null);
        when(gpConfig.getGenePatternURL()).thenReturn(new URL(Demo.proxyUrl));
        assertIsLocalHost(true, mock, Demo.proxyHref + Demo.uploadPath());
    }

    @Test
    public void proxyRequest_upperCase() throws MalformedURLException {
        MockInetUtil mock=new MockInetUtil();
        mock.add(Demo.proxyHost, "34.173.100.22", false, false, null);
        when(gpConfig.getGenePatternURL()).thenReturn(new URL(Demo.proxyUrl));
        assertIsLocalHost(true, mock, "https://GPDEV.BROADINSTITUTE.ORG/gp" + Demo.uploadPath());
    }

    @Test
    public void proxyRequest_gpUrl_upperCase() throws MalformedURLException {
        MockInetUtil mock=new MockInetUtil();
        mock.add(Demo.proxyHost, "34.173.100.22", false, false, null);
        when(gpConfig.getGenePatternURL()).thenReturn(new URL("https://GPDEV.BROADINSTITUTE.ORG/gp/"));
        assertIsLocalHost(true, mock, Demo.proxyHref + Demo.uploadPath());
    }

    //TODO: implement fix for this
    @Ignore @Test
    public void proxy_genePatternUrl_notSet() throws MalformedURLException {
        MockInetUtil mock=new MockInetUtil();
        mock.add(Demo.proxyHost, "34.173.100.22", false, false, null);
        assertIsLocalHost(true, mock, Demo.proxyHref + Demo.uploadPath());
    }

    @Test
    public void externalHttpFile() {
        assertIsLocalHost(false, Demo.dataHttpDir+"all_aml_test.gct");
    }
    
    @Test
    public void externalHttpDir() {
        assertIsLocalHost(false, Demo.dataHttpDir);
    }
    

    @Test
    public void externalFtpFile() {
        assertIsLocalHost(false, Demo.dataFtpDir+"all_aml_test.gct");
    }

    @Test
    public void externalFtpDir() {
        assertIsLocalHost(false, Demo.dataFtpDir);
    }

    /**
     * url request matches custom GenePatternURL in genepattern.properties:
     *     GenePatternURL=http://{hostname}:8080/gp/
     */
    @Test
    public void customGenePatternURL_hostname() throws Exception {
        final String hostname=InetAddress.getLocalHost().getHostName();
        final String gpHref="http://"+hostname+":8080/gp";
        final URL genePatternURL=new URL(gpHref+"/");
        
        when(gpConfig.getGenePatternURL()).thenReturn(genePatternURL);
        assertIsLocalHost(true, gpHref + Demo.uploadPath());
    }

    /**
     * url request matches custom GenePatternURL in genepattern.properties:
     *     GenePatternURL=http://{hostname}.local:8080/gp/
     */
    @Test
    public void customGenePatternURL_hostname_local() throws Exception {
        final String hostname=InetAddress.getLocalHost().getHostName()+".local";
        final String gpHref="http://"+hostname+":8080/gp";
        final URL genePatternURL=new URL(gpHref+"/");
        
        when(gpConfig.getGenePatternURL()).thenReturn(genePatternURL);
        assertIsLocalHost(true, gpHref + Demo.uploadPath());
    }

    /**
     * url request matches custom GenePatternURL in genepattern.properties:
     *     GenePatternURL=http://{HOSTNAME}.local:8080/gp/
     */
    @Test
    public void customGenePatternURL_hostname_local_upperCase() throws Exception {
        final String hostname=InetAddress.getLocalHost().getHostName().toUpperCase()+".local";
        final String gpHref="http://"+hostname+":8080/gp";
        final URL genePatternURL=new URL(gpHref+"/");
        
        when(gpConfig.getGenePatternURL()).thenReturn(genePatternURL);
        assertIsLocalHost(true, gpHref + Demo.uploadPath());
    }

    // test-cases where the genePatternURL does not exactly match the requested url
    @Test
    public void hostname() throws UnknownHostException {
        final String hostname=InetAddress.getLocalHost().getHostName();
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(true, requestedValue);
    }

    @Test
    public void hostname_local() throws UnknownHostException {
        final String hostname=InetAddress.getLocalHost().getHostName()+".local";
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(true, requestedValue);
    }

    @Test
    public void hostname_local_case_01() {
        final String hostname="test-user.local";
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(true, MockInetUtil.instance(), requestedValue);
    }

    @Test
    public void hostname_local_case_02() {
        final String hostname="test-user.mydomain.org";
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(true, MockInetUtil.instance(), requestedValue);
    }

    @Test
    public void hostname_local_case_03() {
        final String hostname="test-user";
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(true, MockInetUtil.instance(), requestedValue);
    }
    
    @Test
    public void host_ip_local_case_04() {
        final String hostname=MockInetUtil.PRIVATE_IP;
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(true, MockInetUtil.instance(), requestedValue);
    } 
    
    @Test
    public void host_ip_local_no_match_case_05() {
        final String hostname=MockInetUtil.PRIVATE_IP_OTHER;
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(false, MockInetUtil.instance(), requestedValue);
    }

}

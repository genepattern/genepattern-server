package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.config.GpConfig;
import org.junit.Before;
import org.junit.Test;

public class TestGPATisLocalHost {
    GpConfig gpConfig;
    
    @Before
    public void setUp() {
        gpConfig=Demo.gpConfig();
    }
    
    protected void assertIsLocalHost(final boolean expected, final String urlSpec) {
        URL url=null;
        try {
            url=new URL(urlSpec);
        }
        catch (MalformedURLException e) {
            fail(e.getLocalizedMessage());
        }
        assertEquals("isLocalHost('"+urlSpec+"')", 
                expected, 
                GenePatternAnalysisTask.isLocalHost(gpConfig, url));
    }
    
    @Test
    public void localhost() {
        assertIsLocalHost(true, "http://localhost:8080/gp" + Demo.uploadPath());
    }

    @Test
    public void localhost_noPort() {
        assertIsLocalHost(true, "http://localhost/gp" + Demo.uploadPath());
        
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
     * When GpConfig.genePatternURL is set; 
     * use InetAddress.localHost.hostName to set up the test.
     * Note, requires properly configured '/etc/hosts' file. On my MacOS X dev machine:
     * <pre>
127.0.0.1       localhost       gm28f-571
     * </pre>
     */
    @Test
    public void customGenePatternURL_hostname() throws Exception {
        final String hostname=InetAddress.getLocalHost().getHostName();
        final String gpHref="http://"+hostname+":8080/gp";
        final URL genePatternURL=new URL(gpHref+"/");
        
        when(gpConfig.getGenePatternURL()).thenReturn(genePatternURL);
        assertIsLocalHost(true, gpHref + Demo.uploadPath());
    }

    @Test
    public void customGenePatternURL_hostname_local() throws Exception {
        final String hostname=InetAddress.getLocalHost().getHostName()+".local";
        final String gpHref="http://"+hostname+":8080/gp";
        final URL genePatternURL=new URL(gpHref+"/");
        
        when(gpConfig.getGenePatternURL()).thenReturn(genePatternURL);
        assertIsLocalHost(true, gpHref + Demo.uploadPath());
    }

    @Test
    public void customGenePatternURL_hostname_local_upperCase() throws Exception {
        final String hostname=InetAddress.getLocalHost().getHostName().toUpperCase()+".local";
        final String gpHref="http://"+hostname+":8080/gp";
        final URL genePatternURL=new URL(gpHref+"/");
        
        when(gpConfig.getGenePatternURL()).thenReturn(genePatternURL);
        assertIsLocalHost(true, gpHref + Demo.uploadPath());
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

    // testing different cases where the gpurl does not exactly match the requested url
    @Test
    public void hostname() throws UnknownHostException {
        final String hostname=InetAddress.getLocalHost().getHostName();
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(true, requestedValue);
    }

    //TODO: @Ignore @Test
    public void hostname_local() throws UnknownHostException {
        final String hostname=InetAddress.getLocalHost().getHostName()+".local";
        final String requestedValue="http://"+hostname+":8080/gp"+Demo.uploadPath();
        assertIsLocalHost(true, requestedValue);
    }

    //TODO: @Ignore @Test
    public void canonical() throws Exception {
        //final String hostname="pcarr-test.mydomain.org";
        final String hostname="pcarr-test";
        final String gpHref="http://"+hostname+":8080/gp";
        //final URL genePatternURL=new URL(gpHref+"/");
        
        //when(gpConfig.getGenePatternURL()).thenReturn(genePatternURL);
        assertIsLocalHost(true, gpHref + Demo.uploadPath());
    }

}

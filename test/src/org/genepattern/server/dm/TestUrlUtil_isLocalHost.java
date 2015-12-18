package org.genepattern.server.dm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.config.GpConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.net.InetAddresses;

/**
 * test-cases for UrlUtil#isLocalHost; 
 * 
 * (1) default GenePatternURL=http://127.0.0.1:8080/gp/
 *    baseHref set to 'localhost'
 *    baseHref set to '127.0.0.1'
 *    baseHref set to '{local-hostName}', as determined by InetAddress#localHost#hostName
 *    baseHref set to '{local-hostAddress}', as determined by InetAddress#localHost#hostAddress
 *    baseHref set to '{local-network-name}', as set by DHCP server
 *    baseHref set to '{local-network-ip-address}', as set by DHCP server
 *    baseHref set to '{proxy-network-name}', as set by proxy server
 *    baseHref set to '{proxy-network-ip-address}', as set by proxy server
 *
 * (2) proxy server, e.g. GenePatternURL=https://gpdev.broadinstitute.org/gp/
 *     
 * 
 * @author pcarr
 *
 */
@RunWith(Parameterized.class)
public class TestUrlUtil_isLocalHost {
    
    /**
     * Utility method to get my hosts; a set of host names and addresses for the machine on which this
     * process is running.
     * 
     * @return
     * @throws UnknownHostException
     */
    protected static Set<String> getLocalHostnames() throws UnknownHostException {
        final Set<String> hostnames=new LinkedHashSet<String>();
        hostnames.add(InetAddress.getLoopbackAddress().getHostName());
        hostnames.add(InetAddress.getLoopbackAddress().getHostAddress());
        hostnames.add(InetAddress.getLocalHost().getHostName());
        hostnames.add(InetAddress.getLocalHost().getHostAddress());
        for(final InetAddress addr : getComputerAddresses()) {
            final String uriStr=InetAddresses.toUriString(addr);
            hostnames.add(uriStr);
        }
        return hostnames;
    }
    
    /**
     * Utility method to get my IP addresses, those of the machine on which this process is running. 
     * @return a list of InetAddress (IPv4 and IPv6)
     */
    protected static List<InetAddress> getComputerAddresses() {
        final List<InetAddress> rval=new ArrayList<InetAddress>();
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    rval.add(addr);
                }
            }
        } 
        catch (SocketException e) {
            fail("error getting computer ip addresses: "+e.getLocalizedMessage());
        }
        return rval;
    }
    
    /**
     * Parameterize test-cases for baseGpHref as set in the JobInput object.
     * Test each variant of the local host address, 
     *     e.g. localhost, 127.0.0.1, et cetera
     * And the demo proxy host (to simulate custom GenePatternURL).
     * 
     * @return
     * @throws UnknownHostException
     */
    @Parameters(name="hostname={0}")
    public static Collection<Object[]> data() throws UnknownHostException {
        final Set<String> hostnames=new LinkedHashSet<String>();
        hostnames.addAll(getLocalHostnames());
        // special-case: case-sensitive
        hostnames.add("LOCALHOST");
        // special-case: proxy host
        hostnames.add( Demo.proxyHost );
        // special-case: baseGpHref can be null
        hostnames.add( null );

        final List<Object[]> tests=new ArrayList<Object[]>();
        for(final String hostname : hostnames) {
            tests.add(new Object[]{ hostname });
        }
        return tests;
    }
    
    private static final GpConfig gpConfig=Demo.gpConfig();
    private static GpConfig proxyConfig;
    private static final InetUtil inetUtil=InetUtilDefault.instance();

    @BeforeClass
    public static void beforeClass() throws MalformedURLException {
        proxyConfig=Demo.gpConfig();
        when(proxyConfig.getGenePatternURL()).thenReturn(new URL(Demo.proxyUrl));
        when(proxyConfig.toString()).thenReturn("proxy");
    }
 
    /** the hostname of the baseGpHref */
    public String hostname;
    private String baseGpHref;
    
    public TestUrlUtil_isLocalHost(String hostname) {
        this.hostname=hostname;
        if (hostname != null) {
            this.baseGpHref="http://"+hostname+":8080/gp";
        }
        else {
            this.baseGpHref=null;
        }
    }

    protected static void assertIsLocalHost(final boolean expected, final GpConfig gpConfig, final String baseGpHref, InetUtil inetUtil, final String urlSpec) {
        URI uri=null;
        try {
            uri=new URI(urlSpec);
        }
        catch (URISyntaxException e) {
            fail(e.getLocalizedMessage());
        }
        assertEquals("isLocalHost('"+urlSpec+"')", 
                expected, 
                UrlUtil.isLocalHost(gpConfig, baseGpHref, inetUtil, uri));
    }

    @Test
    public void localhost() {
        assertIsLocalHost(true, gpConfig, baseGpHref, inetUtil, "http://localhost:8080/gp" + Demo.uploadPath());
    }

    @Test
    public void localhost_upperCase() {
        assertIsLocalHost(true, gpConfig, baseGpHref, inetUtil, "http://LOCALHOST:8080/gp" + Demo.uploadPath());
    }

    @Test
    public void localhost_proxyConfig() {
        assertIsLocalHost(true, proxyConfig, baseGpHref, inetUtil, "http://localhost:8080/gp" + Demo.uploadPath());
    }
    
    @Test
    public void loopback() {
        assertIsLocalHost(true, gpConfig, baseGpHref, inetUtil, "http://127.0.0.1:8080/gp" + Demo.uploadPath());
    }
    
    @Test
    public void loopback_proxyConfig() {
        assertIsLocalHost(true, proxyConfig, baseGpHref, inetUtil, "http://127.0.0.1:8080/gp" + Demo.uploadPath());
    }

    @Test
    public void computerName() throws UnknownHostException {
        assertIsLocalHost(true, gpConfig, baseGpHref, inetUtil, "http://" + 
                InetAddress.getLocalHost().getHostName() + ":8080/gp" + Demo.uploadPath());
    }

    @Test
    public void computerName_proxyConfig() throws UnknownHostException {
        assertIsLocalHost(true, proxyConfig, baseGpHref, inetUtil, "http://" + 
                InetAddress.getLocalHost().getHostName() + ":8080/gp" + Demo.uploadPath());
    }

    @Test
    public void computerAddress() throws UnknownHostException {
        assertIsLocalHost(true, gpConfig, baseGpHref, inetUtil, "http://" + 
                InetAddress.getLocalHost().getHostAddress() + ":8080/gp" + Demo.uploadPath());
    }

    @Test
    public void computerAddress_proxyConfig() throws UnknownHostException {
        assertIsLocalHost(true, proxyConfig, baseGpHref, inetUtil, "http://" + 
                InetAddress.getLocalHost().getHostAddress() + ":8080/gp" + Demo.uploadPath());
    }

    @Test
    public void proxyRequest_proxyConfig() {
        assertIsLocalHost(true, proxyConfig, baseGpHref, inetUtil, Demo.proxyHref + Demo.uploadPath());
    }
    
    @Test
    public void externalHttpFile() {
        assertIsLocalHost(false, gpConfig, baseGpHref, inetUtil, Demo.dataHttpDir+"all_aml_test.gct");
    }

    @Test
    public void externalHttpFile_proxyConfig() {
        assertIsLocalHost(false, proxyConfig, baseGpHref, inetUtil, Demo.dataHttpDir+"all_aml_test.gct");
    }

    @Test
    public void externalHttpDir() {
        assertIsLocalHost(false, gpConfig, baseGpHref, inetUtil, Demo.dataHttpDir);
    }
    
    @Test
    public void externalFtpFile() {
        assertIsLocalHost(false, gpConfig, baseGpHref, inetUtil, Demo.dataFtpDir+"all_aml_test.gct");
    }

    @Test
    public void externalFtpDir() {
        assertIsLocalHost(false, gpConfig, baseGpHref, inetUtil, Demo.dataFtpDir);
    }

    @Test
    public void resolveBaseUrl() {
        if (baseGpHref != null) {
            final String urlSpec=baseGpHref + Demo.uploadPath();
            assertEquals(baseGpHref, UrlUtil.resolveBaseUrl(urlSpec, Demo.gpPath));
        }
    }


}

package org.genepattern.junitutil;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.server.dm.InetUtil;
import org.genepattern.server.dm.UrlUtil;

import com.google.common.net.InetAddresses;

/**
 * Helper class for mocking system calls to InetAddress and NetworkInterface;
 * Created for testing {@link UrlUtil#isLocalHost(org.genepattern.server.config.GpConfig, InetUtil, java.net.URL)}
 * @author pcarr
 *
 */
public class MockInetUtil implements InetUtil {
    public static InetUtil instance() {
        return Singleton.INSTANCE;
    }
    
    /** 
     * mock the computer name which is mapped to loopback address, e.g.
     *     127.0.0.1    localhost    {loopback_host_01}    {loopback_host_02}
     */
    public static final String COMPUTER_NAME="gm28f-571";
    
    /** 
     * mock private network IP address for this host, visible on the local network.
     * this could be an address assigned by a DHCP server.
     */
    public static final String PRIVATE_IP="10.1.3.17";
    
    /** 
     * mock private network hostname, visible on the local network.
     * this could be a hostname assigned by a DHCP server.
     */
    public static final String PRIVATE_HOST="wm97e-54f";

    /**
     * mock private IP address assigned by the DHCP server to a different host.
     */
    public static final String PRIVATE_IP_OTHER="10.1.3.18";

    /**
     * mock hostname assigned by the DHCP server to a different host.
     */
    public static final String PRIVATE_HOST_OTHER="wm0c7-ece";
    
    /**
     * Create a new instance, initialized with defaults based roughly on 
     * a MacBook hosted GP server connected to a private network.
     * The names have been changed to protect the innocent.
     * 
     * Example loopback address, loopBack=true, nic="lo0"
     *     GET http://127.0.0.1:8080/gp/
     *     
     * Example local address
     *     GET http://{private-host}:8080/, loopBack=false, nic="en1"
     *     
     * Example external address
     *     ftp://ftp.broadinstitute.org/, loopBack=false, nic=null
     * 
     * @return
     */
    public static MockInetUtil initDefault() {
        MockInetUtil mock=new MockInetUtil();
        // http://localhost:8080/... and http://127.0.0.1:8080/gp/...
        mock.add("localhost",       "127.0.0.1", false, true, "lo0");
        // http://{computer-name}:8080/...
        mock.add(COMPUTER_NAME,     "127.0.0.1", false, true, "lo0");
        // http://test-user.local:8080/...
        mock.add("test-user.local", "127.0.0.1", false, true, "lo0");

        // http://{computer-name}.local:8080/...
        mock.add(COMPUTER_NAME+".local",             PRIVATE_IP, false, false, "en1");
        // http://{private-host}:8080/...
        mock.add(PRIVATE_HOST,                       PRIVATE_IP, false, false, "en1");
        // http://{private-host}.broadinstitute.org:8080/...
        mock.add(PRIVATE_HOST+".broadinstitute.org", PRIVATE_IP, false, false, "en1");
        // http://test-user:8080/gp/...
        mock.add("test-user",                        PRIVATE_IP, false, false, "en1");
        // http://test-user.mydomain.org:8080/...
        mock.add("test-user.mydomain.org",           PRIVATE_IP, false, false, "en1");
        // http://{private-id}:8080/gp/...
        mock.add(PRIVATE_HOST+".broadinstitute.org", PRIVATE_IP, false, false, "en1");

        // Mock external addresses 
        // http://{private-ip-other}:8080/gp/...
        mock.add(PRIVATE_HOST_OTHER+".broadinstitute.org", PRIVATE_IP_OTHER, false, false, null);
        // http://www.broadinstitute.org/...
        mock.add("www.broadinstitute.org", "69.173.64.101", false, false, null);
        // ftp://ftp.broadinstitute.org/...
        mock.add("ftp.broadinstitute.org", "69.173.80.251", false, false, null);
        // https://dm.genomespace.org/...
        mock.add("dm.genomespace.org", "23.23.112.114", false, false, null);
        
        return mock;
    }

    protected static void printAddr(final String urlSpec) {
        URL url;
        try {
            url = new URL(urlSpec);
            InetAddress addr=InetAddress.getByName(url.getHost());
            final NetworkInterface ni=NetworkInterface.getByInetAddress(addr);
            printAddr(urlSpec, addr, ni);
        }
        catch (MalformedURLException e) {
            System.out.println("// "+urlSpec);
            System.out.println("//    MalformedURLException: "+e.getLocalizedMessage());
        }
        catch (UnknownHostException e) {
            System.out.println("// "+urlSpec);
            System.out.println("    UnknownHostException: "+e.getLocalizedMessage());
        }
        catch (SocketException e) {
            System.out.println("// "+urlSpec);
            System.out.println("    SocketException: "+e.getLocalizedMessage());
        }
        catch (Throwable t) {
            System.out.println("// "+urlSpec);
            System.out.println("    Error: "+t.getLocalizedMessage());
        }
    }
    
    /**
     * for debugging only, helper class which generated the source code for the initDefault function.
     * @param urlSpec
     * @param addr
     * @param ni
     */
    protected static void printAddr(String urlSpec, InetAddress addr, NetworkInterface ni) {
        System.out.println("// "+urlSpec);
        String nicId = ni == null ? "null" :
            "\""+ni.getDisplayName()+"\"";
        System.out.println("\tmock.add(\""+addr.getHostName()+"\", \""+addr.getHostAddress()+"\", "
                +addr.isAnyLocalAddress()+", "
                +addr.isLoopbackAddress()+", "+nicId+");"
        );
    }
    
    /**
     * for debugging only, helper class which generates source code for the initDefault function.
     * Works with these /etc/hosts settings:
     * <pre>
127.0.0.1       localhost       {mock-hostname}       test-user.local
10.1.3.17       test-user.mydomain.org test-user
     * </pre>
     * 
     * @throws UnknownHostException
     * @throws MalformedURLException
     * @throws SocketException
     */
    public static void printAddresses() { //throws UnknownHostException, MalformedURLException, SocketException {
        // quick test 
        final String[] hostnames={
                "localhost",
                COMPUTER_NAME,
                "test-user.local", // another loopback hostname
                COMPUTER_NAME+".local",
                PRIVATE_HOST,
                PRIVATE_HOST+".broadinstitute.org",
                "test-user", // another private network hostname
                "test-user.mydomain.org",  // another private network hostname
                "127.0.0.1",
                PRIVATE_IP, // private IP, this host
                PRIVATE_IP_OTHER, // private IP, different host
        };
        for(final String hostname : hostnames) {
            final String urlSpec="http://"+hostname+":8080/gp"+Demo.uploadPath();
            printAddr(urlSpec);
        }
        
        // special-case, httpDir
        printAddr(Demo.dataHttpDir);
        
        // special-case, ftpDir
        printAddr(Demo.dataFtpDir);
        
        // special-case, gsDir
        printAddr(Demo.dataGsDir);
    }

    // lazy-init 
    private static class Singleton {
        private static final InetUtil INSTANCE=initDefault();
    }
    
    private Map<String, InetAddress> inetAddrs;
    private Map<String, Object> nicsByNicName;
    private Map<String, Object> nics;
    
    public MockInetUtil() {
        inetAddrs=new HashMap<String, InetAddress>();
        nicsByNicName=new HashMap<String, Object>();
        nics=new HashMap<String, Object>();
    }
    
    public void add(final String hostName, final String hostAddress, boolean anyLocal, boolean loopBack, String nicName) {
        boolean hasNic=nicName != null;
        
        InetAddress addr=mock(InetAddress.class);
        when(addr.toString()).thenReturn(hostName+"/"+hostAddress);
        when(addr.getHostName()).thenReturn(hostName);
        when(addr.getHostAddress()).thenReturn(hostAddress);
        when(addr.isAnyLocalAddress()).thenReturn(anyLocal);
        when(addr.isLoopbackAddress()).thenReturn(loopBack);
        inetAddrs.put(hostName, addr);
        
        if (hasNic) {
            Object nic=nicsByNicName.get(nicName);
            if (nic==null) {
                nic=nicName;
                nicsByNicName.put(nicName,nic);
            }
            nics.put(hostName, nic);
        }
    }

    @Override
    public InetAddress getByName(final String host) throws UnknownHostException {
        boolean is=InetAddresses.isInetAddress(host);
        if (is) {
            InetAddress addr=InetAddresses.forString(host);
            return inetAddrs.get(addr.getHostName());
        }
        return inetAddrs.get(host);
    }

    @Override
    public Object getByInetAddress(InetAddress addr) throws SocketException {
        if (addr==null) {
            return null;
        }
        if (addr.getHostName() == null) {
            return null;
        }
        return nics.get(addr.getHostName());
    }
    
    /**
     * generate source code template for creating a new mock instance;
     * for testing/debugging only.
     * @param args
     */
    public static void main(final String[] args) {
        // for testing only
        try {
            printAddresses();
        }
        catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }
}
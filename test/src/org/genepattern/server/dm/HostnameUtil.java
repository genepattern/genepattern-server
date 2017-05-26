package org.genepattern.server.dm;

import static org.junit.Assert.fail;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.net.InetAddresses;

/**
 * Utility class, for junit tests, to get my hosts; a set of host names and addresses 
 * for the machine on which this process is running.
 * 
 * For Reference
 *   "TEST-NET-1" (example computer name)
 *   "192.0.2.0"  (example computer address)
 *   
 * @author pcarr
 */
public final class HostnameUtil {
    // lazy init singleton idiom
    private static class Holder {
        static final HostnameUtil INSTANCE = new HostnameUtil();
    }

    public static HostnameUtil instance() {
        return Holder.INSTANCE;
    }

    public final String computerName;
    public final String computerAddress;
    public final Set<String> localHostnames;

    // private constructor
    private HostnameUtil() {
        this.computerName=getComputerName();
        this.computerAddress=getComputerAddress();
        this.localHostnames=initLocalhostnames(this.computerName, this.computerAddress);
    }

    protected static String getComputerName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Throwable t) {
            return "localhost";
        }
    }
    
    protected static String getComputerAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (Throwable t) {
            return "127.0.0.1";
        }
        
    }
    
    /**
     * Utility method to get my hosts; a set of host names and addresses for the machine on which this
     * process is running.
     * 
     * @return
     * @throws UnknownHostException
     */
    protected static Set<String> initLocalhostnames(final String localHostname, final String localHostaddress) {
        final Set<String> hostnames=new LinkedHashSet<String>();
        final InetAddress loopback = InetAddress.getLoopbackAddress();
        if (loopback != null) hostnames.add(loopback.getHostName());
        if (loopback != null) hostnames.add(loopback.getHostAddress());
        if (localHostname != null) hostnames.add(localHostname);
        if (localHostaddress != null) hostnames.add(localHostaddress);
        for(final InetAddress addr : getComputerAddresses()) {
            final String uriStr=InetAddresses.toUriString(addr);
            hostnames.add(uriStr);
        }
        return ImmutableSet.copyOf(hostnames);
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
                final NetworkInterface iface = interfaces.nextElement();
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                final Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    final InetAddress addr = addresses.nextElement();
                    rval.add(addr);
                }
            }
        } 
        catch (SocketException e) {
            fail("error getting computer ip addresses: "+e.getLocalizedMessage());
        }
        return rval;
    }

}
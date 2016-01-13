package org.genepattern.server.dm;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class InetUtilDefault implements InetUtil {
    private static InetUtil _instance=new InetUtilDefault();
    
    public static InetUtil instance() {
        return _instance;
    }

    @Override
    public InetAddress getByName(final String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }

    @Override
    public NetworkInterface getByInetAddress(final InetAddress addr) throws SocketException {
        return NetworkInterface.getByInetAddress(addr);
    }

}

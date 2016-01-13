package org.genepattern.server.dm;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * interface to make it possible to avoid static calls to the InetAddress and NetworkInterface java system classes.
 * @author pcarr
 *
 */
public interface InetUtil {
    InetAddress getByName(String host) throws UnknownHostException;
    Object getByInetAddress(InetAddress addr) throws SocketException ;
}

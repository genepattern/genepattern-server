
package com.enterprisedt.util.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.enterprisedt.util.debug.Logger;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.4 $
 */
public class PlainSocket extends Socket implements StreamSocket {
    
    /**
     * Logging object
     */
    private static Logger log = Logger.getLogger("PlainSocket");
    
    protected String remoteHostname;
    
    /**
     * Creates a new PlainSocket object.
     *
     * @throws IOException
     */
    public PlainSocket() {}
    
    /**
     * Creates a new PlainSocket object.
     *
     * @param host
     * @param port
     *
     * @throws IOException
     */
    public PlainSocket(String host, int port)
        throws IOException {
        super(host, port);
    }
    
    /**
     * Creates a new PlainSocket object.
     *
     * @param addr
     * @param port
     *
     * @throws IOException
     */
    public PlainSocket(InetAddress addr, int port)
        throws IOException {
        super(addr, port);
    }
    
    /**
     * Is this socket in secure mode?
     * 
     * @return true if secure mode
     */
    public boolean isSecureMode() {
        return false;
    }
    
    /**
     * Get the actual hostname 
     * 
     * @return remote hostname
     */
    public String getRemoteHost() {
        return remoteHostname;
    }
    
    /**
     * Set the remote hostname
     * 
     * @param remoteHost  remote hostname
     */
    public void setRemoteHost(String remoteHost) {
        this.remoteHostname = remoteHost;
    }


    /**
     *
     *
     * @return
     */
    public String getDetail() {
        return toString();
    }
    
    /**
     * Create a connected socket, using a timeout if it is available. 
     * Availability is tested by trying to create instances of the 
     * required classes and methods (JRE 1.4+)
     * 
     * @param host     remote host to connect to
     * @param port     port on remote host
     * @param timeout  timeout in milliseconds on 
     * @exception IOException
     */
    public static PlainSocket createPlainSocket(String host, int port, int timeout)
            throws IOException {

        PlainSocket sock = new PlainSocket();
        InetSocketAddress addr = new InetSocketAddress(host, port);
        sock.connect(addr, timeout);
        return sock;
    }

    /**
     * Create a connected socket, using a timeout if it is available. 
     * Availability is tested by trying to create instances of the 
     * required classes and methods (JRE 1.4+)
     * 
     * @param host     remote host to connect to
     * @param port     port on remote host
     * @param timeout  timeout in milliseconds on 
     * @exception IOException
     */
    public static PlainSocket createPlainSocket(InetAddress host, int port, int timeout)
            throws IOException {

        PlainSocket sock = new PlainSocket();
        InetSocketAddress addr = new InetSocketAddress(host, port);
        sock.connect(addr, timeout);
        return sock;
    }
}

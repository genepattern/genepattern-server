
package com.enterprisedt.util.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;


/**
 * Interface of all stream sockets
 *
 * @author $author$
 * @version $Revision: 1.4 $
 */
public interface StreamSocket {
        
    /**
     * Close the socket
     *
     * @throws IOException
     */
    public void close() throws IOException;
    
    /**
     * Is the socket connected?
     * 
     * @return
     */
    public boolean isConnected();

    /**
     * Get the socket's input stream
     * 
     * @return stream
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException;
    
    /**
     * Enable/disable SO_TIMEOUT with the specified timeout, in
     *  milliseconds.  
     *   
     * @param timeout
     * @throws SocketException
     */
    public void setSoTimeout(int timeout) throws SocketException;
    
    
    /**
     * Returns setting for SO_TIMEOUT.  
     */
    public int getSoTimeout() throws SocketException;

    /**
     * Get the socket's output stream
     *
     * @return stream
     * @throws IOException
     */
    public OutputStream getOutputStream() throws IOException;
    
    /**
     * Gets the local address to which the socket is bound.
     *
     * @return the local address 
     */
    public InetAddress getLocalAddress();
    
    /**
     * Returns the local port to which this socket is bound.
     *
     * @return  the local port 
     */
    public int getLocalPort();
    
    /**
     * Returns the address to which the socket is connected.
     *
     * @return  the remote IP address
     */
    public InetAddress getInetAddress();
    
    /**
     * Get the actual hostname 
     * 
     * @return remote hostname
     */
    public String getRemoteHost();
    
    /**
     * Set the remote hostname
     * 
     * @param remoteHost  remote hostname
     */
    public void setRemoteHost(String remoteHost);
    
    /**
     * Gets the value of SO_RCVBUF
     */
    public int getReceiveBufferSize() throws SocketException;
    
    /**
     * Sets the SO_RCVBUF option
     */
    public void setReceiveBufferSize(int size) throws SocketException;
    
    /**
     * Sets the SO_SNDBUF option 
     */
    public void setSendBufferSize(int size) throws SocketException;
    
    /**
     * Get value of the SO_SNDBUF option
     */
    public int getSendBufferSize() throws SocketException;
    
    /**
     * Is this socket in secure mode?
     * 
     * @return true if secure mode
     */
    public boolean isSecureMode();

    /**
     * Get details about the socket
     *
     * @return
     */
    public String getDetail();
}

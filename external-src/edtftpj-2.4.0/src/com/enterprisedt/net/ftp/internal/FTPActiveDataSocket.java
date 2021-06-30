/**
 *
 *  Copyright (C) 2000-2003  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  Bug fixes, suggestions and comments should be should posted on 
 *  http://www.enterprisedt.com/forums/index.php
 *
 *  Change Log:
 *
 *        $Log: FTPActiveDataSocket.java,v $
 *        Revision 1.4  2009-09-02 22:02:24  bruceb
 *        fix re wildcard address used for PORT
 *
 *        Revision 1.3  2009-07-17 03:04:22  bruceb
 *        proxy changes
 *
 *        Revision 1.2  2009-03-20 03:54:07  bruceb
 *        send/receive buffer sizes support
 *
 *        Revision 1.1  2008-05-22 04:20:55  bruceb
 *        moved stuff to internal etc
 *
 *        Revision 1.6  2006/10/17 10:27:15  bruceb
 *        added closeChild()
 *
 *        Revision 1.5  2005/06/03 11:26:25  bruceb
 *        comment change
 *
 *        Revision 1.4  2004/07/23 08:27:03  bruceb
 *        made public cvsId
 *
 *        Revision 1.3  2003/11/15 11:23:55  bruceb
 *        changes required for ssl subclasses
 *
 *        Revision 1.1  2003/11/02 21:49:52  bruceb
 *        implement FTPDataSocket interface
 *
 *
 */
package com.enterprisedt.net.ftp.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.enterprisedt.util.debug.Logger;

/**
 * Active data socket handling class
 * 
 * @author Bruce Blackshaw
 * @version $Revision: 1.4 $
 */
public class FTPActiveDataSocket implements FTPDataSocket {

    /**
     * Revision control id
     */
    public static String cvsId = "@(#)$Id: FTPActiveDataSocket.java,v 1.4 2009-09-02 22:02:24 bruceb Exp $";

    /**
     * Logging object
     */
    private static Logger log = Logger.getLogger("FTPActiveDataSocket");

    /**
     * The underlying socket for Active connection.
     */
    protected ServerSocket sock = null;

    /**
     * The socket accepted from server.
     */
    protected Socket acceptedSock = null;
    
    protected int sendBufferSize = 0;
    
    private InetAddress localAddress = null;
    
    /**
     * Constructor
     * 
     * @param sock
     *            the server socket to use
     */
    public FTPActiveDataSocket(ServerSocket sock) {
        this.sock = sock;
    }

    /**
     * Set the TCP timeout on the underlying data socket(s).
     * 
     * If a timeout is set, then any operation which takes longer than the
     * timeout value will be killed with a java.io.InterruptedException.
     * 
     * @param millis
     *            The length of the timeout, in milliseconds
     */
    public void setTimeout(int millis) throws IOException {
        sock.setSoTimeout(millis);
        if (acceptedSock != null)
            acceptedSock.setSoTimeout(millis);
    }
    
    /**
     * Set the size of the data socket's receive buffer.
     * 
     * @param size  must be > 0
     */
    public void setReceiveBufferSize(int size) throws IOException {
        sock.setReceiveBufferSize(size);
        if (acceptedSock != null)
            acceptedSock.setReceiveBufferSize(size);
    }
    
    /**
     * Set the size of the data socket's send buffer.
     * 
     * @param size  must be > 0
     */
    public void setSendBufferSize(int size) throws IOException {
        this.sendBufferSize = size;
        if (acceptedSock != null)
            acceptedSock.setSendBufferSize(size);
    }

    /**
     * Returns the local port to which this socket is bound.
     * 
     * @return the local port number to which this socket is bound
     */
    public int getLocalPort() {
        return sock.getLocalPort();
    }
    
    /**
     * Returns the local address to which this socket is bound. 
     * 
     * @return the local address to which this socket is bound
     */
    public InetAddress getLocalAddress() {
        if (localAddress != null)
            return localAddress;
        return sock.getInetAddress();
    }     
    
    /**
     * Set the local address to be used
     * 
     * @param addr   address to set
     */
    public void setLocalAddress(InetAddress addr) {
        localAddress = addr;
    }

    /**
     * Waits for a connection from the server and then sets the timeout when the
     * connection is made.
     * 
     * @throws IOException
     *             There was an error while waiting for or accepting a
     *             connection from the server.
     */
    protected void acceptConnection() throws IOException {
        log.debug("Calling accept()");
        acceptedSock = sock.accept();
        acceptedSock.setSoTimeout(sock.getSoTimeout());
        acceptedSock.setReceiveBufferSize(sock.getReceiveBufferSize());
        if (sendBufferSize > 0)
            acceptedSock.setSendBufferSize(sendBufferSize);
        log.debug("accept() succeeded");
    }
    

    /**
     * If active mode, accepts the FTP server's connection - in PASV, we are
     * already connected. Then gets the output stream of the connection
     * 
     * @return output stream for underlying socket.
     */
    public OutputStream getOutputStream() throws IOException {
        acceptConnection();
        return acceptedSock.getOutputStream();
    }

    /**
     * If active mode, accepts the FTP server's connection - in PASV, we are
     * already connected. Then gets the input stream of the connection
     * 
     * @return input stream for underlying socket.
     */
    public InputStream getInputStream() throws IOException {
        acceptConnection();
        return acceptedSock.getInputStream();
    }

    /**
     * Closes underlying sockets
     */
    public void close() throws IOException {
        closeChild();
        sock.close();
        log.debug("close() succeeded");
    }

    /**
     * Closes child socket
     */
    public void closeChild() throws IOException {
        if (acceptedSock != null) {
            acceptedSock.close();
            acceptedSock = null;
            log.debug("closeChild() succeeded");
        }
    }
}

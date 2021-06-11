/**
 *
 *  Java FTP client library.
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
 *        $Log: FTPDataSocket.java,v $
 *        Revision 1.3  2009-07-17 03:04:22  bruceb
 *        proxy changes
 *
 *        Revision 1.2  2009-03-20 03:54:07  bruceb
 *        send/receive buffer sizes support
 *
 *        Revision 1.1  2008-05-22 04:20:55  bruceb
 *        moved stuff to internal etc
 *
 *        Revision 1.9  2006/10/17 10:27:44  bruceb
 *        added closeChild()
 *
 *        Revision 1.8  2006/10/11 08:53:13  hans
 *        made cvsId final
 *
 *        Revision 1.7  2005/06/03 11:26:25  bruceb
 *        comment change
 *
 *        Revision 1.6  2003/11/15 11:23:55  bruceb
 *        changes required for ssl subclasses
 *
 *        Revision 1.4  2003/11/02 21:50:14  bruceb
 *        changed FTPDataSocket to an interface
 *
 *        Revision 1.3  2003/05/31 14:53:44  bruceb
 *        1.2.2 changes
 *
 *        Revision 1.2  2002/11/19 22:01:25  bruceb
 *        changes for 1.2
 *
 *        Revision 1.1  2001/10/09 20:53:46  bruceb
 *        Active mode changes
 *
 *        Revision 1.1  2001/10/05 14:42:03  bruceb
 *        moved from old project
 *
 */

package com.enterprisedt.net.ftp.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

/**
 *  Interface for data socket classes, whether active or passive
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.3 $
 */
public interface FTPDataSocket {

    /**
     *  Revision control id
     */
    public static final String cvsId = "@(#)$Id: FTPDataSocket.java,v 1.3 2009-07-17 03:04:22 bruceb Exp $";

    /**
     *   Set the TCP timeout on the underlying control socket.
     *
     *   If a timeout is set, then any operation which
     *   takes longer than the timeout value will be
     *   killed with a java.io.InterruptedException.
     *
     *   @param millis The length of the timeout, in milliseconds
     */
    public void setTimeout(int millis) throws IOException;
    
    /**
     * Set the size of the data socket's receive buffer.
     * 
     * @param size  must be > 0
     */
    public void setReceiveBufferSize(int size) throws IOException;
        
    /**
     * Set the size of the data socket's send buffer.
     * 
     * @param size  must be > 0
     */
    public void setSendBufferSize(int size) throws IOException;
    
    /**
     * Returns the local port to which this socket is bound. 
     * 
     * @return the local port number to which this socket is bound
     */
    public int getLocalPort();
    
    /**
     * Returns the local address to which this socket is bound. 
     * 
     * @return the local address to which this socket is bound
     */
    public InetAddress getLocalAddress();

    /**
     *  Get the appropriate output stream for writing to
     *
     *  @return  output stream for underlying socket.
     */
    public OutputStream getOutputStream() throws IOException;

    /**
     *  Get the appropriate input stream for reading from
     *
     *  @return  input stream for underlying socket.
     */
    public InputStream getInputStream() throws IOException;

     /**
      *  Closes underlying socket(s)
      */
    public void close() throws IOException;
    
    /**
     * Closes child socket
     * 
     * @throws IOException
     */
    public void closeChild() throws IOException;
}


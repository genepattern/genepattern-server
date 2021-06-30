/**
 * 
 *  Copyright (C) 2012 Enterprise Distributed Technologies Ltd
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
 *    $Log: AbstractFTPInputStream.java,v $
 *    Revision 1.2  2012/11/07 02:43:21  bruceb
 *    rename size to pos & update pos in skip()
 *
 *    Revision 1.1  2012/11/06 04:09:16  bruceb
 *    stream changes
 *
 *    Revision 1.4  2012/02/08 06:19:50  bruceb
 *    allow offset to be specified
 *
 *    Revision 1.3  2009-09-21 00:53:34  bruceb
 *    fix bug where LF were stripped when should have been left in ASCII mode
 *
 *    Revision 1.2  2008-07-29 02:58:00  bruceb
 *    fix read() bug
 *
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import com.enterprisedt.util.debug.Logger;

/**
 *  Represents an input stream of bytes coming from an FTP server, permitting
 *  the user to download a file by reading the stream. It can only be used
 *  for one download, i.e. after the stream is closed it cannot be reopened.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
abstract public class AbstractFTPInputStream extends FileTransferInputStream {
    
    private static Logger log = Logger.getLogger("AbstractFTPInputStream");
       
    /**
     * The client being used to perform the transfer
     */
    protected FTPClient client; 
    
    /**
     * The input stream from the FTP server
     */
    protected BufferedInputStream in;

        
    /**
     * Constructor. A connected FTPClient instance must be supplied. This sets up the
     * download. If an offset > 0 is supplied, must be a binary transfer.
     * 
     * @param client            connected FTPClient instance
     * @param remoteFile        remote file
     * @throws IOException
     * @throws FTPException
     */
    public AbstractFTPInputStream(FTPClient client, String remoteFile) throws IOException, FTPException {
        this.client = client;
        this.remoteFile = remoteFile;
        this.monitorInterval = client.getMonitorInterval();
        this.monitor = client.getProgressMonitor();
    }
    
    protected void start() throws IOException {
        start(true);
    }
    
    /**
     * Start the transfer
     * 
     * @throws IOException
     */
    protected void start(boolean firstTime) throws IOException {
        try {
            if (pos > 0)
                client.resumeNextDownload(pos);
            
            client.initGet(remoteFile);

            // get an input stream to read data from ... AFTER we have
            // the ok to go ahead AND AFTER we've successfully opened a
            // stream for the local file
            in = new BufferedInputStream(new DataInputStream(client.getInputStream()));

        } 
        catch (FTPException ex) {
            throw new IOException(ex.getMessage());
        }
        catch (IOException ex) {
            try {
                client.validateTransferOnError(ex);
            }
            catch (FTPException ex2) {
                throw new IOException(ex2.getMessage());
            }
            throw ex;
        }
        if (firstTime && monitorEx != null) {
            monitorEx.transferStarted(TransferDirection.DOWNLOAD, remoteFile);
        }
        started = true;
        closed = false;
        monitorCount = 0;
    }
    

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream. This <b>must</b> be called before any other operations
     * are initiated on the FTPClient.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        
        if (!closed) {
            closed = true;
            
            client.forceResumeOff();
    
            // close streams
            client.closeDataSocket(in);
            
            if (monitor != null)
                monitor.bytesTransferred(pos);  
    
            // log bytes transferred
            log.debug("Transferred " + pos + " bytes from remote host");
            
            // read the reply - may be a 426 as we could have closed early
            try {
                client.readReply();
            }
            catch (FTPException ex) {
                throw new IOException(ex.getMessage());
            }
            
            // don't know if it is truly complete for whole file but our transfer is
            if (monitorEx != null)
                monitorEx.transferComplete(TransferDirection.DOWNLOAD, remoteFile);
        }
    }
    
}

/**
 * 
 *  Copyright (C) 2007 Enterprise Distributed Technologies Ltd
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
 *    $Log: FTPOutputStream.java,v $
 *    Revision 1.3  2012-01-19 11:13:17  bruceb
 *    remove size (to superclass)
 *
 *    Revision 1.2  2009-01-15 03:37:30  bruceb
 *    fix bug re this.remoteFile
 *
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.enterprisedt.util.debug.Logger;

/**
 *  Represents an output stream that writes to an FTP server, permitting
 *  the user to upload a file by writing to the stream. It can only be used
 *  for one upload, i.e. after the stream is closed it cannot be reopened.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.3 $
 */
public class FTPOutputStream extends FileTransferOutputStream {
    
    private static Logger log = Logger.getLogger("FTPOutputStream");
    
    /**
     * Interval that we notify the monitor of progress
     */
    private long monitorInterval;
      
    /**
     * The client being used to perform the transfer
     */
    private FTPClient client; 
    
    /**
     * The output stream to the FTP server
     */
    private BufferedOutputStream out;
        
    /**
     * Is this an ASCII transfer or not?
     */
    private boolean isASCII = false;
    
    /**
     * Count of byte since last the progress monitor was notified.
     */
    private long monitorCount = 0; 

    /**
     * Progress monitor reference
     */
    private FTPProgressMonitor monitor;
    
    /**
     * Progress monitor reference
     */
    private FTPProgressMonitorEx monitorEx;
    
    
    /**
     * Previously read bytes that match the line terminator
     */
    private byte[] prevBuf = new byte[FTPClient.FTP_LINE_SEPARATOR.length];
    
    /**
     * Where we are up to in prevBuf re matches
     */
    private int matchpos = 0;
        
    /**
     * Constructor. A connected FTPClient instance must be supplied. This sets up the
     * download.
     * 
     * @param client            connected FTPClient instance (or subclass)
     * @param remoteFile        name of remote file (if null, the server <b>may</b> 
     *                           be able to generate a name
     * @throws IOException
     * @throws FTPException
     */
    public FTPOutputStream(FTPClient client, String remoteFile) throws IOException, FTPException {
        this(client, remoteFile, false);
    }


    /**
     * Constructor. A connected FTPClient instance must be supplied. This sets up the
     * download.
     * 
     * @param client            connected FTPClient instance (or subclass)
     * @param remoteFile        name of remote file (if null, the server <b>may</b> 
     *                           be able to generate a name
     * @param append            true if appending on the server
     * @throws IOException
     * @throws FTPException
     */
    public FTPOutputStream(FTPClient client, String remoteFile, boolean append) throws IOException, FTPException {
        this.client = client;
        this.remoteFile = remoteFile;
        try {
            this.remoteFile = client.initPut(remoteFile, append);

            // get an input stream to read data from ... AFTER we have
            // the ok to go ahead AND AFTER we've successfully opened a
            // stream for the local file
            out = 
                new BufferedOutputStream(
                        new DataOutputStream(client.getOutputStream()), client.getTransferBufferSize()*2);

        } 
        catch (IOException ex) {
            client.validateTransferOnError(ex);
            throw ex;
        }
        
        this.monitorInterval = client.getMonitorInterval();
        this.monitor = client.getProgressMonitor();
        this.isASCII = (client.getType().equals(FTPTransferType.ASCII));
    }
    
    
    /**
     * The output stream uses the progress monitor currently owned by the FTP client.
     * This method allows a different progress monitor to be passed in, or for the
     * monitor interval to be altered.
     * 
     * @param monitor               progress monitor reference
     * @param monitorInterval       
     */
    public void setMonitor(FTPProgressMonitorEx monitor, long monitorInterval) {
        this.monitor = monitor;
        this.monitorEx = monitor;
        this.monitorInterval = monitorInterval;
    }
 
    /**
     * Writes <code>b.length</code> bytes from the specified byte array 
     * to this output stream.
     *
     * @param      b   the data.
     * @exception  IOException  if an I/O error occurs.
     */
    public void write(int b) throws IOException {
        byte[] tmp = new byte[1];
        tmp[0] = (byte)b;
        write(tmp, 0, 1);
    }
    
    
    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this output stream. 
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs. 
     */
    public void write(byte b[], int off, int len) throws IOException {
        if (isASCII) {
            for (int i = off; i < off+len; i++) {
                // LF without preceding CR (i.e. Unix text file)
                if (b[i] == FTPClient.LINE_FEED && matchpos == 0) {
                    out.write(FTPClient.CARRIAGE_RETURN);
                    out.write(FTPClient.LINE_FEED);
                    size += 2;
                    monitorCount += 2;
                }
                else if (b[i] == FTPClient.FTP_LINE_SEPARATOR[matchpos]) {
                    prevBuf[matchpos] = b[i];
                    matchpos++;
                    if (matchpos == FTPClient.FTP_LINE_SEPARATOR.length) {
                        out.write(FTPClient.CARRIAGE_RETURN);
                        out.write(FTPClient.LINE_FEED);
                        size += 2;
                        monitorCount += 2;
                        matchpos = 0;
                    }
                }
                else { // no match current char 
                    // this must be a matching \r if we matched first char
                    if (matchpos > 0) {
                        out.write(FTPClient.CARRIAGE_RETURN);
                        out.write(FTPClient.LINE_FEED);
                        size += 2;
                        monitorCount += 2;
                    }
                    out.write(b[i]);
                    size++;
                    monitorCount++;
                    matchpos = 0;
                }                              
            }
        }
        else { // binary
            out.write(b, off, len);
            size += len;
            monitorCount += len;
        }
                            
        if (monitor != null && monitorCount > monitorInterval) {
            monitor.bytesTransferred(size); 
            monitorCount = 0;  
        }        
    }


    
    /**
     * Closes this output stream and releases any system resources associated
     * with the stream. This <b>must</b> be called before any other operations
     * are initiated on the FTPClient.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        
        if (!closed) {
            closed = true;
            
            //  write out anything left at the end that has been saved
            if (isASCII && matchpos > 0) {
                out.write(prevBuf, 0, matchpos);
                size += matchpos;
                monitorCount += matchpos;
            }
            
            client.forceResumeOff();
    
            // close streams
            client.closeDataSocket(out);
            
            if (monitor != null)
                monitor.bytesTransferred(size);  
    
            // log bytes transferred
            log.debug("Transferred " + size + " bytes from remote host");
            
            try {
                client.validateTransfer();
            }
            catch (FTPException ex) {
                throw new IOException(ex.getMessage());
            }
            
            if (monitorEx != null)
                monitorEx.transferComplete(TransferDirection.UPLOAD, remoteFile);
        }
    }
    
}

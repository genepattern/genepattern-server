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
 *    $Log: FTPASCIIInputStream.java,v $
 *    Revision 1.2  2012/11/07 02:43:21  bruceb
 *    rename size to pos & update pos in skip()
 *
 *    Revision 1.1  2012/11/06 04:09:16  bruceb
 *    stream changes
 *
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *  Represents an input stream of bytes coming from an FTP server, permitting
 *  the user to download a file by reading the stream. It can only be used
 *  for one download, i.e. after the stream is closed it cannot be reopened.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
class FTPASCIIInputStream extends AbstractFTPInputStream {
        
    /**
     * Line separator
     */
    final private static byte[] LINE_SEPARATOR = System.getProperty("line.separator").getBytes();
    
    /**
     * Buffer that supplies the stream
     */
    private byte [] buffer;
    
    /**
     * Current position to read from in the buffer
     */
    private int bufpos = 0;
    
    /**
     * The length of the buffer
     */
    private int buflen = 0;
    
    /**
     * Buffer read from the FTP server
     */
    private byte[] chunk;
    
    private byte[] prevBuf = new byte[FTPClient.FTP_LINE_SEPARATOR.length];
    
    private int matchpos = 0;
    
    /**
     * Output stream to write to 
     */
    private ByteArrayOutputStream out;
        
    /**
     * Constructor. A connected FTPClient instance must be supplied. This sets up the
     * download 
     * 
     * @param client            connected FTPClient instance
     * @param remoteFile        remote file
     * @throws IOException
     * @throws FTPException
     */
    public FTPASCIIInputStream(FTPClient client, String remoteFile) throws IOException, FTPException {
        this(client, remoteFile, 0);            
    }
    
    /**
     * Constructor. A connected FTPClient instance must be supplied. This sets up the
     * download. If an offset > 0 is supplied, must be a binary transfer.
     * 
     * @param client            connected FTPClient instance
     * @param remoteFile        remote file
     * @param offset            offset to resume downloading from.
     * @throws IOException
     * @throws FTPException
     */
    public FTPASCIIInputStream(FTPClient client, String remoteFile, long offset) throws IOException, FTPException {
        super(client, remoteFile);

        this.chunk = new byte[client.getTransferBufferSize()];
        this.out = new ByteArrayOutputStream(client.getTransferBufferSize());
    }
    
    /**
     * Reads the next byte of data from the input stream. The value byte is 
     * returned as an int in the range 0 to 255. If no byte is available because 
     * the end of the stream has been reached, the value -1 is returned. 
     * This method blocks until input data is available, the end of the stream 
     * is detected, or an exception is thrown. 
     */
    public int read() throws IOException {
        if (!started) {
            start();
        }
        if (buffer == null)
            return -1;
        if (bufpos == buflen) {
            buffer = refreshBuffer();
            if (buffer == null)
                return -1;
        }
        return 0xFF & buffer[bufpos++];
    }
    
    /**
     * Reads up to len bytes of data from the input stream into an array of bytes. 
     * An attempt is made to read as many as len bytes, but a smaller number may 
     * be read, possibly zero. The number of bytes actually read is returned as an integer. 
     * This method blocks until input data is available, end of file is detected, 
     * or an exception is thrown. 
     *
     * @param b    array to read into
     * @param off  offset into the array to start at
     * @param len  the number of bytes to be read
     * 
     * @return  the number of bytes read, or -1 if the end of the stream has been reached.
     */
    public int read(byte b[], int off, int len) throws IOException {
        if (!started) {
            start();
        }
        if (buffer == null || len == 0)
            return -1;
        
        if (bufpos == buflen) {
            buffer = refreshBuffer();
            if (buffer == null)
                return -1;
        }
        
        int available = 0;
        int remaining = len;
        while ( (available = buflen-bufpos) < remaining ) {
            System.arraycopy(buffer, bufpos, b, off, available);
            remaining -= available;
            off += available;
            buffer = refreshBuffer();
            if (buffer == null)
                return len-remaining;
        }
        System.arraycopy(buffer, bufpos, b, off, remaining);
        bufpos += remaining;
        return len;        
    }
    
    protected void start() throws IOException {
        super.start();
        buffer = refreshBuffer();        
    }
    
    /**
     * Refresh the buffer by reading the internal FTP input stream 
     * directly from the server
     * 
     * @return  byte array of bytes read, or null if the end of stream is reached
     * @throws IOException
     */
    private byte[] refreshBuffer() throws IOException {
        bufpos = 0;
        if (client.isTransferCancelled())
            return null;
        int count = client.readChunk(in, chunk, chunk.length);
        if (count < 0) {
            if (matchpos > 0) {               
                pos += matchpos;
                buflen = matchpos;
                monitorCount += matchpos;
                byte[] tmp = new byte[matchpos];
                System.arraycopy(tmp, 0, prevBuf, 0, matchpos);
                matchpos = 0;
                return tmp;
            }
            return null;
        }
        try {
            // transform CRLF
            out.reset();
            for (int i = 0; i < count; i++) {
                if (chunk[i] == FTPClient.FTP_LINE_SEPARATOR[matchpos]) {
                    prevBuf[matchpos] = chunk[i];
                    matchpos++;
                    if (matchpos == FTPClient.FTP_LINE_SEPARATOR.length) {
                        out.write(LINE_SEPARATOR);
                        pos += LINE_SEPARATOR.length;
                        monitorCount += LINE_SEPARATOR.length;
                        matchpos = 0;
                    }
                }
                else { // no match
                    // write out existing matches
                    if (matchpos > 0) {
                        out.write(prevBuf, 0, matchpos);
                        pos += matchpos;
                        monitorCount += matchpos;
                    }
                    out.write(chunk[i]);
                    pos++;
                    monitorCount++;
                    matchpos = 0;
                }                              
            }                
            byte[] result = out.toByteArray();
            buflen = result.length;
            return result;
        }
        finally {
            checkMonitor(); 
        }
    }
    
}

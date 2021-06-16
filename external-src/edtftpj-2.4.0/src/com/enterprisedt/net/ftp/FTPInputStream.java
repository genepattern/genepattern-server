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
 *    $Log: FTPInputStream.java,v $
 *    Revision 1.5  2012/11/06 04:09:16  bruceb
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

import java.io.IOException;

/**
 *  Represents an input stream of bytes coming from an FTP server, permitting
 *  the user to download a file by reading the stream. It can only be used
 *  for one download, i.e. after the stream is closed it cannot be reopened.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.5 $
 */
public class FTPInputStream extends FileTransferInputStream {
    
 
    private AbstractFTPInputStream stream;
    
    /**
     * Constructor. A connected FTPClient instance must be supplied. This sets up the
     * download 
     * 
     * @param client            connected FTPClient instance
     * @param remoteFile        remote file
     * @throws IOException
     * @throws FTPException
     */
    public FTPInputStream(FTPClient client, String remoteFile) throws IOException, FTPException {
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
    public FTPInputStream(FTPClient client, String remoteFile, long offset) throws IOException, FTPException {
        this.remoteFile = remoteFile;
        boolean isASCII = (client.getType().equals(FTPTransferType.ASCII));
        if (isASCII)
        {
            if (offset > 0)
                throw new FTPException("Offset for ASCII transfers must be 0");
            stream = new FTPASCIIInputStream(client, remoteFile, offset);
        }
        else
            stream = new FTPBinaryInputStream(client, remoteFile, offset);
    }
    
    
    /**
     * Reads the next byte of data from the input stream. The value byte is 
     * returned as an int in the range 0 to 255. If no byte is available because 
     * the end of the stream has been reached, the value -1 is returned. 
     * This method blocks until input data is available, the end of the stream 
     * is detected, or an exception is thrown. 
     */
    public int read() throws IOException {
        return stream.read();
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
        return stream.read(b, off, len);
    }
    

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream. This <b>must</b> be called before any other operations
     * are initiated on the FTPClient.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    public void close() throws IOException {
        stream.close(); 
    }
    
    /**
     * Skips over and discards <code>n</code> bytes of data from this input
     * stream. The <code>skip</code> method may, for a variety of reasons, end
     * up skipping over some smaller number of bytes, possibly <code>0</code>.
     * This may result from any of a number of conditions; reaching end of file
     * before <code>n</code> bytes have been skipped is only one possibility.
     * The actual number of bytes skipped is returned.  If <code>n</code> is
     * negative, no bytes are skipped.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the actual number of bytes skipped.
     * @exception  IOException  if the stream does not support seek,
     *              or if some other I/O error occurs.
     */
    public long skip(long n) throws IOException {
        return stream.skip(n);
    }
    
    /**
     * Marks the current position in this input stream. A subsequent call to
     * the <code>reset</code> method repositions this stream at the last marked
     * position so that subsequent reads re-read the same bytes.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     * @see     java.io.InputStream#reset()
     */
    public synchronized void mark(int readlimit) {
        stream.mark(readlimit);
    }

    /**
     * Repositions this stream to the position at the time the
     * <code>mark</code> method was last called on this input stream.
     *
     * @exception  IOException  if this stream has not been marked or if the
     *               mark has been invalidated.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.IOException
     */
    public synchronized void reset() throws IOException {
        stream.reset();
    }

    
    /**
     * Tests if this input stream supports the <code>mark</code> and
     * <code>reset</code> methods. Whether or not <code>mark</code> and
     * <code>reset</code> are supported is an invariant property of a
     * particular input stream instance. The <code>markSupported</code> method
     * of <code>InputStream</code> returns <code>false</code>.
     *
     * @return  <code>true</code> if this stream instance supports the mark
     *          and reset methods; <code>false</code> otherwise.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     */
    public boolean markSupported() {
        return stream.markSupported();
    }
    
}

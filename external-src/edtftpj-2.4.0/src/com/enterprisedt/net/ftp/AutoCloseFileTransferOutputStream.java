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
 *    $Log$
 *    Revision 1.2  2012-01-19 11:12:49  bruceb
 *    add getBytesTransferred
 *
 *    Revision 1.1  2009-06-15 03:35:39  hans
 *    Wrapper for FileTransferOutputStream which calls disconnect() when the stream is closed.
 *
 */
package com.enterprisedt.net.ftp;

import java.io.IOException;

/**
 * Wrapper for FileTransferOutputStream which calls <code>disconnect()</code> when the 
 * stream is closed.
 * 
 * @author Hans Andersen
 */
class AutoCloseFileTransferOutputStream extends FileTransferOutputStream {

	private FileTransferOutputStream outStr;
	private FileTransferClientInterface client;
	
	public AutoCloseFileTransferOutputStream(FileTransferOutputStream outStr, FileTransferClientInterface client) {
		this.outStr = outStr;
		this.client = client;
	}

	public void write(int b) throws IOException {
		outStr.write(b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		outStr.write(b, off, len);
	}

	public String getRemoteFile() {
		return outStr.getRemoteFile();
	}

	public void close() throws IOException {
		outStr.close();
		try {
			client.disconnect(true);
		} catch (Throwable t) {
			throw new IOException(t.getMessage());
		}
	}

	public void flush() throws IOException {
		outStr.flush();
	}

	public void write(byte[] b) throws IOException {
		outStr.write(b);
	}
	
	/**
     * Get the number of bytes transferred
     * 
     * @return long
     */
    public long getBytesTransferred() {
        return outStr.getBytesTransferred();
    }

	public boolean equals(Object obj) {
		return outStr.equals(obj);
	}

	public int hashCode() {
		return outStr.hashCode();
	}

	public String toString() {
		return outStr.toString();
	}
}

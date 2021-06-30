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
 *    Revision 1.1  2009-06-15 03:35:26  hans
 *    Wrapper for FileTransferInputStream which calls disconnect() when the stream is closed.
 *
 */
package com.enterprisedt.net.ftp;

import java.io.IOException;

/**
 * Wrapper for FileTransferInputStream which calls <code>disconnect()</code> when the 
 * stream is closed.
 * 
 * @author Hans Andersen
 */
class AutoCloseFileTransferInputStream extends FileTransferInputStream {

	private FileTransferInputStream inStr;
	private FileTransferClientInterface client;
	
	public AutoCloseFileTransferInputStream(FileTransferInputStream inStr, FileTransferClientInterface client) {
		this.inStr = inStr;
		this.client = client;
	}
	
	public int read() throws IOException {
		return inStr.read();
	}

	public String getRemoteFile() {
		return inStr.getRemoteFile();
	}

	public int available() throws IOException {
		return inStr.available();
	}

	public void close() throws IOException {
		inStr.close();
		try {
			client.disconnect(true);
		} catch (Throwable t) {
			throw new IOException(t.getMessage());
		}
	}

	public synchronized void mark(int readlimit) {
		inStr.mark(readlimit);
	}

	public boolean markSupported() {
		return inStr.markSupported();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return inStr.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		return inStr.read(b);
	}

	public synchronized void reset() throws IOException {
		inStr.reset();
	}

	public long skip(long n) throws IOException {
		return inStr.skip(n);
	}

	public boolean equals(Object obj) {
		return inStr.equals(obj);
	}

	public int hashCode() {
		return inStr.hashCode();
	}

	public String toString() {
		return inStr.toString();
	}
}

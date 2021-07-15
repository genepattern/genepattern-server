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
 *    $Log: FileTransferInputStream.java,v $
 *    Revision 1.3  2012/11/07 02:43:21  bruceb
 *    rename size to pos & update pos in skip()
 *
 *    Revision 1.2  2012/11/06 04:09:16  bruceb
 *    stream changes
 *
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 *  Super class of all input streams supported
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.3 $
 */
abstract public class FileTransferInputStream extends InputStream {

    /** 
     * Name of remote file being transferred
     */
    protected String remoteFile;
    
    /**
     * Has the stream been closed?
     */
    protected boolean closed = false;
    
    /**
     * Interval that we notify the monitor of progress
     */
    protected long monitorInterval;
          
    /**
     * Byte position in file
     */
    protected long pos = 0;
        
    /**
     * Count of byte since last the progress monitor was notified.
     */
    protected long monitorCount = 0; 

    /**
     * Progress monitor reference
     */
    protected FTPProgressMonitor monitor;
    
    /**
     * Progress monitor reference
     */
    protected FTPProgressMonitorEx monitorEx;
    
    /**
     * Flag to indicated we've started downloading
     */
    protected boolean started = false;

    /**
     * Get the name of the remote file 
     * 
     * @return remote filename
     */
    public String getRemoteFile() {
        return remoteFile;
    }
    
    /**
     * The input stream uses the progress monitor currently owned by the FTP client.
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
     * Check if time to invoke the monitor
     */
    protected void checkMonitor() {
        if (monitor != null && monitorCount > monitorInterval) {
            monitor.bytesTransferred(pos); 
            monitorCount = 0;  
       } 
    }
    
}

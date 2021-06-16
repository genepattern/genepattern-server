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
 *    $Log: FileStatistics.java,v $
 *    Revision 1.4  2008-05-22 04:20:55  bruceb
 *    moved stuff to internal etc
 *
 *    Revision 1.3  2008-04-17 04:50:49  bruceb
 *    synchronize methods
 *
 *    Revision 1.2  2008-03-13 00:23:16  bruceb
 *    modify for multiple clients
 *
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.util.Enumeration;
import java.util.Vector;


/**
 *  Statistics on transfers and deletes. This will be continually
 *  updated by the clients.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.4 $
 */
public class FileStatistics {
    
    /**
     * Client with stats
     */
    private Vector clients = new Vector();
    
    /**
     * Default constructor
     * 
     * @param downloadCount
     * @param uploadCount
     * @param deleteCount
     */
    public FileStatistics() {
    }

    /**
     * Constructor
     * 
     * @param downloadCount
     * @param uploadCount
     * @param deleteCount
     */
    FileStatistics(FTPClientInterface client) {
        clients.add(client);
    }
    
    /**
     * Add a client to be used in calculating statistics
     * 
     * @param client    extra client
     */
    public synchronized void addClient(FTPClientInterface client) {
        clients.add(client);
    }

    /**
     * Get the number of files downloaded since the count was
     * reset
     * 
     * @return  download file count
     */
    public synchronized int getDownloadCount() {
        int count = 0;
        Enumeration e = clients.elements();
        while (e.hasMoreElements()) {
            FTPClientInterface client = (FTPClientInterface)e.nextElement();
            count += client.getDownloadCount();
        }
        return count;
    }
        
    /**
     * Get the number of files uploaded since the count was
     * reset
     * 
     * @return  upload file count
     */
    public synchronized int getUploadCount() {
        int count = 0;
        Enumeration e = clients.elements();
        while (e.hasMoreElements()) {
            FTPClientInterface client = (FTPClientInterface)e.nextElement();
            count += client.getUploadCount();
        }
        return count;
    }
    
    
    /**
     * Get the number of files deleted since the count was
     * reset
     * 
     * @return  deleted file count
     */
    public synchronized int getDeleteCount() {
        int count = 0;
        Enumeration e = clients.elements();
        while (e.hasMoreElements()) {
            FTPClientInterface client = (FTPClientInterface)e.nextElement();
            count += client.getDeleteCount();
        }
        return count;
    }
    
    /**
     * Reset the statistics back to zero
     */
    public synchronized void clear() {
        Enumeration e = clients.elements();
        while (e.hasMoreElements()) {
            FTPClientInterface client = (FTPClientInterface)e.nextElement();
            client.resetDownloadCount();
            client.resetDeleteCount();
            client.resetUploadCount();
        }
    }

}

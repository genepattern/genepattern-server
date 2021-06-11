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
 *    $Log: EventAdapter.java,v $
 *    Revision 1.2  2008-03-13 00:24:21  bruceb
 *    added connId to params
 *
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp;

/**
 *  An adapter class that implements dummy methods of the EventListener
 *  interface. Useful for quickly implementing a single method of EventListener.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
public class EventAdapter implements EventListener {
    
    /**
     * Log an FTP command being sent to the server. Not used for SFTP.
     * 
     * @param cmd   command string
     */
    public void commandSent(String connId, String cmd) {} 
    
    /**
     * Log an FTP reply being sent back to the client. Not used for
     * SFTP.
     * 
     * @param reply   reply string
     */
    public void replyReceived(String connId, String reply) {}
    
    /**
     * Report the number of bytes transferred so far. This may
     * not be entirely accurate for transferring text files in ASCII
     * mode, as new line representations can be represented differently
     * on different platforms.
     * 
     * @param count  count of bytes transferred
     */
    public void bytesTransferred(String connId, String remoteFilename, long count) {}
    
    /**
     * Notifies that a download has started
     * 
     * @param remoteFilename   remote file name
     */
    public void downloadStarted(String connId, String remoteFilename) {}
    
    /**
     * Notifies that a download has completed
     * 
     * @param remoteFilename   remote file name
     */
    public void downloadCompleted(String connId, String remoteFilename) {}
    
    /**
     * Notifies that an upload has started
     * 
     * @param remoteFilename   remote file name
     */
    public void uploadStarted(String connId, String remoteFilename) {}
    
    /**
     * Notifies that an upload has completed
     * 
     * @param remoteFilename   remote file name
     */
    public void uploadCompleted(String connId, String remoteFilename) {}

}

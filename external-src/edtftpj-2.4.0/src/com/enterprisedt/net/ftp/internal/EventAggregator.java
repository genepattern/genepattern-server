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
 *    $Log: EventAggregator.java,v $
 *    Revision 1.2  2008-06-18 23:42:25  hans
 *    Fixed bug where arguments were swapped in call to EventListener.bytesTransferred
 *
 *    Revision 1.1  2008-05-22 04:20:55  bruceb
 *    moved stuff to internal etc
 *
 *    Revision 1.2  2008-03-13 00:24:05  bruceb
 *    added connId to params
 *
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp.internal;

import com.enterprisedt.net.ftp.EventListener;
import com.enterprisedt.net.ftp.FTPMessageListener;
import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.net.ftp.FTPProgressMonitorEx;
import com.enterprisedt.net.ftp.TransferDirection;


/**
 *  Implements the legacy listener interfaces and aggregates them into
 *  the one EventListener interface
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.2 $
 */
public class EventAggregator implements FTPMessageListener, FTPProgressMonitor, FTPProgressMonitorEx {

    private EventListener eventListener; 
    
    private String connId;
    private String remoteFile;
    
    /**
     * @param eventListener
     */
    public EventAggregator(EventListener eventListener) {
        this(null, eventListener);
    }
    
    /**
     * @param eventListener
     */
    public EventAggregator(String connId, EventListener eventListener) {
        this.connId = connId;
        this.eventListener = eventListener;
    }
    
    /**
     * Set the connection id 
     * 
     * @param connId  connection id
     */
    public void setConnId(String connId) {
        this.connId = connId;
    }

    public void logCommand(String cmd) {
        if (eventListener != null)
            eventListener.commandSent(connId, cmd);
    }

    public void logReply(String reply) {
        if (eventListener != null)
            eventListener.replyReceived(connId, reply);
        
    }
    
    public void bytesTransferred(long count) {
        if (eventListener != null)
            eventListener.bytesTransferred(connId, remoteFile, count);        
    }

    public void transferComplete(TransferDirection direction, String remoteFile) {
        if (eventListener != null) {
            if (direction.equals(TransferDirection.DOWNLOAD))
                eventListener.downloadCompleted(connId, remoteFile);
            else if (direction.equals(TransferDirection.UPLOAD))
                eventListener.uploadCompleted(connId, remoteFile);
        }
    }

    public void transferStarted(TransferDirection direction, String remoteFile) {
        this.remoteFile = remoteFile;
        if (eventListener != null) {
            if (direction.equals(TransferDirection.DOWNLOAD))
                eventListener.downloadStarted(connId, remoteFile);
            else if (direction.equals(TransferDirection.UPLOAD))
                eventListener.uploadStarted(connId, remoteFile);
        }            
    }

   
}

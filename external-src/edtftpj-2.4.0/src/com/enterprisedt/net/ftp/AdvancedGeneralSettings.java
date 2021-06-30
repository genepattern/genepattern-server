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
 *    $Log: AdvancedGeneralSettings.java,v $
 *    Revision 1.5  2012/11/30 05:40:30  bruceb
 *    flag for filelocking enabled or not
 *
 *    Revision 1.4  2008/05/23 02:45:41  bruceb
 *    moved context
 *
 *    Revision 1.3  2008-05-22 04:20:55  bruceb
 *    moved stuff to internal etc
 *
 *    Revision 1.2  2008-03-31 04:30:04  bruceb
 *    fix comment
 *
 *    Revision 1.1  2008-03-31 00:16:23  bruceb
 *    advanced settings rejig
 *
 *    Revision 1.2  2007-12-20 00:40:16  bruceb
 *    autologin
 *
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp;

import com.enterprisedt.net.ftp.internal.ConnectionContext;


/**
 *  Holds advanced configuration options that are independent of protocol (relevant
 *  for <b>any</b> file transfer protocol, not just FTP). These options must be set prior 
 *  to establishing connections, otherwise they have no effect until a new connection 
 *  is made.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.5 $
 */
public class AdvancedGeneralSettings {

    private ConnectionContext context;
    
    /**
     * Constructor
     * 
     * @param context  context that settings are kept in
     */
    AdvancedGeneralSettings(ConnectionContext context) {
        this.context = context;
    }
    
    /**
     * Determine if auto login is switched on
     * 
     * @return true if auto login
     */
    public boolean isAutoLogin() {
        return context.isAutoLogin();
    }

    /**
     * Set the autoLogin flag
     * 
     * @param autoLogin   true if logging in automatically
     */
    public void setAutoLogin(boolean autoLogin) {
        context.setAutoLogin(autoLogin);
    }
        
    /**
     * Listen on all interfaces for active mode transfers (the default).
     * 
     * @param listenOnAll   true if listen on all interfaces, false to listen on the control interface
     */
    public void setListenOnAllInterfaces(boolean listenOnAll) {
        context.setListenOnAllInterfaces(listenOnAll);
    }
    
    /**
     * Are we listening on all interfaces in active mode, which is the default?
     * 
     * @return true if listening on all interfaces, false if listening just on the control interface
     */
    public boolean isListenOnAllInterfaces() {
        return context.isListenOnAllInterfaces();
    }
    
    /**
     * If true, delete partially written files when exceptions are thrown
     * during a download
     * 
     * @return true if delete local file on error
     */
    public boolean isDeleteOnFailure() {
        return context.isDeleteOnFailure();
    }

    /**
     * Switch on or off the automatic deletion of partially written files 
     * that are left when an exception is thrown during a download
     * 
     * @param deleteOnFailure  true if delete when a failure occurs
     */
    public void setDeleteOnFailure(boolean deleteOnFailure) {
        context.setDeleteOnFailure(deleteOnFailure);
    }
    
    /**
     * Get the encoding used for the control channel
     * 
     * @return Returns the current controlEncoding.
     */
    public String getControlEncoding() {
        return context.getControlEncoding();
    }
    
    /**
     * Set the control channel encoding. 
     * 
     * @param controlEncoding The controlEncoding to set, which is the name of a Charset
     */
    public void setControlEncoding(String controlEncoding) {
        context.setControlEncoding(controlEncoding);
    }
    
    /**
     * Set the size of the data buffers used in reading and writing to the server
     * 
     * @param size  new size of buffer in bytes
     */
    public void setTransferBufferSize(int size) {
        context.setTransferBufferSize(size);
    }
    
    /**
     * Get the size of the data buffers used in reading and writing to the server
     * 
     * @return  transfer buffer size
     */
    public int getTransferBufferSize() {
        return context.getTransferBufferSize();
    }
    
    /**
     * Get the interval used for progress notification of transfers.
     * 
     * @return number of bytes between each notification.
     */
    public int getTransferNotifyInterval() {
        return context.getTransferNotifyInterval();
    }

    /**
     * Set the interval used for progress notification of transfers.
     * 
     * @param notifyInterval  number of bytes between each notification
     */
    public void setTransferNotifyInterval(int notifyInterval) {
        context.setTransferNotifyInterval(notifyInterval);
    }
    
    /**
     * Set file locking to enabled or disabled. When downloading files, by default
     * the local file is locked for exclusive writing to prevent other processes
     * corrupting it. Sometimes this needs to be disabled, e.g. tryLock() fails on
     * NFS drives in versions of Java prior to 7.
     * 
     * @param lockingEnabled true to enable locking, false to disable
     */
    public void setFileLockingEnabled(boolean lockingEnabled) {
        context.setFileLockingEnabled(lockingEnabled);
    }
    
    /**
     * Determine if file locking on local downloaded files is being used or not. Default is true.
     * 
     * @return true if file locking is enabled, false otherwise
     */
    public boolean getFileLockingEnabled() {
        return context.getFileLockingEnabled();
    }

}

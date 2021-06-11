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
 *    $Log: ConnectionContext.java,v $
 *    Revision 1.5  2012/11/30 05:40:30  bruceb
 *    flag for filelocking enabled or not
 *
 *    Revision 1.4  2012/10/24 05:15:30  bruceb
 *    get/setNetworkBufferSize()
 *
 *    Revision 1.3  2011-05-20 04:21:23  bruceb
 *    add support for ACCT
 *
 *    Revision 1.2  2008-09-18 06:55:08  bruceb
 *    retry settings
 *
 *    Revision 1.1  2008-05-23 02:45:41  bruceb
 *    moved context
 *
 *    Revision 1.3  2008-05-22 04:20:55  bruceb
 *    moved stuff to internal etc
 *
 *    Revision 1.2  2007-12-20 00:40:16  bruceb
 *    autologin
 *
 *    Revision 1.1  2007-12-18 07:52:06  bruceb
 *    2.0 changes
 *
 *
 */
package com.enterprisedt.net.ftp.internal;

import java.util.Locale;

import com.enterprisedt.net.ftp.DirectoryEmptyStrings;
import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPControlSocket;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileNotFoundStrings;
import com.enterprisedt.net.ftp.TransferCompleteStrings;



/**
 *  Holds various parameters pertaining to the context of the connection. Used
 *  internally.
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.5 $
 */
public class ConnectionContext implements Cloneable {

    private String remoteHost;
    
    private String controlEncoding = FTPClient.DEFAULT_ENCODING;
    
    private int remotePort = FTPControlSocket.CONTROL_PORT;
    
    private int timeout = FTPClient.DEFAULT_TIMEOUT;
    
    private int networkBufferSize = FTPClient.DEFAULT_TCP_BUFFER_SIZE;
    
    private int transferNotifyInterval = FTPClient.DEFAULT_MONITOR_INTERVAL;
    
    private int retryCount = FTPClient.DEFAULT_RETRY_COUNT;
    
    private int retryDelay = FTPClient.DEFAULT_RETRY_DELAY;
    
    /**
     * Size of transfer buffers
     */
    protected int transferBufferSize = FTPClient.DEFAULT_BUFFER_SIZE;
    
    private String username;
    
    private String password;
    
    private String accountDetails;
        
    private FTPTransferType transferType = FTPTransferType.BINARY;
    
    private FTPConnectMode connectMode = FTPConnectMode.ACTIVE;
        
    private Locale[] parserLocales = FTPClient.DEFAULT_LISTING_LOCALES;
    
    private int lowPort = -1;
    
    private int highPort = -1;
    
    private boolean fileLockingEnabled = true;
    
    /**
     * Use strict return codes if true
     */
    private boolean strictReturnCodes = false;
    
    /**
     * If true, uses the original host IP if an internal IP address
     * is returned by the server in PASV mode
     */
    private boolean autoPassiveIPSubstitution = true;
    
    /**
     * Delete partial files on transfer failure?
     */
    private boolean deleteOnFailure = true;
    
    /**
     * IP address to force to use in active mode
     */
    private String activeIP = null;
    
    /**
     * If true, filetypes are autodetected and transfer mode changed to binary/ASCII as 
     * required
     */
    private boolean detectContentType = false;
    
    /**
     * Listen to all interfaces in active mode
     */
    private boolean listenOnAllInterfaces = true;
    
    /** 
     * No explicit login required after connect
     */
    private boolean autoLogin = true;
    
    /**
     * Matcher for directory empty
     */
    private DirectoryEmptyStrings dirEmptyStrings = new DirectoryEmptyStrings();
    
    /**
     * Matcher for transfer complete
     */
    private TransferCompleteStrings transferCompleteStrings = new TransferCompleteStrings();
    
    /**
     * Matcher for permission denied
     */
    private FileNotFoundStrings fileNotFoundStrings = new FileNotFoundStrings();

    
    public ConnectionContext() {}
    
    public Object clone() {
        // no mutable fields so just grab a copy
        try {
            return super.clone();
        } catch (CloneNotSupportedException ignore) {
        }
        return null;
    }
    
    /**
     * Get the retry count for retrying file transfers. Default
     * is 3 attempts.
     * 
     * @return number of times a transfer is retried
     */
    public synchronized int getRetryCount() {
        return retryCount;
    }

    /**
     * Set the retry count for retrying file transfers.
     * 
     * @param retryCount    new retry count
     */
    public synchronized void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * Get the retry delay between retry attempts, in milliseconds
     * 
     * @return  retry delay in milliseconds
     */
    public synchronized int getRetryDelay() {
        return retryDelay;
    }

    /**
     * Set the retry delay between retry attempts, in milliseconds
     * 
     * @param  new retry delay in milliseconds
     */
    public synchronized void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * Determine if auto login is switched on
     * 
     * @return true if auto login
     */
    public synchronized boolean isAutoLogin() {
        return autoLogin;
    }


    /**
     * Set the autoLogin flag
     * 
     * @param autoLogin   true if logging in automatically
     */
    public synchronized void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    /**
     * Set autodetect of filetypes on or off. If on, the transfer mode is
     * switched from ASCII to binary and vice versa depending on the extension
     * of the file. After the transfer, the mode is always returned to what it
     * was before the transfer was performed. The default is off.
     * 
     * If the filetype is unknown, the transfer mode is unchanged
     * 
     * @param detectContentType    true if detecting transfer mode, false if not
     */
    public synchronized void setDetectContentType(boolean detectContentType) {
        this.detectContentType = detectContentType;
    }
    
    /**
     * Get the detect content type flag
     * 
     * @return true if we are detecting binary and ASCII transfers from the file type
     */
    public synchronized boolean getDetectContentType() {
        return detectContentType;
    }
    
    /**
     * Set file locking to enabled or disabled. When downloading files, by default
     * the local file is locked for exclusive writing to prevent other processes
     * corrupting it. Sometimes this needs to be disabled, e.g. tryLock() fails on
     * NFS drives in versions of Java prior to 7.
     * 
     * @param lockingEnabled true to enable locking, false to disable
     */
    public synchronized void setFileLockingEnabled(boolean lockingEnabled) {
        this.fileLockingEnabled = lockingEnabled;
    }
    
    /**
     * Determine if file locking on local downloaded files is being used or not. Default is true.
     * 
     * @return true if file locking is enabled, false otherwise
     */
    public synchronized boolean getFileLockingEnabled() {
        return fileLockingEnabled;
    }
    
    
    /**
     * We can force PORT to send a fixed IP address, which can be useful with certain
     * NAT configurations. 
     * 
     * @param activeIP     IP address to force, in 192.168.1.0 form or in IPV6 form, e.g.
     *                            1080::8:800:200C:417A
     */
    public synchronized void setActiveIPAddress(String activeIP) {
        
        this.activeIP = activeIP;
    }
    
    /**
     * The active IP address being used, or null if not used
     * @return IP address as a string or null
     */
    public synchronized String getActiveIPAddress() {
        return activeIP;
    }
    
    /**
     * Force a certain range of ports to be used in active mode. This is
     * generally so that a port range can be configured in a firewall. Note
     * that if lowest == highest, a single port will be used. This works well
     * for uploads, but downloads generally require multiple ports, as most
     * servers fail to create a connection repeatedly for the same port.
     * 
     * @param lowest     Lower limit of range.
     * @param highest    Upper limit of range.
     */
    public synchronized void setActivePortRange(int lowest, int highest) {
        this.lowPort = lowest;
        this.highPort = highest;
    }
    
    /**
     * Get the lower limit of the port range for active mode.
     * 
     * @return lower limit, or -1 if not set
     */
    public synchronized int getActiveLowPort() {
        return lowPort;
    }

    /**
     * Get the upper limit of the port range for active mode.
     * 
     * @return upper limit, or -1 if not set
     */
    public synchronized int getActiveHighPort() {
        return highPort;
    }
    
    /**
     * Set strict checking of FTP return codes. If strict 
     * checking is on (the default) code must exactly match the expected 
     * code. If strict checking is off, only the first digit must match.
     * 
     * @param strict    true for strict checking, false for loose checking
     */
    public synchronized void setStrictReturnCodes(boolean strict) {
        this.strictReturnCodes = strict;
    }
    
    /**
     * Determine if strict checking of return codes is switched on. If it is 
     * (the default), all return codes must exactly match the expected code.  
     * If strict checking is off, only the first digit must match.
     * 
     * @return  true if strict return code checking, false if non-strict.
     */
    public synchronized boolean isStrictReturnCodes() {
        return strictReturnCodes;
    }
    
    /**
     * Is automatic substitution of the remote host IP set to
     * be on for passive mode connections?
     * 
     * @return true if set on, false otherwise
     */
    public synchronized boolean isAutoPassiveIPSubstitution() {
        return autoPassiveIPSubstitution;
    }

    /**
     * Set automatic substitution of the remote host IP on if
     * in passive mode
     * 
     * @param autoPassiveIPSubstitution true if set to on, false otherwise
     */
    public synchronized void setAutoPassiveIPSubstitution(boolean autoPassiveIPSubstitution) {
        this.autoPassiveIPSubstitution = autoPassiveIPSubstitution;
    }
    
    /**
     * Listen on all interfaces for active mode transfers (the default).
     * 
     * @param listenOnAll   true if listen on all interfaces, false to listen on the control interface
     */
    public synchronized void setListenOnAllInterfaces(boolean listenOnAll) {
        listenOnAllInterfaces = listenOnAll;
    }
    
    /**
     * Are we listening on all interfaces in active mode, which is the default?
     * 
     * @return true if listening on all interfaces, false if listening just on the control interface
     */
    public synchronized boolean isListenOnAllInterfaces() {
        return listenOnAllInterfaces;
    }
    
    /**
     * If true, delete partially written files when exceptions are thrown
     * during a download
     * 
     * @return true if delete local file on error
     */
    public synchronized boolean isDeleteOnFailure() {
        return deleteOnFailure;
    }

    /**
     * Switch on or off the automatic deletion of partially written files 
     * that are left when an exception is thrown during a download
     * 
     * @param deleteOnFailure  true if delete when a failure occurs
     */
    public synchronized void setDeleteOnFailure(boolean deleteOnFailure) {
        this.deleteOnFailure = deleteOnFailure;
    }
    
    public synchronized FTPConnectMode getConnectMode() {
        return connectMode;
    }

    public synchronized void setConnectMode(FTPConnectMode connectMode) {
        this.connectMode = connectMode;
    }

    public synchronized void setPassword(String password) {
        this.password = password;
    }

    public synchronized void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public synchronized void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public synchronized void setUserName(String username) {
        this.username = username;
    }
    
    public synchronized void setAccountDetails(String accountDetails) {
        this.accountDetails = accountDetails;
    }
 
    public synchronized void setContentType(FTPTransferType transferType) {
        this.transferType = transferType;
    }
        
    public synchronized FTPTransferType getContentType() {
        return transferType;
    }

    public synchronized String getPassword() {
        return password;
    }
    
    public synchronized String getAccountDetails() {
        return accountDetails;
    }

    public synchronized String getRemoteHost() {
        return remoteHost;
    }

    public synchronized int getRemotePort() {
        return remotePort;
    }
    
    /**
     * Set the list of locales to be tried for date parsing of dir listings
     * 
     * @param locales    locales to use
     */
    public synchronized void setParserLocales(Locale[] locales) {
        this.parserLocales = locales;
    }    
    
    public synchronized int getTimeout() {
        return timeout;
    }

    public synchronized void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    /**
     * Get the size of the network buffers (SO_SNDBUF
     * and SO_RCVBUF).
     * 
     * @return network buffer size
     */
    public synchronized int getNetworkBufferSize() {
        return networkBufferSize;
    }
    
    /**
     * Set the size of the network buffers (SO_SNDBUF
     * and SO_RCVBUF).
     * 
     * @param networkBufferSize  new buffer size to set
     */
    public synchronized void setNetworkBufferSize(int networkBufferSize) {
        this.networkBufferSize = networkBufferSize;
    }

    public synchronized String getUserName() {
        return username;
    }

    public synchronized Locale[] getParserLocales() {
    	return parserLocales;
    }
    
    /**
     * Get the encoding used for the control connection
     * 
     * @return Returns the current controlEncoding.
     */
    public synchronized String getControlEncoding() {
        return controlEncoding;
    }
    
    /**
     * Set the encoding used for control channel messages
     * 
     * @param controlEncoding The controlEncoding to set, which is the name of a Charset
     */
    public synchronized void setControlEncoding(String controlEncoding) {
         this.controlEncoding = controlEncoding;
    }
    
    /**
     * Set the size of the buffers used in writing to and reading from
     * the data sockets
     * 
     * @param size  new size of buffer in bytes
     */
    public void setTransferBufferSize(int size) {
        transferBufferSize = size;
    }
    
    /**
     * Get the size of the buffers used in writing to and reading from
     * the data sockets
     * 
     * @return  transfer buffer size
     */
    public int getTransferBufferSize() {
        return transferBufferSize;
    }
    
    /**
     * Get the interval used for progress notification of transfers.
     * 
     * @return number of bytes between each notification.
     */
    public synchronized int getTransferNotifyInterval() {
        return transferNotifyInterval;
    }

    /**
     * Set the interval used for progress notification of transfers.
     * 
     * @param notifyInterval  number of bytes between each notification
     */
    public synchronized void setTransferNotifyInterval(int notifyInterval) {
        this.transferNotifyInterval = notifyInterval;
    }
    
    /**
     * Get class that holds fragments of server messages that indicate a file was 
     * not found. New messages can be added.
     * <p>
     * The fragments are used when it is necessary to examine the message
     * returned by a server to see if it is saying a file was not found. 
     * If an FTP server is returning a different message that still clearly 
     * indicates a file was not found, use this property to add a new server 
     * fragment to the repository via the add method. It would be helpful to
     * email support at enterprisedt dot com to inform us of the message so
     * it can be added to the next build.
     * 
     * @return  messages class
     */
    public synchronized FileNotFoundStrings getFileNotFoundMessages() {
        return fileNotFoundStrings;
    }
    
    /**
     * Get class that holds fragments of server messages that indicate a transfer completed. 
     * New messages can be added.
     * <p>
     * The fragments are used when it is necessary to examine the message
     * returned by a server to see if it is saying a transfer completed. 
     * If an FTP server is returning a different message that still clearly 
     * indicates a transfer failed, use this property to add a new server 
     * fragment to the repository via the add method. It would be helpful to
     * email support at enterprisedt dot com to inform us of the message so
     * it can be added to the next build.
     * 
     * @return  messages class
     */
    public synchronized TransferCompleteStrings getTransferCompleteMessages() {
        return transferCompleteStrings;
    }
    
    /**
     * Get class that holds fragments of server messages that indicate a  
     * directory is empty. New messages can be added.
     * <p>
     * The fragments are used when it is necessary to examine the message
     * returned by a server to see if it is saying a directory is empty. 
     * If an FTP server is returning a different message that still clearly 
     * indicates a directory is empty, use this property to add a new server 
     * fragment to the repository via the add method. It would be helpful to
     * email support at enterprisedt dot com to inform us of the message so
     * it can be added to the next build.
     * 
     * @return  messages class
     */
    public synchronized DirectoryEmptyStrings getDirectoryEmptyMessages() {
        return dirEmptyStrings;
    }
}

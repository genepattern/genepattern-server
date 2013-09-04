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
 *    $Log: FileTransferClient.java,v $
 *    Revision 1.24  2012/11/30 05:40:30  bruceb
 *    flag for filelocking enabled or not
 *
 *    Revision 1.23  2012/10/24 05:16:10  bruceb
 *    get/setNetworkBufferSize()
 *
 *    Revision 1.22  2012/07/18 02:20:01  hans
 *    Added comments about dirDetails and dir returning empty arrays for non-existent dirs on most servers.
 *
 *    Revision 1.21  2012/02/08 06:20:25  bruceb
 *    clean imports
 *
 *    Revision 1.20  2011-03-28 03:41:41  hans
 *    Made logger static.
 *
 *    Revision 1.19  2010-05-29 07:10:36  hans
 *    Fixed doco.
 *
 *    Revision 1.18  2010-04-26 15:55:46  bruceb
 *    add new dirDetails method with callback
 *
 *    Revision 1.17  2009-06-15 04:46:40  hans
 *    Changed downloadURL to downloadURLStream and added downloadURLFile.  Likewise for uploads.
 *
 *    Revision 1.16  2009-06-15 03:36:04  hans
 *    Added downloadURL and uploadURL.
 *
 *    Revision 1.15  2009-03-20 03:54:37  bruceb
 *    added getLastReply()
 *
 *    Revision 1.14  2008-09-18 06:55:34  bruceb
 *    retry settings
 *
 *    Revision 1.13  2008-06-03 03:20:15  bruceb
 *    cleanup
 *
 *    Revision 1.12  2008-06-03 03:17:59  bruceb
 *    clear stats on connect
 *
 *    Revision 1.11  2008-06-02 06:50:17  bruceb
 *    fix bug re messages not being set
 *
 *    Revision 1.10  2008-05-23 02:45:41  bruceb
 *    moved context
 *
 *    Revision 1.9  2008-05-22 04:20:55  bruceb
 *    moved stuff to internal etc
 *
 *    Revision 1.8  2008-05-02 07:41:30  bruceb
 *    setModTime added
 *
 *    Revision 1.7  2008-03-31 02:16:04  bruceb
 *    fix event listener assignment
 *
 *    Revision 1.6  2008-03-31 01:18:02  bruceb
 *    fix isconnected bug
 *
 *    Revision 1.5  2008-03-31 00:16:23  bruceb
 *    advanced settings rejig
 *
 *    Revision 1.4  2008-03-13 00:22:53  bruceb
 *    various tweaks for j/pro
 *
 *    Revision 1.3  2008-01-09 03:54:21  bruceb
 *    executeCommand() now returns reply code
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

import com.enterprisedt.net.ftp.internal.ConnectionContext;
import com.enterprisedt.net.ftp.internal.EventAggregator;
import com.enterprisedt.util.debug.Logger;

/**
 * Easy to use FTP client that is thread safe and provides true FTP streams.
 * This class is intended to replace FTPClient, which will eventually be
 * deprecated. 
 * 
 * @author Bruce Blackshaw
 * @version $Revision: 1.24 $
 */
public class FileTransferClient implements FileTransferClientInterface {
    
    private static Logger log = Logger.getLogger("FileTransferClient");
    
    /**
     * Context for the client that is the starting point for all 
     * new tasks. If we have a change directory task, when it is completed
     * it will update this master context and the updated context will be used
     * for all future tasks
     */
    protected ConnectionContext masterContext = new ConnectionContext();
    
    protected EventAggregator eventAggregator = null;
        
    /**
     * Event listeners
     */
    protected EventListener listener;
    
    /**
     * Instance of FTPClient 
     */
    private FTPClient ftpClient; 
    
    /**
     * Advanced configuration parameters that often aren't used
     */
    private AdvancedFTPSettings advancedFTPSettings;
    
    private AdvancedGeneralSettings advancedSettings;
    
    private FileStatistics statistics;
    
    /**
     * Default constructor
     */
    public FileTransferClient() {
        ftpClient = new FTPClient(); 
        advancedFTPSettings = new AdvancedFTPSettings(masterContext);
        advancedSettings = new AdvancedGeneralSettings(masterContext);
        statistics = new FileStatistics();
        statistics.addClient(ftpClient);
    } 
    
    /**
     * Checks if the client has connected to the server and throws an exception if it hasn't.
     * This is only intended to be used by subclasses
     * 
     * @throws FTPException Thrown if the client has not connected to the server.
     */
    protected void checkConnection(boolean shouldBeConnected) throws FTPException {
        if (shouldBeConnected && !isConnected())
            throw new FTPException("The file transfer client has not yet connected to the server.  "
                    + "The requested action cannot be performed until after a connection has been established.");
        else if (!shouldBeConnected && isConnected())
            throw new FTPException("The file transfer client has already been connected to the server.  "
                    +"The requested action must be performed before a connection is established.");
    }    
        
    /**
     * Is this client currently connected to the server?
     * 
     * @return  true if connected, false otherwise
     */
    public synchronized boolean isConnected() {
        return ftpClient.connected();
    }

    
    /**
     * Returns the IP address or name of the remote host.
     * 
     * @return Returns the remote host.
     */
    public synchronized String getRemoteHost() {
        return masterContext.getRemoteHost();
    }

    /**
     * Set the IP address or name of the remote host
     * 
     * This may only be done if the client is not already connected to the server.
     * 
     * @param remoteHost The IP address or name of the remote host
     * @throws FTPException Thrown if the client is already connected to the server.
     */
    public synchronized void setRemoteHost(String remoteHost) throws FTPException {
        checkConnection(false);
        masterContext.setRemoteHost(remoteHost);
    }
    
    /**
     * Returns the timeout for socket connections. 
     * 
     * @return Returns the connection timeout in milliseconds
     */
    public synchronized int getTimeout() {
        return masterContext.getTimeout();
    }
    
    /** 
     * Set the timeout for socket connections. Can only do this if
     * not already connected.
     * 
     * @param timeout  the timeout to use in milliseconds
     * @throws FTPException Thrown if the client is already connected to the server.
     */
    public synchronized void setTimeout(int timeout) throws FTPException {
        checkConnection(false);
        masterContext.setTimeout(timeout);
    }
    
    /**
     * Get the size of the network buffers (SO_SNDBUF
     * and SO_RCVBUF).
     * 
     * @return network buffer size
     */
    public synchronized int getNetworkBufferSize() {
        return masterContext.getNetworkBufferSize();
    }

    /**
     * Set the size of the network buffers (SO_SNDBUF
     * and SO_RCVBUF).
     * 
     * @param networkBufferSize  new buffer size to set
     * @throws FTPException 
     */
    public synchronized void setNetworkBufferSize(int networkBufferSize) throws FTPException {
        checkConnection(false);
        masterContext.setNetworkBufferSize(networkBufferSize);
    }

    /**
     * Returns the port being connected to on the remote server. 
     * 
     * @return Returns the port being connected to on the remote server. 
     */
    public synchronized int getRemotePort() {
        return masterContext.getRemotePort();
    }
    
    /** 
     * Set the port to connect to on the remote server. Can only do this if
     * not already connected.
     * 
     * @param remotePort The port to use. 
     * @throws FTPException Thrown if the client is already connected to the server.
     */
    public synchronized void setRemotePort(int remotePort) throws FTPException {
        checkConnection(false);
        masterContext.setRemotePort(remotePort);
    }
    
    /**
     * Set the transfer type for all connections, either ASCII or binary. Setting
     * applies to all subsequent transfers that are initiated.
     * 
     * @param type            transfer type
     * @throws FTPException 
     * @throws IOException 
     * @throws FTPException
     */
    public synchronized void setContentType(FTPTransferType type) 
        throws IOException, FTPException {
        masterContext.setContentType(type);
        if (ftpClient != null && ftpClient.connected())
            ftpClient.setType(type);
    }
    
    /**
     * Get the current content type for all connections.
     * 
     * @return  transfer type
     */
    public synchronized FTPTransferType getContentType() {
        return masterContext.getContentType();
    }
    
    /**
     * Set auto detect of filetypes on or off. If on, the transfer mode is
     * switched from ASCII to binary and vice versa depending on the extension
     * of the file. After the transfer, the mode is always returned to what it
     * was before the transfer was performed. The default is off.
     * 
     * If the filetype is unknown, the transfer mode is unchanged
     * 
     * @param detectContentType    true if detecting content type, false if not
     */
    public void setDetectContentType(boolean detectContentType) {
        masterContext.setDetectContentType(detectContentType);
        if (ftpClient != null)
            ftpClient.setDetectTransferMode(detectContentType);
    }
    
    /**
     * Get the detect content type flag
     * 
     * @return true if we are detecting binary and ASCII transfers from the file type
     */
    public boolean isDetectContentType() {
        return masterContext.getDetectContentType();
    }
    
    /**
     * Set the name of the user to log in with. Can only do this if
     * not already connected.
     * 
     * @param userName          user-name to log in with.
     * @throws FTPException
     */
    public synchronized void setUserName(String userName) throws FTPException {
        checkConnection(false);
        masterContext.setUserName(userName);
    }
    
    /**
     * Get the current user password.
     * 
     * @return current user password
     */
    public synchronized String getPassword() {
        return masterContext.getPassword();
    }
    
    /**
     * Set the password of the user to log in with. Can only do this if
     * not already connected.
     * 
     * @param password          password to log in with.
     * @throws FTPException
     */
    public synchronized void setPassword(String password) throws FTPException {
        checkConnection(false);
        masterContext.setPassword(password);
    }
    
    /**
     * Get the current user name.
     * 
     * @return current user name
     */
    public synchronized String getUserName() {
        return masterContext.getUserName();
    }
    
    /**
     * Get the advanced FTP configuration parameters object
     * 
     * @return advanced parameters
     */
    public synchronized AdvancedFTPSettings getAdvancedFTPSettings() {
        return advancedFTPSettings;        
    }
    
    /**
     * Get the advanced general configuration parameters object, for none
     * protocol specific parameters
     * 
     * @return advanced parameters
     */
    public synchronized AdvancedGeneralSettings getAdvancedSettings() {
        return advancedSettings;        
    }
    
    /**
     * Set the event listener for transfer event notification
     * 
     * @param listener  event listener reference
     */
    public synchronized void setEventListener(EventListener listener) {
        this.listener = listener;
        eventAggregator = new EventAggregator(listener);
        if (ftpClient != null) {
            eventAggregator.setConnId(ftpClient.getId());       
            ftpClient.setMessageListener(eventAggregator);
            ftpClient.setProgressMonitor(eventAggregator);
            ftpClient.setProgressMonitorEx(eventAggregator);
        }
    }
    
    /**
     * Make a connection to the FTP server. 
     * 
     * @throws FTPException 
     * @throws IOException 
     */
    public synchronized void connect() throws FTPException, IOException {
        // events
        if (eventAggregator != null) {
            eventAggregator.setConnId(ftpClient.getId());
        
            ftpClient.setMessageListener(eventAggregator);
            ftpClient.setProgressMonitor(eventAggregator);
            ftpClient.setProgressMonitorEx(eventAggregator);
        }
        
        //stats
        statistics.clear();
        
        configureClient();
        log.debug("Configured client");
        ftpClient.connect();
        log.debug("Client connected");
        if (masterContext.isAutoLogin()) {
            log.debug("Logging in");
            ftpClient.login(masterContext.getUserName(), masterContext.getPassword());
            log.debug("Logged in");
            configureTransferType(masterContext.getContentType());
        }
        else {
            log.debug("Manual login enabled");
        }
    }
    
    /**
     * Perform a manual login using the credentials that have been set. This
     * should only be performed if auto login is disabled - normally connect()
     * performs the login automatically.
     * 
     * @throws FTPException
     * @throws IOException
     */
    public void manualLogin() throws FTPException, IOException {
        checkConnection(true);
        log.debug("Logging in");
        ftpClient.login(masterContext.getUserName(), masterContext.getPassword());
        log.debug("Logged in");
        configureTransferType(masterContext.getContentType());
    }
    
    /**
     * Apply the master context's settings to the client
     * 
     * @throws IOException
     * @throws FTPException
     */
    private void configureClient() throws IOException, FTPException {
        ftpClient.setRemoteHost(masterContext.getRemoteHost());
        ftpClient.setRemotePort(masterContext.getRemotePort());
        ftpClient.setTimeout(masterContext.getTimeout());  
        ftpClient.setNetworkBufferSize(masterContext.getNetworkBufferSize());
        ftpClient.setRetryCount(masterContext.getRetryCount());
        ftpClient.setRetryDelay(masterContext.getRetryDelay());
        ftpClient.setControlEncoding(masterContext.getControlEncoding());
        ftpClient.setStrictReturnCodes(masterContext.isStrictReturnCodes());
        ftpClient.setDetectTransferMode(masterContext.getDetectContentType());
        ftpClient.setFileLockingEnabled(masterContext.getFileLockingEnabled());
        ftpClient.setConnectMode(masterContext.getConnectMode());
        ftpClient.setParserLocales(masterContext.getParserLocales());
        ftpClient.setAutoPassiveIPSubstitution(masterContext.isAutoPassiveIPSubstitution());
        ftpClient.setDeleteOnFailure(masterContext.isDeleteOnFailure());
        ftpClient.setActiveIPAddress(masterContext.getActiveIPAddress());
        ftpClient.setMonitorInterval(masterContext.getTransferNotifyInterval());
        ftpClient.setTransferBufferSize(masterContext.getTransferBufferSize());
        ftpClient.setFileNotFoundMessages(masterContext.getFileNotFoundMessages());
        ftpClient.setDirectoryEmptyMessages(masterContext.getDirectoryEmptyMessages());
        ftpClient.setTransferCompleteMessages(masterContext.getTransferCompleteMessages());
        if (masterContext.getActiveHighPort() >= 0 && masterContext.getActiveLowPort() >= 0)
            ftpClient.setActivePortRange(masterContext.getActiveLowPort(), masterContext.getActiveHighPort());
       
    }
    
    
    private void configureTransferType(FTPTransferType type) 
        throws IOException, FTPException {
        ftpClient.setDetectTransferMode(masterContext.getDetectContentType());
        ftpClient.setType(type);
    }
    
    private void checkTransferSettings() throws FTPException {
        
        if (ftpClient.getDetectTransferMode() != masterContext.getDetectContentType())
            ftpClient.setDetectTransferMode(masterContext.getDetectContentType());
        
        if (ftpClient.isStrictReturnCodes() != masterContext.isStrictReturnCodes())
            ftpClient.setStrictReturnCodes(masterContext.isStrictReturnCodes());
        
        if (!ftpClient.getConnectMode().equals(masterContext.getConnectMode()))
            ftpClient.setConnectMode(masterContext.getConnectMode());        
        
        if (ftpClient.isAutoPassiveIPSubstitution() != masterContext.isAutoPassiveIPSubstitution())
            ftpClient.setAutoPassiveIPSubstitution(masterContext.isAutoPassiveIPSubstitution());
        
        if (ftpClient.isDeleteOnFailure() != masterContext.isDeleteOnFailure())
            ftpClient.setDeleteOnFailure(masterContext.isDeleteOnFailure());
        
        if (ftpClient.getActiveIPAddress() != masterContext.getActiveIPAddress())
            ftpClient.setActiveIPAddress(masterContext.getActiveIPAddress());
        
        if (ftpClient.getTransferBufferSize() != masterContext.getTransferBufferSize())
            ftpClient.setTransferBufferSize(masterContext.getTransferBufferSize());
        
        if (ftpClient.getMonitorInterval() != masterContext.getTransferNotifyInterval())
            ftpClient.setMonitorInterval(masterContext.getTransferNotifyInterval());  
        
        if (masterContext.getActiveHighPort() != ftpClient.getActiveHighPort() ||
            masterContext.getActiveLowPort() != ftpClient.getActiveLowPort())
            ftpClient.setActivePortRange(masterContext.getActiveLowPort(), masterContext.getActiveHighPort());
    }
    
    private void checkListingSettings() throws FTPException {
        ftpClient.setParserLocales(masterContext.getParserLocales());
        checkTransferSettings();
    }

	/**
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws FTPException
	 */
	protected static void prepareURLTransfer(URL url, FileTransferClientInterface client) 
		throws IOException, FTPException {
		if (!url.getProtocol().equals("ftp"))
			throw new MalformedURLException("Unsupported protocol: "
					+ url.getProtocol());
    	client.setRemoteHost(url.getHost());
    	if (url.getPort()>0)
    		client.setRemotePort(url.getPort());
    	String userInfo = url.getUserInfo();
    	String userName = "anonymous", password = "";
    	if (userInfo!=null) {
    		int colonPos = userInfo.indexOf(':'); 
	    	if (colonPos>=0) {
	    		userName = userInfo.substring(0, colonPos);
	    		if (colonPos+1<userInfo.length()-1)
	    			password = userInfo.substring(colonPos+1);
	    	} else
	    		userName = userInfo;
    	}
    	client.setUserName(userName);
    	client.setPassword(password);
    	client.setDetectContentType(true);
    	if (url.getQuery()!=null) {
    		String query = url.getQuery();
    		String TYPE = "type=";
    		int typePos = query.indexOf(TYPE); 
    		if (typePos>=0 && query.length()>typePos+TYPE.length()) {
    			char typeChar = query.toUpperCase().charAt(typePos+TYPE.length());
    			if (typeChar==FTPTransferType.BINARY_CHAR.charAt(0)) {
  					client.setContentType(FTPTransferType.BINARY);
  			    	client.setDetectContentType(true);
    			} else if (typeChar==FTPTransferType.ASCII_CHAR.charAt(0)) {
  					client.setContentType(FTPTransferType.ASCII);
  			    	client.setDetectContentType(true);
    			} else
        			throw new MalformedURLException("Unknown type: " + query.substring(typePos));
    		}
    	}
	}

    
    /**
     * Get statistics on file transfers and deletions. 
     * 
     * @return FTPStatistics
     */
    public synchronized FileStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Request that the remote server execute the literal command supplied. In
     * FTP, this is the equivalent of 'quote'. It could be used to send a SITE
     * command to the server, e.g. "SITE recfm=FB lrecl=180 blksize=5400". 
     *  
     * @param command   command string
     * @return result string returned by server
     * @throws FTPException
     * @throws IOException
     */
    public synchronized String executeCommand(String command) 
        throws FTPException, IOException {
        return ftpClient.quote(command);
    }
    
    /**
     * Cancel current transfer if underway
     */
    public void cancelAllTransfers() {

        log.debug("cancelAllTransfers() called");
        ftpClient.cancelTransfer();
    }
    
    
    /**
     *  Gets the last reply from the server, whether valid or not
     *
     *  @return  reply object encapsulating last server response
     */
    public FTPReply getLastReply() {
        return ftpClient.getLastReply();
    }

    
    /**
     * Get a string that represents the remote system that the client is logged
     * into.
     * 
     * @return system string
     * @throws FTPException
     * @throws IOException
     */
    public synchronized String getSystemType() 
        throws FTPException, IOException {
        return ftpClient.system();
    }
    
    /**
     * List the names of files and directories in the current directory on the FTP server.
	 * Most servers return an empty directory listing if a directory
	 * does not exist, and this is also what this method does.
	 * To establish whether or not a directory exists, try changing into it.
     * 
     * @return String[]
     * @throws FTPException, IOException 
     */
    public synchronized String[] directoryNameList() 
        throws FTPException, IOException {
        return directoryNameList("", false);
    }

    
    /**
     * List the names of files and directories of a directory on the FTP server.
	 * Most servers return an empty directory listing if a directory
	 * does not exist, and this is also what this method does.
	 * To establish whether or not a directory exists, try changing into it.
     * 
     * @param directoryName    name of the directory (generally not a path). Some 
     *                          servers will accept a wildcard.
     * @param isLongListing    true if the listing is a long format listing
     * @return String[]
     * @throws FTPException, IOException 
     */
    public synchronized String[] directoryNameList(String directoryName, boolean isLongListing) 
        throws FTPException, IOException {
        checkListingSettings();
        return ftpClient.dir(directoryName, isLongListing);
    }
    
    /**
     * List the current directory on the FTP server.
	 * Most servers return an empty directory listing if a directory
	 * does not exist, and this is also what this method does.
	 * To establish whether or not a directory exists, try changing into it.
     * 
     * @throws FTPException, IOException 
     * @throws ParseException 
     */
    public synchronized FTPFile[] directoryList() 
        throws FTPException, IOException, ParseException {
        return directoryList("");
    }
   
    /**
     * List a directory on the FTP server.
	 * Most servers return an empty directory listing if a directory
	 * does not exist, and this is also what this method does.
	 * To establish whether or not a directory exists, try changing into it.
     * 
     * @param directoryName    name of the directory (generally not a path). Some 
     *                          servers will accept a wildcard.
     * @throws FTPException, IOException 
     * @throws ParseException 
     */
    public synchronized FTPFile[] directoryList(String directoryName) 
        throws FTPException, IOException, ParseException {
        checkListingSettings();
        return ftpClient.dirDetails(directoryName);
    }
    
    
    /**
     * List a directory on the FTP server.
	 * Most servers return an empty directory listing if a directory
	 * does not exist, and this is also what this method does.
	 * To establish whether or not a directory exists, try changing into it.
     * 
     * @param directoryName    name of the directory (generally not a path)
     * @throws FTPException, IOException 
     */
    public synchronized void directoryList(String directoryName, DirectoryListCallback lister) 
        throws FTPException, IOException, ParseException {
        checkListingSettings();
        ftpClient.dirDetails(directoryName, lister);
    }

    
    /**
     * Download a file from the FTP server into a byte array.
     * 
     * @param remoteFileName   name of the remote file to be downloaded
     * @throws FTPException 
     */
    public synchronized byte[] downloadByteArray(String remoteFileName) throws FTPException, IOException {
        checkTransferSettings();
        return ftpClient.get(remoteFileName); 
    }
    
    /**
     * Download a file from the FTP server .
     * 
     * @param localFileName    name (or full path) of the local file to be downloaded to
     * @param remoteFileName   name of the remote file to be downloaded
     * @throws FTPException 
     */
    public synchronized void downloadFile(String localFileName, String remoteFileName) throws FTPException, IOException {
        downloadFile(localFileName, remoteFileName, WriteMode.OVERWRITE);
    }
    
    /**
     * Download a file from the FTP server .
     * 
     * @param localFileName    name (or full path) of the local file to be downloaded to
     * @param remoteFileName   name of the remote file to be downloaded
     * @param writeMode        mode in which the file is written to the client machine
     * @throws FTPException 
     */
    public synchronized void downloadFile(String localFileName, String remoteFileName, WriteMode writeMode) 
        throws FTPException, IOException {
        checkTransferSettings();
        if (writeMode.equals(WriteMode.RESUME)) {
            ftpClient.resume();
        }
        else if (writeMode.equals(WriteMode.APPEND)) {
            throw new FTPException("Append not permitted for downloads");
        }
        ftpClient.get(localFileName, remoteFileName); 
    }
    
    /**
     * Open an InputStream for the given FTP URL.  The URL must be of the form
     * <code>ftp://[user[:password]@]host[:port]/path</code>.  The stream should be closed when 
     * the transfer is complete.
	 * @param ftpURL The URL must be of the form <code>ftp://[user[:password]@]host[:port]/path</code>.
     * @return FileTransferInputStream for the given URL.  The stream should be closed when 
     * the transfer is complete.
     * @throws MalformedURLException Thrown when an invalid URL is provided.
     * @throws IOException Thrown when an I/O-related error occurs.
     * @throws FTPException Thrown when an FTP-protocol-related error occurs.
     */
    public static FileTransferInputStream downloadURLStream(String ftpURL) 
    	throws MalformedURLException, IOException, FTPException {
    	URL url = new URL(ftpURL);
    	if (!url.getProtocol().equals("ftp"))
    		throw new MalformedURLException("Unsupported protocol: " + url.getProtocol());
    	FileTransferClient client = new FileTransferClient();
		prepareURLTransfer(url, client);
    	client.connect();
		return new AutoCloseFileTransferInputStream(client.downloadStream(url.getPath()), client);
    }

	/**
	 * Downloads the given file to the given FTP URL. The URL must be of the form
	 * <code>ftp://[user[:password]@]host[:port]/path</code>.
	 * @param localFileName Path to local file
	 * @param ftpURL The URL must be of the form <code>ftp://[user[:password]@]host[:port]/path</code>.
	 * @return FileTransferOutputStream for the given URL. The stream should be closed when the transfer is complete.
	 * @throws MalformedURLException Thrown when an invalid URL is provided.
	 * @throws IOException Thrown when an I/O-related error occurs.
	 * @throws FTPException Thrown when an FTP-protocol-related error occurs.
	 */
    public static void downloadURLFile(String localFileName, String ftpURL)
			throws MalformedURLException, IOException, FTPException {
		URL url = new URL(ftpURL);
		FileTransferClient client = new FileTransferClient();
		if (!url.getProtocol().equals("ftp"))
			throw new MalformedURLException("Unsupported protocol: "
					+ url.getProtocol());
		prepareURLTransfer(url, client);
		client.connect();
		try {
			client.downloadFile(localFileName, url.getPath());
		} finally {
			client.disconnect();
		}
	}
    
     /**
     * Download a file from the FTP server as a stream. The close() method <b>must</b>
     * be called on the stream once the stream has been read.
     * 
     * @param remoteFileName   name of the remote file to be downloaded
     * @return InputStream
     * @throws FTPException 
     */
     public synchronized FileTransferInputStream downloadStream(String remoteFileName) 
         throws FTPException, IOException {
         checkTransferSettings();
         return new FTPInputStream(ftpClient, remoteFileName);
    }
     
      /**
       * Upload a file to the FTP server. If a null is supplied as
       * the remoteFileName, the STOU (store unique) FTP feature will be used (if 
       * available) and the FTP server will generate a unique name for the file. 
       * 
       * @param localFileName
       *            name (or full path) of the local file to be downloaded to
       * @param remoteFileName
       *            name of the remote file to be downloaded (or null to generate a unique name)
       * @return name of remote file
       * @throws FTPException 
       */
      public synchronized String uploadFile(String localFileName, String remoteFileName) 
          throws FTPException, IOException {
          return uploadFile(localFileName, remoteFileName, WriteMode.OVERWRITE);
      }
      
      /**
       * Upload a file to the FTP server. If a null is supplied as
       * the remoteFileName, the STOU (store unique) FTP feature will be used (if 
       * available) and the FTP server will generate a unique name for the file. 
       * 
       * @param localFileName
       *            name (or full path) of the local file to be downloaded to
       * @param remoteFileName
       *            name of the remote file to be downloaded (or null to generate a unique name)
       * @param writeMode   mode to write to the remote file with
       * @return name of remote file
       * @throws FTPException 
       */
      public synchronized String uploadFile(String localFileName, String remoteFileName, WriteMode writeMode) 
          throws FTPException, IOException {  
          checkTransferSettings();
          boolean append = false;
          if (writeMode.equals(WriteMode.RESUME)) {
              ftpClient.resume();
          }
          else if (writeMode.equals(WriteMode.APPEND)) {
              append = true;
          }
          return ftpClient.put(localFileName, remoteFileName, append);
      }
      
      /**
       * Upload a file to the FTP server by writing to a stream. The close() method <b>must</b>
       * be called on the stream once the stream has been read.
       * If a null is supplied as
       * the remoteFileName, the STOU (store unique) FTP feature will be used (if 
       * available) and the FTP server will generate a unique name for the file. This
       * name can be obtained afterwards from {@see FileTransferOutputStream#getRemoteFile()}
       * 
       * @param remoteFileName   name of the remote file to be uploaded
       * @return FTPOutputStream
       * @throws FTPException 
       * @throws IOException 
       */
       public synchronized FileTransferOutputStream uploadStream(String remoteFileName) 
           throws FTPException, IOException {
           return uploadStream(remoteFileName, WriteMode.OVERWRITE);
      }

       /**
        * Upload a file to the FTP server by writing to a stream. The close() method *must*
        * be called on the stream once the stream has been read.
        * If a null is supplied as
        * the remoteFileName, the STOU (store unique) FTP feature will be used (if 
        * available) and the FTP server will generate a unique name for the file. This
        * name can be obtained afterwards from {@see FileTransferOutputStream#getRemoteFile()}
        * 
        * @param remoteFileName   name of the remote file to be uploaded
        * @param writeMode        mode for writing to the server (supporting use of append)
        * @return FTPOutputStream
        * @throws FTPException 
        * @throws IOException 
        */
        public synchronized FileTransferOutputStream uploadStream(String remoteFileName, WriteMode writeMode) 
            throws FTPException, IOException {
            checkTransferSettings();
            if (WriteMode.RESUME.equals(writeMode))
                throw new FTPException("Resume not supported for stream uploads");
            boolean append = WriteMode.APPEND.equals(writeMode);
            return new FTPOutputStream(ftpClient, remoteFileName, append);
       }
        
    /**
	 * Open an OutputStream for the given FTP URL. The URL must be of the form
	 * <code>ftp://[user[:password]@]host[:port]/path</code>. The stream should
	 * be closed when the transfer is complete.
	 * @param ftpURL The URL must be of the form <code>ftp://[user[:password]@]host[:port]/path</code>.
	 * @return FileTransferOutputStream for the given URL. The stream should be closed when the transfer is complete.
	 * @throws MalformedURLException Thrown when an invalid URL is provided.
	 * @throws IOException Thrown when an I/O-related error occurs.
	 * @throws FTPException Thrown when an FTP-protocol-related error occurs.
	 */
	public static FileTransferOutputStream uploadURLStream(String ftpURL)
			throws MalformedURLException, IOException, FTPException {
		URL url = new URL(ftpURL);
		FileTransferClient client = new FileTransferClient();
		if (!url.getProtocol().equals("ftp"))
			throw new MalformedURLException("Unsupported protocol: "
					+ url.getProtocol());
		prepareURLTransfer(url, client);
		client.connect();
		return new AutoCloseFileTransferOutputStream(client.uploadStream(url
				.getPath()), client);
	}

	/**
	 * Uploads the given file to the given FTP URL. The URL must be of the form
	 * <code>ftp://[user[:password]@]host[:port]/path</code>.
	 * @param localFileName Path to local file
	 * @param ftpURL The URL must be of the form <code>ftp://[user[:password]@]host[:port]/path</code>.
	 * @return FileTransferOutputStream for the given URL. The stream should be closed when the transfer is complete.
	 * @throws MalformedURLException Thrown when an invalid URL is provided.
	 * @throws IOException Thrown when an I/O-related error occurs.
	 * @throws FTPException Thrown when an FTP-protocol-related error occurs.
	 */
    public static void uploadURLFile(String localFileName, String ftpURL)
			throws MalformedURLException, IOException, FTPException {
		URL url = new URL(ftpURL);
		FileTransferClient client = new FileTransferClient();
		prepareURLTransfer(url, client);
		client.connect();
		try {
			client.uploadFile(localFileName, url.getPath());
		} finally {
			client.disconnect();
		}
	}
        
     /**
      * Get the size of a remote file.
      * 
      * @param remoteFileName   name of remote file
      * @return long
      * @throws FTPException 
      */
     public synchronized long getSize(String remoteFileName) 
         throws FTPException,IOException {
         return ftpClient.size(remoteFileName);
     }
     

    /**
     * Get the modified-time of a remote file.
     * 
     * @param remoteFileName   name of remote file
     * @return Date
     * @throws FTPException 
     */
    public synchronized Date getModifiedTime(String remoteFileName) throws FTPException, IOException {
        return ftpClient.modtime(remoteFileName);
    }
    
    /**
     * Set the modified-time of a remote file. May not be supported by
     * all servers.
     * 
     * @param remoteFileName   name of remote file
     * @param modifiedTime    new modified time
     * @throws FTPException 
     */
    public synchronized void setModifiedTime(String remoteFileName, Date modifiedTime) 
        throws FTPException, IOException {
        ftpClient.setModTime(remoteFileName, modifiedTime);
    }

    
    /**
     * Determine if a remote file exists.
     * 
     * @param remoteFileName   name of remote file
     * @throws FTPException 
     */
    public synchronized boolean exists(String remoteFileName) 
        throws FTPException, IOException {  
        return ftpClient.exists(remoteFileName);
    }
    
    
    /**
     * Deletes a remote file.
     * 
     * @param remoteFileName   name of remote file
     * @throws FTPException 
     * @throws IOException 
     */
    public synchronized void deleteFile(String remoteFileName) 
        throws FTPException, IOException {
        ftpClient.delete(remoteFileName);
    }

    
    /**
     * Rename a remote file or directory.
     * 
     * @param renameFromName
     *            original name
     * @param renameToName
     *            new name
      * @throws FTPException, IOException
     */
    public synchronized void rename(String renameFromName, String renameToName) 
        throws FTPException, IOException {
        ftpClient.rename(renameFromName, renameToName);
    }
    
    
    /**
     * Change directory on the FTP server. 
     * 
     * @param directoryName
     *            name the remote directory to change into
     * @throws FTPException, IOException 
     */
    public synchronized void changeDirectory(String directoryName) throws FTPException, IOException {
        ftpClient.chdir(directoryName);
    }
   
 
    /**
     * Change to parent directory on the FTP server. 
     * 
     * @throws FTPException, IOException 
     */
    public synchronized void changeToParentDirectory() throws FTPException, IOException {
        ftpClient.cdup();
    }    
    
    /**
     * Get the current remote directory.
     * 
     * @return current remote directory
     * @throws FTPException 
     * @throws IOException 
     */
    public synchronized String getRemoteDirectory() throws IOException, FTPException {
        return ftpClient.pwd();
    }
    
    /**
     * Create directory on the FTP server. 
     * 
     * @param directoryName
     *            name the remote directory to create
     * @throws FTPException, IOException 
     */
    public synchronized void createDirectory(String directoryName) throws FTPException, IOException {
        ftpClient.mkdir(directoryName);        
    }

    
    /**
     * Delete directory on the FTP server. The directory must be empty. Note
     * that edtFTPj/PRO supports recursive directory deletions.
     * 
     * @param directoryName
     *            name the remote directory to create
     * @throws FTPException, IOException 
     */
    public synchronized void deleteDirectory(String directoryName) 
        throws FTPException, IOException {
        ftpClient.rmdir(directoryName);
     }

    /**
     * Disconnect from the FTP server.
     * 
     * @throws FTPException, IOException
     */
    public synchronized void disconnect() 
        throws FTPException, IOException {
        ftpClient.quit();
    }
    
    /**
     * Disconnect from the FTP server.
     * 
     * @throws FTPException, IOException
     */
    public synchronized void disconnect(boolean immediate) 
        throws FTPException, IOException {
        if (immediate)
            ftpClient.quitImmediately();
        else
            ftpClient.quit();
    }
    
}

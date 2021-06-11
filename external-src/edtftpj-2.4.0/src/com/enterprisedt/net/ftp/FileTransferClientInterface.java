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
 *    $Log: FileTransferClientInterface.java,v $
 *    Revision 1.6  2012/10/24 05:16:23  bruceb
 *    get/setNetworkBufferSize()
 *
 *    Revision 1.5  2010-05-28 07:04:55  bruceb
 *    allow aborting the listing
 *
 *    Revision 1.4  2010-04-26 15:55:46  bruceb
 *    add new dirDetails method with callback
 *
 *    Revision 1.3  2008-05-22 04:20:55  bruceb
 *    moved stuff to internal etc
 *
 *    Revision 1.2  2008-05-02 07:41:30  bruceb
 *    setModTime added
 *
 *    Revision 1.1  2008-03-31 00:18:19  bruceb
 *    common interface
 *
 *
 *
 */
package com.enterprisedt.net.ftp;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

/**
 * Easy to use FTP client interface
 * 
 * @author Bruce Blackshaw
 * @version $Revision: 1.6 $
 */
public interface FileTransferClientInterface {
    
       
    /**
     * Is this client currently connected to the server?
     * 
     * @return  true if connected, false otherwise
     */
    public boolean isConnected();

    
    /**
     * Returns the IP address or name of the remote host.
     * 
     * @return Returns the remote host.
     */
    public String getRemoteHost();

    /**
     * Set the IP address or name of the remote host
     * 
     * This may only be done if the client is not already connected to the server.
     * 
     * @param remoteHost The IP address or name of the remote host
     * @throws FTPException Thrown if the client is already connected to the server.
     */
    public void setRemoteHost(String remoteHost) throws FTPException;
    
    /**
     * Returns the timeout for socket connections. 
     * 
     * @return Returns the connection timeout in milliseconds
     */
    public int getTimeout();
    
    /** 
     * Set the timeout for socket connections. Can only do this if
     * not already connected.
     * 
     * @param timeout  the timeout to use in milliseconds
     * @throws FTPException Thrown if the client is already connected to the server.
     */
    public void setTimeout(int timeout) throws FTPException;
    
    /**
     * Get the size of the network buffers (SO_SNDBUF
     * and SO_RCVBUF).
     * 
     * @return network buffer size
     */
    public int getNetworkBufferSize();

    /**
     * Set the size of the network buffers (SO_SNDBUF
     * and SO_RCVBUF).
     * 
     * @param networkBufferSize  new buffer size to set
     */
    public void setNetworkBufferSize(int networkBufferSize) throws FTPException;

    /**
     * Returns the port being connected to on the remote server. 
     * 
     * @return Returns the port being connected to on the remote server. 
     */
    public int getRemotePort();
    
    /** 
     * Set the port to connect to on the remote server. Can only do this if
     * not already connected.
     * 
     * @param remotePort The port to use. 
     * @throws FTPException Thrown if the client is already connected to the server.
     */
    public void setRemotePort(int remotePort) throws FTPException;
    
    /**
     * Set the transfer type for all connections, either ASCII or binary. Setting
     * applies to all subsequent transfers that are initiated.
     * 
     * @param type            transfer type
     * @throws FTPException 
     * @throws IOException 
     * @throws FTPException
     */
    public void setContentType(FTPTransferType type) 
        throws IOException, FTPException;
    
    /**
     * Get the current content type for all connections.
     * 
     * @return  transfer type
     */
    public FTPTransferType getContentType() ;
    
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
    public void setDetectContentType(boolean detectContentType);
    
    /**
     * Get the detect content type flag
     * 
     * @return true if we are detecting binary and ASCII transfers from the file type
     */
    public boolean isDetectContentType();
    
    /**
     * Set the name of the user to log in with. Can only do this if
     * not already connected.
     * 
     * @param userName          user-name to log in with.
     * @throws FTPException
     */
    public void setUserName(String userName) throws FTPException;
    
    /**
     * Get the current user password.
     * 
     * @return current user password
     */
    public String getPassword();
    
    /**
     * Set the password of the user to log in with. Can only do this if
     * not already connected.
     * 
     * @param password          password to log in with.
     * @throws FTPException
     */
    public void setPassword(String password) throws FTPException;
    
    /**
     * Get the current user name.
     * 
     * @return current user name
     */
    public String getUserName();
    
    /**
     * Get the advanced FTP configuration parameters object
     * 
     * @return advanced parameters
     */
    public AdvancedFTPSettings getAdvancedFTPSettings();
    
    /**
     * Get the advanced general configuration parameters object
     * 
     * @return advanced parameters
     */
    public AdvancedGeneralSettings getAdvancedSettings();
    
    /**
     * Set the event listener for transfer event notification
     * 
     * @param listener  event listener reference
     */
    public void setEventListener(EventListener listener) ;
    
    /**
     * Make a connection to the FTP server. 
     * 
     * @throws FTPException 
     * @throws IOException 
     */
    public void connect() throws FTPException, IOException;    
    
    /**
     * Get statistics on file transfers and deletions. 
     * 
     * @return FTPStatistics
     */
    public FileStatistics getStatistics();
    
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
    public String executeCommand(String command) 
        throws FTPException, IOException;
    
    /**
     * Cancel current transfer if underway
     */
    public void cancelAllTransfers();
    
    /**
     * Get a string that represents the remote system that the client is logged
     * into.
     * 
     * @return system string
     * @throws FTPException
     * @throws IOException
     */
    public String getSystemType() 
        throws FTPException, IOException;
    
    /**
     * List the names of files and directories in the current directory on the FTP server.
     * 
     * @return String[]
     * @throws FTPException, IOException 
     */
    public String[] directoryNameList() 
        throws FTPException, IOException;

    
    /**
     * List the names of files and directories of a directory on the FTP server.
     * 
     * @param directoryName    name of the directory (generally not a path). Some 
     *                          servers will accept a wildcard.
     * @param isLongListing    true if the listing is a long format listing
     * @return String[]
     * @throws FTPException, IOException 
     */
    public String[] directoryNameList(String directoryName, boolean isLongListing) 
        throws FTPException, IOException;
    
    /**
     * List the current directory on the FTP server.
     * 
     * @throws FTPException, IOException 
     * @throws ParseException 
     */
    public FTPFile[] directoryList() 
        throws FTPException, IOException, ParseException;
   
    /**
     * List a directory on the FTP server.
     * 
     * @param directoryName    name of the directory (generally not a path). Some 
     *                          servers will accept a wildcard.
     * @throws FTPException, IOException 
     * @throws ParseException 
     */
    public FTPFile[] directoryList(String directoryName) 
        throws FTPException, IOException, ParseException;
    
    /**
     * List a directory on the FTP server.
     * 
     * @param directoryName    name of the directory (generally not a path)
     * @throws FTPException, IOException 
     */
    public void directoryList(String directoryName, DirectoryListCallback lister) 
        throws FTPException, IOException, ParseException;
    
    /**
     * Download a file from the FTP server into a byte array.
     * 
     * @param remoteFileName   name of the remote file to be downloaded
     * @throws FTPException 
     */
    public byte[] downloadByteArray(String remoteFileName) 
        throws FTPException, IOException;
    
    /**
     * Download a file from the FTP server .
     * 
     * @param localFileName    name (or full path) of the local file to be downloaded to
     * @param remoteFileName   name of the remote file to be downloaded
     * @throws FTPException 
     */
    public void downloadFile(String localFileName, String remoteFileName) 
        throws FTPException, IOException;
    
    /**
     * Download a file from the FTP server .
     * 
     * @param localFileName    name (or full path) of the local file to be downloaded to
     * @param remoteFileName   name of the remote file to be downloaded
     * @param writeMode        mode in which the file is written to the client machine
     * @throws FTPException 
     */
    public void downloadFile(String localFileName, String remoteFileName, 
            WriteMode writeMode) 
        throws FTPException, IOException;
    
    
    /**
     * Download a file from the FTP server as a stream. The close() method <b>must</b>
     * be called on the stream once the stream has been read.
     * 
     * @param remoteFileName   name of the remote file to be downloaded
     * @return InputStream
     * @throws FTPException 
     */
     public FileTransferInputStream downloadStream(String remoteFileName) 
         throws FTPException, IOException;
     
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
      public String uploadFile(String localFileName, String remoteFileName) 
          throws FTPException, IOException;
      
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
      public String uploadFile(String localFileName, String remoteFileName, WriteMode writeMode) 
          throws FTPException, IOException;
      
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
       public FileTransferOutputStream uploadStream(String remoteFileName) 
           throws FTPException, IOException;

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
        public FileTransferOutputStream uploadStream(String remoteFileName, WriteMode writeMode) 
            throws FTPException, IOException;
    
     /**
      * Get the size of a remote file.
      * 
      * @param remoteFileName   name of remote file
      * @return long
      * @throws FTPException 
      */
     public long getSize(String remoteFileName) 
         throws FTPException,IOException;
     

    /**
     * Get the modified-time of a remote file. May not be supported by
     * all servers.
     * 
     * @param remoteFileName   name of remote file
     * @return Date
     * @throws FTPException 
     */
    public Date getModifiedTime(String remoteFileName) throws FTPException, IOException;
    
    /**
     * Set the modified-time of a remote file. May not be supported by
     * all servers.
     * 
     * @param remoteFileName   name of remote file
     * @param modifiedTime    new modified time
     * @throws FTPException 
     */
    public void setModifiedTime(String remoteFileName, Date modifiedTime) 
        throws FTPException, IOException;
    
    /**
     * Determine if a remote file exists.
     * 
     * @param remoteFileName   name of remote file
     * @throws FTPException 
     */
    public boolean exists(String remoteFileName) 
        throws FTPException, IOException;
    
    
    /**
     * Deletes a remote file.
     * 
     * @param remoteFileName   name of remote file
     * @throws FTPException 
     * @throws IOException 
     */
    public void deleteFile(String remoteFileName) 
        throws FTPException, IOException;
    
    /**
     * Rename a remote file or directory.
     * 
     * @param renameFromName
     *            original name
     * @param renameToName
     *            new name
      * @throws FTPException, IOException
     */
    public void rename(String renameFromName, String renameToName) 
        throws FTPException, IOException;
    
    
    /**
     * Change directory on the FTP server. 
     * 
     * @param directoryName
     *            name the remote directory to change into
     * @throws FTPException, IOException 
     */
    public void changeDirectory(String directoryName) throws FTPException, IOException;
   
 
    /**
     * Change to parent directory on the FTP server. 
     * 
     * @throws FTPException, IOException 
     */
    public void changeToParentDirectory() throws FTPException, IOException;
    
    /**
     * Get the current remote directory.
     * 
     * @return current remote directory
     * @throws FTPException 
     * @throws IOException 
     */
    public String getRemoteDirectory() throws IOException, FTPException;
    
    /**
     * Create directory on the FTP server. 
     * 
     * @param directoryName
     *            name the remote directory to create
     * @throws FTPException, IOException 
     */
    public void createDirectory(String directoryName) throws FTPException, IOException;

    
    /**
     * Delete directory on the FTP server. The directory must be empty. Note
     * that edtFTPj/PRO supports recursive directory deletions.
     * 
     * @param directoryName
     *            name the remote directory to create
     * @throws FTPException, IOException 
     */
    public void deleteDirectory(String directoryName) 
        throws FTPException, IOException;

    /**
     * Disconnect from the FTP server.
     * 
     * @throws FTPException, IOException
     */
    public void disconnect() 
        throws FTPException, IOException;
    
    /**
     * Disconnect from the FTP server.
     * 
     * @throws FTPException, IOException
     */
    public void disconnect(boolean immediate) 
        throws FTPException, IOException;
    
}

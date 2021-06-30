/**
*
*  edtFTPj
*
*  Copyright (C) 2000-2003  Enterprise Distributed Technologies Ltd
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
*        $Log: FTPClientInterface.java,v $
*        Revision 1.28  2012/12/05 02:13:52  bruceb
*        fix wrong comment re timeout of 0
*
*        Revision 1.27  2012/11/30 05:00:55  bruceb
*        flag for filelocking enabled or not
*
*        Revision 1.26  2012/10/24 05:17:22  bruceb
*        get/setNetworkBufferSize()
*
*        Revision 1.25  2012-02-08 06:19:57  bruceb
*        resumeNextDownload
*
*        Revision 1.24  2011-08-09 10:49:44  bruceb
*        Fix javadoc re setTimeout
*
*        Revision 1.23  2011-05-21 05:03:50  bruceb
*        fix doco for timeout indicating default is 60s
*
*        Revision 1.22  2010-04-26 15:55:46  bruceb
*        add new dirDetails method with callback
*
*        Revision 1.21  2008-05-02 07:41:30  bruceb
*        setModTime added
*
*        Revision 1.20  2008-04-17 04:51:04  bruceb
*        add setControlEncoding
*
*        Revision 1.19  2008-03-13 04:23:29  bruceb
*        changed to system()
*
*        Revision 1.18  2008-03-13 00:21:57  bruceb
*        added executeCommand
*
*        Revision 1.17  2007-08-07 04:45:04  bruceb
*        added counts for transfers and deletes
*
*        Revision 1.16  2007/03/19 22:06:57  bruceb
*        add connected()
*
*        Revision 1.15  2007/02/07 23:02:55  bruceb
*        added keepAlive() & quitImmediately()
*
*        Revision 1.14  2007/02/01 05:10:32  bruceb
*        enhance comment
*
*        Revision 1.13  2007/01/15 23:04:51  bruceb
*        minor comment change
*
*        Revision 1.12  2007/01/10 02:38:25  bruceb
*        added getId() and modified gets to return filename
*
*        Revision 1.11  2006/11/14 11:40:42  bruceb
*        fix comment
*
*        Revision 1.10  2006/09/11 12:34:00  bruceb
*        added exists() method
*
*        Revision 1.9  2006/02/09 09:02:15  bruceb
*        fixed comment re dirname
*
*        Revision 1.8  2005/11/15 21:02:14  bruceb
*        augment javadoc
*
*        Revision 1.7  2005/11/10 19:45:18  bruceb
*        added resume & cancel methods
*
*        Revision 1.6  2005/11/09 21:15:19  bruceb
*        added set/get for autodetect
*
*        Revision 1.5  2005/10/10 20:42:56  bruceb
*        append now in FTPClientInterface
*
*        Revision 1.4  2005/07/11 21:14:58  bruceb
*        add set/get transfer type
*
*        Revision 1.3  2005/06/16 21:41:34  hans
*        Added RemoteHost and RemotePort accessors as well as connect() method.
*
*        Revision 1.2  2005/06/03 11:26:25  bruceb
*        comment change
*/

package com.enterprisedt.net.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;

/**
 * Defines operations in common with a number of FTP implementations.
 * 
 * @author     Hans Andersen
 * @version    $Revision: 1.28 $
 */
public interface FTPClientInterface {
   	
    /**
     * Get the identifying string for this instance
     * 
     * @return identifying string
     */
    public String getId();
    
    /**
     * Set the identifying string for this instance
     * 
     * @param id    identifying string
     */
    public void setId(String id);
    
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
    public void setRemoteHost(String remoteHost) throws IOException, FTPException;
    
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
     *   Get the timeout used for sockets and other resources
     *  
     *  @return timeout that is used, in milliseconds
     */
    public int getTimeout();
    
	/**
	 *   Set the timeout on the underlying sockets and other resources
	 *   that may be used. A timeout of 0 should not be used - for an infinite
	 *   timeout use a large number. Timeouts should be set before connections are made.
	 *   If a timeout is set, then any operation which
	 *   takes longer than the timeout value will be
	 *   result in an IOException being thrown. The default is 
	 *   60,000 (60 seconds). 
	 *
	 *   @param millis The length of the timeout, in milliseconds
	 */
	public void setTimeout(int millis) throws IOException, FTPException;
	
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
    public void setNetworkBufferSize(int networkBufferSize);

	/**
	 *  Set a progress monitor for callbacks. The bytes transferred in
	 *  between callbacks is only indicative. In many cases, the data is
	 *  read in chunks, and if the interval is set to be smaller than the
	 *  chunk size, the callback will occur after after chunk transfer rather
	 *  than the interval. Depending on the implementation, the chunk size can
     *  be as large as 64K.
	 *
	 *  @param  monitor   the monitor object
	 *  @param  interval  bytes transferred in between callbacks
	 */
	public void setProgressMonitor(FTPProgressMonitor monitor,
			long interval);

	/**
	 *  Set a progress monitor for callbacks. Uses default callback
	 *  interval
	 *
	 *  @param  monitor   the monitor object
	 */
	public void setProgressMonitor(FTPProgressMonitor monitor);

	/**
	 *  Get the bytes transferred between each callback on the
	 *  progress monitor
	 * 
	 * @return long     bytes to be transferred before a callback
	 */
	public long getMonitorInterval();
    
    /**
     * Set autodetect of filetypes on or off. If on, the transfer mode is
     * switched from ASCII to binary and vice versa depending on the extension
     * of the file. After the transfer, the mode is always returned to what it
     * was before the transfer was performed. The default is off.
     * 
     * If the filetype is unknown, the transfer mode is unchanged
     * 
     * @param detectTransferMode    true if detecting transfer mode, false if not
     */
    public void setDetectTransferMode(boolean detectTransferMode);
    
    /**
     * Get the detect transfer mode
     * 
     * @return true if we are detecting binary and ASCII transfers from the file type
     */
    public boolean getDetectTransferMode();  
    
    /**
     * Set file locking to enabled or disabled. When downloading files, by default
     * the local file is locked for exclusive writing to prevent other processes
     * corrupting it. Sometimes this needs to be disabled, e.g. tryLock() fails on
     * NFS drives in versions of Java prior to 7.
     * 
     * @param lockingEnabled true to enable locking, false to disable
     */
    public void setFileLockingEnabled(boolean lockingEnabled);
    
    /**
     * Determine if file locking on local downloaded files is being used or not. Default is true.
     * 
     * @return true if file locking is enabled, false otherwise
     */
    public boolean getFileLockingEnabled();
    
    /**
     * Set the encoding used on the control channel. Can only do this if
     * not connected
     * 
     * @param controlEncoding The controlEncoding to set, which is the name of a Charset
     * @see java.nio.charset.Charset
     * @throws FTPException
     */
    public void setControlEncoding(String controlEncoding) throws FTPException;
    
    
    /**
     * Connects to the server at the address and port number defined
     * in the constructor.
     * 
     * @throws IOException Thrown if there is a TCP/IP-related error.
     * @throws FTPException Thrown if there is an error related to the FTP protocol. 
     */
    public void connect() throws IOException, FTPException;
    
    /**
     * Is the client currently connected?
     * 
     * @return true if connected, false otherwise
     */
    public boolean connected();

	/**
	 *  Get the size of a remote file. This is not a standard FTP command, it
	 *  is defined in "Extensions to FTP", a draft RFC 
	 *  (draft-ietf-ftpext-mlst-16.txt)
	 *
	 *  @param  remoteFile  name or path of remote file in current directory
	 *  @return size of file in bytes      
	 */
	public long size(String remoteFile) throws IOException,
	    FTPException;
    
    /**
     * Does the named file exist in the current server directory?
     * 
     * @param remoteFile        name of remote file
     * @return true if exists, false otherwise
     * @throws IOException
     * @throws FTPException
     */
    public boolean exists(String remoteFile) throws IOException,
        FTPException;
    
    /**
     * Request that the remote server execute the literal command supplied. 
     * In FTP and SFTP, this might be a SITE command, while in SFTP it might
     * be a shell command.
     * <p>
     * It is up to the user to send a sensible command. 
     *  
     * @param command   command string
     * @return result string by server
     * @throws FTPException
     * @throws IOException
     */
    public String executeCommand(String command) 
        throws FTPException, IOException;
    
    /**
     * Get a string representing the remote system
     * 
     * @return system string
     * @throws FTPException
     * @throws IOException
     */
    public String system() 
        throws FTPException, IOException;
    
    /**
     *  Get the current transfer type
     *
     *  @return  the current type of the transfer,
     *           i.e. BINARY or ASCII
     */
    public FTPTransferType getType();
    
    /**
     *  Set the transfer type
     *
     *  @param  type  the transfer type to
     *                set the server to
     */
    public void setType(FTPTransferType type)
        throws IOException, FTPException; 
    
    /**
     * Make the next file transfer (put or get) resume. For puts(), the
     * bytes already transferred are skipped over, while for gets(), if 
     * writing to a file, it is opened in append mode, and only the bytes
     * required are transferred.
     * 
     * Currently resume is only supported for BINARY transfers (which is
     * generally what it is most useful for).
     * 
     * @throws FTPException
     */
    public void resume() throws FTPException;
    
    /**
     * Make the next download resume at a specific point.
     * 
     * This resume method allows the resume offset to be set explicitly for downloads. 
     * Offset bytes are skipped before downloading the file.
     * This means you can download only the last few bytes of the file rather than the
     * whole file, irrespective of the size of the local file. 
     * 
     * Currently resume is only supported for BINARY transfers (which is
     * generally what it is most useful for).
     *
     * @throws FTPException
     */
    public void resumeNextDownload(long offset) throws FTPException;
    
    /**
     * Cancel the resume. Use this method if something goes wrong
     * and the server is left in an inconsistent state
     * 
     * @throws IOException
     * @throws FTPException
     */
    public void cancelResume() throws IOException, FTPException;
    
    /**
     *  Cancels the current transfer. Generally called from a separate
     *  thread. Note that this may leave partially written files on the
     *  server or on local disk, and should not be used unless absolutely
     *  necessary. After the transfer is cancelled the connection may be in
     *  an inconsistent state, therefore it is best to quit and reconnect.
     *  It may cause exceptions to be thrown depending on the underlying protocol
     *  being used. Note that this can also be used to cancel directory listings,
     *  which can involve large amounts of data for directories containing many
     *  files.
     */
    public void cancelTransfer();

	/**
	 *  Put a local file onto the FTP server. It
	 *  is placed in the current directory. If a remote file name is supplied,
     *  it is stored as that name on the server. If null is supplied, the server
     *  will generate a unique filename (via STOU) if it supports this option.
	 *
	 *  @param  localPath   path of the local file
	 *  @param  remoteFile  name of remote file in
	 *                      current directory, or null if 
     *                       a unique filename is to be generated by the server
     *  @return The name of the remote file - normally the name supplied, or else
     *           the unique name generated by the server.
	 */
	public String put(String localPath, String remoteFile)
			throws IOException, FTPException;

	/**
	 *  Put a stream of data onto the FTP server. It
	 *  is placed in the current directory. If a remote file name is supplied,
     *  it is stored as that name on the server. If null is supplied, the server
     *  will generate a unique filename (via STOU) if it supports this option.
	 *
	 *  @param  srcStream   input stream of data to put
	 *  @param  remoteFile  name of remote file in
	 *                      current directory, or null if 
     *                       a unique filename is to be generated by the server
     *  @return The name of the remote file - normally the name supplied, or else
     *           the unique name generated by the server.
	 */
	public String put(InputStream srcStream, String remoteFile)
			throws IOException, FTPException;
    
    /**
     *  Put a stream of data onto the FTP server. It
     *  is placed in the current directory. If a remote file name is supplied,
     *  it is stored as that name on the server. If null is supplied, the server
     *  will generate a unique filename (via STOU) if it supports this option.
     *  Allows appending if current file exists. 
     *
     *  @param  srcStream   input stream of data to put
     *  @param  remoteFile  name of remote file in
     *                      current directory, or null if 
     *                       a unique filename is to be generated by the server
     *  @param  append      true if appending, false otherwise
     *  @return The name of the remote file - normally the name supplied, or else
     *           the unique name generated by the server.
     */
    public String put(InputStream srcStream, String remoteFile,
                    boolean append)
        throws IOException, FTPException;

	/**
	 *  Put data onto the FTP server. It
	 *  is placed in the current directory. If a remote file name is supplied,
     *  it is stored as that name on the server. If null is supplied, the server
     *  will generate a unique filename (via STOU) if it supports this option.
	 *
	 *  @param  bytes        array of bytes
	 *  @param  remoteFile  name of remote file in
	 *                      current directory, or null if 
     *                       a unique filename is to be generated by the server
     *  @return The name of the remote file - normally the name supplied, or else
     *           the unique name generated by the server.
	 */
	public String put(byte[] bytes, String remoteFile)
			throws IOException, FTPException;
    
    /**
     *  Put data onto the FTP server. It
     *  is placed in the current directory. If a remote file name is supplied,
     *  it is stored as that name on the server. If null is supplied, the server
     *  will generate a unique filename (via STOU) if it supports this option.
     *  Allows appending if current file exists.
     *
     *  @param  bytes        array of bytes
     *  @param  remoteFile  name of remote file in
     *                      current directory, or null if 
     *                       a unique filename is to be generated by the server
     *  @param  append      true if appending, false otherwise
     *  @return The name of the remote file - normally the name supplied, or else
     *           the unique name generated by the server.
     */
    public String put(byte[] bytes, String remoteFile, boolean append)
        throws IOException, FTPException;   

    /**
     *  Put a local file onto the FTP server. It
     *  is placed in the current directory. If a remote file name is supplied,
     *  it is stored as that name on the server. If null is supplied, the server
     *  will generate a unique filename (via STOU) if it supports this option. 
     *  Allows appending if current file exists.
     *
     *  @param  localPath   path of the local file
     *  @param  remoteFile  name of remote file in current directory, or null if 
     *                       a unique filename is to be generated by the server
     *  @param  append      true if appending, false otherwise
     *  @return The name of the remote file - normally the name supplied, or else
     *           the unique name generated by the server.
     */
    public String put(String localPath, String remoteFile,
                    boolean append)
        throws IOException, FTPException;
    
	/**
	 *  Get data from the FTP server. Uses the currently
	 *  set transfer mode.
	 *
	 *  @param  localPath   local file to put data in
	 *  @param  remoteFile  name of remote file in
	 *                      current directory
	 */
	public void get(String localPath, String remoteFile)
			throws IOException, FTPException;

	/**
	 *  Get data from the FTP server. Uses the currently
	 *  set transfer mode.
	 *
	 *  @param  destStream  data stream to write data to
	 *  @param  remoteFile  name of remote file in
	 *                      current directory
	 */
	public void get(OutputStream destStream, String remoteFile)
			throws IOException, FTPException;

	/**
	 *  Get data from the FTP server. Transfers in
	 *  whatever mode we are in. Retrieve as a byte array. Note
	 *  that we may experience memory limitations as the
	 *  entire file must be held in memory at one time.
	 *
	 *  @param  remoteFile  name of remote file in
	 *                      current directory
	 */
	public byte[] get(String remoteFile) throws IOException,
			FTPException;
    
    /**
     * Get the number of files downloaded since the count was
     * reset
     * 
     * @return  download file count
     */
    public int getDownloadCount();
    
    /**
     * Reset the count of downloaded files to zero.
     *
     */
    public void resetDownloadCount();
    
    /**
     * Get the number of files uploaded since the count was
     * reset
     * 
     * @return  upload file count
     */
    public int getUploadCount();
    
    /**
     * Reset the count of uploaded files to zero.
     *
     */
    public void resetUploadCount();
    
    /**
     * Get the number of files deleted since the count was
     * reset
     * 
     * @return  deleted file count
     */
    public int getDeleteCount();
    
    /**
     * Reset the count of deleted files to zero.
     *
     */
    public void resetDeleteCount();
    
    /**
     *  List a directory's contents via a callback. The callback is notified
     *  for each directory entry, meaning they can be processed individually. It
     *  also avoids out of memory problems if the directory is huge, and an array
     *  of thousands of FTPFile objects would otherwise be returned.
     *
     *  @param   dirname  name of directory (some servers permit a filemask)
     *  @param   lister   callback to be notified of errors
      */
    public void dirDetails(String dirname, DirectoryListCallback lister) throws IOException,
            FTPException, ParseException;


	/**
	 *  List a directory's contents as an array of FTPFile objects.
	 *  Should work for Windows and most Unix FTP servers - let us know
	 *  about unusual formats (http://www.enterprisedt.com/forums/index.php).
     *  If accurate timestamps are required (i.e. to the second), it is 
     *  generally better to use @see #modtime(String).
	 *
	 *  @param   dirname  name of directory (some servers permit a filemask)
	 *  @return  an array of FTPFile objects
	 */
	public FTPFile[] dirDetails(String dirname) throws IOException,
			FTPException, ParseException;

	/**
	 *  List current directory's contents as an array of strings of
	 *  filenames.
	 *
	 *  @return  an array of current directory listing strings
	 */
	public String[] dir() throws IOException, FTPException;

	/**
	 *  List a directory's contents as an array of strings of filenames.
	 *
	 *  @param   dirname  name of directory OR filemask
	 *  @return  an array of directory listing strings
	 */
	public String[] dir(String dirname) throws IOException,
			FTPException;

	/**
	 *  List a directory's contents as an array of strings. A detailed
	 *  listing is available, otherwise just filenames are provided.
	 *  The detailed listing varies in details depending on OS and
	 *  FTP server. Note that a full listing can be used on a file
	 *  name to obtain information about a file
	 *
	 *  @param  dirname  name of directory OR filemask
	 *  @param  full     true if detailed listing required
	 *                   false otherwise
	 *  @return  an array of directory listing strings
	 */
	public String[] dir(String dirname, boolean full)
			throws IOException, FTPException;

	/**
	 *  Delete the specified remote file
	 *
	 *  @param  remoteFile  name of remote file to
	 *                      delete
	 */
	public void delete(String remoteFile) throws IOException,
			FTPException;

	/**
	 *  Rename a file or directory
	 *
	 * @param from  name of file or directory to rename
	 * @param to    intended name
	 */
	public void rename(String from, String to) throws IOException,
			FTPException;

	/**
	 *  Delete the specified remote working directory
	 *
	 *  @param  dir  name of remote directory to
	 *               delete
	 */
	public void rmdir(String dir) throws IOException, FTPException;

	/**
	 *  Create the specified remote working directory
	 *
	 *  @param  dir  name of remote directory to
	 *               create
	 */
	public void mkdir(String dir) throws IOException, FTPException;

	/**
	 *  Change the remote working directory to
	 *  that supplied
	 *
	 *  @param  dir  name of remote directory to
	 *               change to
	 */
	public void chdir(String dir) throws IOException, FTPException;
    
    /**
     *  Change the remote working directory to
     *  the parent directory
     */
    public void cdup() throws IOException, FTPException;

	/**
	 *  Get modification time for a remote file. For accurate
     *  modification times (e.g. to the second) this method is to
     *  be preferred over @see #dirDetails(java.lang.String) which
     *  parses a listing returned by the server. The time zone is UTC.
	 *
	 *  @param    remoteFile   name of remote file
	 */
	public Date modtime(String remoteFile) throws IOException,
			FTPException;
	
	/**
     * Set the last modified time (UTC) for the supplied file. This is 
     * not supported by all servers.
     * 
     * @param path    the path to the file/directory on the remote server
     * @param modTime   the time stamp to set the modified time to in UTC
     * @return Date that it has been set to (UTC)
     * @throws IOException
     * @throws FTPException
     */
    public void setModTime(String path, Date modTime) throws IOException, FTPException;

	/**
	 *  Get the current remote working directory
	 *
	 *  @return   the current working directory
	 */
	public String pwd() throws IOException, FTPException;
    
    /**
     *  Tries to keep the current connection alive by 
     *  some means, usually by sending an innocuous commmand.
     */
    public void keepAlive() throws IOException, FTPException;

	/**
	 *  Quit the FTP session
	 *
	 */
	public void quit() throws IOException, FTPException;
    
    /**
     *  Quit the FTP session immediately. If a transfer is underway
     *  it will be terminated.
     *
     */
    public void quitImmediately() throws IOException, FTPException; 
    
}
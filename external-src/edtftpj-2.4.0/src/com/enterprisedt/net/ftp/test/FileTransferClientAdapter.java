/**
 *
 *  Copyright (C) 2000-2007  Enterprise Distributed Technologies Ltd
 *
 *  www.enterprisedt.com
 *
 *  Change Log:
 *
 *        $Log: FileTransferClientAdapter.java,v $
 *        Revision 1.16  2012/11/30 06:43:08  bruceb
 *        flag for filelocking enabled or not
 *
 *        Revision 1.15  2012/10/24 05:17:42  bruceb
 *        get/setNetworkBufferSize()
 *
 *        Revision 1.14  2012-02-08 06:19:29  bruceb
 *        resumeNextDownload
 *
 *        Revision 1.13  2011-03-28 03:41:41  hans
 *        Made logger static.
 *
 *        Revision 1.12  2010-04-26 15:56:42  bruceb
 *        tesing for new dirDetails method with callback
 *
 *        Revision 1.11  2008-06-06 00:19:51  bruceb
 *        changes to get the FTC client
 *
 *        Revision 1.10  2008-06-03 05:52:39  bruceb
 *        reconnect changes
 *
 *        Revision 1.9  2008-05-22 04:21:14  bruceb
 *        small tweaks
 *
 *        Revision 1.8  2008-05-02 07:41:43  bruceb
 *        setModTime added
 *
 *        Revision 1.7  2008-04-17 04:51:36  bruceb
 *        use FileTransferClientInterface
 *
 *        Revision 1.6  2008-03-31 04:21:42  bruceb
 *        logging added
 *
 *        Revision 1.5  2008-03-31 00:38:12  bruceb
 *        rename fix
 *
 *        Revision 1.4  2008-03-13 04:23:41  bruceb
 *        changed to system()
 *
 *        Revision 1.3  2008-03-13 00:24:38  bruceb
 *        added connId to params
 *
 *        Revision 1.2  2007-12-20 00:40:27  bruceb
 *        downloadByteArray
 *
 *        Revision 1.1  2007-12-18 07:55:19  bruceb
 *        prepare for FileTransferClient
 *
 *
 */
package com.enterprisedt.net.ftp.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;

import com.enterprisedt.net.ftp.DirectoryListCallback;
import com.enterprisedt.net.ftp.EventListener;
import com.enterprisedt.net.ftp.FTPClientInterface;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPProgressMonitor;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClientInterface;
import com.enterprisedt.net.ftp.FileTransferOutputStream;
import com.enterprisedt.net.ftp.WriteMode;
import com.enterprisedt.util.debug.Logger;

/**
 *  Adapts the FileTransferClient for use in the standard unit
 *  tests
 *
 *  @author      Bruce Blackshaw
 *  @version     $Revision: 1.16 $
 */
public class FileTransferClientAdapter implements FTPClientInterface, EventListener {
    
    private static Logger log = Logger.getLogger("FileTransferClientAdapter");
    
    protected FileTransferClientInterface client;
    
    private boolean resume = false;

    private FTPProgressMonitor monitor;  

    /**
     * Constructor
     * 
     * @param client
     */
    public FileTransferClientAdapter(FileTransferClientInterface client) {
        this.client = client;
    }
    
    public FileTransferClientInterface getFileTransferClient() {
        return client;
    }

    public void cancelResume() throws IOException, FTPException {
        resume = false;
    }

    public void cancelTransfer() {
        log.debug("Cancelling all transfers");
        client.cancelAllTransfers();
    }

    public void cdup() throws IOException, FTPException {
        client.changeToParentDirectory();        
    }

    public void chdir(String dir) throws IOException, FTPException {
        client.changeDirectory(dir);
    }

    public void connect() throws IOException, FTPException {
        client.connect();
        
    }

    public boolean connected() {
        return client.isConnected();
    }

    public void delete(String remoteFile) throws IOException, FTPException {
        client.deleteFile(remoteFile);
    }

    public String[] dir() throws IOException, FTPException {
        return client.directoryNameList("", false);
    }

    public String[] dir(String dirname) throws IOException, FTPException {
        return client.directoryNameList(dirname, false);
    }

    public String[] dir(String dirname, boolean full) throws IOException,
            FTPException {
        return client.directoryNameList(dirname, full);
    }

    public FTPFile[] dirDetails(String dirname) throws IOException,
            FTPException, ParseException {
        return client.directoryList(dirname);
    }
    
    
    public void dirDetails(String dirname, DirectoryListCallback lister)
            throws IOException, FTPException, ParseException {
        client.directoryList(dirname, lister);
    }


    public boolean exists(String remoteFile) throws IOException, FTPException {
        return client.exists(remoteFile);
    }

    public void get(String localPath, String remoteFile) throws IOException,
            FTPException {
        try {
            client.downloadFile(localPath, remoteFile, resume ? WriteMode.RESUME : WriteMode.OVERWRITE);
        }
        finally {
            resume = false;
        }
    }

    public void get(OutputStream destStream, String remoteFile)
            throws IOException, FTPException {
        InputStream str = null;
        try {
            str = client.downloadStream(remoteFile);
        
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = str.read(buf)) >= 0) {
                destStream.write(buf, 0, len);
            }
        }
        finally {
            str.close();
        }
    }

    public byte[] get(String remoteFile) throws IOException, FTPException {
        return client.downloadByteArray(remoteFile);
    }

    public int getDeleteCount() {
        return client.getStatistics().getDeleteCount();
    }

    public boolean getDetectTransferMode() {
        return client.isDetectContentType();
    }

    public int getDownloadCount() {
        return client.getStatistics().getDownloadCount();
    }

    public String getId() {
        return null;
    }

    public long getMonitorInterval() {
        return client.getAdvancedSettings().getTransferNotifyInterval();
    }

    public String getRemoteHost() {
        return client.getRemoteHost();
    }

    public int getRemotePort() {
        return client.getRemotePort();
    }

    public int getTimeout() {
        return client.getTimeout();
    }

    public FTPTransferType getType() {
        return client.getContentType();
    }

    public int getUploadCount() {
        return client.getStatistics().getUploadCount();
    }

    public void keepAlive() throws IOException, FTPException {
        
    }

    public void mkdir(String dir) throws IOException, FTPException {
        client.createDirectory(dir);
    }

    public Date modtime(String remoteFile) throws IOException, FTPException {
        return client.getModifiedTime(remoteFile);
    }
    
    public void setModTime(String path, Date modTime) 
        throws IOException, FTPException {
     
        client.setModifiedTime(path, modTime);
    }

    public String put(String localPath, String remoteFile) throws IOException,
            FTPException {
        try {
            return client.uploadFile(localPath, remoteFile, (resume ? WriteMode.RESUME : WriteMode.OVERWRITE));
        }
        finally {
            resume = false;
        }
    }

    public String put(InputStream srcStream, String remoteFile)
            throws IOException, FTPException {
        return put(srcStream, remoteFile, false);
    }

    public String put(InputStream srcStream, String remoteFile, boolean append)
            throws IOException, FTPException {
        FileTransferOutputStream str = null;
        try {
            str = client.uploadStream(remoteFile, (append ? WriteMode.APPEND : WriteMode.OVERWRITE));
            byte[] buf = new byte[2048];
            int len = 0;
            while ((len = srcStream.read(buf)) >= 0) {
                str.write(buf, 0, len);
            }
            return str.getRemoteFile();
        }
        finally {
            if (str != null) str.close();
        }
    }

    public String put(byte[] bytes, String remoteFile) throws IOException,
            FTPException {
        return put(bytes, remoteFile, false);
    }

    public String put(byte[] bytes, String remoteFile, boolean append)
            throws IOException, FTPException {
        ByteArrayInputStream str = new ByteArrayInputStream(bytes);
        return put(str, remoteFile, append);
    }

    public String put(String localPath, String remoteFile, boolean append)
            throws IOException, FTPException {
        return client.uploadFile(localPath, remoteFile, WriteMode.APPEND);
    }

    public String pwd() throws IOException, FTPException {
        return client.getRemoteDirectory();
    }

    public void quit() throws IOException, FTPException {
        client.disconnect();
    }

    public void quitImmediately() throws IOException, FTPException {
        client.disconnect(true);
    }

    public void rename(String from, String to) throws IOException, FTPException {
        client.rename(from, to);
    }

    public void resetDeleteCount() {
        client.getStatistics().clear();
    }

    public void resetDownloadCount() {
        client.getStatistics().clear();
    }

    public void resetUploadCount() {
        client.getStatistics().clear();
    }

    public void resume() throws FTPException {
        resume = true;
    }
    
    public void resumeNextDownload(long offset) throws FTPException {
        // TODO Auto-generated method stub
        
    }

    public void rmdir(String dir) throws IOException, FTPException {
        client.deleteDirectory(dir);
    }

    public void setDetectTransferMode(boolean detectTransferMode) {
        client.setDetectContentType(detectTransferMode);
    }

    public void setId(String id) {
        // TODO Auto-generated method stub
        
    }

    public void setProgressMonitor(FTPProgressMonitor monitor, long interval) {
       this.monitor = monitor;
       client.setEventListener(this);
       client.getAdvancedSettings().setTransferNotifyInterval((int)interval);
    }

    public void setProgressMonitor(FTPProgressMonitor monitor) {
        this.monitor = monitor;
        client.setEventListener(this);
    }

    public void setRemoteHost(String remoteHost) throws IOException,
            FTPException {
        client.setRemoteHost(remoteHost);
    }

    public void setRemotePort(int remotePort) throws FTPException {
        client.setRemotePort(remotePort);
    }

    public void setTimeout(int timeout) throws IOException, FTPException {
        client.setTimeout(timeout);
    }

    public void setType(FTPTransferType type) throws IOException, FTPException {
        client.setContentType(type);
    }

    public long size(String remoteFile) throws IOException, FTPException {
        return client.getSize(remoteFile);
    }
    
    public String executeCommand(String command) throws FTPException,
        IOException {
        return client.executeCommand(command);
    }
    
    public String system() throws FTPException, IOException {
        return client.getSystemType();
    }
    
    public void setControlEncoding(String controlEncoding) throws FTPException {
        client.getAdvancedSettings().setControlEncoding(controlEncoding);  
    }

    public void bytesTransferred(String connId, String remoteFile, long count) {
        log.debug("bytesTransferred(" + remoteFile + ": " + count + " bytes");
        if (monitor != null)
            monitor.bytesTransferred(count);        
    }

    public void commandSent(String connId, String cmd) {
        // TODO Auto-generated method stub
        
    }

    public void downloadCompleted(String connId, String remoteFilename) {
        // TODO Auto-generated method stub
        
    }

    public void downloadStarted(String connId, String remoteFilename) {
        // TODO Auto-generated method stub
    }

    public void replyReceived(String connId, String reply) {
        // TODO Auto-generated method stub
        
    }

    public void uploadCompleted(String connId, String remoteFilename) {
        // TODO Auto-generated method stub
        
    }

    public void uploadStarted(String connId, String remoteFilename) {
        // TODO Auto-generated method stub
        
    }

    public int getNetworkBufferSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setNetworkBufferSize(int networkBufferSize) {
        // TODO Auto-generated method stub
        
    }

    public void setFileLockingEnabled(boolean lockingEnabled) {
        client.getAdvancedSettings().setFileLockingEnabled(lockingEnabled);
    }

    public boolean getFileLockingEnabled() {
        return client.getAdvancedSettings().getFileLockingEnabled();
    }

}

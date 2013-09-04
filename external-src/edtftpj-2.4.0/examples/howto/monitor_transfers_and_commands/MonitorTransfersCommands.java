/*
 * 
 * Copyright (C) 2006 Enterprise Distributed Technologies Ltd
 * 
 * www.enterprisedt.com
 */

import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.net.ftp.EventListener;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;
import java.io.File;

public class MonitorTransfersCommands {

    public static void main(String[] args) {

        // we want remote host, user name and password
        if (args.length < 3) {
            System.out
                    .println("Usage: run remote-host username password");
            System.exit(1);
        }

        // extract command-line arguments
        String host = args[0];
        String username = args[1];
        String password = args[2];

        // set up logger so that we get some output
        Logger log = Logger.getLogger(MonitorTransfersCommands.class);
        Logger.setLevel(Level.INFO);

        FileTransferClient ftp = null;

        try {
            // create client
            log.info("Creating FTP client");
            ftp = new FileTransferClient();

            // set remote host
            ftp.setRemoteHost(host);
            ftp.setUserName(username);
            ftp.setPassword(password);
            
            // set up listener
            ftp.setEventListener(new EventListenerImpl());
            
            // the transfer notify interval must be greater than buffer size
            ftp.getAdvancedSettings().setTransferBufferSize(500);
            ftp.getAdvancedSettings().setTransferNotifyInterval(1000);

            // connect to the server
            log.info("Connecting to server " + host);
            ftp.connect();
            log.info("Connected and logged in to server " + host);

            log.info("Uploading file");
            String name = "MonitorTransfersCommands.java";

            // put the file
            ftp.uploadFile(name, name);
            log.info("File uploaded");

            // now delete remote file
            ftp.deleteFile(name);

            // Shut down client
            log.info("Quitting client");
            ftp.disconnect();

            log.info("Example complete");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

/**
 * Listens to events
 */
class EventListenerImpl implements EventListener {

    private static Logger log = Logger.getLogger(EventListenerImpl.class);

    public void bytesTransferred(String connId, String remoteFilename, long bytes) {
        log.info("Bytes transferred=" + bytes);
    }
    
    /**
     * Log an FTP command being sent to the server. Not used for SFTP.
     * 
     * @param cmd   command string
     */
    public void commandSent(String connId, String cmd) {
        log.info("Command sent: " + cmd);
    }
    
    /**
     * Log an FTP reply being sent back to the client. Not used for
     * SFTP.
     * 
     * @param reply   reply string
     */
    public void replyReceived(String connId, String reply) {
        log.info("Reply received: " + reply);
    }
        
    /**
     * Notifies that a download has started
     * 
     * @param remoteFilename   remote file name
     */
    public void downloadStarted(String connId, String remoteFilename) {
        log.info("Started download: " + remoteFilename);
    }
    
    /**
     * Notifies that a download has completed
     * 
     * @param remoteFilename   remote file name
     */
    public void downloadCompleted(String connId, String remoteFilename) {
        log.info("Completed download: " + remoteFilename);
    }
    
    /**
     * Notifies that an upload has started
     * 
     * @param remoteFilename   remote file name
     */
    public void uploadStarted(String connId, String remoteFilename) {
        log.info("Started upload: " + remoteFilename);
    }
    
    /**
     * Notifies that an upload has completed
     * 
     * @param remoteFilename   remote file name
     */
    public void uploadCompleted(String connId, String remoteFilename) {
        log.info("Completed upload: " + remoteFilename);
    }
}


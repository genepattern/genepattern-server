/*
 * 
 * Copyright (C) 2006 Enterprise Distributed Technologies Ltd
 * 
 * www.enterprisedt.com
 */

import java.io.File;

import com.enterprisedt.net.ftp.EventAdapter;
import com.enterprisedt.net.ftp.FTPTransferCancelledException;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.net.ftp.WriteMode;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

public class PauseResumeTransfers {

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
        Logger log = Logger.getLogger(PauseResumeTransfers.class);
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
            CancelListener cl = new CancelListener(ftp);
            ftp.setEventListener(cl);
            
            // the transfer notify interval must be greater than buffer size
            ftp.getAdvancedSettings().setTransferBufferSize(500);
            ftp.getAdvancedSettings().setTransferNotifyInterval(1000);
            
            // connect to the server
            log.info("Connecting to server " + host);
            ftp.connect();
            log.info("Connected and logged in to server " + host);

            log.info("Uploading file");
            String name = "PauseResumeTransfers.java";

            // the upload will be interrupted by the listener - it will call
            // cancelAllTransfers(). We catch the expected exception.
            try {
                ftp.uploadFile(name, name);
            }
            catch (FTPTransferCancelledException ex) {
                log.debug("Transfer cancelled");
            }
            int len = (int) ftp.getSize(name);
            File file = new File(name);
            log.info("Bytes transferred=" + cl.getBytesTransferred());
            log.info("File partially uploaded (localsize=" + file.length()
                    + " remotesize=" + len);

            log.info("Completing upload by resuming");
            ftp.uploadFile(name, name, WriteMode.RESUME);
            len = (int) ftp.getSize(name);

            // only the remaining bytes are transferred as can be seen
            log.info("Bytes transferred=" + cl.getBytesTransferred());
            log.info("File uploaded (localsize=" + file.length()
                    + " remotesize=" + len);

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
 * As soon it receives notification of bytes transferred, it cancels the
 * transfer
 */
class CancelListener extends EventAdapter {

    private static Logger log = Logger.getLogger(CancelListener.class);
    
    /**
     * True if cancelled
     */
    private boolean cancelled = false;
    
    /**
     * Keep the last reported byte count
     */
    private long bytesTransferred = 0;

    /**
     * FTPClient reference
     */
    private FileTransferClient ftp;
    
    /**
     * Constructor
     * 
     * @param ftp
     */
    public CancelListener(FileTransferClient ftp) {
        this.ftp = ftp;
    }

    public void bytesTransferred(String connId, String remoteFilename, long bytes) {
        log.info("Bytes transferred=" + bytes);
        if (!cancelled) {
            ftp.cancelAllTransfers();
            cancelled = true;
        }
        bytesTransferred = bytes;
    }
    
    /**
     * Will contain the total bytes transferred once the transfer is complete
     */
    public long getBytesTransferred() {
        return bytesTransferred;
    }
    
}


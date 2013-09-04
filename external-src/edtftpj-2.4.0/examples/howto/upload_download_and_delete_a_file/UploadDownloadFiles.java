/*
 * 
 * Copyright (C) 2006 Enterprise Distributed Technologies Ltd
 * 
 * www.enterprisedt.com
 */

import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;
import java.io.File;

public class UploadDownloadFiles {

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
        String filename = "UploadDownloadFiles.java";

        // set up logger so that we get some output
        Logger log = Logger.getLogger(UploadDownloadFiles.class);
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

            // connect to the server
            log.info("Connecting to server " + host);
            ftp.connect();
            log.info("Connected and logged in to server " + host);

            log.info("Uploading file");
            ftp.uploadFile(filename, filename);
            log.info("File uploaded");

            log.info("Downloading file");
            ftp.downloadFile(filename + ".copy", filename);
            log.info("File downloaded");

            log.info("Deleting remote file");
            ftp.deleteFile(filename);
            log.info("Deleted remote file");

            File file = new File(filename + ".copy");
            file.delete();
            log.info("Deleted local file copy");

            // Shut down client
            log.info("Quitting client");
            ftp.disconnect();

            log.info("Example complete");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

/*
 * 
 * Copyright (C) 2006 Enterprise Distributed Technologies Ltd
 * 
 * www.enterprisedt.com
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;


import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

public class TransferUsingStreams {

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
        Logger log = Logger.getLogger(TransferUsingStreams.class);
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

            // byte array transfers
            String s1 = "Hello world";

            log.info("Putting s1");
            OutputStream out = ftp.uploadStream("Hello.txt");
            try {
                out.write(s1.getBytes());
            }
            finally {
                out.close(); // MUST be closed to complete the transfer
            }

            log.info("Retrieving as s2");
            StringBuffer s2 = new StringBuffer();
            InputStream in = ftp.downloadStream("Hello.txt");
            try {
                int ch = 0;
                while ((ch = in.read()) >= 0) {
                    s2.append((char)ch);
                }
            }
            finally {
                in.close(); // MUST be closed to complete the transfer
            }
                
 
            log.info("s1 == s2: " + s1.equals(s2.toString()));
            
            ftp.deleteFile("Hello.txt");

            // Shut down client
            log.info("Quitting client");
            ftp.disconnect();

            log.info("Example complete");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

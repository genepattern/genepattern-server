/*
 * 
 * Copyright (C) 2006 Enterprise Distributed Technologies Ltd
 * 
 * www.enterprisedt.com
 */

import com.enterprisedt.net.ftp.FileTransferClient;
import com.enterprisedt.util.debug.Level;
import com.enterprisedt.util.debug.Logger;

public class ConnectToServer {

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
        Logger log = Logger.getLogger(ConnectToServer.class);
        Logger.setLevel(Level.INFO);

        FileTransferClient ftp = null;

        try {
            // create client
            log.info("Creating FTP client");
            ftp = new FileTransferClient();

            // set remote host
            log.info("Setting remote host");
            ftp.setRemoteHost(host);
            ftp.setUserName(username);
            ftp.setPassword(password);

            // connect to the server
            log.info("Connecting to server " + host);
            ftp.connect();
            log.info("Connected and logged in to server " + host);

            // Shut down client
            log.info("Quitting client");
            ftp.disconnect();

            log.info("Example complete");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

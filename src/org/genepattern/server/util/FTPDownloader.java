package org.genepattern.server.util;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FTPDownloader {

    FTPClient ftp = null;

    public FTPDownloader(String host, String user, String pwd) throws Exception {
        ftp = new FTPClient();
        ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
        int reply;
        ftp.connect(host);
        reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }
        ftp.login(user, pwd);
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        ftp.enterLocalPassiveMode();
    }

    public void downloadFile(String remoteFilePath, String localFilePath) {
        try (FileOutputStream fos = new FileOutputStream(localFilePath)) {
            this.ftp.retrieveFile(remoteFilePath, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InputStream downloadFileStream(String remoteFilePath) throws IOException{
         InputStream is = this.ftp.retrieveFileStream(remoteFilePath);
        
        
         return is;
    }
    
    
    public void downloadFile2(String remoteFilePath, String localFilePath) throws IOException{
        InputStream is = downloadFileStream(remoteFilePath);
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(localFilePath));
        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        is.close();
        outputStream.close();
    }
    
    
    
    public void disconnect() {
        if (this.ftp.isConnected()) {
            try {
                this.ftp.logout();
                this.ftp.disconnect();
            } catch (IOException f) {
                // do nothing as file is already downloaded from FTP server
            }
        }
    }

    public static void main(String[] args) {
        try {
            URL url = new URL("ftp://ftp.broadinstitute.org/genepattern/all_aml/all_aml_test.cls");
            
            FTPDownloader ftpDownloader =
                new FTPDownloader(url.getHost(), "anonymous", "genepattern@ucsd.edu");
            ftpDownloader.downloadFile(url.getPath(), "/Users/liefeld/Desktop/ftp3.txt");
            ftpDownloader.downloadFile2(url.getPath(), "/Users/liefeld/Desktop/ftp4.txt");
            
            System.out.println("FTP File downloaded successfully");
            ftpDownloader.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

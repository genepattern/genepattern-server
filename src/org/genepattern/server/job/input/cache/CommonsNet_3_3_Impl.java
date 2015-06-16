package org.genepattern.server.job.input.cache;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;

public final class CommonsNet_3_3_Impl extends CachedFtpFile {
    private static final Logger log = Logger.getLogger(CommonsNet_3_3_Impl.class);

    public CommonsNet_3_3_Impl(final GpConfig gpConfig, final String urlString) {
        super(gpConfig, urlString);
    }

    public boolean isDirectory() throws SocketException, IOException {
        FTPClient ftpClient=null;
        try {
            ftpClient=connect("anonymous", "gp-help@broadinstute.org");
            boolean success=ftpClient.changeWorkingDirectory(getUrl().getPath());
            return success;
        }
        finally {
            disconnect(ftpClient);
        }
    }
    
    /**
     * Connects to a remote FTP server
     */
    protected FTPClient connect(final String username, final String password) throws SocketException, IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(getUrl().getHost());
        int returnCode = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(returnCode)) {
            throw new IOException("Could not connect");
        }
        boolean loggedIn = 
                ftpClient.login(username, password);
        if (!loggedIn) {
            throw new IOException("Could not login");
        }
        if (log.isDebugEnabled()) {
            log.debug("Connected and logged in.");
        }
        return ftpClient;
    }

    /**
     * Logs out and disconnects from the server
     */
    protected void disconnect(FTPClient ftpClient) throws IOException {
        if (ftpClient != null && ftpClient.isConnected()) {
            ftpClient.logout();
            ftpClient.disconnect();
            if (log.isDebugEnabled()) {
                log.debug("Logged out");
            }
        }
    }
    
    @Override
    public boolean downloadFile(URL fromUrl, File toFile, boolean deleteExisting, int connectTimeout_ms, int readTimeout_ms) throws IOException, InterruptedException, DownloadException { 
        if (deleteExisting==false) {
            throw new DownloadException("deleteExisting must be false");
        }
        FileUtils.copyURLToFile(fromUrl, toFile, connectTimeout_ms, readTimeout_ms);
        return true;
    }
}
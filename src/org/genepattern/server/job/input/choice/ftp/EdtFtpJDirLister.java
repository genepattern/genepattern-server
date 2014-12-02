package org.genepattern.server.job.input.choice.ftp;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.job.input.choice.RemoteDirLister;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPFile;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;

public class EdtFtpJDirLister implements RemoteDirLister<FTPFile,ListFtpDirException> {
    private static final Logger log = Logger.getLogger(EdtFtpJDirLister.class);

    protected FileTransferClient initFtpClient(final URL fromUrl) throws ListFtpDirException {
        final String host=fromUrl.getHost();
        final FileTransferClient ftp = new FileTransferClient();
        boolean error=false;
        try {
            ftp.setRemoteHost(host);
            ftp.setUserName("anonymous");
            ftp.setPassword("gp-help@broadinstute.org");
            ftp.connect();
            ftp.setContentType(FTPTransferType.BINARY);
            ftp.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
        }
        catch (FTPException e) {
            error=true;
            log.debug("Error initializing connection to "+fromUrl);
            throw new ListFtpDirException("Error initializing connection to "+fromUrl);
        }
        catch (IOException e) {
            error=true;
            log.debug("Error connecting to "+fromUrl, e);
            throw new ListFtpDirException("Error connecting to "+fromUrl);
        }
        finally {
            if (error) {
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    }
                    catch (Throwable t) {
                        log.error("Error disconnecting ftp client, fromUrl="+fromUrl, t);
                    }
                }
            }
        }
        return ftp;
    }

    protected URL initUrl(String str) throws ListFtpDirException {
        try {
            return new URL(str);
        }
        catch (Throwable t) {
            log.error("Invalid url="+str, t);
            throw new ListFtpDirException("Invalid url="+str);
        }
    }
    
    @Override
    public FTPFile[] listFiles(String ftpDir) throws ListFtpDirException {
        URL url = initUrl(ftpDir);
        FileTransferClient ftpClient=initFtpClient(url);
        try {
            return ftpClient.directoryList(url.getPath());
        }
        catch (Throwable t) {
            log.error("Error listing ftpDir="+ftpDir, t);
            throw new ListFtpDirException("Error listing ftpDir="+ftpDir);
        }        
    }

}


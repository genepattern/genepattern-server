/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.cache;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;

/**
 * FTP downloader implemented with edtFTPj library, wrapped in a new thread, so that we can
 * cancel the download in response to an interrupted exception.
 * 
 * @author pcarr
 *
 */
public final class EdtFtpJImpl extends CachedFtpFile {
    private static Logger log = Logger.getLogger(EdtFtpJImpl.class);
    final ExecutorService ex;

    EdtFtpJImpl(final GpConfig gpConfig, final String urlString, final ExecutorService ex) {
        super(gpConfig, urlString);
        this.ex=ex;
    }

    protected FileTransferClient initFtpClient(final URL fromUrl) throws DownloadException, IOException {
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
            throw new DownloadException("Error downloading file from "+fromUrl, e);
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
    
    public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting, final int connectTimeout_ms, final int readTimeout_ms) throws IOException, InterruptedException, DownloadException {  
        if (deleteExisting==false) {
            throw new DownloadException("deleteExisting must be false");
        }
        CachedFileUtil.mkdirs(toFile);
        final FileTransferClient ftp = initFtpClient(fromUrl);

        //start download in new thread
        Future<Boolean> future = ex.submit( new Callable<Boolean> () {
            @Override
            public Boolean call() throws IOException, FTPException {
                try {
                    ftp.downloadFile(toFile.getAbsolutePath(), fromUrl.getPath());
                    return true;
                }
                finally {
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
        });

        //monitor the process, so that we can be cancelled by an interrupt
        try {
            final boolean status = future.get();
            return status;
        }
        catch (ExecutionException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            else if (e.getCause() instanceof FTPException) {
                throw new DownloadException("Error downloading file from "+fromUrl, e.getCause());
            }
            else {
                throw new DownloadException("Error downloading file from "+fromUrl, e.getCause());
            }
        }
        catch (InterruptedException e) {
            //if we are interrupted, cancel the download
            ftp.cancelAllTransfers();
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
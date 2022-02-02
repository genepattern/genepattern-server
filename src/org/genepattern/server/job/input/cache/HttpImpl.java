/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
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
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.genepattern.FileDownloader;
import org.genepattern.server.webservice.server.Status;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;

/**
 * HTTP downloader meant to blend in with the ridiculously complex 
 * FTP downloaders implemented with edtFTPj library, wrapped in a new thread, so that we can
 * cancel the download in response to an interrupted exception.
 *
 */
public final class HttpImpl extends CachedFtpFile {
    private static Logger log = Logger.getLogger(HttpImpl.class);
    final ExecutorService ex;

    public HttpImpl(final HibernateSessionManager mgr, final GpConfig gpConfig, final String urlString, final ExecutorService ex) {
        super(mgr, gpConfig, urlString);
        this.ex=ex;
    }

    public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting, final int connectTimeout_ms, final int readTimeout_ms) throws IOException, InterruptedException, DownloadException {  
        if (deleteExisting==false) {
            throw new DownloadException("deleteExisting must be false");
        }
        CachedFileUtil.mkdirs(toFile);
  
        //start download in new thread
        Future<Boolean> future = ex.submit( new Callable<Boolean> () {
            @Override
            public Boolean call() throws IOException {
                long downloadedBytes = FileDownloader.downloadFile(toFile, fromUrl, null,99);
                return downloadedBytes >0;
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
            
            Thread.currentThread().interrupt();
            return false;
        }
    }

}

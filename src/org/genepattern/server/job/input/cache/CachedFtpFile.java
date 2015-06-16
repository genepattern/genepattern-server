/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.cache;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.JobInputHelper;


/**
 * A file downloader for an external FTP file. 
 * It saves the file as a virtual user upload file for the hidden, '.cache', user account.
 * 
 */
abstract public class CachedFtpFile implements CachedFile {
    private static final Logger log = Logger.getLogger(CachedFtpFile.class);
    
    private final GpConfig gpConfig;
    private final URL url;
    private final GpFilePath localPath;
    
    protected CachedFtpFile(final GpConfig gpConfigIn, final String urlString) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing CachedFtpFile, type="+this.getClass().getName());
        }
        if (gpConfigIn==null) {
            this.gpConfig=ServerConfigurationFactory.instance();
        }
        else {
            this.gpConfig=gpConfigIn;
        }
        this.url=JobInputHelper.initExternalUrl(urlString);
        if (url==null) {
            throw new IllegalArgumentException("value is not an external url: "+urlString);
        }
        this.localPath=CachedFileUtil.getLocalPath(gpConfig, url);
        if (this.localPath==null) {
            throw new IllegalArgumentException("error initializing local path for external url: "+urlString);
        }
        if (log.isDebugEnabled()) {
            log.debug("url="+url.toExternalForm());
            log.debug("localPath.serverFile="+localPath.getServerFile());
        }
    }
    
    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public GpFilePath getLocalPath() {
        return localPath;
    }

    /**
     * Do we already have a local copy of the file?
     * @return
     */
    @Override
    public boolean isDownloaded() {
        return localPath.getServerFile().exists();
    }

    /**
     * Download the file. This method blocks until the transfer is complete.
     * @return
     * @throws DownloadException
     */
    @Override
    public GpFilePath download() throws DownloadException {
        try {
            doDownload(localPath, url);
            return localPath;
        }
        catch (DownloadException e) {
            throw e;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return localPath;
    }
    
    /**
     * Download from the url into a file, creating the file and all parent directories if necessary.
     * This is implemented using basic Java I/O capabilities (pre Java 7 NIO).
     * 
     * see: http://www.ibm.com/developerworks/java/library/j-jtp05236/index.html
     * 
     * @param fromUrl
     * @param toFile
     * @throws IOException
     */
    public final void downloadFile(final URL fromUrl, final File toFile) throws IOException, InterruptedException, DownloadException {
        final boolean replaceExisting=true;
        downloadFile(fromUrl, toFile, replaceExisting);
    }
    public final boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting) throws IOException, InterruptedException, DownloadException {
        final int connectTimeout_ms=60*1000; //wait up to 60 seconds to establish a connection
        final int readTimeout_ms=60*1000; //wait up to 60 seconds when reading from the input 
        return downloadFile(fromUrl, toFile, deleteExisting, connectTimeout_ms, readTimeout_ms);
    }

    abstract public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting, final int connectTimeout_ms, final int readTimeout_ms)
    throws IOException, InterruptedException, DownloadException;

    /**
     * Copy data from an external URL into a file in the GP user's uploads directory.
     * This method blocks until the data file has been transferred. If the file has 
     * already been cached, and the cached copy is up to date, it doesn't transfer 
     * the file at all, but relies on the cached copy.
     * 
     * TODO: limit the size of the file which can be transferred
     * TODO: implement a timeout
     * 
     * Notes:
     *     Is it possible to interrupt FileUtils.copyURLToFile? I'm not sure. This thread is inconclusive.
     *     http://stackoverflow.com/questions/10535335/apache-commons-copyurltofile-possible-to-stop-copying
     * 
     * @param realPath
     * @param url
     * @throws Exception
     */
    private void doDownload(final GpFilePath realPath, final URL url) throws DownloadException, InterruptedException {
        // If the real path exists, assume it's up to date
        final File realFile = realPath.getServerFile();
        if (realFile.exists()) {
            if (log.isDebugEnabled()) {
                log.debug("file already downloaded to "+
                        realPath.getServerFile()+"\n"+
                        "from url "+url);
            }
            return;
        }

        // otherwise, download to tmp location
        final GpFilePath tempPath = CachedFileUtil.getLocalPathForDownloadingFile(gpConfig, url);
        final File tempFile = tempPath.getServerFile();
        boolean deleteExisting=true;
        boolean interrupted=false;
        try {
            if (log.isDebugEnabled()) {
                log.debug("downloading from "+url+" to "+tempFile);
            }
            boolean success=downloadFile(url, tempFile, deleteExisting);
            if (!success) {
                throw new DownloadException("Error downloading from '"+url.toExternalForm()+"' to temp file: "+tempFile.getAbsolutePath());
            }
        }
        catch (IOException e) {
            log.error("I/O Exception while downloading file: "+url.toExternalForm(), e);
            throw new DownloadException("I/O Exception while downloading file: "+url.toExternalForm());
        }
        catch (InterruptedException e) {
            interrupted=true;
        }
        if (interrupted) {
            if (log.isDebugEnabled()) {
                log.debug("download interrupted, deleting partial download");
            }
            // Note: we may want to leave it around so that we can pick up from where we left off
            boolean deleted=tempFile.delete();
            if (!deleted) {
                log.error("failed to delete tempFile: "+tempFile);
            }
            else if (log.isDebugEnabled()) {
                log.debug("deleted tempFile: "+tempFile);
            }
            throw new InterruptedException();
        }

        // Add it to the database
        try {
            JobInputFileUtil.__addUploadFileToDb(realPath);
        }
        catch (Throwable t) {
            //ignore this, because we don't rely on the DB entry for managing cached data files
            log.error("Unexpected error recording uploaded file to DB for realPath="+realPath, t);
        }
        // Once complete, move the file to the real location and return
        final File realParent=realFile.getParentFile();
        if (realParent != null && !realParent.exists()) {
            boolean createdDir=realParent.mkdirs();
            if (log.isDebugEnabled()) {
                log.debug(realParent+".mkdirs returned "+createdDir);
            }
        }
        boolean success = tempFile.renameTo(realFile);
        if (!success) {
            String message = "Error moving temp file to real location: temp=" + tempFile.getPath() + ", real=" + realFile.getPath();
            log.error(message);
            throw new DownloadException(message);
        }
    }
}

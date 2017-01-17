package org.genepattern.server.genepattern;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.genepattern.server.webservice.server.Status;

/**
 * File download utility for transferring files from remote URLs into the GP server file system.
 * 
 * Originally created from code in GenePatternAnalysisTask for downloading zip files 
 * from the module repository.
 * 
 * @author pcarr
 *
 */
public class FileDownloader {
    private static final Logger log = Logger.getLogger(FileDownloader.class);

    public static String downloadTask(final String zipURL, final Status statusMonitor)
            throws IOException {
        return downloadTask(new URL(zipURL), statusMonitor);
    }

    /**
     * downloads a file from a URL and returns the path to the local file to the caller.
     * 
     * @param zipURL String URL of file to download
     * @param statusMonitor
     * @param expectedLength
     * @param verbose
     * @return String filename of temporary downloaded file on server
     * @throws IOException
     */
    public static String downloadTask(final URL zipURL, final Status statusMonitor)
            throws IOException {
        
        final File zipFile = File.createTempFile("task", ".zip");
        zipFile.deleteOnExit();
        try {
            final long downloadedBytes = downloadFile(zipFile, zipURL, statusMonitor);
            log.debug("downloaded " + downloadedBytes + " bytes");
            if (statusMonitor != null) {
                statusMonitor.endProgress();
                statusMonitor.statusMessage("downloaded " + downloadedBytes + " bytes");
            }
            return zipFile.getPath();
        }
        catch (IOException ioe) {
            zipFile.delete();
            log.info("Error in downloadTask: " + ioe.getMessage());
            zipFile.delete();
            throw ioe;
        }
    }

    protected static long getContentLength(final URLConnection uc) {
        if (uc instanceof HttpURLConnection) {
            return ((HttpURLConnection) uc).getHeaderFieldInt("Content-Length", -1);
        }
        else {
            return uc.getContentLength();
        }
    }

    public static long downloadFile(final File toFile, final URL fromUrl, final Status statusMonitor)
            throws IOException {
        long downloadedBytes = 0;
        log.debug("downloading file from url='"+fromUrl+"'");
        final FileOutputStream os = new FileOutputStream(toFile);
        final URLConnection uc = fromUrl.openConnection();
        log.debug("opened connection");
        final long downloadSize = getContentLength(uc);
        if ((statusMonitor != null) && (downloadSize != -1)) {
            statusMonitor.statusMessage("Download length: " + (long) downloadSize + " bytes."); 
        }
        if ((statusMonitor != null)) {
            statusMonitor.beginProgress("download");
        }
        InputStream is = uc.getInputStream();
        byte[] buf = new byte[100000];
        int i;
        long lastPercent = 0;
        while ((i = is.read(buf, 0, buf.length)) > 0) {
            downloadedBytes += i;
            os.write(buf, 0, i);
            if (downloadSize > 0) {
                long pctComplete = 100 * downloadedBytes / downloadSize;
                if (lastPercent != pctComplete) {
                    if (statusMonitor != null) {
                        // Each dot represents 100KB.
                        statusMonitor.continueProgress((int) pctComplete);
                    }
                    lastPercent = pctComplete;
                }
            }
        }
        is.close();
        os.close();
        if (downloadedBytes == 0) {
            throw new IOException("Nothing downloaded from " + fromUrl);
        }
        return downloadedBytes;
    }

}

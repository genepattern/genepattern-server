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

    protected static long getContentLength(final URLConnection uc) {
        if (uc instanceof HttpURLConnection) {
            return ((HttpURLConnection) uc).getHeaderFieldInt("Content-Length", -1);
        }
        else {
            return uc.getContentLength();
        }
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
    public static String downloadTask(final String zipURL, final Status statusMonitor)
            throws IOException {
        File zipFile = null;
        long downloadedBytes = 0;
        try {
            log.debug("downloading file from zipUrl='"+zipURL+"'");
            zipFile = File.createTempFile("task", ".zip");
            zipFile.deleteOnExit();
            final FileOutputStream os = new FileOutputStream(zipFile);
            final URLConnection uc = new URL(zipURL).openConnection();
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
                throw new IOException("Nothing downloaded from " + zipURL);
            }
            return zipFile.getPath();
        }
        catch (IOException ioe) {
            log.info("Error in downloadTask: " + ioe.getMessage());
            zipFile.delete();
            throw ioe;
        }
        finally {
            log.debug("downloaded " + downloadedBytes + " bytes");
            if (statusMonitor != null) {
                statusMonitor.endProgress();
                statusMonitor.statusMessage("downloaded " + downloadedBytes + " bytes");
            }
        }
    }

}

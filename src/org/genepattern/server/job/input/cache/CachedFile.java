package org.genepattern.server.job.input.cache;

import java.net.URL;

import org.genepattern.server.dm.GpFilePath;

public interface CachedFile {

    URL getUrl();

    GpFilePath getLocalPath();

    /**
     * Do we already have a local copy of the file?
     * @return
     */
    boolean isDownloaded();

    /**
     * Download the file. This method blocks until the transfer is complete.
     * @return
     * @throws DownloadException
     */
    GpFilePath download() throws DownloadException;

}
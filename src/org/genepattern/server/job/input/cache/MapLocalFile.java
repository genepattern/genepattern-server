/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.cache;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;

/**
 * A virtual CachedFile, which is loaded directly from a local directory path,
 * configured in the config.yaml local.choiceDirs map.
 * 
 * This is instead of a file which is downloaded into a the cache from an external file.
 */
public class MapLocalFile implements CachedFile {
    private final static Logger log = Logger.getLogger(MapLocalFile.class);

    final URL url;
    final GpFilePath localPath;
    
    public MapLocalFile(final String fromUrl, final File toLocalPath) throws MalformedURLException {
        this.url=new URL(fromUrl);
        this.localPath=new MappedGpFilePath(toLocalPath);
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public GpFilePath getLocalPath() {
        return localPath;
    }

    @Override
    public boolean isDownloaded() {
        if (localPath.getServerFile().exists()) {
            return true;
        }
        return false;
    }

    @Override
    public GpFilePath download() throws DownloadException {
        if (!localPath.getServerFile().exists()) {
            log.error("local file does not exist: "+localPath.getServerFile()+", invalid mapping from external url="+url);
        }
        return localPath;
    }

}

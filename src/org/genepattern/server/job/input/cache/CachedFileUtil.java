package org.genepattern.server.job.input.cache;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;

public class CachedFileUtil {
    private static final Logger log = Logger.getLogger(CachedFileUtil.class);

    /**
     * Initialize a GpFilePath instance from the external url.
     * This method does not download the file, it does define the path
     * to where the external URL is to be downloaded.
     * 
     * This method was created for the specific use-case of caching an external url
     * selected from a File Choice parameter.
     * 
     * @param url
     * @return
     */
    public static final GpFilePath getLocalPath(final GpConfig gpConfig, final URL url) {
        return getLocalPathForFile(gpConfig, url);
    }
    
    public static final GpFilePath getLocalPathForDownloadingFile(final GpConfig gpConfig, final URL url) {
        return getLocalPath(gpConfig, url, "cache.downloading");
    }
    
    public static final GpFilePath getLocalPathForFile(final GpConfig gpConfig, final URL url) {
        return getLocalPath(gpConfig, url, "cache");
    }
    
    public static final GpFilePath getLocalPathForDir(final GpConfig gpConfig, final URL url) {
        return getLocalPath(gpConfig, url, "cache.dir");
    }
    
    protected static final GpFilePath getLocalPath(final GpConfig gpConfig, final URL fromExternalUrl, final String toRootDir) {
        final GpContext userContext=GpContext.getContextForUser(FileCache.CACHE_USER_ID);
        final String relPath= toRootDir+"/"+fromExternalUrl.getHost()+"/"+fromExternalUrl.getPath();
        final File relFile=new File(relPath);
        try {
            GpFilePath localPath=GpFileObjFactory.getUserUploadFile(gpConfig, userContext, relFile);
            return localPath;
        }
        catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    /**
     * If necessary, create the parent directory.
     */
    public static void mkdirs(final File toFile) throws DownloadException {
        final File parentDir=toFile.getParentFile();
        if (log.isDebugEnabled()) { log.debug("parentDir="+parentDir.getAbsolutePath()); }
        if (parentDir != null) {
            if (!parentDir.exists()) {
                if (log.isDebugEnabled()) {
                    log.debug("parentDir doesn't exist, mkdirs for "+parentDir.getAbsolutePath());
                }
                final boolean success=parentDir.mkdirs();
                log.debug("mkdirs result="+success);
                if (!success) {
                    log.warn("mkdirs result="+success+", for parentDir="+parentDir.getAbsolutePath());
                }
            }
        }
        if (!parentDir.exists()) {
            //log.error("Error downloading file from '"+fromUrl+"' to '"+toFile.getAbsolutePath()+"', parentDir doesn't exist: "+parentDir.getAbsolutePath());
            throw new DownloadException("Error creating parent download directory: "+parentDir.getAbsolutePath());
        }
    }

}

package org.genepattern.server.job.input.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.JobInputHelper;

/**
 * A file downloader for an external FTP file. 
 * It saves the file as a virtual user upload file for the hidden, '.cache', user account.
 * 
 */
public class CachedFtpFile implements CachedFile {
    private static Logger log = Logger.getLogger(CachedFtpFile.class);
    
    // this is the same default as edtFTP
    final public static int DEFAULT_BUFFER_SIZE = 16384;

    private final URL url;
    private final GpFilePath localPath;
    
    public CachedFtpFile(final String urlString) {
        this.url=JobInputHelper.initExternalUrl(urlString);
        if (url==null) {
            throw new IllegalArgumentException("value is not an external url: "+urlString);
        }
        this.localPath=getLocalPath(url);
        if (this.localPath==null) {
            throw new IllegalArgumentException("error initializing local path for external url: "+urlString);
        }
    }
    
    private GpFilePath getLocalPath(final URL url) {
        final Context userContext=ServerConfiguration.Context.getContextForUser(".cache");
        final String relPath="cache/"+url.getHost()+"/"+url.getPath();
        final File relFile=new File(relPath);
        try {
            GpFilePath localPath=GpFileObjFactory.getUserUploadFile(userContext, relFile);
            return localPath;
        }
        catch (Exception e) {
            log.error(e);
        }
        return null;
    }
    
    public URL getUrl() {
        return url;
    }
    
    public GpFilePath getLocalPath() {
        return localPath;
    }

    /**
     * Do we already have a local copy of the file?
     * @return
     */
    public boolean isDownloaded() {
        return localPath.getServerFile().exists();
    }

    /**
     * Download the file. This method blocks until the transfer is complete.
     * @return
     * @throws DownloadException
     */
    public GpFilePath download() throws DownloadException {
        try {
            copyExternalUrlToUserUploads(localPath, url);
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
     * Given the real path for the download, get the temp path
     * @param realPath
     * @return
     * @throws Exception 
     */
    private GpFilePath getTempPath(GpFilePath realPath) throws DownloadException {
        Context userContext = ServerConfiguration.Context.getContextForUser(".cache");
        //String tempPath = realPath.getRelativePath() + ".downloading";
        String tempPath = FilenameUtils.getPath(realPath.getRelativePath()) + ".downloading/" + FilenameUtils.getName(realPath.getRelativePath());
        File tempFile = new File(tempPath);
        try {
            GpFilePath gpFilePath=GpFileObjFactory.getUserUploadFile(userContext, tempFile);
            return gpFilePath;
        }
        catch (Exception e) {
            log.error(e);
            throw new DownloadException("GP server error initializing temp path: "+tempPath);
        }
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
    public static void downloadFile(final URL fromUrl, final File toFile) throws IOException, InterruptedException {
        final boolean replaceExisting=true;
        downloadFile(fromUrl, toFile, replaceExisting);
    }
    public static boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting) throws IOException, InterruptedException {
        if (toFile==null) {
            throw new IllegalArgumentException("toFile==null");
        }
        if (toFile.exists()) {
            if (deleteExisting) {
                boolean success=toFile.delete();
                if (!success) {
                    throw new IllegalArgumentException("failed to delete existing file: "+toFile.getAbsolutePath());
                }
            }
            else {
                throw new IllegalArgumentException("file already exists: "+toFile.getAbsolutePath());
            }
        }

        //if necessary, create parent download directory
        final File parentDir=toFile.getParentFile();
        if (parentDir != null) {
            if (!parentDir.exists()) {
                boolean success=parentDir.mkdirs();
                if (!success) {
                    throw new IllegalArgumentException("Failed to create parent download directory for file: "+toFile.getAbsolutePath());
                }
            }
        }
        boolean interrupted=false;
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            final int bufsize=DEFAULT_BUFFER_SIZE;

            in = new BufferedInputStream(fromUrl.openStream());
            fout = new FileOutputStream(toFile);

            final byte data[] = new byte[bufsize];
            int count;
            while (!interrupted && (count = in.read(data, 0, bufsize)) != -1) {
                fout.write(data, 0, count);
                if (Thread.interrupted()) {
                    interrupted=true;
                }
                //sleep for a bit so that we can allow other threads to move along
                Thread.sleep(100);
            }
        }
        finally {
            if (in != null) {
                in.close();
            }
            if (fout != null) {
                fout.close();
            }
        }
        if (interrupted) {
            //Thread.currentThread().interrupt();
            throw new InterruptedException();
        }
        return true;
    }

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
    public void copyExternalUrlToUserUploads(final GpFilePath realPath, final URL url) throws DownloadException, InterruptedException {
        // If the real path exists, assume it's up to date
        final File realFile = realPath.getServerFile();
        if (realFile.exists()) {
            return;
        }

        // otherwise, download to tmp location
        final GpFilePath tempPath = getTempPath(realPath);
        final File tempFile = tempPath.getServerFile();

        boolean deleteExisting=true;
        boolean interrupted=false;
        try {
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
            // blow away the partial download
            // Note: we may want to leave it around so that we can pick up from where we left off
            boolean deleted=tempFile.delete();
            if (!deleted) {
                log.error("failed to delete tempFile: "+tempFile);
            }
            throw new InterruptedException();
        }

        // Add it to the database
        try {
            JobInputFileUtil.__addUploadFileToDb(realPath);
        }
        catch (Exception e) {
            //ignore this, because we don't rely on the DB entry for managing cached data files
            log.error(e);
        }
        // Once complete, move the file to the real location and return
        boolean success = tempFile.renameTo(realFile);
        if (!success) {
            String message = "Error moving temp file to real location: temp=" + tempFile.getPath() + ", real=" + realFile.getPath();
            log.error(message);
            throw new DownloadException(message);
        }
    }

}
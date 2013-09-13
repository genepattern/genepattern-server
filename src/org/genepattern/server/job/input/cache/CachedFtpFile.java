package org.genepattern.server.job.input.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.JobInputHelper;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;

/**
 * A file downloader for an external FTP file. 
 * It saves the file as a virtual user upload file for the hidden, '.cache', user account.
 * 
 */
abstract public class CachedFtpFile implements CachedFile {
    private static Logger log = Logger.getLogger(CachedFtpFile.class);
    
    
    /**
     * Enum of downloaders, used to specify which implementation to use.
     * @author pcarr
     */
    public static enum Type {

        /** Hand coded implementation with Java 6 standard library methods */
        JAVA_6 {
            @Override
            public CachedFtpFile newCachedFtpFile(final String urlString) {
                return Factory.instance().newStdJava6Impl(urlString);
            }
        },
        /** Apache Commons Net 3.3 implementation */
        COMMONS_NET_3_3 {
            @Override
            public CachedFtpFile newCachedFtpFile(String urlString) {
                return Factory.instance().newApacheCommonsImpl(urlString);
            }
        },
        /** edtFTPj, Enterprise Distributed Technologies library, with additional handling for interrupted exceptions */
        EDT_FTP_J {
            @Override
            public CachedFtpFile newCachedFtpFile(String urlString) {
                return Factory.instance().newEdtFtpJImpl(urlString);
            }
        },
        /** edtFTPj, Enterprise Distributed Technologies library, single line static method call, with no support for handling interrupted exceptions */
        EDT_FTP_J_SIMPLE {
            @Override
            public CachedFtpFile newCachedFtpFile(String urlString) {
                return Factory.instance().newEdtFtpJImpl_simple(urlString);
            }
        };

        public static final String PROP_FTP_DOWNLOADER_TYPE="ftpDownloader.type";

        /**
         * Get or create a new instance of the downloader for the given url value.
         * @param urlString
         * @return
         */
        public abstract CachedFtpFile newCachedFtpFile(final String urlString);
    }

    public static class Factory {
        private Factory() {}
 
        private static class LazyHolder {
            private static final Factory INSTANCE = new Factory();
        }
 
        public static Factory instance() {
            return LazyHolder.INSTANCE;
        }
        
        private final ExecutorService interruptionService=Executors.newCachedThreadPool();
        public void shutdownNow() {
            interruptionService.shutdownNow();
        }
        
        private Type defaultType=Type.JAVA_6;

        public CachedFtpFile newCachedFtpFile(final String urlString) {
            ServerConfiguration.Context serverContext=ServerConfiguration.Context.getServerContext();
            String str=ServerConfiguration.instance().getGPProperty(serverContext, Type.PROP_FTP_DOWNLOADER_TYPE, Type.JAVA_6.name());
            Type type=null;
            try {
                type=Type.valueOf(str);
            }
            catch (Throwable t) {
                log.error(t);
                type=defaultType;
            }
            return newCachedFtpFile(type, urlString);
        }

        public CachedFtpFile newCachedFtpFile(final Type type, final String urlString) {
            if (type==null) {
                log.error("type==null");
                return defaultType.newCachedFtpFile(urlString);
            }
            return type.newCachedFtpFile(urlString);
        }

        /**
         * This FTP downloader is implemented with standard Java 6 libraries.
         * @param urlString
         * @return
         */
        public CachedFtpFile newStdJava6Impl(final String urlString) {
            return new StdJava_6_Impl(urlString);
        }
        
        /**
         * This FTP downloader uses the apache commons FTPClient. 
         * @param urlString
         * @return
         */
        public CachedFtpFile newApacheCommonsImpl(final String urlString) {
            return new CommonsNet_3_3_Impl(urlString);
        }
        
        /**
         * This FTP downloader uses the edtFTPj library, with additional support
         * for handling interrupted exceptions.
         * 
         * @param urlString
         * @return
         */
        public CachedFtpFile newEdtFtpJImpl(final String urlString) {
            return new EdtFtpJImpl(urlString, interruptionService);
        }
        
        /**
         * strictly for debugging and testing, this implementation uses the basic static 
         * method available from the edtFTPj library. It is not wrapped in a separate
         * thread for handling interrupted exceptions.
         * 
         * @param urlString
         * @return
         */
        public CachedFtpFile newEdtFtpJImpl_simple(final String urlString) {
            return new EdtFtpJ_simple(urlString);
        }


    }

    private final URL url;
    private final GpFilePath localPath;
    
    private CachedFtpFile(final String urlString) {
        this.url=JobInputHelper.initExternalUrl(urlString);
        if (url==null) {
            throw new IllegalArgumentException("value is not an external url: "+urlString);
        }
        this.localPath=getLocalPath(url);
        if (this.localPath==null) {
            throw new IllegalArgumentException("error initializing local path for external url: "+urlString);
        }
    }
    
    protected GpFilePath getLocalPath(final URL url) {
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
    
    /**
     * If necessary, create the parent directory.
     */
    protected void mkdirs(final File toFile) throws DownloadException {
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
     * Given the real path for the download, get the temp path
     * @param realPath
     * @return
     * @throws Exception 
     */
    public GpFilePath getTempPath(GpFilePath realPath) throws DownloadException {
        Context userContext = ServerConfiguration.Context.getContextForUser(".cache");
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
    public void downloadFile(final URL fromUrl, final File toFile) throws IOException, InterruptedException, DownloadException {
        final boolean replaceExisting=true;
        downloadFile(fromUrl, toFile, replaceExisting);
    }
    public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting) throws IOException, InterruptedException, DownloadException {
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
    public void doDownload(final GpFilePath realPath, final URL url) throws DownloadException, InterruptedException {
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
    
    public static final class StdJava_6_Impl extends CachedFtpFile {
        private static Logger log = Logger.getLogger(StdJava_6_Impl.class);
        // this is the same default as edtFTP
        final public static int DEFAULT_BUFFER_SIZE = 16384;

        private StdJava_6_Impl(final String urlString) {
            super(urlString);
        }

        public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting, final int connectTimeout_ms, final int readTimeout_ms) 
                throws IOException, InterruptedException, DownloadException {
            if (toFile==null) {
                throw new DownloadException("Invalid arg: toFile==null");
            }
            if (toFile.exists()) {
                if (log.isDebugEnabled()) { log.debug("toFile exists: "+toFile.getAbsolutePath()); }
                if (deleteExisting) {
                    if (log.isDebugEnabled()) { 
                        log.debug("deleting existing file: "+toFile.getAbsolutePath());
                    }
                    final boolean success=toFile.delete();
                    log.debug("success="+success);
                    if (!success) {
                        throw new DownloadException("failed to delete existing file: "+toFile.getAbsolutePath());
                    }
                }
                else {
                    throw new DownloadException("file already exists: "+toFile.getAbsolutePath());
                }
            }

            //if necessary, create parent download directory
            mkdirs(toFile);

            boolean interrupted=false;
            BufferedInputStream in = null;
            FileOutputStream fout = null;
            try {
                final int bufsize=DEFAULT_BUFFER_SIZE;

                URLConnection connection=fromUrl.openConnection();
                connection.setConnectTimeout(connectTimeout_ms);
                connection.setReadTimeout(readTimeout_ms);
                connection.connect();
                InputStream urlIn=connection.getInputStream();
                in = new BufferedInputStream(urlIn);
                fout = new FileOutputStream(toFile);

                final byte data[] = new byte[bufsize];
                int count;
                while (!interrupted && (count = in.read(data, 0, bufsize)) != -1) {
                    fout.write(data, 0, count);
                    if (Thread.interrupted()) {
                        interrupted=true;
                    }
                    Thread.yield();
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
                throw new InterruptedException();
            }
            return true;
        }
    }

    public static final class CommonsNet_3_3_Impl extends CachedFtpFile {

        private CommonsNet_3_3_Impl(final String urlString) {
            super(urlString);
        }

        @Override
        public boolean downloadFile(URL fromUrl, File toFile, boolean deleteExisting, int connectTimeout_ms, int readTimeout_ms) throws IOException, InterruptedException, DownloadException { 
            if (deleteExisting==false) {
                throw new DownloadException("deleteExisting must be false");
            }
            FileUtils.copyURLToFile(fromUrl, toFile, connectTimeout_ms, readTimeout_ms);
            return true;
        }
    }

    public static final class EdtFtpJ_simple extends CachedFtpFile {
        private EdtFtpJ_simple(final String urlString) {
            super(urlString);
        }

        @Override
        public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting, final int connectTimeout_ms, final int readTimeout_ms) throws IOException, InterruptedException, DownloadException {
            if (deleteExisting==false) {
                throw new DownloadException("deleteExisting must be false");
            }
            mkdirs(toFile);
            try {
                FileTransferClient.downloadURLFile(toFile.getAbsolutePath(), fromUrl.toExternalForm());
                return true;
            }
            catch (FTPException e) {
                throw new DownloadException("Error downloading file from "+fromUrl, e);
            }
        }
    }

    /**
     * FTP downloader implemented with edtFTPj library, wrapped in a new thread, so that we can
     * cancel the download in response to an interrupted exception.
     * 
     * @author pcarr
     *
     */
    public static final class EdtFtpJImpl extends CachedFtpFile {
        private static Logger log = Logger.getLogger(EdtFtpJImpl.class);
        final ExecutorService ex;

        private EdtFtpJImpl(final String urlString, final ExecutorService ex) {
            super(urlString);
            this.ex=ex;
        }

        public boolean downloadFile(final URL fromUrl, final File toFile, final boolean deleteExisting, final int connectTimeout_ms, final int readTimeout_ms) throws IOException, InterruptedException, DownloadException {  
            if (deleteExisting==false) {
                throw new DownloadException("deleteExisting must be false");
            }
            mkdirs(toFile);
            final FileTransferClient ftp = new FileTransferClient();
            boolean error=false;
            try {
                ftp.setRemoteHost(fromUrl.getHost());
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

            if (error) {
                return false;
            }

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
}
package org.genepattern.server.job.input.cache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.log4j.Logger;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.choice.DynamicChoiceInfoParser;
import org.genepattern.server.job.input.choice.FtpDirFilter;

/**
 * For sync'ing a remote directory.
 * Example use-case, a user selects a remote ftp directory from a dynamic drop-down menu, e.g.
 *     bowtie.index=ftp://gpftp.broadinstitute.org/module_support_files/bowtie2/index/by_genome/Mus_musculus_UCSC_mm10
 * @author pcarr
 *
 */
public class CachedFtpDir implements CachedFile {
    private static Logger log = Logger.getLogger(CachedFtpDir.class);

    public static void writeToFile(final String message, final File toFile) {
        toFile.getParentFile().mkdirs();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(toFile));
            writer.write(message);
        } 
        catch (IOException e) {
            log.error("Error writing file="+toFile, e);
        } 
        finally {
            if (writer != null) {
                try {
                    writer.close();
                } 
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final URL url;
    private final GpFilePath localPath;
    // the tmpDir contains a file indicating the status of the file transfer, so that we can
    // check for completed downloads after a server restart
    private final GpFilePath tmpDir;
    
    public CachedFtpDir(final String urlString) {
        if (log.isDebugEnabled()) {
            log.debug("Initializing CachedFtpFile, type="+this.getClass().getName());
        }
        this.url=JobInputHelper.initExternalUrl(urlString);
        if (url==null) {
            throw new IllegalArgumentException("value is not an external url: "+urlString);
        }
        this.localPath=CachedFtpFile.getLocalPath(url);
        if (this.localPath==null) {
            throw new IllegalArgumentException("error initializing local path for external url: "+urlString);
        }
        if (log.isDebugEnabled()) {
            log.debug("url="+url.toExternalForm());
            log.debug("localPath.serverFile="+localPath.getServerFile());
        }
        this.tmpDir=CachedFtpFile.getLocalPathForDir(url);
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
     * Download the file. This method blocks until the transfer is complete.
     * @return
     * @throws DownloadException
     */
    @Override
    public GpFilePath download() throws DownloadException {
        try {
            doDownload();
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

    private GpFilePath doDownload() throws DownloadException, InterruptedException {
        try {
            clean();
        }
        catch (Throwable t) {
            log.error("Error preparing tmp dir before directory download from url="+url+" to "+localPath, t);
            throw new DownloadException(t.getLocalizedMessage());
        }
        
        final List<String> filesToDownload = getFilesToDownload();
        // loop through all of the files and start downloading ...
        try {
            final boolean isDir=false;
            for(final String fileToDownload : filesToDownload) {
                try {
                    final Future<?> f = FileCache.instance().getFutureObj(fileToDownload, isDir);
                    f.get(100, TimeUnit.MILLISECONDS);
                }
                catch (TimeoutException e) {
                    //skip, it means the file is still downloading
                }
                Thread.yield();
            }
            // now loop through all of the files and wait for each download to complete
            for(final String fileToDownload : filesToDownload) {
                final Future<?> f = FileCache.instance().getFutureObj(fileToDownload, isDir);
                f.get();
            }
        }
        catch (ExecutionException e) {
            log.error(e);
            throw new DownloadException(e.getLocalizedMessage());
        }
        setStatusToFinished();
        return localPath;
    }

    @Override
    public boolean isDownloaded() {
        if (tmpDir.getServerFile().exists()) {
            final String[] files=tmpDir.getServerFile().list();
            if (files != null) {
                for(final String file : files) {
                    if (file.equalsIgnoreCase("complete")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void clean() throws IOException {
        FileUtils.deleteDirectory(tmpDir.getServerFile());
    }

    private void setStatusToFinished() {
        //create a file in the tmpDir called "complete"
        writeToFile("Finished", new File(tmpDir.getServerFile(), "complete"));
    }

    private List<String> getFilesToDownload() throws DownloadException {
        final String ftpDir=url.toExternalForm();
        //1) get the listing of data files
        FTPFile[] files=null;
        try {
            files=DynamicChoiceInfoParser.listFiles(ftpDir);
        }
        catch (DynamicChoiceInfoParser.ListFtpDirException e) {
            log.error(e);
            throw new DownloadException("Error listing files from :"+ftpDir, e);
        }
        catch (Throwable t) {
            files=new FTPFile[0];
        }
        
        final List<String> filesToDownload=new ArrayList<String>();
        // filter
        final FTPFileFilter ftpDirFilter = new FtpDirFilter("type=file");
        for(FTPFile ftpFile : files) {
            if (!ftpDirFilter.accept(ftpFile)) {
                log.debug("Skipping '"+ftpFile.getName()+ "' from ftpDir="+ftpDir);
            }
            else {
                final String name=ftpFile.getName();
                final String encodedName=UrlUtil.encodeURIcomponent(name);
                final String value;
                if (ftpDir.endsWith("/")) {
                    value=ftpDir + encodedName;
                }
                else {
                    value=ftpDir + "/" + encodedName;
                }
                filesToDownload.add(value);
            }
        }
        return filesToDownload;
    }

}

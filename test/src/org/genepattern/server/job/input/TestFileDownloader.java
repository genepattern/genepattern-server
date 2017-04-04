/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.cache.CachedFtpDir;
import org.genepattern.server.job.input.cache.CachedFtpFile;
import org.genepattern.server.job.input.cache.CachedFtpFileFactory;
import org.genepattern.server.job.input.cache.CommonsNet_3_3_Impl;
import org.genepattern.server.job.input.cache.DownloadException;
import org.genepattern.server.job.input.cache.EdtFtpJImpl;
import org.genepattern.server.job.input.choice.ftp.FtpEntry;
import org.genepattern.server.job.input.choice.ftp.ListFtpDirException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * jUnit tests for downloading an input file from an external URL.
 * 
 * @author pcarr
 *
 */
public class TestFileDownloader {
    
    //large file on gpftp site (3147288982 b, ~3.0G)
    private static final String largeFile="ftp://gpftp.broadinstitute.org/module_support_files/sequence/whole_genome/Homo_sapiens_GRCh37_Ensembl.fa";
    private static long largeFile_expectedLength=3147288982L;
    
    //tiny file on gpftp site
    final String smallFileUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.file/dummy_file_2.txt";
    final long smallFile_expectedLength=13L;
    final String smallFile_expectedName="dummy_file_2.txt";
    
    final String dirUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/";
    final String fileUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/01.txt";
    
    // this class creates tmp dirs, but cleans up after itself. 
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    private File tmpDir;
    private HibernateSessionManager mgr;
    private GpConfig gpConfig;
    private GpContext gpContext;
    
    @Before
    public void setUp() throws IOException, ExecutionException {
        tmpDir=temp.newFolder();
        final File userDir=new File(tmpDir, "users");
        mgr=DbUtil.getTestDbSession();
        gpConfig=new GpConfig.Builder()
            .addProperty("user.root.dir", userDir.getAbsolutePath())  // <---- the root directory into which user files are saved
            // for reference, the following properties are FTP client settings 
            //.addProperty("ftpDownloader.ftp_socketTimeout", "15000")
            //.addProperty("ftpDownloader.ftp_dataTimeout", "15000")
            //.addProperty("ftpDownloader.ftp_username", "anonymous")
            //.addProperty("ftpDownloader.ftp_password", "gp-help@broadinstitute.org")
        .build();
        gpContext=GpContext.getServerContext();
    }

    private void cancellationTest(boolean expected, final File toFile, final Callable<File> downloader) throws MalformedURLException, InterruptedException {
        final int sleep_interval_ms=2000;
        final ExecutorService service=Executors.newSingleThreadExecutor();
        final Future<File> future=service.submit( downloader );

        Thread.sleep(sleep_interval_ms);
        final boolean mayInterruptIfRunning=true;
        final boolean isCancelled=future.cancel(mayInterruptIfRunning);
        if (!isCancelled) {
            fail("future.cancel returned false");
        }
        
        //does cancel have any effect?
        long ts01=toFile.lastModified();
        long sz01=toFile.length();
        Thread.sleep(sleep_interval_ms);
        long ts02=toFile.lastModified();
        boolean cancelWorked=ts02==ts01;
        if (sz01==largeFile_expectedLength) {
            //must have already finished downloading
            System.err.println("Must have already finished downloading before cancelling");
            return;
        }

        //does shutdown have any effect? Note: always shut down, even if cancel worked
        service.shutdownNow();
        if (!cancelWorked) {
            Thread.sleep(sleep_interval_ms);
            long ts03=toFile.lastModified();
            long sz03=toFile.length();
            Thread.sleep(sleep_interval_ms);
            long ts04=toFile.lastModified();
            boolean shutdownWorked=ts04==ts03;
            
            if (sz03==largeFile_expectedLength) {
                //must have already finished downloading
                System.err.println("Must have already finished downloading before shutting down");
                return;
            }
            if (expected) {
                assertTrue( "file transfer still going, cancelWorked="+cancelWorked+", shutdownWorked="+shutdownWorked, cancelWorked || shutdownWorked);
            }
            else {
                assertFalse("Expecting cancellation to fail", cancelWorked || shutdownWorked);
            }
        }
    }

    @Test
    public void testDownloadFromBroadFtp() throws MalformedURLException, InterruptedException, DownloadException {
        final URL fromUrl=new URL(smallFileUrl);
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFileFactory.instance().newStdJava6Impl(mgr, gpConfig, fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (IOException e) {
            fail("IOException downloading file: "+e.getLocalizedMessage());
        }
        
        //size check
        assertEquals("name check", smallFile_expectedName, toFile.getName());
        assertEquals("size check", smallFile_expectedLength, toFile.length());
    }
    
    @Test
    public void testDownloadFromGpFtp() throws MalformedURLException, InterruptedException, DownloadException {
        final URL fromUrl=new URL(smallFileUrl);
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFileFactory.instance().newStdJava6Impl(mgr, gpConfig, fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (IOException e) {
            fail("IOException downloading file: "+e.getLocalizedMessage());
        }
        
        //size check
        assertEquals("name check", smallFile_expectedName, toFile.getName());
        assertEquals("size check", smallFile_expectedLength, toFile.length());
    }
    
    @Test
    public void testDownload_apacheCommons() throws MalformedURLException {
        final URL fromUrl=new URL(smallFileUrl);
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFileFactory.instance().newApacheCommonsImpl(mgr, gpConfig, fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (Throwable e) {
            fail("Error downloading file: "+e.getClass().getName()+" - "+e.getLocalizedMessage());
        }
        
        //size check
        assertEquals("name check", smallFile_expectedName, toFile.getName());
        assertEquals("size check", smallFile_expectedLength, toFile.length());
    }

    @Test
    public void testDownload_EdtFtp_simple() throws MalformedURLException {
        final URL fromUrl=new URL(smallFileUrl);
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFileFactory.instance().newEdtFtpJImpl_simple(mgr, gpConfig, fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (Throwable e) {
            fail("Error downloading file: "+e.getClass().getName()+" - "+e.getLocalizedMessage());
        }

        //size check
        assertEquals("name check", smallFile_expectedName, toFile.getName());
        assertEquals("size check", smallFile_expectedLength, toFile.length());
    }

    @Test
    public void testDownload_EdtFtp_advanced() throws MalformedURLException {
        final URL fromUrl=new URL(smallFileUrl);
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFileFactory.instance().newEdtFtpJImpl(mgr, gpConfig, fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (Throwable e) {
            fail("Error downloading file: "+e.getClass().getName()+" - "+e.getLocalizedMessage());
        }
        //size check
        assertEquals("name check", smallFile_expectedName, toFile.getName());
        assertEquals("size check", smallFile_expectedLength, toFile.length());
    }

    /**
     * The java std library based downloader (pre nio) is interruptible, so it does stop
     * downloading when the task is cancelled.
     * 
     * @throws MalformedURLException
     * @throws InterruptedException
     */
    @Test
    public void testCancelDownload() throws MalformedURLException, InterruptedException {
        final URL fromUrl=new URL(largeFile);
        final File toFile=new File(tmpDir, fromUrl.getFile());
        cancellationTest(true, toFile, new Callable<File>() {
            @Override
            public File call() throws Exception {
            CachedFtpFile cachedFtpFile = CachedFtpFileFactory.instance().newStdJava6Impl(mgr, gpConfig, fromUrl.toExternalForm());
                cachedFtpFile.downloadFile(fromUrl, toFile);
                return toFile;
            }
        });
    }

    /**
     * The apache commons downloader does not respond to an interrupt, so it will continue to
     * download until the thread is killed, in jUnit I assume it is because it's created as a daemon
     * thread. In the GP server, the thread runs to completion before the server JVM exits.
     * 
     * See: https://issues.apache.org/jira/browse/NET-419, for a description of the problem
     */
    @Test
    public void testCancelDownloadApacheCommons() throws MalformedURLException, InterruptedException {
        final URL fromUrl=new URL(largeFile);
        final File toFile=new File(tmpDir, fromUrl.getFile());
        cancellationTest(false, toFile, new Callable<File>() {
            @Override
            public File call() throws Exception {
                CachedFtpFile cachedFtpFile = CachedFtpFileFactory.instance().newApacheCommonsImpl(mgr, gpConfig, fromUrl.toExternalForm());
                cachedFtpFile.downloadFile(fromUrl, toFile);
                return toFile;
            }
        });
    }
    
    /**
     * When wrapped in a helper method, the edtFtp downloader can respond to an interrupt.
     * @throws MalformedURLException
     * @throws InterruptedException
     */
    @Test
    public void testCancelDownloadEdtFtp()  throws MalformedURLException, InterruptedException {
        final URL fromUrl=new URL(largeFile);
        final File toFile=new File(tmpDir, fromUrl.getFile());
        final File toParent=toFile.getParentFile();
        if (!toParent.exists()) {
            boolean success=toFile.getParentFile().mkdirs();
            if (!success) {
                fail("failed to create parent dir before download, parentDir="+toParent);
            }
        }
        cancellationTest(true, toFile, new Callable<File>() {
            @Override
            public File call() throws Exception {
                CachedFtpFile cachedFtpFile = CachedFtpFileFactory.instance().newEdtFtpJImpl(mgr, gpConfig, fromUrl.toExternalForm());
                cachedFtpFile.downloadFile(fromUrl, toFile);
                return toFile;
            }
        });
    }

    @Test
    public void testCachedFtpDir_getFilesToDownload() throws DownloadException, ListFtpDirException {
        final CachedFtpDir cachedFtpDir=new CachedFtpDir(mgr, gpConfig, gpContext, dirUrl);
        final List<FtpEntry> files=cachedFtpDir.getFilesToDownload();
        
        final List<FtpEntry> expected=new ArrayList<FtpEntry>();
        expected.add(new FtpEntry("01.txt", "ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/01.txt"));
        expected.add(new FtpEntry("02.txt", "ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/02.txt"));
        expected.add(new FtpEntry("03.txt", "ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/03.txt"));
        expected.add(new FtpEntry("04.txt", "ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/04.txt"));
        
        assertEquals("filesToDownload", expected, files);
    }
    
    /**
     * Test case for sync'ing a directory based on a user selection from a drop-down menu. 
     * @throws DownloadException
     */
    @Test
    public void testDirectoryDownload() throws DownloadException {
        final CachedFtpDir cachedFtpDir = new CachedFtpDir(mgr, gpConfig, gpContext, dirUrl);

        assertFalse("cachedFtpDir is already downloaded, localPath="+cachedFtpDir.getLocalPath().getServerFile(), cachedFtpDir.isDownloaded());
        final GpFilePath localDirPath=cachedFtpDir.download();
        assertTrue("after: isDownloaded", cachedFtpDir.isDownloaded());
        final File localDir=localDirPath.getServerFile();
        assertTrue("localDir.exists", localDir.exists());
        assertEquals("localDir.name", "A", localDir.getName());
        assertTrue("localDir.isDirectory", localDir.isDirectory());

        final File[] localFiles=localDir.listFiles();
        Arrays.sort(localFiles);
        
        assertEquals("expecting 4 files", 4, localFiles.length);
        assertEquals("localFiles[0].name", "01.txt", localFiles[0].getName());
        assertEquals("localFiles[1].name", "02.txt", localFiles[1].getName());
        assertEquals("localFiles[2].name", "03.txt", localFiles[2].getName());
        assertEquals("localFiles[3].name", "04.txt", localFiles[3].getName());
    }
    
    @Test
    public void isDir_edtFtpJ() throws Exception {
        ExecutorService ex=Executors.newCachedThreadPool();
        try {
            EdtFtpJImpl ftpFile=new EdtFtpJImpl(mgr, gpConfig, dirUrl, ex);
            boolean isDirectory=ftpFile.isDirectory();
            assertEquals(true, isDirectory);
            
            ftpFile=new EdtFtpJImpl(mgr, gpConfig, fileUrl, ex);
            isDirectory=ftpFile.isDirectory();
            assertEquals(false, isDirectory);
        }
        finally {
            ex.shutdownNow();
        }
    }

    @Test
    public void isDir_commons_net() throws Exception {
        CommonsNet_3_3_Impl ftpFile=new CommonsNet_3_3_Impl(mgr, gpConfig, dirUrl);
        boolean isDirectory=ftpFile.isDirectory();
        assertEquals(true, isDirectory);

        ftpFile=new CommonsNet_3_3_Impl(mgr, gpConfig, fileUrl);
        isDirectory=ftpFile.isDirectory();
        assertEquals(false, isDirectory);
    }

}

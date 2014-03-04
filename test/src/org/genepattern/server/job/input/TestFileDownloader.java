package org.genepattern.server.job.input;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.cache.CachedFtpDir;
import org.genepattern.server.job.input.cache.CachedFtpFile;
import org.genepattern.server.job.input.cache.DownloadException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;


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

    /*
     * this class creates tmp dirs, but cleans up after itself. 
     */
    private static final boolean cleanupTmpDirs=true;
    private static List<File> cleanupDirs;
    private static final File newTmpDir() {
        File tmpDir=Files.createTempDir();
        if (!tmpDir.exists()) {
            Assert.fail("tmpDir exists!");
        }
        if (tmpDir.list().length>0) {
            Assert.fail("tmpDir already contains "+tmpDir.list().length+" files");
        }
        cleanupDirs.add(tmpDir);
        return tmpDir;
    }
    
    @BeforeClass
    public static void initTest() {
        cleanupDirs=new ArrayList<File>();
        final File userRootDir=newTmpDir();
        System.setProperty("user.root.dir", userRootDir.getAbsolutePath());
    }
    
    @AfterClass
    public static void cleanup() throws Exception {
        if (cleanupTmpDirs && cleanupDirs != null) {
            for(final File cleanupDir : cleanupDirs) {
                boolean deleted=FileUtils.deleteQuietly(cleanupDir);
                if (!deleted) {
                    throw new Exception("tmpDir not deleted: "+cleanupDir.getAbsolutePath());
                }
            }
        }
    }

    private void cancellationTest(boolean expected, final File toFile, final Callable<File> downloader) throws MalformedURLException, InterruptedException {
        final int sleep_interval_ms=2000;
        final ExecutorService service=Executors.newSingleThreadExecutor();
        final Future<File> future=service.submit( downloader );

        Thread.sleep(sleep_interval_ms);
        final boolean mayInterruptIfRunning=true;
        final boolean isCancelled=future.cancel(mayInterruptIfRunning);
        if (!isCancelled) {
            Assert.fail("future.cancel returned false");
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
                Assert.assertTrue( "file transfer still going, cancelWorked="+cancelWorked+", shutdownWorked="+shutdownWorked, cancelWorked || shutdownWorked);
            }
            else {
                Assert.assertFalse("Expecting cancellation to fail", cancelWorked || shutdownWorked);
            }
        }
    }

    @Test
    public void testDownloadFromBroadFtp() throws MalformedURLException, InterruptedException, DownloadException {
        final URL fromUrl=new URL(smallFileUrl);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFile.Factory.instance().newStdJava6Impl(fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (IOException e) {
            Assert.fail("IOException downloading file: "+e.getLocalizedMessage());
        }
        
        //size check
        Assert.assertEquals("name check", smallFile_expectedName, toFile.getName());
        Assert.assertEquals("size check", smallFile_expectedLength, toFile.length());
    }
    
    @Test
    public void testDownloadFromGpFtp() throws MalformedURLException, InterruptedException, DownloadException {
        final URL fromUrl=new URL(smallFileUrl);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFile.Factory.instance().newStdJava6Impl(fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (IOException e) {
            Assert.fail("IOException downloading file: "+e.getLocalizedMessage());
        }
        
        //size check
        Assert.assertEquals("name check", smallFile_expectedName, toFile.getName());
        Assert.assertEquals("size check", smallFile_expectedLength, toFile.length());
    }
    
    @Test
    public void testDownload_apacheCommons() throws MalformedURLException {
        final URL fromUrl=new URL(smallFileUrl);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFile.Factory.instance().newApacheCommonsImpl(fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (Throwable e) {
            Assert.fail("Error downloading file: "+e.getClass().getName()+" - "+e.getLocalizedMessage());
        }
        
        //size check
        Assert.assertEquals("name check", smallFile_expectedName, toFile.getName());
        Assert.assertEquals("size check", smallFile_expectedLength, toFile.length());
    }

    @Test
    public void testDownload_EdtFtp_simple() throws MalformedURLException {
        final URL fromUrl=new URL(smallFileUrl);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFile.Factory.instance().newEdtFtpJImpl_simple(fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (Throwable e) {
            Assert.fail("Error downloading file: "+e.getClass().getName()+" - "+e.getLocalizedMessage());
        }

        //size check
        Assert.assertEquals("name check", smallFile_expectedName, toFile.getName());
        Assert.assertEquals("size check", smallFile_expectedLength, toFile.length());
    }

    @Test
    public void testDownload_EdtFtp_advanced() throws MalformedURLException {
        final URL fromUrl=new URL(smallFileUrl);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile cachedFtpFile = CachedFtpFile.Factory.instance().newEdtFtpJImpl(fromUrl.toExternalForm());
            cachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (Throwable e) {
            Assert.fail("Error downloading file: "+e.getClass().getName()+" - "+e.getLocalizedMessage());
        }
        //size check
        Assert.assertEquals("name check", smallFile_expectedName, toFile.getName());
        Assert.assertEquals("size check", smallFile_expectedLength, toFile.length());
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
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        cancellationTest(true, toFile, new Callable<File>() {
            @Override
            public File call() throws Exception {
            CachedFtpFile cachedFtpFile = CachedFtpFile.Factory.instance().newStdJava6Impl(fromUrl.toExternalForm());
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
        final File apacheDir=newTmpDir();
        final File toFile=new File(apacheDir, fromUrl.getFile());
        cancellationTest(false, toFile, new Callable<File>() {
            @Override
            public File call() throws Exception {
                CachedFtpFile cachedFtpFile = CachedFtpFile.Factory.instance().newApacheCommonsImpl(fromUrl.toExternalForm());
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
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        final File toParent=toFile.getParentFile();
        if (!toParent.exists()) {
            boolean success=toFile.getParentFile().mkdirs();
            if (!success) {
                Assert.fail("failed to create parent dir before download, parentDir="+toParent);
            }
        }
        cancellationTest(true, toFile, new Callable<File>() {
            @Override
            public File call() throws Exception {
                CachedFtpFile cachedFtpFile = CachedFtpFile.Factory.instance().newEdtFtpJImpl(fromUrl.toExternalForm());
                cachedFtpFile.downloadFile(fromUrl, toFile);
                return toFile;
            }
        });
    }

    @Test
    public void testCachedFtpDir_getFilesToDownload() throws DownloadException {
        final String dirUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A";
        final CachedFtpDir cachedFtpDir=new CachedFtpDir(dirUrl);
        final List<String> files=cachedFtpDir.getFilesToDownload();
        
        final List<String> expected=new ArrayList<String>();
        expected.add("ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/01.txt");
        expected.add("ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/02.txt");
        expected.add("ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/03.txt");
        expected.add("ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/04.txt");
        
        Assert.assertEquals("filesToDownload", expected, files);
    }
    
    /**
     * Test case for sync'ing a directory based on a user selection from a drop-down menu. 
     * @throws DownloadException
     */
    @Test
    public void testDirectoryDownload() throws DownloadException {
        final String dirUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/";
        final CachedFtpDir cachedFtpDir = new CachedFtpDir(dirUrl);
        cachedFtpDir.getLocalPath();

        Assert.assertFalse("test failed because cachedFtpDir is already downloaded", cachedFtpDir.isDownloaded());
        final GpFilePath localDirPath=cachedFtpDir.download();
        Assert.assertTrue("after: isDownloaded", cachedFtpDir.isDownloaded());
        final File localDir=localDirPath.getServerFile();
        Assert.assertTrue("localDir.exists", localDir.exists());
        Assert.assertEquals("localDir.name", "A", localDir.getName());
        Assert.assertTrue("localDir.isDirectory", localDir.isDirectory());

        final File[] localFiles=localDir.listFiles();
        Arrays.sort(localFiles);
        
        Assert.assertEquals("expecting 4 files", 4, localFiles.length);
        Assert.assertEquals("localFiles[0].name", "01.txt", localFiles[0].getName());
        Assert.assertEquals("localFiles[1].name", "02.txt", localFiles[1].getName());
        Assert.assertEquals("localFiles[2].name", "03.txt", localFiles[2].getName());
        Assert.assertEquals("localFiles[3].name", "04.txt", localFiles[3].getName());
    }
    
/*
 * Experimental code ...
    //test a connection timeout
    private static InputStream getMockInputStream() {
        final InputStream is=Mockito.mock(InputStream.class);
        //BDDMockito.given(is.read()).w
        return is;
    }

    public static URL getMockUrl(final String filename) throws MalformedURLException {
        final URLConnection mockConnection = Mockito.mock(URLConnection.class);
        //BDDMockito.given(mockConnection.getInputStream()).willReturn( new FileInputStream(file) );
        
//        try {
//            BDDMockito.given(mockConnection.getInputStream()).willReturn( getMockInputStream() );
//            BDDMockito.given(mockConnection.getInputStream()).willThrow(new Throwable("Should directly invoke "));
//        }
//        catch (IOException e) {
//            Assert.fail("Unexpected IOException initializing mockUrl( "+filename+" )");
//        }

        final URLStreamHandler handler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(final URL arg0) throws IOException {
                try {
                    Thread.sleep(120*1000);
                    //throw new IOException("connection timeout");
                    return mockConnection;
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return mockConnection;
                }
            }
            
        };
        URL actual=new URL(filename);
        final URL url = new URL(actual.getProtocol(), actual.getHost(), actual.getPort(), actual.getFile(), handler);
        return url;
    }

     //URLConnection connection=fromUrl.openConnection();
     //   connection.setConnectTimeout(connectTimeout_ms);
     //   connection.setReadTimeout(readTimeout_ms);
     //   connection.connect();
    static class MockUrlConnection extends URLConnection {
        private final URLConnection actualUrlConnection;
        private final int wait_ms;
        
        public MockUrlConnection(final URLConnection actualUrlConnection) {
            this(actualUrlConnection, 60*1000);
        }
        public MockUrlConnection(final URLConnection actualUrlConnection, final int wait_ms) {
            super(actualUrlConnection.getURL());
            this.actualUrlConnection=actualUrlConnection;
            this.wait_ms=wait_ms;
        }
        
        @Override
        public void setConnectTimeout(int connectTimeout) {
            this.actualUrlConnection.setConnectTimeout(connectTimeout);
        }
        
        @Override
        public void setReadTimeout(int readTimeout) {
            this.actualUrlConnection.setReadTimeout(readTimeout);
        }

        @Override
        public void connect() throws IOException {
            try {
                Thread.sleep(wait_ms);
                actualUrlConnection.connect();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("we were interrupted!");
            }            
        }
    }

    private static URL getMockUrlLongConnectionTime(final int connectionTime_ms, final String urlStr) throws MalformedURLException, IOException {
        final URL actual=new URL(urlStr);
        //final URLConnection mockConnection = Mockito.mock(URLConnection.class);
//        doAnswer(new Answer<Object>() {
//            @Override
//            public Object answer(InvocationOnMock invocation) throws Throwable {
//                Thread.sleep(connectionTime_ms);
//                actual.openConnection().connect();
//                return null;
//            }
//        })
//        .when(mockConnection).connect();

        final URLStreamHandler handler = new URLStreamHandler() {
            URLConnection urlConnection=null;
            @Override
            protected URLConnection openConnection(final URL arg0) throws IOException {
                if (urlConnection==null) {
                    actual.openConnection();
                    urlConnection=new MockUrlConnection(actual.openConnection(), connectionTime_ms);
                }
                return urlConnection;
                
//                final URLConnection urlConnection = spy(actual.openConnection());
//                doAnswer(new Answer<Object>() {
//                    @Override
//                    public Object answer(InvocationOnMock invocation) throws Throwable {
//                        Thread.sleep(connectionTime_ms);
//                        actual.openConnection().connect();
//                        return null;
//                    }
//                })
//                .when(urlConnection).connect();
                
                
//                return mockConnection;
//                try {
//                    Thread.sleep(connectionTime_ms);
//                    return actual.openConnection();
//                }
//                catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//                return null;
            }
        };

        final URL url = new URL(actual.getProtocol(), actual.getHost(), actual.getPort(), actual.getFile(), handler);
        return url;
    }

//    @Test
//    public void testConnectionTimeout() throws IOException, MalformedURLException, InterruptedException {
//        // the amount of time to wait before cancelling the download
//        final boolean deleteExisting=true;
//        final int connectTimeout_ms=5000;
//        final int readTimeout_ms=5000;
//
//        // the actual amount of time to delay the connection, to force the issue
//        final int actualConnectTimeout_ms=60*1000;
//        
//        final URL fromUrl=getMockUrlLongConnectionTime(actualConnectTimeout_ms, gpftpFile);
//        final File tmpDir=newTmpDir();
//        final File toFile=new File(tmpDir, fromUrl.getFile());
//        
//        try {
//            CachedFtpFile.downloadFile(fromUrl, toFile, deleteExisting, connectTimeout_ms, readTimeout_ms);
//            Assert.fail("Expecting connect timeout after 5 sec");
//        }
//        catch (IOException e) {
//            //expected
//        }
//        finally {
//            //do we need to clean up?
//        }
//    }
 * 
 *  end experimental code block comment.
 */
}

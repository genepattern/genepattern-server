package org.genepattern.server.job.input;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.genepattern.server.job.input.cache.CachedFtpFile;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPTransferType;
import com.enterprisedt.net.ftp.FileTransferClient;
import com.google.common.io.Files;


/**
 * jUnit tests for downloading an input file from an external URL.
 * 
 * @author pcarr
 *
 */
public class TestFileDownloader {
    //large file (3073525 KB, ~2.9G)
    final String largeFile="ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/whole_genomes/Homo_sapiens_Ensembl_GRCh37.fa";
    //smaller file (118811 KB)
    final String smallFileFromBroadFtp="ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/whole_genomes/Arabidopsis_thaliana_Ensembl_TAIR10.fa";
    //smaller file on gpftp site (116036 KB)
    final String gpftpFile="ftp://gpftp.broadinstitute.org/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf";

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
        final ExecutorService service=Executors.newSingleThreadExecutor();
        final Future<File> future=service.submit( downloader );

        Thread.sleep(2000);
        final boolean mayInterruptIfRunning=true;
        final boolean isCancelled=future.cancel(mayInterruptIfRunning);
        if (!isCancelled) {
            Assert.fail("future.cancel returned false");
        }
        
        //does cancel have any effect?
        long ts01=toFile.lastModified();
        Thread.sleep(2000);
        long ts02=toFile.lastModified();
        boolean cancelWorked=ts02==ts01;
        
        
        //does shutdown have any effect? Note: always shut down, even if cancel worked
        service.shutdownNow();
        if (!cancelWorked) {
            Thread.sleep(4000);
            long ts03=toFile.lastModified();
            Thread.sleep(4000);
            long ts04=toFile.lastModified();
            boolean shutdownWorked=ts04==ts03;
            
            if (expected) {
                Assert.assertTrue( "file transfer still going, cancelWorked="+cancelWorked+", shutdownWorked="+shutdownWorked, cancelWorked || shutdownWorked);
            }
            else {
                Assert.assertFalse("Expecting cancellation to fail", cancelWorked || shutdownWorked);
            }
        }
    }

    @Test
    public void testDownloadFromBroadFtp() throws MalformedURLException, InterruptedException {
        final URL fromUrl=new URL(smallFileFromBroadFtp);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (IOException e) {
            Assert.fail("IOException downloading file: "+e.getLocalizedMessage());
        }
        
        //size check
        long expectedLength=121662238L;
        Assert.assertEquals("name check", "Arabidopsis_thaliana_Ensembl_TAIR10.fa", toFile.getName());
        Assert.assertEquals("size check", expectedLength, toFile.length());
    }
    
    @Test
    public void testDownloadFromGpFtp() throws MalformedURLException, InterruptedException {
        final URL fromUrl=new URL(gpftpFile);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFtpFile.downloadFile(fromUrl, toFile);
        }
        catch (IOException e) {
            Assert.fail("IOException downloading file: "+e.getLocalizedMessage());
        }
        
        //size check
        long expectedLength=118820495L;
        Assert.assertEquals("name check", "Homo_sapiens_UCSC_hg18.gtf", toFile.getName());
        Assert.assertEquals("size check", expectedLength, toFile.length());
    }
    
    @Test
    public void testDownload_apacheCommons() throws MalformedURLException {
        final URL fromUrl=new URL(gpftpFile);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            FileUtils.copyURLToFile(fromUrl, toFile);
        }
        catch (Throwable e) {
            Assert.fail("Error downloading file: "+e.getClass().getName()+" - "+e.getLocalizedMessage());
        }
        
        //size check
        long expectedLength=118820495L;
        Assert.assertEquals("name check", "Homo_sapiens_UCSC_hg18.gtf", toFile.getName());
        Assert.assertEquals("size check", expectedLength, toFile.length());
    }

    @Test
    public void testDownload_EdtFtp_simple() throws MalformedURLException {
        final URL fromUrl=new URL(gpftpFile);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        
        //Note: must create download dir or the ftp transfer will fail ... after a long interval (200 s or so)
        final File toParent=toFile.getParentFile();
        if (!toParent.exists()) {
            boolean success=toFile.getParentFile().mkdirs();
            if (!success) {
                Assert.fail("failed to create parent dir before download, parentDir="+toParent);
            }
        }
        
        try {
            FileTransferClient.downloadURLFile(toFile.getAbsolutePath(), fromUrl.toExternalForm());
        }
        catch (IOException e) {
            Assert.fail(e.getClass().getName()+" - " + e.getLocalizedMessage());
        }
        catch (FTPException e) {
            Assert.fail(e.getClass().getName()+" - " + e.getLocalizedMessage());
        }
        //size check
        long expectedLength=118820495L;
        Assert.assertEquals("name check", "Homo_sapiens_UCSC_hg18.gtf", toFile.getName());
        Assert.assertEquals("size check", expectedLength, toFile.length());
    }

    @Test
    public void testDownload_EdtFtp_advanced() throws MalformedURLException {
        final URL fromUrl=new URL(gpftpFile);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        
        final File toParent=toFile.getParentFile();
        if (!toParent.exists()) {
            boolean success=toFile.getParentFile().mkdirs();
            if (!success) {
                Assert.fail("failed to create parent dir before download, parentDir="+toParent);
            }
        }

        FileTransferClient ftp = new FileTransferClient();
        try {
            ftp.setRemoteHost("gpftp.broadinstitute.org");
            ftp.setUserName("anonymous");
            ftp.setPassword("gp-help@broadinstute.org");
            ftp.connect();
            ftp.setContentType(FTPTransferType.BINARY);
            ftp.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);
            ftp.changeDirectory("/rna_seq/referenceAnnotation/gtf");
            ftp.downloadFile(toFile.getAbsolutePath(), "Homo_sapiens_UCSC_hg18.gtf");
        } 
        catch (Exception e) {
            Assert.fail(e.getClass().getName()+" - " + e.getLocalizedMessage());
        } 
        finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } 
                catch (Exception e) {
                    Assert.fail(e.getClass().getName()+" - " + e.getLocalizedMessage());
                }
            }
        }
        //size check
        long expectedLength=118820495L;
        Assert.assertEquals("name check", "Homo_sapiens_UCSC_hg18.gtf", toFile.getName());
        Assert.assertEquals("size check", expectedLength, toFile.length());
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
                CachedFtpFile.downloadFile(fromUrl, toFile);
                return toFile;
            }
        });
    }

    /**
     * The apache commons downloader does not respond to an interrupt, so it will continue to
     * download until the thread is killed, in jUnit I assume it is because it's created as a daemon
     * thread. In the GP server, the thread runs to completion before the server JVM exits.
     */
    @Test
    public void testCancelDownloadApacheCommons() throws MalformedURLException, InterruptedException {
        final URL fromUrl=new URL(largeFile);
        final File apacheDir=newTmpDir();
        final File toFile=new File(apacheDir, fromUrl.getFile());
        cancellationTest(false, toFile, new Callable<File>() {
            @Override
            public File call() throws Exception {
                FileUtils.copyURLToFile(fromUrl, toFile);
                return toFile;
            }
        });
    }
    
    /**
     * The edtFtp downloader does not respond to an interrupt.
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
        cancellationTest(false, toFile, new Callable<File>() {
            @Override
            public File call() throws Exception {
                FileTransferClient.downloadURLFile(toFile.getAbsolutePath(), fromUrl.toExternalForm());
                return toFile;
            }
        });
    }

}

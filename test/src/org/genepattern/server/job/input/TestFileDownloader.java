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
import org.genepattern.server.job.input.cache.CachedFileObj;
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
    //large file (3073525 KB, ~2.9G)
    final String largeFile="ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/whole_genomes/Homo_sapiens_Ensembl_GRCh37.fa";
    //smaller file (118811 KB)
    final String smallFile="ftp://ftp.broadinstitute.org/pub/genepattern/rna_seq/whole_genomes/Arabidopsis_thaliana_Ensembl_TAIR10.fa";

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
    public void testDownloadSmallFile() throws MalformedURLException, InterruptedException {
        final URL fromUrl=new URL(smallFile);
        final File tmpDir=newTmpDir();
        final File toFile=new File(tmpDir, fromUrl.getFile());
        try {
            CachedFileObj.downloadFile(fromUrl, toFile);
        }
        catch (IOException e) {
            Assert.fail("IOException downloading file: "+e.getLocalizedMessage());
        }
        
        //size check
        long expectedLength=121662238L;
        Assert.assertEquals("name check", "Arabidopsis_thaliana_Ensembl_TAIR10.fa", toFile.getName());
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
                CachedFileObj.downloadFile(fromUrl, toFile);
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
    
}

package org.genepattern.junitutil;

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

public class TempFileUtil {
    /*
     * this class creates tmp dirs, but cleans up after itself. 
     */
    private final boolean cleanupTmpDirs;
    private final List<File> cleanupDirs;

    public TempFileUtil() {
        this(true);
    }
    public TempFileUtil(final boolean cleanupTmpDirs) {
        this.cleanupTmpDirs=cleanupTmpDirs;
        this.cleanupDirs=new ArrayList<File>();
    }

    public final File newTmpDir() {
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
    
    public void cleanup() throws Exception {
        if (cleanupTmpDirs && cleanupDirs != null) {
            for(final File cleanupDir : cleanupDirs) {
                boolean deleted=FileUtils.deleteQuietly(cleanupDir);
                if (!deleted) {
                    throw new Exception("tmpDir not deleted: "+cleanupDir.getAbsolutePath());
                }
            }
        }
    }

}

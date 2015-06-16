/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;

import com.google.common.io.Files;
@Ignore
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

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.cache;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.job.input.cache.MapLocalEntry;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for the MapLocalEntry class.
 * 
 * @author pcarr
 *
 */
public class TestMapLocalEntry {
    private static File localDir;
    private static File expectedLocalFile;
    private final static String remoteUrl="ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml";
    private final static String expectedFilename="all_aml_test.cls";
    
    @BeforeClass
    public static void beforeClass() {
        localDir=FileUtil.getDataFile("all_aml");
        expectedLocalFile=new File(localDir,expectedFilename);
    }
    
    @Test
    public void testInitLocalValue() {
        final String toLocalPath=localDir.getAbsolutePath()+"/";
        final MapLocalEntry mapLocalEntry=new MapLocalEntry(remoteUrl+"/", toLocalPath);
        final File actualLocalFile=mapLocalEntry.initLocalValue(remoteUrl+"/"+expectedFilename);
        Assert.assertNotNull("localFile is null", actualLocalFile);
        Assert.assertEquals(expectedLocalFile.getAbsolutePath(), actualLocalFile.getAbsolutePath());
    }

    @Test
    public void testInitLocalValue_missingSlashes() {
        final String toLocalPath=localDir.getAbsolutePath();
        final MapLocalEntry mapLocalEntry=new MapLocalEntry(remoteUrl, toLocalPath);
        final File actualLocalFile=mapLocalEntry.initLocalValue(remoteUrl+"/"+expectedFilename);
        Assert.assertNotNull("localFile is null", actualLocalFile);
        Assert.assertEquals(expectedLocalFile.getAbsolutePath(), actualLocalFile.getAbsolutePath());
    }

    @Test
    public void testInitLocalValueMissingSlashInUrl() {
        final String toLocalPath=localDir.getAbsolutePath()+"/";
        final MapLocalEntry mapLocalEntry=new MapLocalEntry(remoteUrl, toLocalPath);
        final File actualLocalFile=mapLocalEntry.initLocalValue(remoteUrl+"/"+expectedFilename);
        Assert.assertNotNull("localFile is null", actualLocalFile);
        Assert.assertEquals(expectedLocalFile.getAbsolutePath(), actualLocalFile.getAbsolutePath());
    }

    @Test
    public void testInitLocalValueMissingSlashInFile() {
        final String toLocalPath=localDir.getAbsolutePath();
        final MapLocalEntry mapLocalEntry=new MapLocalEntry(remoteUrl+"/", toLocalPath);
        final File actualLocalFile=mapLocalEntry.initLocalValue(remoteUrl+"/"+expectedFilename);
        Assert.assertNotNull("localFile is null", actualLocalFile);
        Assert.assertEquals(expectedLocalFile.getAbsolutePath(), actualLocalFile.getAbsolutePath());
    }
    
    @Test 
    public void testNoMatch() {
        MapLocalEntry obj=new MapLocalEntry("ftp://gpftp.broadinstitute.org/", "/Volumes/xchip_gpdev/gpftp/pub/");
        File localFile=obj.initLocalValue("ftp://ftp.broadinstitute.org/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf");
        Assert.assertNull("No match, should return null", localFile);
    }

}

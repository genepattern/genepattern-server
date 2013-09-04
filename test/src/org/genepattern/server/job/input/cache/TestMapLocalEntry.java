package org.genepattern.server.job.input.cache;

import java.io.File;

import org.genepattern.server.job.input.cache.MapLocalEntry;
import org.junit.Assert;
import org.junit.Test;

/**
 * jUnit tests for the MapLocalEntry class.
 * 
 * @author pcarr
 *
 */
public class TestMapLocalEntry {
    
    @Test
    public void testInitLocalValue() {
        MapLocalEntry obj=new MapLocalEntry("ftp://gpftp.broadinstitute.org/", "/Volumes/xchip_gpdev/gpftp/pub/");
        File localFile=obj.initLocalValue("ftp://gpftp.broadinstitute.org/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf");
        Assert.assertEquals("/Volumes/xchip_gpdev/gpftp/pub/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf", localFile.getPath());
    }

    @Test
    public void testInitLocalValue_missingSlashes() {
        MapLocalEntry obj=new MapLocalEntry("ftp://gpftp.broadinstitute.org", "/Volumes/xchip_gpdev/gpftp/pub");
        File localFile=obj.initLocalValue("ftp://gpftp.broadinstitute.org/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf");
        Assert.assertEquals("/Volumes/xchip_gpdev/gpftp/pub/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf", localFile.getPath());
    }

    @Test
    public void testInitLocalValueMissingSlashInUrl() {
        MapLocalEntry obj=new MapLocalEntry("ftp://gpftp.broadinstitute.org", "/Volumes/xchip_gpdev/gpftp/pub/");
        File localFile=obj.initLocalValue("ftp://gpftp.broadinstitute.org/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf");
        Assert.assertEquals("/Volumes/xchip_gpdev/gpftp/pub/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf", localFile.getPath());
    }

    @Test
    public void testInitLocalValueMissingSlashInFile() {
        MapLocalEntry obj=new MapLocalEntry("ftp://gpftp.broadinstitute.org/", "/Volumes/xchip_gpdev/gpftp/pub");
        File localFile=obj.initLocalValue("ftp://gpftp.broadinstitute.org/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf");
        Assert.assertEquals("/Volumes/xchip_gpdev/gpftp/pub/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf", localFile.getPath());
    }
    
    @Test 
    public void testNoMatch() {
        MapLocalEntry obj=new MapLocalEntry("ftp://gpftp.broadinstitute.org/", "/Volumes/xchip_gpdev/gpftp/pub/");
        File localFile=obj.initLocalValue("ftp://ftp.broadinstitute.org/rna_seq/referenceAnnotation/gtf/Homo_sapiens_UCSC_hg18.gtf");
        Assert.assertNull("No match, should return null", localFile);
    }

}

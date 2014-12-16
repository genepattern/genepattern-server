package org.genepattern.server.job.input.cache;

import java.util.Arrays;
import java.util.List;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.choice.ftp.FtpEntry;
import org.genepattern.server.job.input.choice.ftp.ListFtpDirException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration tests for the CachedFtpDir class.
 * Some of these tests make calls to an FTP server hosted at the Broad Institute.
 * 
 * @author pcarr
 *
 */
public class TestCachedFtpDir {
    final String dirUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/";
    final FtpEntry[] expectedFtpEntries=new FtpEntry[] {
            new FtpEntry("01.txt", dirUrl+"01.txt"), new FtpEntry("02.txt", dirUrl+"02.txt"), new FtpEntry("03.txt", dirUrl+"03.txt"), new FtpEntry("04.txt", dirUrl+"04.txt") };
    
    GpConfig gpConfig;
    GpContext gpContext=null;
    
    @Test
    public void testWithDefaultClient() throws DownloadException, ListFtpDirException {
        gpConfig=new GpConfig.Builder().build();
        CachedFtpDir cachedFtpDir=new CachedFtpDir(gpConfig, gpContext, dirUrl);
        List<FtpEntry> ftpEntries=cachedFtpDir.getFilesToDownload();
        Assert.assertEquals( Arrays.asList(expectedFtpEntries), ftpEntries );
    }
    
    @Test
    public void testWithEdtFtpClient() throws DownloadException, ListFtpDirException {
        gpConfig=new GpConfig.Builder()
            .addProperty(CachedFtpFile.Type.PROP_FTP_DOWNLOADER_TYPE, CachedFtpFile.Type.EDT_FTP_J.name())
        .build();
        CachedFtpDir cachedFtpDir=new CachedFtpDir(gpConfig, gpContext, dirUrl);
        List<FtpEntry> ftpEntries=cachedFtpDir.getFilesToDownload();
        Assert.assertEquals( Arrays.asList(expectedFtpEntries), ftpEntries );
    }
    
    @Test
    public void testWithCommonsNetClient()  throws DownloadException, ListFtpDirException {
        gpConfig=new GpConfig.Builder()
            .addProperty(CachedFtpFile.Type.PROP_FTP_DOWNLOADER_TYPE, CachedFtpFile.Type.COMMONS_NET_3_3.name())
        .build();
        CachedFtpDir cachedFtpDir=new CachedFtpDir(gpConfig, gpContext, dirUrl);
        List<FtpEntry> ftpEntries=cachedFtpDir.getFilesToDownload();
        Assert.assertEquals( Arrays.asList(expectedFtpEntries), ftpEntries );
    }

}

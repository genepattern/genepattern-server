package org.genepattern.server.job.input.choice.ftp;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.genepattern.server.job.input.choice.DirFilter;
import org.junit.Test;

/**
 * Integration tests for the FTP directory downloader.
 * @author pcarr
 *
 */
public class TestCommonsNet_3_3_DirLister {
    final String dirUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/";
    
    @Test
    public void listContents() throws ListFtpDirException {
        FtpDirLister dirLister = new FtpDirListerCommonsNet_3_3();
        List<FtpEntry> files=dirLister.listFiles(dirUrl, new DirFilter());
        
        assertEquals("num files", 4, files.size());
        assertEquals("files[0].name", "01.txt", files.get(0).getName());
        assertEquals("files[1].name", "02.txt", files.get(1).getName());
        assertEquals("files[2].name", "03.txt", files.get(2).getName());
        assertEquals("files[3].name", "04.txt", files.get(3).getName());
    }

}

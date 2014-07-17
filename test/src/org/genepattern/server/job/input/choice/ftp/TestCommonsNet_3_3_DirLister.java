package org.genepattern.server.job.input.choice.ftp;

import static org.junit.Assert.assertEquals;

import org.apache.commons.net.ftp.FTPFile;
import org.genepattern.server.job.input.choice.RemoteDirLister;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the FTP directory downloader.
 * @author pcarr
 *
 */
public class TestCommonsNet_3_3_DirLister {
    final String dirUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/";
    
    private RemoteDirLister<FTPFile,ListFtpDirException> dirLister;
    
    @Before
    public void setUp() {
        dirLister=CommonsNet_3_3_DirLister.createDefault();
    }
    
    @Test
    public void listContents() throws ListFtpDirException {
        FTPFile[] files=dirLister.listFiles(dirUrl);
        assertEquals("num files", 4, files.length);
        assertEquals("files[0].name", "01.txt", files[0].getName());
        assertEquals("files[1].name", "02.txt", files[1].getName());
        assertEquals("files[2].name", "03.txt", files[2].getName());
        assertEquals("files[3].name", "04.txt", files[3].getName());
    }

}

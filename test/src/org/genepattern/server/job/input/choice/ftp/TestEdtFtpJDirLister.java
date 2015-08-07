/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.choice.ftp;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.genepattern.server.job.input.choice.DirFilter;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the FTP directory downloader.
 * @author pcarr
 *
 */
public class TestEdtFtpJDirLister {
    final String dirUrl="ftp://gpftp.broadinstitute.org/example_data/gpservertest/DemoFileDropdown/input.dir/A/";
    
    private FtpDirLister dirLister;
    
    @Before
    public void setUp() {
        dirLister=new FtpDirListerEdtFtpJ();
    }
    
    @Test
    public void listContents() throws ListFtpDirException {
        List<FtpEntry> files=dirLister.listFiles(dirUrl, new DirFilter());
        //FTPFile[] files=dirLister.listFiles(dirUrl);
        assertEquals("num files", 4, files.size());
        assertEquals("files[0]", new FtpEntry("01.txt", dirUrl+"01.txt"), files.get(0));
        assertEquals("files[1]", new FtpEntry("02.txt", dirUrl+"02.txt"), files.get(1));
        assertEquals("files[2]", new FtpEntry("03.txt", dirUrl+"03.txt"), files.get(2));
        assertEquals("files[3]", new FtpEntry("04.txt", dirUrl+"04.txt"), files.get(3));
    }

}

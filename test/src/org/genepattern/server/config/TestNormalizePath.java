package org.genepattern.server.config;

import static org.junit.Assert.*;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.junit.Before;
import org.junit.Test;

public class TestNormalizePath {
    private File jobResultsDir;
    
    @Before
    public void setUp() {
        jobResultsDir=FileUtil.getDataFile("jobResults/");
        assertTrue("Missing required file jobResultsDir="+jobResultsDir, jobResultsDir.exists());
    }
    
    @Test
    public void relativePath() {
        File target=new File(jobResultsDir, "0");
        String str=target.toString()+"/../../jobResults";

        assertEquals("relative path", jobResultsDir.getAbsoluteFile().toString()+"/", GpConfig.normalizePath(str));
    }
    
    @Test
    public void withSpaces() { 
        File target=new File(jobResultsDir, "0/result with spaces.txt");
        String expected=jobResultsDir.getAbsolutePath()+"/0/result with spaces.txt";
        assertEquals("result with spaces.txt", expected, GpConfig.normalizePath(target.toString()));
    }

}

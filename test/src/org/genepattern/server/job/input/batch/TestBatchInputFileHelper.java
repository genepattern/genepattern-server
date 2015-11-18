package org.genepattern.server.job.input.batch;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.genepattern.server.dm.GpFilePath;
import org.junit.Before;
import org.junit.Test;

public class TestBatchInputFileHelper {
    final String baseGpHref="http://127.0.0.1:8080/gp";
    final String filename="all_aml_test.gct";
    GpFilePath gpFilePath;
    
    @Before
    public void setUp() {
        gpFilePath=mock(GpFilePath.class);
    }

    @Test
    public void getFilename_fromName() {
        when(gpFilePath.getName()).thenReturn(filename);
        assertEquals("gpFilePath.name is set",
                filename,
                BatchInputFileHelper.getFilename(gpFilePath));
    }

    @Test
    public void getFilename_fromRelativeFile() {
        when(gpFilePath.getRelativeFile()).thenReturn(new File(filename));
        assertEquals("gpFilePath.relativeFile, when name not set",
                filename,
                BatchInputFileHelper.getFilename(gpFilePath));
    }
    
    @Test
    public void getFilename_fromRelativeFile_scenarios() {
        when(gpFilePath.getRelativeFile()).thenReturn(new File(""));
        assertEquals("empty string",
                "",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("/"));
        assertEquals("'/'",
                "",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("./"));
        assertEquals("'./'",
                ".",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("parent/child_dir"));
        assertEquals("'parent/child_dir' (no extension)",
                "child_dir",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("parent/child_dir/"));
        assertEquals("'parent/child_dir/' (trailing '/')",
                "child_dir",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("parent/child_dir/.hidden"));
        assertEquals("'parent/child_dir/.hidden'",
                ".hidden",
                BatchInputFileHelper.getFilename(gpFilePath));
    }

    @Test
    public void getFilename_fromRelativeUri() throws URISyntaxException {
        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/users/test_user/"+filename));
        assertEquals("gpFilePath.relativeFile, when name not set",
                filename,
                BatchInputFileHelper.getFilename(gpFilePath));
    }

    @Test
    public void getFilename_fromRelativeUri_scenarios() throws URISyntaxException {
        when(gpFilePath.getRelativeUri()).thenReturn(new URI(""));
        assertEquals("empty string",
                "",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/"));
        assertEquals("'/'",
                "",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("users"));
        assertEquals("'users' (no leading '/')",
                "users",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/users"));
        assertEquals("'/users' (no extension, presumably a dir)",
                "users",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/users/test_user/gp_tutorial_files/"));
        assertEquals("'/users/test_user/gp_tutorial_files/'  (trailing '/', presumably a dir)",
                "gp_tutorial_files",
                BatchInputFileHelper.getFilename(gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/users/test_user/.hidden"));
        assertEquals("'.hidden'",
                ".hidden",
                BatchInputFileHelper.getFilename(gpFilePath));
    }

    @Test
    public void getFilename_nullName() {
        assertEquals("", BatchInputFileHelper.getFilename(gpFilePath));
    }

    @Test
    public void getFilename_nullArg() {
        assertEquals("", BatchInputFileHelper.getFilename(null));
    }
    
    @Test
    public void getFilenameFromUrl() throws MalformedURLException {
        assertEquals(filename, 
                BatchInputFileHelper.getFilenameFromUrl(
                        new URL(baseGpHref+"/users/test_user/"+filename)));
    }

    @Test
    public void getFilenameFromUrl_ignoreQueryString() throws MalformedURLException {
        assertEquals("ignore queryString", filename, 
                BatchInputFileHelper.getFilenameFromUrl(
                        new URL(baseGpHref+"/users/test_user/"+filename+"?key=val&flag#anchor")));
    }

    @Test
    public void getFilenameFromUrl_nullArg() throws MalformedURLException {
        assertEquals("null arg", null, BatchInputFileHelper.getFilenameFromUrl(null));
        assertEquals("baseGpHref (no trailing slash), returns servlet context path", 
                "gp", 
                BatchInputFileHelper.getFilenameFromUrl(new URL(baseGpHref)));
        assertEquals("baseGpUrl (with trailing slash), also returns servlet context path", 
                "gp",
                BatchInputFileHelper.getFilenameFromUrl(new URL(baseGpHref+"/")));
    }

    @Test
    public void getFilenameFromUrl_baseGpHref_noTrailingSlash() throws MalformedURLException {
        assertEquals("baseGpHref (no trailing slash), returns servlet context path", 
                "gp", 
                BatchInputFileHelper.getFilenameFromUrl(new URL(baseGpHref)));
        assertEquals("baseGpUrl (with trailing slash), also returns servlet context path", 
                "gp",
                BatchInputFileHelper.getFilenameFromUrl(new URL(baseGpHref+"/")));
    }

    @Test
    public void getFilenameFromUrl_baseGpUrl_withTrailingSlash() throws MalformedURLException {
        assertEquals("baseGpUrl (with trailing slash), also returns servlet context path", 
                "gp",
                BatchInputFileHelper.getFilenameFromUrl(new URL(baseGpHref+"/")));
    }

}

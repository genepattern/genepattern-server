package org.genepattern.server.job.input.batch;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.dm.GpFilePath;
import org.junit.Before;
import org.junit.Test;

public class TestBatchInputFileHelper {
    GpConfig gpConfig;
    final String filename="all_aml_test.gct";
    GpFilePath gpFilePath;
    
    @Before
    public void setUp() {
        gpConfig=mock(GpConfig.class);
        gpFilePath=mock(GpFilePath.class);
    }

    @Test
    public void getFilename_fromName() {
        when(gpFilePath.getName()).thenReturn(filename);
        assertEquals("gpFilePath.name is set",
                filename,
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }

    @Test
    public void getFilename_fromRelativeFile() {
        when(gpFilePath.getRelativeFile()).thenReturn(new File(filename));
        assertEquals("gpFilePath.relativeFile, when name not set",
                filename,
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }
    
    @Test
    public void getFilename_fromRelativeFile_scenarios() {
        when(gpFilePath.getRelativeFile()).thenReturn(new File(""));
        assertEquals("empty string",
                "",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("/"));
        assertEquals("'/'",
                "",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("./"));
        assertEquals("'./'",
                ".",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("parent/child_dir"));
        assertEquals("'parent/child_dir' (no extension)",
                "child_dir",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("parent/child_dir/"));
        assertEquals("'parent/child_dir/' (trailing '/')",
                "child_dir",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeFile()).thenReturn(new File("parent/child_dir/.hidden"));
        assertEquals("'parent/child_dir/.hidden'",
                ".hidden",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }

    @Test
    public void getFilename_fromRelativeUri() throws URISyntaxException {
        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/users/test_user/"+filename));
        assertEquals("gpFilePath.relativeFile, when name not set",
                filename,
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }

    @Test
    public void getFilename_fromRelativeUri_scenarios() throws URISyntaxException {
        when(gpFilePath.getRelativeUri()).thenReturn(new URI(""));
        assertEquals("empty string",
                "",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/"));
        assertEquals("'/'",
                "",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("users"));
        assertEquals("'users' (no leading '/')",
                "users",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/users"));
        assertEquals("'/users' (no extension, presumably a dir)",
                "users",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/users/test_user/gp_tutorial_files/"));
        assertEquals("'/users/test_user/gp_tutorial_files/'  (trailing '/', presumably a dir)",
                "gp_tutorial_files",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));

        when(gpFilePath.getRelativeUri()).thenReturn(new URI("/users/test_user/.hidden"));
        assertEquals("'.hidden'",
                ".hidden",
                BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }
    
    @Test
    public void getFilename_fromExternalUrl_http() throws Exception {
        URL extUrl=new URL(dataHttpDir+"all_aml_test.gct");
        when(gpFilePath.getUrl(gpConfig)).thenReturn(extUrl);
        assertEquals("all_aml_test.gct", BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }

    @Test
    public void getFilename_fromExternalUrl_http_dir() throws Exception {
        URL extUrl=new URL(dataHttpDir);
        when(gpFilePath.getUrl(gpConfig)).thenReturn(extUrl);
        assertEquals("all_aml", BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }

    @Test
    public void getFilename_fromExternalUrl_genomeSpace() throws Exception {
        URL extUrl=new URL(dataGsDir+"all_aml_test.gct");
        when(gpFilePath.getUrl(gpConfig)).thenReturn(extUrl);
        assertEquals("all_aml_test.gct", BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }

    @Test
    public void getFilename_fromExternalUrl_ftp() throws Exception {
        URL extUrl=new URL(dataFtpDir+"all_aml_test.gct");
        when(gpFilePath.getUrl(gpConfig)).thenReturn(extUrl);
        assertEquals("all_aml_test.gct", BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }

    @Test
    public void getFilename_nullName() {
        assertEquals("", BatchInputFileHelper.getFilename(gpConfig, gpFilePath));
    }

    @Test
    public void getFilename_nullArg() {
        assertEquals("", BatchInputFileHelper.getFilename(null));
    }
    
    @Test
    public void getFilenameFromUrl() throws MalformedURLException {
        assertEquals(filename, 
                BatchInputFileHelper.getFilenameFromUrl(
                        new URL(gpHref+"/users/test_user/"+filename)));
    }

    @Test
    public void getFilenameFromUrl_ignoreQueryString() throws MalformedURLException {
        assertEquals("ignore queryString", filename, 
                BatchInputFileHelper.getFilenameFromUrl(
                        new URL(gpHref+"/users/test_user/"+filename+"?key=val&flag#anchor")));
    }

    @Test
    public void getFilenameFromUrl_nullArg() throws MalformedURLException {
        assertEquals("null arg", null, BatchInputFileHelper.getFilenameFromUrl(null));
    }

    @Test
    public void getFilenameFromUrl_baseGpHref_noTrailingSlash() throws MalformedURLException {
        assertEquals("baseGpHref (no trailing slash), returns servlet context path", 
                "gp", 
                BatchInputFileHelper.getFilenameFromUrl(new URL(gpHref)));
    }

    @Test
    public void getFilenameFromUrl_gpUrl() throws MalformedURLException {
        assertEquals("'{gpUrl}' (with trailing slash), returns servlet context path", 
                "gp",
                BatchInputFileHelper.getFilenameFromUrl(new URL(gpUrl)));
    }

    public void doGetFilenameFromUrl(final String expectedFilename, final String urlSpec) throws MalformedURLException {
        assertEquals("fromUrl('"+urlSpec+"')", 
                expectedFilename,
                BatchInputFileHelper.getFilenameFromUrl(new URL(urlSpec)));
    }

    @Test
    public void getFilenameFromUrl_scenarios() throws MalformedURLException {
        doGetFilenameFromUrl("", "http:");
        doGetFilenameFromUrl("", "http://");
        doGetFilenameFromUrl("", "http://");
        doGetFilenameFromUrl("", "http://127.0.0.1");
        doGetFilenameFromUrl("", "http://127.0.0.1:8080");
        doGetFilenameFromUrl("", "http://127.0.0.1:8080/");
        doGetFilenameFromUrl("gp", gpHref);
        doGetFilenameFromUrl("gp", gpUrl);
        doGetFilenameFromUrl("gp", gpHref+"?name=value&name=value#pathfragment");
        doGetFilenameFromUrl("data", gpHref+"/data//?name=value&name=value#pathfragment");
        doGetFilenameFromUrl("xchip", gpHref+"/data//xchip/?name=value&name=value#pathfragment");
        doGetFilenameFromUrl("all_aml_test.gct", gpHref+"/data//xchip/all_aml_test.gct?name=value&name=value#pathfragment");
        doGetFilenameFromUrl("all aml test.gct", gpHref+"/all%20aml%20test.gct");
        doGetFilenameFromUrl("all+aml+test.gct ('+' chars in path element are not encoded)", gpHref+"/all+aml+test.gct");
    }

}

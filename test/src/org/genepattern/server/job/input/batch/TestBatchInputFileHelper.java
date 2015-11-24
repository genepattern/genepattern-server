package org.genepattern.server.job.input.batch;

import static org.genepattern.junitutil.Demo.dataFtpDir;
import static org.genepattern.junitutil.Demo.dataGsDir;
import static org.genepattern.junitutil.Demo.dataHttpDir;
import static org.genepattern.junitutil.Demo.gpHref;
import static org.genepattern.junitutil.Demo.gpUrl;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.genepattern.server.dm.ExternalFile;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.genomespace.GenomeSpaceFileHelper;
import org.junit.Before;
import org.junit.Test;

public class TestBatchInputFileHelper {
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
    public void getFilename_fromExternalUrl_http() throws Exception {
        URL extUrl=new URL(dataHttpDir+"all_aml_test.gct");
        when(gpFilePath.getUrl()).thenReturn(extUrl);
        assertEquals("all_aml_test.gct", BatchInputFileHelper.getFilename(gpFilePath));
    }

    @Test
    public void getFilename_fromExternalUrl_http_dir() throws Exception {
        URL extUrl=new URL(dataHttpDir);
        when(gpFilePath.getUrl()).thenReturn(extUrl);
        assertEquals("all_aml/", BatchInputFileHelper.getFilename(gpFilePath));
    }

    @Test
    public void getFilename_fromExternalUrl_genomeSpace() throws Exception {
        URL extUrl=new URL(dataGsDir+"all_aml_test.gct");
        when(gpFilePath.getUrl()).thenReturn(extUrl);
        assertEquals("all_aml_test.gct", BatchInputFileHelper.getFilename(gpFilePath));
    }

    @Test
    public void getFilename_fromExternalUrl_ftp() throws Exception {
        URL extUrl=new URL(dataFtpDir+"all_aml_test.gct");
        when(gpFilePath.getUrl()).thenReturn(extUrl);
        assertEquals("all_aml_test.gct", BatchInputFileHelper.getFilename(gpFilePath));
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
                UrlUtil.getFilenameFromUrl(
                        new URL(gpHref+"/users/test_user/"+filename)));
    }

    @Test
    public void getFilenameFromUrl_ignoreQueryString() throws MalformedURLException {
        assertEquals("ignore queryString", filename, 
                UrlUtil.getFilenameFromUrl(
                        new URL(gpHref+"/users/test_user/"+filename+"?key=val&flag#anchor")));
    }

    @Test
    public void getFilenameFromUrl_nullArg() throws MalformedURLException {
        assertEquals("null arg", null, UrlUtil.getFilenameFromUrl(null));
    }

    @Test
    public void getFilenameFromUrl_baseGpHref_noTrailingSlash() throws MalformedURLException {
        assertEquals("baseGpHref (no trailing slash), returns servlet context path", 
                "gp", 
                UrlUtil.getFilenameFromUrl(new URL(gpHref)));
    }

    @Test
    public void getFilenameFromUrl_gpUrl() throws MalformedURLException {
        assertEquals("'{gpUrl}' (with trailing slash), returns servlet context path", 
                "gp/",
                UrlUtil.getFilenameFromUrl(new URL(gpUrl)));
    }

    public void doGetFilenameFromUrlSpec(final String expectedFilename, final String urlSpec) throws MalformedURLException {
        doGetFilenameFromUrlSpec("", expectedFilename, urlSpec);
    }

    public void doGetFilenameFromUrlSpec(final String message, final String expectedFilename, final String urlSpec) throws MalformedURLException {
        final URL url=new URL(urlSpec);
        assertEquals(message+"fromUrl('"+urlSpec+"')", 
                expectedFilename,
                UrlUtil.getFilenameFromUrl(url));
        
        // double-check ExternalFile
        GpFilePath externalGpFilePath = new ExternalFile(url);
        assertEquals("new ExternalFile('"+urlSpec+"').name", 
                expectedFilename, 
                externalGpFilePath.getName());
    }

    @Test
    public void getFilenameFromUrl_scenarios() throws MalformedURLException {
        doGetFilenameFromUrlSpec("", "http:");
        doGetFilenameFromUrlSpec("", "http://");
        doGetFilenameFromUrlSpec("", "http://");
        doGetFilenameFromUrlSpec("", "http://127.0.0.1");
        doGetFilenameFromUrlSpec("", "http://127.0.0.1:8080");
        doGetFilenameFromUrlSpec("root path, ", "/", "http://127.0.0.1:8080/");
        doGetFilenameFromUrlSpec("gp", gpHref);
        doGetFilenameFromUrlSpec("gp/", gpUrl);
        doGetFilenameFromUrlSpec("gp", gpHref+"?name=value&name=value#pathfragment");
        doGetFilenameFromUrlSpec("data/", gpHref+"/data//?name=value&name=value#pathfragment");
        doGetFilenameFromUrlSpec("xchip/", gpHref+"/data//xchip/?name=value&name=value#pathfragment");
        doGetFilenameFromUrlSpec("all_aml_test.gct", gpHref+"/data//xchip/all_aml_test.gct?name=value&name=value#pathfragment");
        doGetFilenameFromUrlSpec("all aml test.gct", gpHref+"/all%20aml%20test.gct");
        doGetFilenameFromUrlSpec("('+' chars in path element are not encoded), ", "all+aml+test.gct", gpHref+"/all+aml+test.gct");
    }
    
    // helper test for GenomeSpace_Google drive url
    protected void checkGoogleDocGsUrl(final String message, final String expected, final String urlSpec, final boolean checkGsHelper) throws IOException {
        final URL url=new URL(urlSpec);
        assertEquals(message, // e.g. root folder 
                expected, // e.g. "googledrive:test_user@email.com", 
                UrlUtil.getFilenameFromUrl(url));
        if (checkGsHelper) {
            assertEquals(message+", GsHelper", 
                expected, 
                GenomeSpaceFileHelper.extractFilename(url));
        }
    }
    
    @Test
    public void getFilenameFromUrl_GenomeSpace_GoogleDrive() throws IOException {
        // simulate root path to a GenomeSpace user's shared google drive folder
        final String rootDir="https://dm.genomespace.org/datamanager/v1.0/file/Home/googledrive:test_user@email.com(mIv4L0eliPcQ21HRCqwWQg==)";
        final String subDir=rootDir+"/GenomeSpacePublic/all_aml(0By2oidMlPWQtQ2RlQV8zd25Md1E)";
        final String file=subDir+"/all_aml_test.gct";
        final String subDirWithParen=rootDir+"/all_aml(test%20dir)(0Cz0uuuUUUUaXo2SFR0000)";
        
        checkGoogleDocGsUrl("root folder", "googledrive:test_user@email.com", rootDir, true);
        checkGoogleDocGsUrl("sub dir", "all_aml", subDir, true);
        checkGoogleDocGsUrl("file in sub dir", "all_aml_test.gct", file, true);
        checkGoogleDocGsUrl("file name with paren", "all_aml_test(paren).gct", rootDir+"/all_aml_test(paren).gct", 
                // not working in GenomeSpaceHelper
                false);
        checkGoogleDocGsUrl("sub dir with paren", "all_aml(test dir)", subDirWithParen, 
                // not working in GenomeSpaceHelper
                false);
        
    }

    @Test
    public void rootDirTest() {
        //what is the name of the root directory
        assertEquals("name of root directory", "", new File("/").getName());
    }

}

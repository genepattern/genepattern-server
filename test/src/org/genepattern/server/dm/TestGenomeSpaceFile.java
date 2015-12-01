package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.dataGsDir;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.genomespace.GenomeSpaceFile;
import org.genepattern.server.genomespace.GenomeSpaceFileHelper;
import org.genepattern.util.SemanticUtil;
import org.genomespace.client.GsSession;
import org.genomespace.datamanager.core.GSFileMetadata;
import org.junit.Test;
import org.mockito.Mockito;

public class TestGenomeSpaceFile {

    /**
     * Create a mock GenomeSpaceFile. Initialize name, kind, and extension in 
     * GenomeSpaceFileHelper#createFile (warts and all).
     * Matches runtime behavior circa GP 3.9.5 release.
     * 
     * @param filename, a relative path, not prefixed with a '/', e.g. "all_aml_test.gct"
     * @return
     * @throws MalformedURLException
     */
    protected static GenomeSpaceFile mockGsFileFromGsHelper(final String filename) throws MalformedURLException {
        final String urlSpec=dataGsDir+filename;
        final GsSession gsSession=Mockito.mock(GsSession.class);
        final GSFileMetadata metadata=Mockito.mock(GSFileMetadata.class);
        final URL url=new URL(urlSpec);
        return GenomeSpaceFileHelper.createFile(gsSession, url, metadata);
    }
    
    /**
     * Create a mock GenomeSpaceFile. Initialize name, kind, and extension from the incoming url.
     * Uses the same heuristic as for other ExternalFile instances. 
     * These differ slightly from the GsFileHelper.
     * 
     * @param filename, a relative path, not prefixed with a '/', e.g. "all_aml_test.gct"
     * @return
     * @throws MalformedURLException
     */
    protected static GenomeSpaceFile mockGsFile(final String filename) throws MalformedURLException {
        final String urlSpec=dataGsDir+filename;
        final GsSession gsSession=Mockito.mock(GsSession.class);
        final URL url=new URL(urlSpec);
        if (!GenomeSpaceFileHelper.isGenomeSpaceFile(url)) {
            throw new IllegalArgumentException("Not a GenomeSpace URL: " + url);
        }

        if (gsSession == null) {
            throw new IllegalArgumentException("gsSession=null");
        }

        final GenomeSpaceFile file = new GenomeSpaceFile(gsSession);
        // use generic helper method in GpFilePath to initialize name, kind and extension from the incoming url
        file.initNameKindExtensionFromUrl(url);
        file.setUrl(url);
        return file;
    }
    
    @Test
    public void createGsFileFromGsHelper() throws MalformedURLException {
        String expectedUri=Demo.dataGsDir+"all_aml_test.gct";
        final GpFilePath gpFilePath=mockGsFileFromGsHelper("all_aml_test.gct");
        assertEquals("getName", "all_aml_test.gct", gpFilePath.getName());
        assertEquals("getExtension", "gct", gpFilePath.getExtension());
        assertEquals("getKind", "gct", gpFilePath.getKind());
        assertEquals("relativeUri, expecting fully qualified uri", expectedUri,
                gpFilePath.getRelativeUri().toString());
    }

    @Test
    public void createGsFile() throws MalformedURLException {
        String expectedUri=Demo.dataGsDir+"all_aml_test.gct";
        final GpFilePath gpFilePath=mockGsFile("all_aml_test.gct");
        assertEquals("getName", "all_aml_test.gct", gpFilePath.getName());
        assertEquals("getExtension", "gct", gpFilePath.getExtension());
        assertEquals("getKind", "gct", gpFilePath.getKind());
        assertEquals("relativeUri, expecting fully qualified uri", expectedUri,
                gpFilePath.getRelativeUri().toString());
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
    
    // helper test for GenomeSpace custom converter url
    protected void checkGsKind(final String expectedKind, final String queryString, final boolean checkGsHelper) throws IOException {
        final String expectedName="CEL_IK50.tab";
        URL url=new URL(dataGsDir+expectedName+queryString);
        
        String filename=UrlUtil.getFilenameFromUrl(url);
        String extension=SemanticUtil.getExtension(filename);
        String kind=UrlUtil.getKindFromUrl(url, queryString, extension);
        assertEquals("'"+queryString+"', filename", expectedName, filename);
        assertEquals("'"+queryString+"', kind", expectedKind, kind);
        
        if (checkGsHelper) {
            filename=GenomeSpaceFileHelper.extractFilename(url);
            kind=GenomeSpaceFileHelper.extractKind(url, filename);
            assertEquals("GsHelper, '"+queryString+"', filename", expectedName, filename);
            assertEquals("GsHelper, '"+queryString+"', kind", expectedKind, kind);
        }
    }
    
    @Test public void getKindFromUrl_GS_converter() throws Exception {
         checkGsKind("gct", "?dataformat=http://www.genomespace.org/datamanager/dataformat/gct", true);
    }

    @Test public void getKindFromUrl_GS_converter_corner_cases() throws Exception {
         checkGsKind("tab", "", true);
         checkGsKind("tab", "?", true);
         checkGsKind("tab", "?dataformat", true);
         checkGsKind("tab", "?dataformat=", true);
         checkGsKind("tab", "?dataformat=gct", true);
         checkGsKind("tab", "?dataformat=/gct", true);
         checkGsKind("gct", "?dataformat=/dataformat/gct", true);

         // GenomeSpaceFileHelper anomalies, still converts but probably shouldn't
         checkGsKind("tab", "?/dataformat=/dataformat/gct", false);
         checkGsKind("tab", "?A=/dataformat/gct", false);
         checkGsKind("tab", "?/dataformat/gct", false);
    }

}

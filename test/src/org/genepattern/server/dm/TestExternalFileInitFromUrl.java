package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.Arrays;

import org.genepattern.server.genomespace.GenomeSpaceClient;
import org.genepattern.server.genomespace.GenomeSpaceFile;
import org.genepattern.server.genomespace.GenomeSpaceFileHelper;
import org.genomespace.client.GsSession;
import org.genomespace.datamanager.core.GSFileMetadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

/**
 * Parameterized tests for new ExternalFile(urlSpec) and GenomeSpaceFile.setUrl(urlSpec), 
 * to verify that the name, extension, kind, isDirectory and isLocal
 * fields are properly initialized for standard use-cases.
 * 
 * Example input:
 *     GenomeSpace: "https://dm.genomespace.org/datamanager/file/Home/Public/test/atm_test.gct";
 *     ExternalFile: "http://www.broadinstitute.org/cancer/software/genepattern/data/all_aml/all_aml_test.gct"
 *     
 *     https://dm.genomespace.org/datamanager/file/Home/googledrive:pcarr@broadinstitute.org(lHv4L0eliPcV19HRCqwWQg==)/GenomeSpacePublic/all_aml(0Bx1oidMlPWQtN2RlQV8zd25Md1E)/all_aml_test.gct
 * 
 * Note: specialized cases are not presently covered by these test:
 *     - character encoding (e.g. '%20' and '+'
 *     - hostname, with no path info (e.g. 'http://www.host.com')
 *     - ignore queryString, e.g. 'http://www.host.com/file.txt?name=val'
 * 
 * @see https://github.com/junit-team/junit/wiki/Parameterized-tests
 * 
 * @author pcarr
 */
@RunWith(Parameterized.class)
public class TestExternalFileInitFromUrl {
    
    //@Parameters(name = "setUrl: {0}")
    @Parameters(name = "{0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                //{ _name_, _expected_extension_, _expected_kind_, _expected_isDirectory_ }, 
                { "all_aml_train.gct", "gct", "gct", false },
                { "all_aml_train.GCT", "GCT", "gct", false },
                { "all_aml", "", "", false }, 
                { "all_aml/", "", "directory", true }, 
                { ".hidden.txt", "txt", "txt", false },
                { ".hidden_no_ext", "", "", false },
                { ".hidden_dir/", "", "directory", true },
                { "mock.gz","gz", "gz", false }, 
                { "mock.tar.gz","gz", "tar.gz", false }, 
                { "mock.fasta.gz","gz", "fasta.gz", false }, 
                // Note, not yet ready to test custom conversion for GenomeSpace file, gct or genomica-tab 
                //{ "foo.gct","gct", "tab", false }, 
        });
    }
    
    private final String expectedName;
    private final String expectedExtension;
    private final String expectedKind;
    private final boolean isDirectory;
    
    private final ExternalFile extFilePath;
    private final GenomeSpaceFile gsFilePath;
    private Throwable gsFilePathInitError=null;
    
    public TestExternalFileInitFromUrl(final String expectedName, final String expectedExtension, final String expectedKind, final boolean isDirectory) 
    {
        this.expectedName=expectedName;
        this.expectedExtension=expectedExtension;
        this.expectedKind=expectedKind;
        this.isDirectory=isDirectory;
        
        // initialize ExternalFile
        this.extFilePath=new ExternalFile(dataHttpDir+expectedName);
        // initialize GsFile (proposed new behavior to match ExternalFile values)
        this.gsFilePath=initGsFileForTest(dataGsDir+expectedName);
        // initialize GsFile (this version causes some tests to fail)
        //this.gsFilePath=initGsFileFromGsFileHelper(dataGsDir+expectedName);
    }

    // Note: circa GP 3.9.5, the default (name, extension, kind, isDirectory) for a GenomeSpace file will not pass these tests
    // should come to agreement on this
    protected GenomeSpaceFile initGsFileFromGsFileHelper(final String urlSpec) {
        final GsSession gsSession=Mockito.mock(GsSession.class);
        final GSFileMetadata metadata=Mockito.mock(GSFileMetadata.class);
        final URL url;
        try {
            url=new URL(urlSpec);
            return GenomeSpaceFileHelper.createFile(gsSession, url, metadata);
        }
        catch (Throwable t) {
            gsFilePathInitError=t;
        }
        return null;
    }

    protected GenomeSpaceFile initGsFileForTest(final String urlSpec) {
        final GsSession gsSession=Mockito.mock(GsSession.class);
        final URL url;
        try {
            url=new URL(urlSpec);
            return createFileForTest(gsSession, url);
        }
        catch (Throwable t) {
            gsFilePathInitError=t;
        }
        return null;
    }
    
    /**
     * Testing the unified way to initialize name, kind, and extension from the incoming url.
     * this fixes some potential bugs in 
     * @param gsClient
     * @param url
     * @return a new GenomeSpaceFile initialized from the URL
     */
    public static GenomeSpaceFile createFileForTest(final GsSession gsSession, final URL url) {
        if (!GenomeSpaceFileHelper.isGenomeSpaceFile(url)) {
            throw new IllegalArgumentException("Not a GenomeSpace URL: " + url);
         }

        if (gsSession == null) {
            throw new IllegalArgumentException("gsSession=null");
        }

        final GenomeSpaceFile file = new GenomeSpaceFile(gsSession);
        // use generic helper method in GpFilePath to init name, kind and extenstion from the incoming url
        file.initNameKindExtensionFromUrl(url);
        return file;
    }
    
    @Test
    public void createFile_GenomeSpaceFileHelper() throws Throwable {
        if (gsFilePathInitError != null) {
            throw gsFilePathInitError;
        }
        assertNotNull("failed to createFile", gsFilePath);
    }
    
    @Test
    public void getName_GenomeSpace() {
        assertEquals(expectedName, gsFilePath.getName());
    }

    @Test
    public void getName() {
        assertEquals(expectedName, extFilePath.getName());
    }
    
    @Test
    public void getExtension_GenomeSpace() {
        assertEquals(expectedExtension, gsFilePath.getExtension());
    }

    @Test
    public void getExtension() {
        assertEquals(expectedExtension, extFilePath.getExtension());
    }

    @Test
    public void getKind_GenomeSpace() {
        assertEquals(expectedKind, gsFilePath.getKind());
    }

    @Test
    public void getKind() {
        assertEquals(expectedKind, extFilePath.getKind());
    }
    
    @Test
    public void isDirectory_GenomeSpace() {
        assertEquals(isDirectory, gsFilePath.isDirectory());
    }
    
    @Test
    public void isDirectory() {
        assertEquals(isDirectory, extFilePath.isDirectory());
    }
    
    @Test
    public void isLocal_GenomeSpace() {
        assertEquals(false, gsFilePath.isLocal());
    }

    @Test
    public void isLocal() {
        assertEquals(false, extFilePath.isLocal());
    }

}

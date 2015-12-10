package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.genepattern.server.genomespace.GenomeSpaceFile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
                { "mock.gz", "gz", "gz", false }, 
                { "mock.tar.gz", "gz", "tar.gz", false }, 
                { "mock.fasta.gz", "gz", "fasta.gz", false }, 
                
                // See TestGenomeSpaceFile for tests for custom conversion for GenomeSpace files, e.g. 
                //     CEL_IK50.tab?dataformat=.../dataformat/gct
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
        this.gsFilePath=initGsFileForTest(expectedName);
        // initialize GsFile (this version causes some tests to fail)
        //this.gsFilePath=initGsFileFromGsFileHelper(expectedName);
    }

    // Note: circa GP 3.9.5, the default (name, extension, kind, isDirectory) for a GenomeSpace file will not pass these tests
    // should come to agreement on this
    protected GenomeSpaceFile initGsFileFromGsFileHelper(final String expectedName) {
        try {
            return TestGenomeSpaceFile.mockGsFileFromGsHelper(expectedName);
        }
        catch (Throwable t) {
            gsFilePathInitError=t;
        }
        return null;
    }

    protected GenomeSpaceFile initGsFileForTest(final String expectedName) {
        try {
            return TestGenomeSpaceFile.mockGsFile(expectedName);
        }
        catch (Throwable t) {
            gsFilePathInitError=t;
        }
        return null;
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

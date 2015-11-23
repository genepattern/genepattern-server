package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.assertEquals;

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
                //{ _hostprefix_, _name_, _extension_, _kind_, _isDirectory_ }, 
                { "all_aml_train.gct", "gct", "gct", false },
                { "all_aml_train.GCT", "GCT", "gct", false },
                { "all_aml", null, null, false }, 
                { "all_aml/", null, "directory", true }, 
                { "mock.gz","gz", "gz", false }, 
                { "mock.tar.gz","gz", "tar.gz", false }, 
                { "mock.fasta.gz","gz", "fasta.gz", false }, 
        });
    }
    
    private final String expectedName;
    private final String expectedExtension;
    private final String expectedKind;
    private final boolean isDirectory;
    
    private final ExternalFile extFilePath;
    private final GenomeSpaceFile gsFilePath;
    
    public TestExternalFileInitFromUrl(final String expectedName, final String expectedExtension, final String expectedKind, final boolean isDirectory) 
    {
        this.expectedName=expectedName;
        this.expectedExtension=expectedExtension;
        this.expectedKind=expectedKind;
        this.isDirectory=isDirectory;
        
        // initialize
        final Object gsSession=new Object();
        gsFilePath=new GenomeSpaceFile(gsSession);
        gsFilePath.setUrl(dataGsDir+expectedName);
        extFilePath=new ExternalFile(dataHttpDir+expectedName);
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

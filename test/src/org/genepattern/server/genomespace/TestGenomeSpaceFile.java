package org.genepattern.server.genomespace;

import static org.genepattern.junitutil.Demo.dataGsDir;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.net.MalformedURLException;
import java.net.URL;

import org.genepattern.junitutil.Demo;
import org.genepattern.server.dm.GpFilePath;
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
    public static GenomeSpaceFile mockGsFileFromGsHelper(final String filename) throws MalformedURLException {
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
    public static GenomeSpaceFile mockGsFile(final String filename) throws MalformedURLException {
        final GsSession gsSession=mock(GsSession.class);
        final URL url=new URL(dataGsDir+filename);
        if (!GenomeSpaceFileHelper.isGenomeSpaceFile(url)) {
            throw new IllegalArgumentException("Not a GenomeSpace URL: " + url);
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
        assertEquals("isLocal", false, gpFilePath.isLocal());
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
        assertEquals("isLocal", false, gpFilePath.isLocal());
        assertEquals("relativeUri, expecting fully qualified uri", expectedUri,
                gpFilePath.getRelativeUri().toString());
    }
    
}

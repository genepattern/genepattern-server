package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Test;

public class TestExternalFile {
    public static final String externalHref="http://www.broadinstitute.org:8080";
    
    @Test
    public void isLocal_ExternalFile() throws MalformedURLException {
        final String urlSpec=dataFtpDir+"all_aml_train.gct";
        final URL url=new URL(urlSpec);
        
        GpFilePath gpFilePath=new ExternalFile(urlSpec);
        assertEquals("isLocal, from urlSpec="+urlSpec, false, gpFilePath.isLocal());
        assertEquals("extension", "gct", gpFilePath.getExtension());
        
        gpFilePath=new ExternalFile(url);
        assertEquals("isLocal, from URL="+url, false, ((GpFilePath)(new ExternalFile(url))).isLocal());
    }
    
    protected void checkExtension(final String urlSpec, final String expectedExtension) {
        checkExtension(urlSpec, expectedExtension, expectedExtension);
    }
    protected void checkExtension(final String urlSpec, final String expectedExtension, final String expectedKind) {
        GpFilePath gpFilePath=new ExternalFile(urlSpec);
        assertEquals("extension, '"+urlSpec+"'", expectedExtension, 
                gpFilePath.getExtension());
        assertEquals("kind, '"+urlSpec+"'", expectedKind, 
                gpFilePath.getKind());
    }

    /** initialize extension from URL in ExternalFile constructor */
    @Test
    public void initExtension() {
        checkExtension(dataFtpDir+"all_aml_train.gct", "gct");
        // match existing functionality, preserve case of extension, always convert kind to lower case
        checkExtension(dataFtpDir+"all_aml_train.GCT", "GCT", "gct");
        checkExtension(dataFtpDir+"all_aml", null);
        checkExtension(dataFtpDir+"all_aml/", null, "directory");
        checkExtension("http://www.broadinstitute.org", null);
        checkExtension(dataHttpDir+"mock.tar.gz", "gz", "tar.gz");
        checkExtension(dataHttpDir+"mock.gz", "gz", "gz");
        checkExtension(dataHttpDir+"mock.fasta.gz", "gz", "fasta.gz");
    }
    
    @Test
    public void fromUrlSpec_emptyPath() {
        // scheme://hostname:port, no trailing slash
        final String urlSpec=externalHref;
        assertEquals("getName("+urlSpec+"')", 
                "", 
                new ExternalFile(urlSpec).getName());
    }

    @Test
    public void fromUrlSpec_rootPath() {
        // scheme://hostname:port/, with trailing slash
        // equivalent to root directory
        final String urlSpec=externalHref+"/"; 
        assertEquals("getName("+urlSpec+"')", 
                "/", 
                new ExternalFile(urlSpec).getName());
    }

    @Test
    public void fromUrlSpec_filePath() {
        final String urlSpec=externalHref+"/all_aml_test.gct";
        assertEquals("getName("+urlSpec+"')", 
                "all_aml_test.gct", new ExternalFile(urlSpec).getName());
    }

    @Test
    public void fromUrlSpec_dirPath() {
        // path ends with trailing slash, '/'
        final String urlSpec=externalHref+"/all_aml/";
        assertEquals("getName("+urlSpec+"')", 
                "all_aml/", new ExternalFile(urlSpec).getName());
    }

    @Test
    public void fromUrlSpec_encodedfilePath() {
        final String urlSpec=externalHref+"/all%20aml%20test.gct";
        assertEquals("getName("+urlSpec+"')", "all aml test.gct", 
                new ExternalFile(urlSpec).getName());
    }

    @Test
    public void fromUrlSpec_ignoreQueryString() {
        final String urlSpec=externalHref+"/all_aml_test.gct?name=value&name=value#pathfragment";
        assertEquals("getName("+urlSpec+"')", "all_aml_test.gct", 
                new ExternalFile(urlSpec).getName());
    }
}

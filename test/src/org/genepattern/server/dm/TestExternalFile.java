package org.genepattern.server.dm;

import static org.genepattern.junitutil.Demo.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestExternalFile {
    public static final String externalHref="http://www.broadinstitute.org:8080";
    
    // help method for testing 'all_aml_test.gct' href
    protected void checkValues(final String urlSpec, final GpFilePath gpFilePath) {
        assertEquals("getName('"+urlSpec+"')", "all_aml_test.gct", 
                gpFilePath.getName());
        assertEquals("getExtension('"+urlSpec+"')", "gct", 
                gpFilePath.getExtension());
        assertEquals("getKind('"+urlSpec+"')", "gct", 
                gpFilePath.getKind());
        assertEquals("relativeUri, expecting fully qualified uri", urlSpec, 
                gpFilePath.getRelativeUri().toString());
        assertEquals("isLocal", false, 
                gpFilePath.isLocal());
    }
    
    @Test
    public void fromHttpFile() {
        final String urlSpec=dataHttpDir+"/all_aml_test.gct";
        checkValues(urlSpec, new ExternalFile(urlSpec));
    }

    @Test
    public void fromFtpFile() {
        final String urlSpec=dataFtpDir+"all_aml_test.gct";
        checkValues(urlSpec, new ExternalFile(urlSpec));
    }
    
    @Test
    public void fromHttpsFile() {
        final String urlSpec=dataHttpsDir+"all_aml_test.gct";
        checkValues(urlSpec, new ExternalFile(urlSpec));
    }
    
    @Test
    public void fromUrlSpec_emptyPath() {
        // scheme://hostname:port, no trailing slash
        final String urlSpec=externalHref;
        GpFilePath extFilePath=new ExternalFile(urlSpec);
        assertEquals("getName("+urlSpec+"')", 
                "", 
                extFilePath.getName());
        assertEquals("isDirectory", false, extFilePath.isDirectory());
    }

    @Test
    public void fromUrlSpec_rootPath() {
        // scheme://hostname:port/, with trailing slash
        // equivalent to root directory
        final String urlSpec=externalHref+"/"; 
        GpFilePath extFilePath=new ExternalFile(urlSpec);
        assertEquals("getName("+urlSpec+"')", 
                "/", 
                extFilePath.getName());
        assertEquals("isDirectory", true, extFilePath.isDirectory());
    }

    @Test
    public void fromUrlSpec_encodedfilePath() {
        final String urlSpec=externalHref+"/all%20aml%20test.gct";
        final GpFilePath gpFilePath=new ExternalFile(urlSpec);

        assertEquals("getName("+urlSpec+"')", "all aml test.gct", 
                gpFilePath.getName());
        assertEquals("relativeUri, expecting fully qualified uri", urlSpec, 
                gpFilePath.getRelativeUri().toString());
    }

    @Test
    public void fromUrlSpec_ignoreQueryString() {
        final String urlSpec=externalHref+"/all_aml_test.gct?name=value&name=value#pathfragment";
        assertEquals("getName("+urlSpec+"')", "all_aml_test.gct", 
                new ExternalFile(urlSpec).getName());
    }
}

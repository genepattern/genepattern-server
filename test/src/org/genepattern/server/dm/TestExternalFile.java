package org.genepattern.server.dm;

import static org.junit.Assert.assertEquals;

import org.genepattern.util.SemanticUtil;
import org.junit.Test;

public class TestExternalFile {
    public static final String externalHref="http://www.broadinstitute.org:8080";
    
    
    protected void checkExtension(final String filename, final String expected_extension) {
        assertEquals("getExtension("+filename+")", expected_extension,
                SemanticUtil.getExtension(filename));
    }
    
    @Test
    public void semanticUtil_getExtensionFromName() {
        checkExtension(null, null);
        checkExtension("filename.txt", "txt");
        checkExtension("name_no_ext", "");
        checkExtension("dirname/", "");
        checkExtension(".hidden.txt", "txt");
        checkExtension(".hidden_no_ext", "");
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
    public void fromUrlSpec_filePath() {
        final String urlSpec=externalHref+"/all_aml_test.gct";
        assertEquals("getName("+urlSpec+"')", 
                "all_aml_test.gct", new ExternalFile(urlSpec).getName());
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

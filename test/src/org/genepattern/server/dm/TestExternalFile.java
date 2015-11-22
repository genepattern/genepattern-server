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
        
        gpFilePath=new ExternalFile(url);
        assertEquals("isLocal, from URL="+url, false, ((GpFilePath)(new ExternalFile(url))).isLocal());
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

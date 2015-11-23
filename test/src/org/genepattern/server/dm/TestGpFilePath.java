package org.genepattern.server.dm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;

import org.genepattern.server.config.GpContext;
import org.junit.Test;

/**
 * Test cases for the GpFilePath class.
 * 
 * @author pcarr
 */
public class TestGpFilePath {
    
    @Test
    public void isLocal_ConcreteGpFilePath() {
        final GpFilePath gpFilePath=new GpFilePath() {

            @Override
            public URI getRelativeUri() {
                return null;
            }

            @Override
            public File getServerFile() {
                return null;
            }

            @Override
            public File getRelativeFile() {
                return null;
            }

            @Override
            public boolean canRead(boolean isAdmin, GpContext userContext) {
                return false;
            }

            @Override
            public String getFormFieldValue() {
                return null;
            }

            @Override
            public String getParamInfoValue() {
                return null;
            }
        };
        
        assertEquals("isLocal", true, gpFilePath.isLocal());
    }

}

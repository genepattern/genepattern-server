package org.genepattern.server.webapp;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.servlet.ServletConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

/**
 * jUnit tests for the StartupServlet.
 * @author pcarr
 *
 */
public class TestStartupServlet {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    private ServletConfig servletConfig;

    private File expectedGpHomeDir;
    
    @Before
    public void setUp() {
        expectedGpHomeDir=tmp.newFolder("gp_home");
        servletConfig=Mockito.mock(StartupServlet.class);
    }
    
    @Test
    public void initGpHomeDir_fromServletConfig() {
        when(servletConfig.getInitParameter("GENEPATTERN_HOME")).thenReturn(expectedGpHomeDir.getAbsolutePath());
        File gpHomeDir=new StartupServlet().initGpHomeDir(servletConfig);
        assertEquals(expectedGpHomeDir, gpHomeDir);
    }
    
    @Test
    public void initGpHomeDir_fromServletConfig_altSpelling() {
        when(servletConfig.getInitParameter("gp.home")).thenReturn(expectedGpHomeDir.getAbsolutePath());
        File gpHomeDir=new StartupServlet().initGpHomeDir(servletConfig);
        assertEquals(expectedGpHomeDir, gpHomeDir);
    }
    
    @Test
    public void initGpHomeDir_notSet() {
        File gpHomeDir=new StartupServlet().initGpHomeDir(servletConfig);
        assertNull("by default GENEPATTERN_HOME is not set", gpHomeDir);
    }

    @Test
    public void initGpHomeDir_fromSystemProp() {
        File gpHomeDir=new StartupServlet().initGpHomeDir(expectedGpHomeDir.getAbsolutePath(), servletConfig);
        assertEquals(expectedGpHomeDir.getAbsolutePath(), gpHomeDir.getAbsolutePath());
    }

    @Test
    public void initGpHomeDir_fromSystemProp_emptyString() {
        String gpHomeProp="";
        File gpHomeDir=new StartupServlet().initGpHomeDir(gpHomeProp, servletConfig);
        assertNull("by default GENEPATTERN_HOME is not set", gpHomeDir);
    }
    
    @Test
    public void initGpHomeDir_fromSystemProp_relativePath() {
        String gpHomeProp="gp_home";
        File gpHomeDir=new StartupServlet().initGpHomeDir(gpHomeProp, servletConfig);
        File expected=new File(System.getProperty("user.dir"), gpHomeProp);
        assertEquals(expected, gpHomeDir);
    }
    
//    @Test
//    public void initResourcesDir() {
//        //assume GENEPATTERN_HOME is set
//        new StartupServlet().initResourcesDir(gpWorkingDir);
//    }
    
}

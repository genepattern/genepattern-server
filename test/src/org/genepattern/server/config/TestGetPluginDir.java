package org.genepattern.server.config;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test cases for getting the path to the plugin (aka patches) directory.
 * @author pcarr
 *
 */
public class TestGetPluginDir {
    private GpContext serverContext;
    private File userInstallDir;
    private File gpHomeDir;
    
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    @Before
    public void setUp() {
        serverContext=GpContext.getServerContext();
        userInstallDir=tmp.newFolder("GenePatternServer");
        gpHomeDir=tmp.newFolder(".gp_home");
    }

    @Test
    public void getRootPluginDir_legacy() {
        // simulate 'patches=$USER_INSTALL_DIR$' in genepattern.properties file
        String expectedPluginDir=new File(userInstallDir, "patches").getAbsolutePath();
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
            .addProperty("patches", expectedPluginDir)
        .build();
        //assertNotNull(gpConfig.getRootPluginDir(serverContext));
        assertEquals(expectedPluginDir, gpConfig.getRootPluginDir(serverContext).getAbsolutePath());
    }
    
    @Test
    public void getRootPluginDir_gpHome() {
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
        File pluginDir=gpConfig.getRootPluginDir(serverContext);
        assertNotNull(pluginDir);
        String expectedPluginDir=new File(gpHomeDir, "patches").getAbsolutePath();
        assertEquals(expectedPluginDir, pluginDir.getAbsolutePath());
    }

}

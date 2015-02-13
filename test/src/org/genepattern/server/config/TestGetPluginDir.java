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
    private File userDir;
    private File gpHomeDir;
    private File gpWorkingDir;
    
    private File patchesDir;
    private File customPatchesDir;
    
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    @Before
    public void setUp() {
        serverContext=GpContext.getServerContext();
        userDir=new File(System.getProperty("user.dir"));
        // simulate 'patches=$USER_INSTALL_DIR$/patches' in genepattern.properties file
        File userInstallDir=tmp.newFolder("GenePatternServer");
        gpHomeDir=tmp.newFolder(".gp_home");
        gpWorkingDir=new File(userInstallDir, "Tomcat");
        patchesDir=new File(gpWorkingDir.getParentFile(), "patches");
        customPatchesDir=tmp.newFolder("patches").getAbsoluteFile();
    }

    /** When gpHome is set and 'patches' is not set in one of the config file(s) */
    @Test
    public void getRootPluginDir() {
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
        
        assertEquals(
                "Expecting GP_HOME/patches",
                new File(gpHomeDir, "patches"), 
                gpConfig.getRootPluginDir(serverContext));
    }
    
    /** When gpHome is set and 'patches' is set to an absolute path in one of the config file(s) */
    @Test
    public void getRootPluginDirCustomAbsolutePath() {
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
            .addProperty("patches", customPatchesDir.getPath())
        .build();
        assertEquals(
                "Expecting custom absolute path",
                customPatchesDir, 
                gpConfig.getRootPluginDir(serverContext));
    }

    /** When gpHome is set and 'patches' is set to a relative path in one of the config file(s) */
    @Test
    public void getRootPluginDirCustomRelativePath() {
        File expected = new File(gpHomeDir, "altName");
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
            .addProperty("patches", "altName")
        .build();
        
        assertEquals(expected, gpConfig.getRootPluginDir(serverContext));
    }

    /** When gpHome and gpWorking and patches are not set */
    @Test
    public void getRootPluginDir_legacy() {
        GpConfig gpConfig=new GpConfig.Builder()
        .build();
        
        assertEquals("when gpHome, gpWorking, and patches are not set, expect '../patches'", 
                new File(userDir.getParent(), "patches"), gpConfig.getRootPluginDir(serverContext));
    }

    /** When gpHome and gpWorking are not set and 'patches' is set to an absolute path in one of the config file(s) */
    @Test
    public void getRootPluginDir_legacy_absolutePath() {
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty("patches", customPatchesDir.getPath())
        .build();
        assertEquals(customPatchesDir, gpConfig.getRootPluginDir(serverContext));
    }

    /** When gpHome and gpWorking are not set and 'patches' is set to a relative path in one of the config file(s) */
    @Test
    public void getRootPluginDir_legacy_relativePath() {
        File expected=new File(userDir.getParent(), "patches");
        GpConfig gpConfig=new GpConfig.Builder()
            .addProperty("patches", "../patches")
        .build();
        
        // expected to be relative to the working directory
        assertEquals(expected, gpConfig.getRootPluginDir(serverContext));
    }
    

    /** When only gpWorking is set and patches is not set, expect ../patches. */
    @Test
    public void getRootPluginDir_gpWorkingDir() { 
        GpConfig gpConfig=new GpConfig.Builder()
            .gpWorkingDir(gpWorkingDir)
        .build();
        assertEquals(new File(gpWorkingDir.getParentFile(), "patches"), gpConfig.getRootPluginDir(serverContext));
    }
    

    /** When only gpWorking is set and 'patches' is set to an absolute path in one of the config file(s) */
    @Test
    public void getRootPluginDir_gpWorkingDir_absolutePath() {
        GpConfig gpConfig=new GpConfig.Builder()
            .gpWorkingDir(gpWorkingDir)
            .addProperty("patches", customPatchesDir.getPath())
        .build();
        assertEquals(customPatchesDir, gpConfig.getRootPluginDir(serverContext));
    }

    /** When only gpWorking is set and 'patches' is set to a relative path in one of the config file(s) */
    @Test
    public void getRootPluginDir_gpWorkingDir_relativePath() {
        GpConfig gpConfig=new GpConfig.Builder()
            .gpWorkingDir(gpWorkingDir)
            .addProperty("patches", "../patches")
        .build();
        assertEquals(patchesDir, gpConfig.getRootPluginDir(serverContext));
    }
    
    @Test
    public void getPatchesSubstitution() {
        GpConfig gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
        
        assertEquals("getGPProperty('patches')", 
                new File(gpHomeDir, "patches").getAbsolutePath(), 
                gpConfig.getGPProperty(serverContext, GpConfig.PROP_PLUGIN_DIR));
    }

}

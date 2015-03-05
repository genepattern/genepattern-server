package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test cases for getting the user directory for a given GP user account.
 * @author pcarr
 *
 */
public class TestGetUserRootDir {
    private GpConfig gpConfig;
    private GpContext userContext;
    private File javaWorkingDir;
    private File gpHomeDir;
    final String userId="test_user";
    
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    @Before
    public void setUp() {
        javaWorkingDir=new File(System.getProperty("user.dir"));
        //Note: see StartupServlet for how GENEPATTERN_HOME gets passed from the command line to the GpConfig
        gpHomeDir=temp.newFolder("genepattern_home").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
        userContext=new GpContext.Builder()
            .userId(userId)
        .build();
    }

    @Test
    public void getGpHomeFromConfig() {
        assertEquals("", gpHomeDir, gpConfig.getGpHomeDir());
    }
    
    @Test
    public void getGpWorkingDir_default() {
        assertEquals(javaWorkingDir, gpConfig.getGpWorkingDir());
    }

    @Test
    public void getGpWorkingDir_custom() {
        File gpWorkingDir=temp.newFolder("custom_gp_working_dir");
        gpConfig=new GpConfig.Builder()
            .gpWorkingDir(gpWorkingDir)
        .build();
        assertEquals("custom gpWorkingDir", gpWorkingDir, gpConfig.getGpWorkingDir());
    }
    
    /**
     * By default it's relative to GENEPATTERN_HOME. 
     */
    @Test
    public void rootUserDir_gpHome() {
        File expected=new File(gpHomeDir, "users");
        assertEquals("default rootUserDir", expected, gpConfig.getRootUserDir());
    }
    
    @Test
    public void rootUserDir_gpHomeNotSet() {
        gpConfig=new GpConfig.Builder().build();
        File expected=new File(javaWorkingDir.getParentFile(), "users").getAbsoluteFile();
        assertEquals(expected, gpConfig.getRootUserDir());
    }
    
    @Test
    public void userDir_gpHome() {
        File expected=new File(gpHomeDir, "users/"+userId);
        assertEquals("userDir", expected, gpConfig.getUserDir(userContext));
    }
    
    @Test
    public void userDir_gpHomeNotSet() {
        gpHomeDir=null;
        gpConfig=new GpConfig.Builder()
            .gpHomeDir(null)
        .build();
        // <user.dir>/../users/<user_id>
        File expected=new File(new File(javaWorkingDir.getParentFile(), "users"), userId).getAbsoluteFile();
        assertEquals("userDir", expected, gpConfig.getUserDir(userContext)); 
    }

}

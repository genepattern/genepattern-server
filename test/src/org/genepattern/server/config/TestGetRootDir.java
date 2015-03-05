package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test cases for getting the data directory paths for the GpConfig.
 * <pre>
    <GENEPATTERN_HOME>/users
    <GENEPATTERN_HOME>/jobResults
 *  </pre>
 * @author pcarr
 *
 */
public class TestGetRootDir {
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
    public void rootUserDir_custom() {
        File customDir=temp.newFolder("custom_user_dir").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, customDir.toString())
        .build();
        assertEquals(customDir, gpConfig.getRootUserDir());
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

    @Test
    public void userDir_custom() {
        File customDir=temp.newFolder("custom_user_dir").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
            .addProperty(GpConfig.PROP_USER_ROOT_DIR, customDir.toString())
        .build();
        File expected=new File(customDir, userId).getAbsoluteFile();
        assertEquals(expected, gpConfig.getUserDir(userContext)); 
    }
    
    /**
     * By default it's relative to GENEPATTERN_HOME. 
     * @throws ServerConfigurationException 
     */
    @Test
    public void rootJobDir_gpHome() throws ServerConfigurationException {
        File expected=new File(gpHomeDir, "jobResults");
        assertEquals("default rootJobDir", expected, gpConfig.getRootJobDir(userContext));
    }
    
    @Test
    public void rootJobDir_gpHomeNotSet() throws ServerConfigurationException {
        gpConfig=new GpConfig.Builder().build();
        File expected=new File(javaWorkingDir.getParentFile(), "jobResults").getAbsoluteFile();
        assertEquals(expected, gpConfig.getRootJobDir(userContext));
    }
    
    @Test
    public void rootJobDir_custom() throws ServerConfigurationException {
        File customDir=temp.newFolder("custom_job_results").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .addProperty(GpConfig.PROP_JOBS, customDir.toString())
        .build();
        assertEquals(customDir, gpConfig.getRootJobDir(userContext));
    }

    /**
     * By default it's relative to GENEPATTERN_HOME. 
     * @throws ServerConfigurationException 
     */
    @Test
    public void rootSoapAttachmentDir_gpHome() throws ServerConfigurationException {
        File expected=new File(gpHomeDir, "temp/attachments");
        assertEquals(expected, gpConfig.getSoapAttDir(userContext));
    }
    
    @Test
    public void rootSoapAttachmentDir_gpHomeNotSet() throws ServerConfigurationException {
        gpConfig=new GpConfig.Builder().build();
        File expected=new File(javaWorkingDir.getParentFile(), "temp/attachments").getAbsoluteFile();
        assertEquals(expected, gpConfig.getSoapAttDir(userContext));
    }

}

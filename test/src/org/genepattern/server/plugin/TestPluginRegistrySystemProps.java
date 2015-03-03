package org.genepattern.server.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Properties;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.util.GPConstants;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestPluginRegistrySystemProps {
    GpConfig gpConfig=null;
    GpContext gpContext=null;
    PluginRegistrySystemProps pr=new PluginRegistrySystemProps();
    File resourcesDir;

    
    public static final String defaultInstalledPatchLSIDs=
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00002:1,urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00004:1,urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00006:1,urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00007:1,urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00008:1,urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00009:1,urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00012:1";

    public static final List<PatchInfo> initDefaultInstalledPatchInfos() throws MalformedURLException {
        return PluginRegistrySystemProps.getInstalledPatches(defaultInstalledPatchLSIDs);
    }
    
    public static final String GenePattern_3_4="urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00012:1";
    public static final String BWA="urn:lsid:broadinstitute.org:plugin:BWA_0_7_4:2";
    public static final String TopHat="urn:lsid:broadinstitute.org:plugin:TopHat_2.0.9:3";

    @Rule
    public TemporaryFolder temp=new TemporaryFolder();
    
    @Before
    public void setUp() {
        // create a 'genepattern.properties' file
        resourcesDir=temp.newFolder("resources");
        File gpProps=new File(resourcesDir, "genepattern.properties");
        Properties defaultProps=new Properties();
        defaultProps.setProperty(GPConstants.INSTALLED_PATCH_LSIDS, defaultInstalledPatchLSIDs);
        try {
            defaultProps.store(new FileOutputStream(gpProps), "");
        }
        catch (Throwable t) {
            fail("error creating genepattern.properties file for test");
        }
        
        gpConfig=new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .serverProperties(new GpServerProperties.Builder()
                .resourcesDir(resourcesDir)
            .build())
        .build();
        
        gpContext=new GpContext.Builder().build();
    }
    
    @Test
    public void defaultPlugins_fromSysProps() throws Exception {
        System.setProperty(GPConstants.INSTALLED_PATCH_LSIDS, defaultInstalledPatchLSIDs);
        List<PatchInfo> actual=pr.getInstalledPatches(gpConfig, gpContext);
        assertEquals("installedPatches.size", 7, actual.size());
//        List<PatchInfo> expected=Arrays.asList( defaultInstalledPatchLSIDs.split(",") );
//        assertEquals(
//                //expected
//                expected,
//                // actual
//                actual
//                );
    }
    
    @Test
    public void isInstalled_fromSysProps_yes() throws Exception {
        System.setProperty(GPConstants.INSTALLED_PATCH_LSIDS, defaultInstalledPatchLSIDs);
        assertEquals(true, 
                pr.isInstalled(gpConfig, gpContext, new PatchInfo("urn:lsid:broad.mit.edu:cancer.software.genepattern.server.patch:00002:1")));
    }
    
    @Test
    public void isInstalled_fromSysProps_GP_3_4_patch() throws Exception {
        System.setProperty(GPConstants.INSTALLED_PATCH_LSIDS, defaultInstalledPatchLSIDs);
        assertEquals(true, pr.isInstalled(gpConfig, gpContext, new PatchInfo(GenePattern_3_4)));
    }
    
    @Test
    public void isInstalled_fromSysProps_no() throws Exception {
        System.setProperty(GPConstants.INSTALLED_PATCH_LSIDS, defaultInstalledPatchLSIDs);
        assertEquals(false, pr.isInstalled(gpConfig, gpContext, new PatchInfo(BWA)));
    }
    
    @Test
    public void recordPatch() throws Exception {
        System.setProperty("resources", resourcesDir.getAbsolutePath());
        assertEquals("isInstalled before", false, pr.isInstalled(gpConfig, gpContext, new PatchInfo(TopHat)));
        pr.recordPatch(gpConfig, gpContext, new PatchInfo(TopHat));
        assertEquals("isInstalled after", true, pr.isInstalled(gpConfig, gpContext, new PatchInfo(TopHat)));   
    }

}

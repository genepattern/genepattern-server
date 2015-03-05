package org.genepattern.server.plugin;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.domain.Props;
import org.junit.Before;
import org.junit.Test;

//import org.hamcrest.Matchers;
//import static org.hamcrest.Matchers.*;

/**
 * jUnit tests for the MigratePlugins class.
 * @author pcarr
 *
 */
public class TestMigratePlugins {
    GpConfig gpConfig;
    GpContext gpContext;
    PluginRegistry pluginRegistry;
    File rootPluginDir;
    MigratePlugins migratePlugins;
    
    @Before
    public void setUp() {
        rootPluginDir=FileUtil.getDataFile("patches").getAbsoluteFile();
        gpConfig=mock(GpConfig.class);
        gpContext=new GpContext.Builder().build();
        when(gpConfig.getRootPluginDir(gpContext)).thenReturn(rootPluginDir);
        pluginRegistry=mock(PluginRegistry.class);
        migratePlugins=new MigratePlugins(gpConfig, gpContext, pluginRegistry);
    }
    
    /**
     * To help with junit test evaluation, get the list of lsid from the list of PathcInfo
     * @param patchInfos
     * @return
     */
    public static List<String> getLsids(List<PatchInfo> patchInfos) {
        if (patchInfos==null) return null;
        if (patchInfos.size()==0) return Collections.emptyList();
        List<String> lsidStr=new ArrayList<String>(patchInfos.size());
        for(PatchInfo patchInfo : patchInfos) {
            lsidStr.add(patchInfo.getLsid());
        }
        return lsidStr;
    }
    
    /**
     * Hand-coded replacement for assertEquals(String message, List<PatchInfo> expected, List<PatchInfo> actual),
     * compare by lsid only.
     * 
     * @param message
     * @param expected
     * @param actual
     */
    public static void assertComparePatchInfo(String message, List<PatchInfo> expected, List<PatchInfo> actual) {
        assertEquals(message,
                getLsids(expected),
                getLsids(actual));
    }
        
    @Test
    public void dbCheck() throws Exception {
        DbUtil.initDb();
        assertEquals("checkDb before migrate", false, migratePlugins.checkDb());
        
        migratePlugins.updateDb();
        assertEquals("checkDb after migrate", true, migratePlugins.checkDb());
        
        Props.saveProp(MigratePlugins.PROP_DB_CHECK, "false");
        assertEquals("after Props.saveProp(...,'false')", false, migratePlugins.checkDb());
        
        // cleanup
        Props.removeProp(MigratePlugins.PROP_DB_CHECK);
        assertEquals("checkDb after Props.removeProp", false, migratePlugins.checkDb());
    }
    
    @Test
    public void initPatchInfo_Ant() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.Ant_1.8.1/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest, null);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:Ant_1.8:1", patchInfo.getLsid());
    }
    
    @Test
    public void initPatchInfo_Bowtie() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.Bowtie_2.1.0.2/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest, null);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:2", patchInfo.getLsid());
    }

    @Test
    public void initPatchInfo_CheckPython() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.Check_Python_2.6.2/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest, null);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:Check_Python_2.6:2", patchInfo.getLsid());
    }

    @Test
    public void initPatchInfo_SAMTools() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.SAMTools_0_1_19.2/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest, null);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:SAMTools_0.1.19:2", patchInfo.getLsid());
    }

    @Test
    public void initPatchInfo_TopHat() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.TopHat_2.0.9.3/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest, null);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:TopHat_2.0.9:3", patchInfo.getLsid());
    }
    
    @Test
    public void scanRootPluginDir() throws Exception {
        MigratePlugins migratePlugins=new MigratePlugins(gpConfig, gpContext, pluginRegistry);
        File rootPluginDir=FileUtil.getDataFile("patches").getAbsoluteFile();
        migratePlugins.scanPluginDir(rootPluginDir);
        assertEquals("patchInfos.size", 5, migratePlugins.getPatchInfos().size()); 
        PatchInfo[] patchInfos=migratePlugins.getPatchInfos().toArray(new PatchInfo[0]);
        assertEquals("patchInfos[0]", "urn:lsid:broadinstitute.org:plugin:Ant_1.8:1", patchInfos[0].getLsid());
        assertEquals("patchInfos[1]", "urn:lsid:broadinstitute.org:plugin:Check_Python_2.6:2", patchInfos[1].getLsid());
        assertEquals("patchInfos[2]", "urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:2", patchInfos[2].getLsid());
        assertEquals("patchInfos[3]", "urn:lsid:broadinstitute.org:plugin:SAMTools_0.1.19:2", patchInfos[3].getLsid());
        assertEquals("patchInfos[4]", "urn:lsid:broadinstitute.org:plugin:TopHat_2.0.9:3", patchInfos[4].getLsid());
        
        assertEquals("Expectiung to find 4 custom properties files", 4, migratePlugins.getCustomPropFiles().size());
        
        Properties customProps=migratePlugins.collectPluginCustomProps();

        assertEquals("customProps.size", 5, customProps.size());
        assertTrue(customProps.containsKey("TopHat_2.0.9"));
        assertEquals("/Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/TopHat/2.0.9/tophat-2.0.9.OSX_x86_64",
                customProps.getProperty("TopHat_2.0.9"));
        assertEquals("/Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/SAMTools/0.1.19",
                customProps.getProperty("SAMTools_0.1.19"));
        assertEquals("/Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/Bowtie/2.1.0/bowtie2-2.1.0",
                customProps.getProperty("Bowtie_2.1.0"));
        assertEquals("<java> -jar /Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/ant/apache-ant-1.8.4/lib/ant-launcher.jar -Dant.home=/Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/ant/apache-ant-1.8.4",
                customProps.getProperty("ant-1.8"));
        assertEquals("/Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/ant/apache-ant-1.8.4",
                customProps.getProperty("ant-1.8_HOME"));
    }
    
    @Test
    public void initPatchDir() throws Exception {
        File manifest=new File(rootPluginDir, "broadinstitute.org.plugin.Check_Python_2.6.2/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest, null);
        assertEquals("patchInfo.patchDir", manifest.getParentFile().getAbsolutePath(), patchInfo.getPatchDir());
    }
    
    @Test
    public void collectPluginCustomProps_noProps() throws Exception {
        File manifest=new File(rootPluginDir, "broadinstitute.org.plugin.Check_Python_2.6.2/manifest");
        migratePlugins.visitPluginManifest(manifest);
        assertEquals("Check_Python, customProperties.size", 0, migratePlugins.collectPluginCustomProps().size());
    }

    @Test
    public void collectPluginCustomProps_oneProp() throws Exception {
        File manifest=new File(rootPluginDir, "broadinstitute.org.plugin.Bowtie_2.1.0.2/manifest");
        migratePlugins.visitPluginManifest(manifest);
        Properties customProps=migratePlugins.collectPluginCustomProps();
        assertEquals("Bowtie, customProperties.size", 1, customProps.size());
        assertEquals("/Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/Bowtie/2.1.0/bowtie2-2.1.0",
                customProps.getProperty("Bowtie_2.1.0"));
    }

    @Test
    public void collectPluginCustomProps_twoProps() throws Exception {
        File manifest=new File(rootPluginDir, "broadinstitute.org.plugin.Ant_1.8.1/manifest");
        migratePlugins.visitPluginManifest(manifest);
        Properties customProps=migratePlugins.collectPluginCustomProps();
        assertEquals("Ant, customProperties.size", 2, customProps.size());
        assertEquals("<java> -jar /Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/ant/apache-ant-1.8.4/lib/ant-launcher.jar -Dant.home=/Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/ant/apache-ant-1.8.4",
                customProps.getProperty("ant-1.8"));
        assertEquals("/Users/Shared/Broad/Applications/GP-3.9.0-prod/GenePatternServer/patches/ant/apache-ant-1.8.4",
                customProps.getProperty("ant-1.8_HOME"));
    }

}

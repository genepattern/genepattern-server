/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.plugin;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.domain.PropsTable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * jUnit tests for the MigratePlugins class.
 * @author pcarr
 *
 */
public class TestMigratePlugins {
    /**
     * For testing/debugging, implements the PluginRegistry in-memory using java collections classes.
     * @author pcarr
     *
     */
    static class PluginRegistryInMemory implements PluginRegistry {
        private Map<String,PatchInfo> installedPatches=new HashMap<String,PatchInfo>();

        @Override
        public List<PatchInfo> getInstalledPatches(GpConfig gpConfig, GpContext gpContext) throws Exception {
            Collection<PatchInfo> col=installedPatches.values();
            return new ArrayList<PatchInfo>(col);
        }

        @Override
        public boolean isInstalled(GpConfig gpConfig, GpContext gpContext, PatchInfo patchInfo) throws Exception {
            return installedPatches.containsKey(patchInfo.getLsid());
        }

        @Override
        public void recordPatch(GpConfig gpConfig, GpContext gpContext, PatchInfo patchInfo) throws Exception {
            installedPatches.put(patchInfo.getLsid(), patchInfo);
        }
    }
    
    GpConfig gpConfig;
    GpContext gpContext;
    PluginRegistry pluginRegistry;
    File rootPluginDir;
    MigratePlugins migratePlugins;
    
    @Before
    public void setUp() {
        rootPluginDir=FileUtil.getDataFile("patches").getAbsoluteFile();
        gpConfig=new GpConfig.Builder()
            .rootPluginDir(rootPluginDir)
        .build();
        gpContext=new GpContext.Builder().build();
        pluginRegistry=new PluginRegistryInMemory();
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
        
        PropsTable.saveProp(MigratePlugins.PROP_DB_CHECK, "false");
        assertEquals("after Props.saveProp(...,'false')", false, migratePlugins.checkDb());
        
        // cleanup
        PropsTable.removeProp(MigratePlugins.PROP_DB_CHECK);
        assertEquals("checkDb after Props.removeProp", false, migratePlugins.checkDb());
    }
    
    @Test
    public void initPatchInfo_Ant() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.Ant_1.8.1/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:Ant_1.8:1", patchInfo.getLsid());
        assertEquals("patchInfo.customPropFiles.size", 1, patchInfo.getCustomPropFiles().size());
        assertEquals("patchInfo.customPropFiles[0].name", "Ant_1_8.custom.properties", patchInfo.getCustomPropFiles().get(0).getName());
    }

    /**
     * test case for incorrectly escaped file paths in the Ant_1_8.custom.properties file for Windows installations.
     * it's not implemented
     */
    @Ignore @Test
    public void initPatchInfo_Ant_1_8_WinHack() throws Exception {
        File manifest=FileUtil.getDataFile("patches2/broadinstitute.org.plugin.Ant_1.8.1_Win/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:Ant_1.8:1", patchInfo.getLsid());
        assertEquals("patchInfo.customPropFiles.size", 1, patchInfo.getCustomPropFiles().size());
        assertEquals("patchInfo.customPropFiles[0].name", "Ant_1_8.custom.properties", patchInfo.getCustomPropFiles().get(0).getName());
        
        assertEquals("ant-1.8", 
                "<java> -jar C\\:GenePatternServer\\patches\\ant\\apache-ant-1.8.4\\lib\\ant-launcher.jar -Dant.home\\=C\\:GenePatternServer\\patches\\ant/apache-ant-1.8.4",
                patchInfo.getCustomProps().getProperty("ant-1.8"));
        assertEquals("ant-1.8_HOME", 
                "", 
                patchInfo.getCustomProps().getProperty("ant-1.8_HOME"));
    }
    
    @Test
    public void initPatchInfo_Bowtie() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.Bowtie_2.1.0.2/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:2", patchInfo.getLsid());
        assertEquals("patchInfo.customPropFiles.size", 1, patchInfo.getCustomPropFiles().size());
        assertEquals("patchInfo.customPropFiles[0].name", "Bowtie_2.1.0.custom.properties", patchInfo.getCustomPropFiles().get(0).getName());
    }

    @Test
    public void initPatchInfo_CheckPython() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.Check_Python_2.6.2/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:Check_Python_2.6:2", patchInfo.getLsid());
        assertEquals("patchInfo.customPropFiles.size", 0, patchInfo.getCustomPropFiles().size());
    }

    @Test
    public void initPatchInfo_SAMTools() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.SAMTools_0_1_19.2/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:SAMTools_0.1.19:2", patchInfo.getLsid());
        assertEquals("patchInfo.customPropFiles.size", 1, patchInfo.getCustomPropFiles().size());
        assertEquals("patchInfo.customPropFiles[0].name", "SAMTools_0.1.19.custom.properties", patchInfo.getCustomPropFiles().get(0).getName());
    }

    @Test
    public void initPatchInfo_TopHat() throws Exception {
        File manifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.TopHat_2.0.9.3/manifest");
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest);
        assertEquals("patchInfo.lsid", "urn:lsid:broadinstitute.org:plugin:TopHat_2.0.9:3", patchInfo.getLsid());
        assertEquals("patchInfo.customPropFiles.size", 1, patchInfo.getCustomPropFiles().size());
        assertEquals("patchInfo.customPropFiles[0].name", "TopHat_2.0.9.custom.properties", patchInfo.getCustomPropFiles().get(0).getName());
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
        
        assertEquals("Expecting to find 4 custom properties files", 4, migratePlugins.collectPluginCustomPropFiles().size());
        
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
        PatchInfo patchInfo=MigratePlugins.initPatchInfoFromManifest(manifest);
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

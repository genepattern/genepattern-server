package org.genepattern.server.plugin;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
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
    public void scanRootPluginDir() throws Exception {
        migratePlugins.scanPluginDir(rootPluginDir);
        assertEquals("Expecting to find 5 patches", 5, migratePlugins.getPatchInfos().size()); 
        PatchInfo[] patchInfos=migratePlugins.getPatchInfos().toArray(new PatchInfo[0]);
        assertEquals("patchInfos[0]", new PatchInfo("urn:lsid:broadinstitute.org:plugin:Ant_1.8:1"), patchInfos[0]);
        assertEquals("patchInfos[1]", new PatchInfo("urn:lsid:broadinstitute.org:plugin:Check_Python_2.6:2"), patchInfos[1]);
        assertEquals("patchInfos[2]", new PatchInfo("urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:2"), patchInfos[2]);
        assertEquals("patchInfos[3]", new PatchInfo("urn:lsid:broadinstitute.org:plugin:SAMTools_0.1.19:2"), patchInfos[3]);
        assertEquals("patchInfos[4]", new PatchInfo("urn:lsid:broadinstitute.org:plugin:TopHat_2.0.9:3"), patchInfos[4]);
        
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

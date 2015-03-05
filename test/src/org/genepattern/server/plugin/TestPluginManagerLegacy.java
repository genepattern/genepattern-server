package org.genepattern.server.plugin;

import static org.genepattern.server.plugin.TestMigratePlugins.assertComparePatchInfo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.executor.JobDispatchException;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.TaskInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class TestPluginManagerLegacy {
    final String ANT="urn:lsid:broadinstitute.org:plugin:Ant_1.8:1";
    final String BWA="urn:lsid:broadinstitute.org:plugin:BWA_0_7_4:2";
    final String Bowtie_2_1_0="urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:2";
    final String Check_Python_2_6="urn:lsid:broadinstitute.org:plugin:Check_Python_2.6:2";
    final String SAMTools_0_1_19="urn:lsid:broadinstitute.org:plugin:SAMTools_0_1_19:2";
    final String TopHat_2_0_11="urn:lsid:broadinstitute.org:plugin:TopHat_2.0.11:4";
    
    private GpConfig gpConfig;
    private GpContext gpContext;
    private File gpHomeDir;
    private File pluginDir;
    private File resourcesDir;
    
    private final String ant_val="<java> -cp <tomcatCommonLib>/tools.jar -jar <tomcatCommonLib>/ant-launcher.jar -Dant.home=<tomcatCommonLib> -lib <tomcatCommonLib>";
    private final String java_val="java";
    private final String tomcatCommonLib_val=".";

    private Properties systemProps;
    private final String cmdLine="<ant> -f installAnt.xml -Dresources=<resources> -Dplugin.dir=<patches> -Dant-1.8_HOME=<ant-1.8_HOME>";
    private List<String> expected;
    
    private List<PatchInfo> topHatPatchInfos;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    
    @Before
    public void setUp() throws MalformedURLException {
        gpHomeDir=tmp.newFolder(".genepattern");
        pluginDir=tmp.newFolder("patches");
        resourcesDir=tmp.newFolder("resources");
        
        systemProps=new Properties();
        systemProps.setProperty("java", java_val);
        systemProps.setProperty("tomcatCommonLib", tomcatCommonLib_val);
        systemProps.setProperty("ant", ant_val);
        systemProps.setProperty(GpConfig.PROP_PLUGIN_DIR, pluginDir.getAbsolutePath());

        gpConfig=new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .addProperties(systemProps)
        .build();
        gpContext=GpContext.getServerContext();
        
        expected=Arrays.asList(
            java_val, "-cp", tomcatCommonLib_val+"/tools.jar", "-jar", tomcatCommonLib_val+"/ant-launcher.jar", "-Dant.home="+tomcatCommonLib_val, "-lib", tomcatCommonLib_val, "-f", "installAnt.xml", "-Dresources="+resourcesDir.getAbsolutePath(), "-Dplugin.dir="+pluginDir.getAbsolutePath(), "-Dant-1.8_HOME=");
        
        topHatPatchInfos=Arrays.asList( 
            new PatchInfo(ANT,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip"),
            new PatchInfo(Check_Python_2_6,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Check_Python_2.6/broadinstitute.org:plugin/Check_Python_2.6/2/Check_Python_2_6.zip"),
            new PatchInfo(Bowtie_2_1_0,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Bowtie_2.1.0/broadinstitute.org:plugin/Bowtie_2.1.0/2/Bowtie_2_1_0.zip"),
            new PatchInfo(SAMTools_0_1_19,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/SAMTools_0.1.19/broadinstitute.org:plugin/SAMTools_0.1.19/2/SAMTools_0_1_19.zip"),
            new PatchInfo(TopHat_2_0_11,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/TopHat_2.0.11/broadinstitute.org:plugin/TopHat_2.0.11/4/TopHat_2_0_11.zip")
        );

    }

    @Test
    public void getPatchDirectory() throws ConfigurationException {
        assertEquals(new File(pluginDir, ANT), 
                PluginManagerLegacy.getPatchDirectory(gpConfig, gpContext, ANT));
    }
    
    @Test(expected=ConfigurationException.class)
    public void getPatchDirectory_ConfigurationException() throws ConfigurationException {
        gpConfig=Mockito.mock(GpConfig.class);
        gpContext=Mockito.mock(GpContext.class);
        when(gpConfig.getRootPluginDir(gpContext)).thenReturn(null);
        PluginManagerLegacy.getPatchDirectory(gpConfig, gpContext, ANT);
    }
    
    @Test
    public void createPluginCmdLine_from_systemProps() {
        systemProps.setProperty("resources", resourcesDir.getAbsolutePath());
        assertEquals(expected, PluginManagerLegacy.initCmdLineArray(systemProps, cmdLine));
    }
    
    @Test
    public void createPluginCmdLine() {
        List<String> actual=PluginManagerLegacy.initCmdLineArray(gpConfig, gpContext, cmdLine);
        assertEquals(expected, actual);
    }
    
    @Test
    public void substitutePatches_whenPatchesNotSet() {
        String cmdLine="echo patches=<patches>";
        List<String> expected=Arrays.asList("echo", "patches="+new File(gpHomeDir,"patches").getAbsolutePath());
        gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
        assertEquals(expected, PluginManagerLegacy.initCmdLineArray(gpConfig, gpContext, cmdLine));
    }
    
    @Test
    public void substituteResources_whenResourcesNotSet() {
        String cmdLine="echo resources=<resources>";
        List<String> expected=Arrays.asList("echo", "resources="+new File(gpHomeDir,"resources").getAbsolutePath());
        gpConfig=new GpConfig.Builder()
            .gpHomeDir(gpHomeDir)
        .build();
        assertEquals(expected, PluginManagerLegacy.initCmdLineArray(gpConfig, gpContext, cmdLine));
    }
    
    @Test
    public void requiredPatches_none() throws Exception {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy();
        
        List<PatchInfo> expected=Collections.emptyList();
        assertEquals("no '"+GPConstants.REQUIRED_PATCH_LSIDS+"' in manifest",
                expected,
                pluginMgr.getRequiredPatches(taskInfo));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void requiredPatches_empty() throws Exception {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getAttributes().put(GPConstants.REQUIRED_PATCH_LSIDS, "");
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy();
        
        List<PatchInfo> expected=Collections.emptyList();
        assertEquals("'"+GPConstants.REQUIRED_PATCH_LSIDS+"=' in manifest",
                expected,
                pluginMgr.getRequiredPatches(taskInfo));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void requiredPatches_onePatchLsidNoPatchUrl() throws Exception {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getAttributes().put(GPConstants.REQUIRED_PATCH_LSIDS, BWA);
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy();
        
        assertComparePatchInfo("'"+GPConstants.REQUIRED_PATCH_LSIDS+"=' in manifest",
                Arrays.asList(new PatchInfo(BWA, null)),
                pluginMgr.getRequiredPatches(taskInfo));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void requiredPatches_twoPatchLsidNoPatchUrl() throws Exception {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getAttributes().put(GPConstants.REQUIRED_PATCH_LSIDS, ANT+","+BWA);
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy();
        
        assertComparePatchInfo("'"+GPConstants.REQUIRED_PATCH_LSIDS+"=' in manifest",
                Arrays.asList(new PatchInfo(ANT, null), new PatchInfo(BWA, null)),
                pluginMgr.getRequiredPatches(taskInfo));
    }
    
    @Test
    public void requiredPatches_TopHat() throws Exception { 
        File tophatManifest=FileUtil.getSourceFile(this.getClass(), "TopHat_manifest");
        TaskInfo taskInfo=TaskUtil.getTaskInfoFromManifest(tophatManifest);
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy();
        List<PatchInfo> actual=pluginMgr.getRequiredPatches(taskInfo);
        assertNotNull(actual);
        assertComparePatchInfo("", topHatPatchInfos, actual);
    }
    
    @Test(expected=JobDispatchException.class)
    public void requiredPatches_mismatchedLsidAndUrl() throws Exception {
        String requiredPatchLSIDs=ANT+","+Check_Python_2_6;
        String requiredPatchURLs="http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip";
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy();
        pluginMgr.getRequiredPatches(requiredPatchLSIDs, requiredPatchURLs);
    }
    
    @Test
    public void patchesToInstall_TopHat_none_installed() throws Exception {
        GpConfig gpConfig=mock(GpConfig.class);
        GpContext gpContext=new GpContext.Builder().build();
        PluginRegistry pluginRegistry=mock(PluginRegistry.class);
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy(gpConfig, gpContext, pluginRegistry);
        
        File tophatManifest=FileUtil.getSourceFile(this.getClass(), "TopHat_manifest");
        TaskInfo taskInfo=TaskUtil.getTaskInfoFromManifest(tophatManifest);
        List<PatchInfo> patchesToInstall=pluginMgr.getPatchesToInstall(taskInfo);
        assertComparePatchInfo("none installed", topHatPatchInfos, patchesToInstall);
    }

    @Test
    public void patchesToInstall_TopHat_some_installed() throws Exception {
        final File tophatManifest=FileUtil.getSourceFile(this.getClass(), "TopHat_manifest");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromManifest(tophatManifest);
        final List<PatchInfo> installedPatches=Arrays.asList(
                new PatchInfo(ANT),
                new PatchInfo(Check_Python_2_6),
                new PatchInfo(Bowtie_2_1_0)
                );
        final List<PatchInfo> expected=Arrays.asList(
            new PatchInfo(SAMTools_0_1_19,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/SAMTools_0.1.19/broadinstitute.org:plugin/SAMTools_0.1.19/2/SAMTools_0_1_19.zip"),
            new PatchInfo(TopHat_2_0_11,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/TopHat_2.0.11/broadinstitute.org:plugin/TopHat_2.0.11/4/TopHat_2_0_11.zip")
                );
        
        PluginRegistry pluginRegistry=new PluginRegistry() {

            @Override
            public List<PatchInfo> getInstalledPatches(GpConfig gpConfig, GpContext gpContext) throws Exception {
                return installedPatches;
            }

            @Override
            public boolean isInstalled(GpConfig gpConfig, GpContext gpContext, PatchInfo patchInfo) throws Exception {
                boolean in=installedPatches.contains(patchInfo);
                return in;
            }

            @Override
            public void recordPatch(GpConfig gpConfig, GpContext gpContext, PatchInfo patchInfo) throws Exception {
                throw new IllegalArgumentException("Not implemented!");
            }
        };
        
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy(gpConfig, gpContext, pluginRegistry);

        List<PatchInfo> patchesToInstall=pluginMgr.getPatchesToInstall(taskInfo);
        //assertComparePatchInfo("some installed", expected, patchesToInstall);
        assertComparePatchInfo("some installed", expected, patchesToInstall);

    }

}

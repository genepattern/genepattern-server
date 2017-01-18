/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.plugin;

import static org.genepattern.server.plugin.TestMigratePlugins.assertComparePatchInfo;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.genepattern.junitutil.Demo;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.executor.JobDispatchException;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.TaskInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestPluginManagerLegacy {
    final static String ANT="urn:lsid:broadinstitute.org:plugin:Ant_1.8:1";
    final static String BWA="urn:lsid:broadinstitute.org:plugin:BWA_0_7_4:2";
    final static String Bowtie_2_1_0="urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:2";
    final static String Check_Python_2_6="urn:lsid:broadinstitute.org:plugin:Check_Python_2.6:2";
    final static String SAMTools_0_1_19="urn:lsid:broadinstitute.org:plugin:SAMTools_0_1_19:2";
    final static String TopHat_2_0_11="urn:lsid:broadinstitute.org:plugin:TopHat_2.0.11:4";
    
    private HibernateSessionManager mgr;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    @Before
    public void setUp() {
        mgr=mock(HibernateSessionManager.class);
    }
    
    private static List<PatchInfo> initTopHatPatchInfos() throws MalformedURLException {
        return Arrays.asList( 
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
    public void getPatchDirectory() throws Exception {
        final GpConfig gpConfig = mock(GpConfig.class);
        final File rootPluginDir = new File(tmp.newFolder(), "patches");
        when(gpConfig.getRootPluginDir(Demo.serverContext)).thenReturn(rootPluginDir);
        final PluginManagerLegacy pluginMgr=new PluginManagerLegacy(mgr, gpConfig, Demo.serverContext, null);
        assertEquals(new File(rootPluginDir, "broadinstitute.org.plugin.Ant_1.8.1"), 
                pluginMgr.getPatchDirectory(new LSID(ANT)));
    }
    
    @Test(expected=JobDispatchException.class)
    public void getPatchDirectory_JobDispatchException() throws Exception {
        final GpConfig gpConfig=mock(GpConfig.class);
        when(gpConfig.getRootPluginDir(Demo.serverContext)).thenReturn(null);
        final PluginManagerLegacy pluginMgr=new PluginManagerLegacy(mgr, gpConfig, Demo.serverContext, null);
        pluginMgr.getPatchDirectory(new LSID(ANT));
    }
    
    @Test
    public void createPluginCmdLine() throws IOException {
        final String pluginCmdLineFromManifest="<ant> -f installAnt.xml -Dresources=<resources> -Dplugin.dir=<patches> -Dant-1.8_HOME=<ant-1.8_HOME>";

        // setup ...
        final File tmpDir=tmp.newFolder();
        final File resourcesDir=new File(tmpDir, "resources");
        final File pluginDir=new File(tmpDir, "patches");
        final String java_val="java";
        final String ant_val="<java> -Dant.home=<ant-1.8_HOME> -cp <ant-1.8_HOME>/lib/ant-launcher.jar org.apache.tools.ant.launch.Launcher";
        final String ant_1_8_home=new File("website/WEB-INF/tools/ant/apache-ant-1.8.4").getAbsolutePath();

        final GpConfig gpConfig = Demo.gpConfig();
        when(gpConfig.getGPProperty(Demo.serverContext, "ant")).thenReturn(ant_val);
        when(gpConfig.getGPProperty(Demo.serverContext, "ant-1.8_HOME")).thenReturn(ant_1_8_home);
        when(gpConfig.getGPProperty(Demo.serverContext, "java")).thenReturn(java_val);
        when(gpConfig.getResourcesDir()).thenReturn(resourcesDir);
        when(gpConfig.getRootPluginDir(Demo.serverContext)).thenReturn(pluginDir);
        // ... end setup
        
        final List<String> expected=Arrays.asList(
                java_val, "-Dant.home="+ant_1_8_home, "-cp", ant_1_8_home+"/lib/ant-launcher.jar", "org.apache.tools.ant.launch.Launcher",
                    "-f", "installAnt.xml", "-Dresources="+resourcesDir.getAbsolutePath(), 
                    "-Dplugin.dir="+pluginDir.getAbsolutePath(), 
                    "-Dant-1.8_HOME="+ant_1_8_home);
        assertThat(
                // actual
                PluginManagerLegacy.initCmdLineArray(gpConfig, Demo.serverContext, pluginCmdLineFromManifest), 
                // expected
                is(expected));
    }
    
    @Test
    public void substitutePatches_whenPatchesNotSet() throws IOException {
        final String cmdLine="echo patches=<patches>";
        
        // setup ...
        final File tmpDir=tmp.newFolder();
        final File pluginDir=new File(tmpDir, "patches");
        final GpConfig gpConfig = mock(GpConfig.class);
        when(gpConfig.getRootPluginDir(Demo.serverContext)).thenReturn(pluginDir);
        // ... end setup
        
        assertThat(
                // actual
                PluginManagerLegacy.initCmdLineArray(gpConfig, Demo.serverContext, cmdLine), 
                // expected
                is(Arrays.asList("echo", "patches="+pluginDir.getAbsolutePath())));
    }
    
    @Test
    public void substituteResources_whenResourcesNotSet() throws IOException {
        final String cmdLine="echo resources=<resources>";
        
        // setup ...
        final File tmpDir=tmp.newFolder();
        final File resourcesDir=new File(tmpDir, "resources");
        final GpConfig gpConfig = mock(GpConfig.class);
        when(gpConfig.getResourcesDir()).thenReturn(resourcesDir);
        // ... end setup
        
        assertThat(
                // actual
                PluginManagerLegacy.initCmdLineArray(gpConfig, Demo.serverContext, cmdLine),
                // expected
                is(Arrays.asList("echo", "resources="+resourcesDir.getAbsolutePath()))
                );
    }
    
    @Test
    public void requiredPatches_none() throws Exception {
        final TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        
        assertEquals("no '"+GPConstants.REQUIRED_PATCH_LSIDS+"' in manifest",
                Collections.emptyList(),
                PluginManagerLegacy.getRequiredPatches(taskInfo));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void requiredPatches_empty() throws Exception {
        TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getAttributes().put(GPConstants.REQUIRED_PATCH_LSIDS, "");
        
        assertEquals("'"+GPConstants.REQUIRED_PATCH_LSIDS+"=' in manifest",
                Collections.emptyList(),
                PluginManagerLegacy.getRequiredPatches(taskInfo));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void requiredPatches_onePatchLsidNoPatchUrl() throws Exception {
        final TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getAttributes().put(GPConstants.REQUIRED_PATCH_LSIDS, BWA);
        
        assertComparePatchInfo("'"+GPConstants.REQUIRED_PATCH_LSIDS+"=' in manifest",
                Arrays.asList(new PatchInfo(BWA, null)),
                PluginManagerLegacy.getRequiredPatches(taskInfo));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void requiredPatches_twoPatchLsidNoPatchUrl() throws Exception {
        final TaskInfo taskInfo=new TaskInfo();
        taskInfo.giveTaskInfoAttributes();
        taskInfo.getAttributes().put(GPConstants.REQUIRED_PATCH_LSIDS, ANT+","+BWA);
        
        assertComparePatchInfo("'"+GPConstants.REQUIRED_PATCH_LSIDS+"=' in manifest",
                Arrays.asList(new PatchInfo(ANT, null), new PatchInfo(BWA, null)),
                PluginManagerLegacy.getRequiredPatches(taskInfo));
    }
    
    @Test
    public void requiredPatches_TopHat() throws Exception { 
        final File tophatManifest=FileUtil.getSourceFile(this.getClass(), "TopHat_manifest");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromManifest(tophatManifest);
        final List<PatchInfo> actual=PluginManagerLegacy.getRequiredPatches(taskInfo);
        assertNotNull(actual);
        assertComparePatchInfo("", initTopHatPatchInfos(), actual);
    }
    
    @Test
    public void updateUrlIfNecessary_oldUrl() {
        final String fromUrl="http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip";
        assertEquals("from old Broad repository", 
                // expected
                "http://software.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip", 
                PluginManagerLegacy.updateUrlIfNecessary(fromUrl));
    }
    
    @Test
    public void updateUrlIfNecessary_newUrl() {
        final String fromUrl="http://software.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip";
        assertEquals("from new Broad repository", 
                // expected
                "http://software.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip", 
                PluginManagerLegacy.updateUrlIfNecessary(fromUrl));
    }

    @Test
    public void updateUrlIfNecessary_noChange() {
        final String fromUrl="http://www.example.com/repository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip";
        assertEquals("from different repository", 
                // expected
                fromUrl, 
                PluginManagerLegacy.updateUrlIfNecessary(fromUrl));
    }
    @Test
    public void updateUrlIfNecessary_empty() {
        assertEquals("empty arg", "", PluginManagerLegacy.updateUrlIfNecessary(""));
    }

    @Test
    public void updateUrlIfNecessary_null() {
        assertEquals("null arg", null, PluginManagerLegacy.updateUrlIfNecessary(null));
    }
    
    @Test
    public void requiredPatches_updateUrl() throws JobDispatchException {
        final String lsidFromManifest=ANT;
        final String urlFromManifest="http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip";
        final String urlExpected="http://software.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip";
        
        final TaskInfo taskInfo = new TaskInfo();
        taskInfo.giveTaskInfoAttributes().put(GPConstants.REQUIRED_PATCH_LSIDS, lsidFromManifest);
        taskInfo.giveTaskInfoAttributes().put(GPConstants.REQUIRED_PATCH_URLS, urlFromManifest);
        
        final List<PatchInfo> patchInfos = PluginManagerLegacy.getRequiredPatches(taskInfo);
        assertEquals("num patches", 1, patchInfos.size());
        assertEquals("patchInfos[0].lsid", lsidFromManifest, patchInfos.get(0).getLsid());
        assertEquals("patchInfos[1].url", 
                urlExpected,
                patchInfos.get(0).getUrl());
    }
    
    @Test(expected=JobDispatchException.class)
    public void requiredPatches_mismatchedLsidAndUrl() throws Exception {
        final String requiredPatchLSIDs=ANT+","+Check_Python_2_6;
        final String requiredPatchURLs="http://software.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Ant_1_8/broadinstitute.org:plugin/Ant_1.8/1/Ant_1_8.zip";
        PluginManagerLegacy.getRequiredPatches(requiredPatchLSIDs, requiredPatchURLs);
    }
    
    @Test
    public void patchesToInstall_TopHat_none_installed() throws Exception {
        final GpConfig gpConfig=mock(GpConfig.class);
        final PluginRegistry pluginRegistry=mock(PluginRegistry.class);
        PluginManagerLegacy pluginMgr=new PluginManagerLegacy(mgr, gpConfig, Demo.serverContext, pluginRegistry);
        
        File tophatManifest=FileUtil.getSourceFile(this.getClass(), "TopHat_manifest");
        TaskInfo taskInfo=TaskUtil.getTaskInfoFromManifest(tophatManifest);
        List<PatchInfo> patchesToInstall=pluginMgr.getPatchesToInstall(taskInfo);
        assertComparePatchInfo("none installed", initTopHatPatchInfos(), patchesToInstall);
    }

    @Test
    public void patchesToInstall_TopHat_some_installed() throws Exception {
        final File tophatManifest=FileUtil.getSourceFile(this.getClass(), "TopHat_manifest");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromManifest(tophatManifest);
        final List<String> installedPatchLsids=Arrays.asList(ANT, Check_Python_2_6, Bowtie_2_1_0);
        final List<PatchInfo> installedPatches=new ArrayList<PatchInfo>();
        for(final String lsid : installedPatchLsids) {
            installedPatches.add(new PatchInfo(lsid));
        }
        final List<PatchInfo> expected=Arrays.asList(
            new PatchInfo(SAMTools_0_1_19,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/SAMTools_0.1.19/broadinstitute.org:plugin/SAMTools_0.1.19/2/SAMTools_0_1_19.zip"),
            new PatchInfo(TopHat_2_0_11,
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/TopHat_2.0.11/broadinstitute.org:plugin/TopHat_2.0.11/4/TopHat_2_0_11.zip")
                );
        
        final PluginRegistry pluginRegistry=new PluginRegistry() {

            @Override
            public List<PatchInfo> getInstalledPatches(GpConfig gpConfig, GpContext gpContext) throws Exception {
                return installedPatches;
            }

            @Override
            public boolean isInstalled(GpConfig gpConfig, GpContext gpContext, PatchInfo patchInfo) throws Exception {
                boolean in=installedPatchLsids.contains(patchInfo.getLsid());
                return in;
            }

            @Override
            public void recordPatch(GpConfig gpConfig, GpContext gpContext, PatchInfo patchInfo) throws Exception {
                throw new IllegalArgumentException("Not implemented!");
            }
        };
        
        final GpConfig gpConfig = mock(GpConfig.class);
        final PluginManagerLegacy pluginMgr=new PluginManagerLegacy(mgr, gpConfig, Demo.serverContext, pluginRegistry);
        final List<PatchInfo> patchesToInstall=pluginMgr.getPatchesToInstall(taskInfo);
        assertComparePatchInfo("some installed", expected, patchesToInstall);
    }

    @Test
    public void lsidMatch() throws MalformedURLException, IOException, JobDispatchException {
        // values from module manifest file
        final String requiredPatchLSID="urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:2"; 
        final String requiredPatchURL="http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Bowtie_2.1.0/broadinstitute.org:plugin/Bowtie_2.1.0/2/Bowtie_2_1_0.zip";
        final PatchInfo patchInfoFromModuleManifest=new PatchInfo(requiredPatchLSID, requiredPatchURL);
        assertNotNull(patchInfoFromModuleManifest);

        File patchManifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.Bowtie_2.1.0.2/manifest");
        File patchDirectory=patchManifest.getParentFile();
        Properties patchProperties=PluginManagerLegacy.loadManifest(patchDirectory);
        assertNotNull("has LSID", patchProperties.getProperty("LSID"));
        PluginManagerLegacy.validatePatchLsid(requiredPatchLSID, patchProperties);
    }

    /**
     * Test patch LSID mismatch
     */
    @Test(expected=JobDispatchException.class)
    public void lsidMismatchSAMToolsTypo() throws MalformedURLException, IOException, JobDispatchException {
        // values from module manifest file
        final String requiredPatchLSID="urn:lsid:broadinstitute.org:plugin:SAMTools_0_1_19:2"; 
        final String requiredPatchURL="http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/SAMTools_0.1.19/broadinstitute.org:plugin/SAMTools_0.1.19/2/SAMTools_0_1_19.zip";
        final PatchInfo patchInfoFromModuleManifest=new PatchInfo(requiredPatchLSID, requiredPatchURL);
        assertNotNull(patchInfoFromModuleManifest);

        File patchManifest=FileUtil.getDataFile("patches/broadinstitute.org.plugin.SAMTools_0_1_19.2/manifest");
        File patchDirectory=patchManifest.getParentFile();
        Properties patchProperties=PluginManagerLegacy.loadManifest(patchDirectory);
        assertNotNull("has LSID", patchProperties.getProperty("LSID"));
        PluginManagerLegacy.validatePatchLsid(requiredPatchLSID, patchProperties);
    }

    /**
     * Simulate a typo in the module manifest where the requiredPatchLSID does not match the requiredPatchURL
     * @throws MalformedURLException
     * @throws IOException
     * @throws JobDispatchException
     */
    @Test(expected=JobDispatchException.class)
    public void lsidMatchVersion() throws MalformedURLException, IOException, JobDispatchException {
        // values from module manifest file
        final String requiredPatchLSID="urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:2";   // <---- lsid is Bowtie v. 2
        final String requiredPatchURL= // <---- url is Bowtie v. 1 (which has a different patch LSID)
                "http://www.broadinstitute.org/webservices/gpModuleRepository/download/prod/patch/?file=/Bowtie_2.1.0/broadinstitute.org:plugin/Bowtie_2.1.0/1/Bowtie_2_1_0.zip";
        final PatchInfo patchInfoFromModuleManifest=new PatchInfo(requiredPatchLSID, requiredPatchURL);
        assertNotNull(patchInfoFromModuleManifest);

        Properties patchProperties=new Properties();
        patchProperties.put("LSID", "urn:lsid:broadinstitute.org:plugin:Bowtie_2.1.0:1");
        PluginManagerLegacy.validatePatchLsid(requiredPatchLSID, patchProperties);
    }

}

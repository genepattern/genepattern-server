package org.genepattern.server.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.genepattern.server.config.ConfigurationException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class TestPluginManagerLegacy {
    private GpConfig gpConfig;
    private GpContext gpContext;
    private String patchName="broadinstitute.org.plugin.Ant_1.8.1";
    private File gpHomeDir;
    private File pluginDir;
    private File resourcesDir;
    
    private final String ant_val="java -cp ./tools.jar -jar ./ant-launcher.jar -Dant.home=./ -lib ./";
    private final String java_val="java";
    private final String tomcatCommonLib_val="./";

    private Properties systemProps;
    private final String cmdLine="<ant> -f installAnt.xml -Dresources=<resources> -Dplugin.dir=<patches> -Dant-1.8_HOME=<ant-1.8_HOME>";
    private List<String> expected;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    
    @Before
    public void setUp() {
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
            ant_val, "-f", "installAnt.xml", "-Dresources="+resourcesDir.getAbsolutePath(), "-Dplugin.dir="+pluginDir.getAbsolutePath(), "-Dant-1.8_HOME=");
    }

    @Test
    public void getPatchDirectory() throws ConfigurationException {
        assertEquals(new File(pluginDir, patchName), 
                PluginManagerLegacy.getPatchDirectory(gpConfig, gpContext, patchName));
    }
    
    @Test(expected=ConfigurationException.class)
    public void getPatchDirectory_ConfigurationException() throws ConfigurationException {
        gpConfig=Mockito.mock(GpConfig.class);
        gpContext=Mockito.mock(GpContext.class);
        when(gpConfig.getRootPluginDir(gpContext)).thenReturn(null);
        PluginManagerLegacy.getPatchDirectory(gpConfig, gpContext, patchName);
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
}

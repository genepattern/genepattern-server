package org.genepattern.server.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;

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
    
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    
    @Before
    public void setUp() {
        gpConfig=Mockito.mock(GpConfig.class);
        gpContext=Mockito.mock(GpContext.class);
    }
    
    @Test
    public void getPatchDirectory() throws ConfigurationException {
        File pluginDir=tmp.newFolder("patches");
        when(gpConfig.getRootPluginDir(gpContext)).thenReturn(pluginDir);
        assertEquals(new File(pluginDir, patchName), 
                PluginManagerLegacy.getPatchDirectory(gpConfig, gpContext, patchName));
    }
    
    @Test(expected=ConfigurationException.class)
    public void getPatchDirectory_ConfigurationException() throws ConfigurationException {
        when(gpConfig.getRootPluginDir(gpContext)).thenReturn(null);
        PluginManagerLegacy.getPatchDirectory(gpConfig, gpContext, patchName);
    }
}

package org.genepattern.server.config;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestGpServerProperties {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    @Test
    public void testDefaultInstall() {
        GpServerProperties props=new GpServerProperties.Builder()
            .build();
        
        Assert.assertEquals("GenePatternURL", null, props.getProperty("GenePatternURL"));
    }
    
    @Test
    public void testResourcesDirNotExist() {
        File resourcesDir=tmp.newFolder("resources");
        boolean deleted=resourcesDir.delete();
        Assert.assertEquals("deleted tmp resourcesDir", true, deleted);
        
        GpServerProperties props=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .build();
        Assert.assertEquals("GenePatternURL", null, props.getProperty("GenePatternURL"));
    }
    
    @Test
    public void testNullCustomPropFile() {
        GpServerProperties props=new GpServerProperties.Builder()
            .customProperties(null)
            .build();
        Assert.assertEquals("GenePatternURL", null, props.getProperty("GenePatternURL"));
        
    }
    
}

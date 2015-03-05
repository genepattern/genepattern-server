package org.genepattern.server.config;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestGpServerProperties {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    @Test
    public void testDefaultInstall() {
        GpServerProperties props=new GpServerProperties.Builder()
            .build();
        
        Assert.assertEquals("GenePatternURL", null, props.getProperty("GenePatternURL"));
    }
    
    @Test
    public void testResourcesDirNotExist() {
        File resourcesDir=temp.newFolder("resources");
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

        @Test
    public void addCustomPropertyToEmptyFile() throws Exception {
        File customPropsFile=temp.newFile("custom.properties");
        
        Properties updated=new Properties();
        updated.setProperty("key_01", "custom_value_01_from_plugin");
        GpServerProperties.writeProperties(updated, customPropsFile, null);
        boolean skipExisting=true;
        GpServerProperties.updateCustomProperties(customPropsFile, new Properties(), "", skipExisting);
        
        Properties after=GpServerProperties.loadProps(customPropsFile);
        assertEquals("num properties after update", 1, after.size());
        assertEquals("custom_value_01_from_plugin", after.getProperty("key_01"));
    }
    
    @Test
    public void addCustomProperty() throws Exception {
        final File customPropsFile=temp.newFile("custom.properties");
        Properties before=new Properties();
        before.setProperty("key_01", "value_01");
        GpServerProperties.writeProperties(before, customPropsFile, "Original values");
        
        Properties updated=new Properties();
        updated.setProperty("key_01", "custom_value_01_from_plugin");
        updated.setProperty("key_02", "custom_value_02_from_plugin");

        boolean skipExisting=true;
        GpServerProperties.updateCustomProperties(customPropsFile, updated, "adding custom property", skipExisting);
        
        Properties after=GpServerProperties.loadProps(customPropsFile);
        assertEquals("num properties after update", 2, after.size());
        assertEquals("key_01 already defined in custom.properties", "value_01", after.getProperty("key_01"));
        assertEquals("key_02 not defined in custom.properties", "custom_value_02_from_plugin", after.getProperty("key_02"));
    }
    
    @Test
    public void changeCustomProperty() throws Exception {
        boolean skipExisting=false;

        final File customPropsFile=temp.newFile("custom.properties");
        Properties before=new Properties();
        before.setProperty("key_01", "value_01");
        GpServerProperties.writeProperties(before, customPropsFile, "Original values");
        
        Properties updated=new Properties();
        updated.setProperty("key_01", "custom_value_01_from_plugin");
        updated.setProperty("key_02", "custom_value_02_from_plugin");

        GpServerProperties.updateCustomProperties(customPropsFile, updated, "adding custom property", skipExisting);
        
        Properties after=GpServerProperties.loadProps(customPropsFile);
        assertEquals("num properties after update", 2, after.size());
        assertEquals("key_01 already defined in custom.properties, should be updated", "custom_value_01_from_plugin", after.getProperty("key_01"));
        assertEquals("key_02 already defined in custom.properties, should be updated", "custom_value_02_from_plugin", after.getProperty("key_02"));
    }
    
    @Test
    public void isPropDefinedInFile() throws Exception {
        Properties gpProps=new Properties();
        gpProps.setProperty("GenePatternVersion", "3.9.2");
        gpProps.setProperty("java.io.tmpdir", temp.getRoot().toString());
        
        File gpPropFile=temp.newFile("genepattern.properties");
        // given a GpServerProperties instance which has been initialized by loading a properties file ...
        GpServerProperties.writeProperties(gpProps, gpPropFile, "for junit test");
        
        GpServerProperties serverProps=new GpServerProperties.Builder()
            .gpProperties(gpPropFile)
        .build();

        assertEquals("GenePatternVersion", "3.9.2", serverProps.getProperty("GenePatternVersion"));
        assertEquals("java.io.tmpdir", temp.getRoot().toString(), serverProps.getProperty("java.io.tmpdir"));
        
        assertEquals("isSet(GenePatternVersion)", true, serverProps.isSetInGpProperties("GenePatternVersion"));
        assertEquals("isSet(java.io.tmpdir)", true, serverProps.isSetInGpProperties("java.io.tmpdir"));
        assertEquals("isSet(custom_key)", false, serverProps.isSetInGpProperties("custom_key"));
    }

}

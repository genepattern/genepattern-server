/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;


import org.genepattern.junitutil.FileUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestGpServerPropertiesRecord {
    private static void writePropsToFile(final Properties props, final File file) throws IOException {
        FileWriter fw=null;
        try {
            fw=new FileWriter(file);
            props.store(fw, null);
        }
        finally {
            if (fw!=null) {
                fw.close();
            }
        }
    }
    
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test()
    public void testFromFile() {
        final File testProps=FileUtil.getSourceFile(this.getClass(), "test.properties");
        GpServerProperties.Record record=new GpServerProperties.Record(testProps);
        assertEquals("properties.size", 1, record.getProperties().size());
        assertEquals("GenePatternURL", "http://testserver.test.com/gp/", record.getProperties().get("GenePatternURL"));
    }

    @Test()
    public void testFromProperties() {
        final Properties props=new Properties();
        props.put("GenePatternURL", "http://127.0.0.1:8080/gp/");
        GpServerProperties.Record record=new GpServerProperties.Record(props);
        // verify that the record is based on a snapshot copy of the properties from the constructor
        props.clear();
        assertEquals("properties.size", 1, record.getProperties().size());
        assertEquals("GenePatternURL", "http://127.0.0.1:8080/gp/", record.getProperties().get("GenePatternURL"));
    }

    @Test(expected = IllegalArgumentException.class )
    public void testNullFile() {
        final File nullFile=null;
        //GpServerProperties.Record record=
                new GpServerProperties.Record(nullFile);
    }
    
    @Test
    public void testReloadAfterFileDelete() throws IOException, InterruptedException {
        final Properties props=new Properties();
        props.put("GenePatternURL", "http://127.0.0.1:8080/gp/");
        File testProps=tmp.newFile("test.properties");
        writePropsToFile(props, testProps);
        
        GpServerProperties.Record record=new GpServerProperties.Record(testProps);
        assertEquals("properties.size", 1, record.getProperties().size());
        assertEquals("GenePatternURL", "http://127.0.0.1:8080/gp/", record.getProperties().get("GenePatternURL"));
        boolean success=testProps.delete();
        assertEquals("deleted tmp testPropsFile="+testProps, true, success);
        final long initDateLoaded=record.getDateLoaded();
        Thread.sleep(100);
        record = record.reloadProps();
        assertEquals("properties.size", 0, record.getProperties().size());
        assertTrue("check dateLoaded", record.getDateLoaded()>initDateLoaded);
    }
    
    @SuppressWarnings("deprecation")
    @Test 
    public void immutableProperties() {
        final File testProps=FileUtil.getSourceFile(this.getClass(), "test.properties");
        GpServerProperties.Record record=new GpServerProperties.Record(testProps);
        try {
            record.getProperties().put("GenePatternURL", "bogusValue");
        }
        catch (Throwable t) {
            // ignore (expected = UnsupportedOperationException.class)
        }
        assertEquals("properties.size", 1, record.getProperties().size());
        assertEquals("GenePatternURL", "http://testserver.test.com/gp/", record.getProperties().get("GenePatternURL"));
    }
    
}

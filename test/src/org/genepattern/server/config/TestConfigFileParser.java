/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.config;

import static org.junit.Assert.*;

import java.io.File;

import org.genepattern.drm.Memory;
import org.genepattern.junitutil.FileUtil;
import org.junit.Before;
import org.junit.Test;

public class TestConfigFileParser {
    private static final String configFilename="test.yaml";
    
    private GpContext gpContext;
    private GpConfig gpConfig;
    

    @Before
    public void setUp() throws Exception {
        gpContext=GpContext.getContextForUser("test_user");
        File configFile=FileUtil.getSourceFile(this.getClass(), configFilename);
        gpConfig = new GpConfig.Builder().configFile(configFile).build();
    }
    
    @Test
    public void listOfValues() throws ConfigurationException {
        Value val=gpConfig.getValue(gpContext, "listOfString");
        assertTrue("fromCollection", val.isFromCollection());
        assertEquals("listOfString.size", 3, val.getValues().size());
        assertEquals("listOfString[0]", "A", val.getValues().get(0));
        assertEquals("listOfString[1]", "B", val.getValues().get(1));
        assertEquals("listOfString[2]", "C", val.getValues().get(2));
    }
    
    @Test
    public void valueNotSet() {
        assertEquals("Expecting null value for '    valueNotSet:  ' ", 
                null, 
                gpConfig.getValue(gpContext, "valueNotSet").getValue());
    }
    
    @Test
    public void valueAsNull() {
        assertEquals("Expecting null value for '    valueAsNull: null'", 
                null, 
                gpConfig.getValue(gpContext, "valueAsNull").getValue());
    }
    
    @Test
    public void valueAsNegativeOne() {
        assertEquals("Expecting -1 value for '    valueAsNegativeOne: -1'", 
                new Value("-1"), 
                gpConfig.getValue(gpContext, "valueAsNegativeOne"));
    }

    @Test
    public void valueAsString() {
        assertEquals("Expecting -1 value for '    valueAsString: 2 Gb'", 
                new Value("2 Gb"), 
                gpConfig.getValue(gpContext, "valueAsString"));
    }
    
    @Test
    public void memoryFromNotSet() {
        assertEquals("initialize Memory from value: <not set>", 
                null, 
                gpConfig.getGPMemoryProperty(gpContext, "valueNotSet"));
    }
    
    @Test
    public void memoryFromNull() {
        assertEquals("initialize Memory from value: null", 
                null, 
                gpConfig.getGPMemoryProperty(gpContext, "valueAsNull"));
        
    }
    
    @Test
    public void memoryFromNegativeOne() {
        assertEquals("initialize Memory from value: -1", 
                null, 
                gpConfig.getGPMemoryProperty(gpContext, "valueAsNegativeOne"));
    }
    
    @Test
    public void memoryFromZero() {
        assertEquals("initialize Memory from value: 0", 
                new Memory(0L), 
                gpConfig.getGPMemoryProperty(gpContext, "valueAsZero"));
    }
    
    @Test
    public void memoryFromMemSpec() {
        assertEquals("initialize Memory from value: 2 Gb", 
                Memory.fromString("2 Gb"), 
                gpConfig.getGPMemoryProperty(gpContext, "valueAsString"));

    }

}

package org.genepattern.server.config;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.junit.Assert;
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
        Assert.assertTrue("fromCollection", val.isFromCollection());
        Assert.assertEquals("listOfString.size", 3, val.getValues().size());
        Assert.assertEquals("listOfString[0]", "A", val.getValues().get(0));
        Assert.assertEquals("listOfString[1]", "B", val.getValues().get(1));
        Assert.assertEquals("listOfString[2]", "C", val.getValues().get(2));
    }
}

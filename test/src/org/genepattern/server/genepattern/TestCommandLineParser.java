package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import clover.retrotranslator.edu.emory.mathcs.backport.java.util.Arrays;

public class TestCommandLineParser {
    private GpConfig gpConfig;
    private GpContext gpContext;
    private Map<String,ParameterInfo> parameterInfoMap;
    
    private String java_val="java";
    private String tomcatCommonLib_val=".";
    @Before
    public void setUp() {
        gpConfig=new GpConfig.Builder()
            .addProperty("java", java_val)
            .addProperty("tomcatCommonLib", tomcatCommonLib_val)
            .addProperty("ant", "<java> -cp <tomcatCommonLib>/tools.jar -jar <tomcatCommonLib>/ant-launcher.jar -Dant.home=<tomcatCommonLib> -lib <tomcatCommonLib>")
        .build();
        gpContext=new GpContext();
        parameterInfoMap=new HashMap<String,ParameterInfo>();
    }
    
    @Test
    public void resolveValue_nested() {
        List<String> expected=Arrays.asList(new String[]{
            java_val, "-cp", tomcatCommonLib_val+"/tools.jar", "-jar", tomcatCommonLib_val+"/ant-launcher.jar", "-Dant.home="+tomcatCommonLib_val, "-lib", tomcatCommonLib_val });

        assertEquals(
                expected, 
                CommandLineParser.resolveValue(gpConfig, gpContext, "<ant>", parameterInfoMap, 0));
    }
    
    @Test
    public void substituteValue_notSet() {
        String arg="<not_set>";
        List<String> expected=new ArrayList<String>();
        List<String> actualValue=CommandLineParser.substituteValue(gpConfig, gpContext, arg, parameterInfoMap);
        Assert.assertEquals(expected, actualValue);
    }

    @Test
    public void subsituteValue_nullParameterInfoMap() {
        parameterInfoMap=null;
        Assert.assertEquals(
                Arrays.asList(new String[]{"literal"}), 
                CommandLineParser.substituteValue(gpConfig, gpContext, "literal", parameterInfoMap));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void nullGpConfig() {
        CommandLineParser.substituteValue( (GpConfig) null, gpContext, "literal", parameterInfoMap );
    }
    
}

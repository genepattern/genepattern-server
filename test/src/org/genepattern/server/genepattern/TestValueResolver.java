package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.Demo;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.ParameterInfoBuilder;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.ListMode;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by nazaire on 8/13/15.
 */
public class TestValueResolver
{
    private static final String choiceSpec1 ="value1=value1;value2;value3=value3;value4;actual5=display5";
    private static final String choiceSpec2="choiceVal1=choiceVal1;choiceVal2;choiceVal3=choiceVal3;" +
            "choiceVal4;actualChoice5=displayChoice5";

    private static final String singleValueParam = "single.param";
    private final String multiValueParam = "multivalue.param";
    private Map<String,ParameterInfoRecord> paramInfoMap;


    @Before
    public void setUp() {
        ParameterInfo formalSingleParam;
        ParameterInfo formalMultiParam; 

        formalSingleParam = new ParameterInfo();
        formalSingleParam.setName(singleValueParam);
        HashMap<String, String> attributesOne = new HashMap<String, String>();
        attributesOne.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--input ");
        attributesOne.put("name", singleValueParam);
        attributesOne.put("optional", "on");
        attributesOne.put("type", "java.lang.String");
        formalSingleParam.setAttributes(attributesOne);
        
        formalMultiParam = new ParameterInfo();
        HashMap<String, String> attributesTwo = new HashMap<String, String>();
        formalMultiParam.setName(multiValueParam);
        attributesTwo.put("numValues", "0+");
        attributesTwo.put("listMode", "cmd");
        attributesTwo.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--multi=");
        attributesTwo.put("name", multiValueParam);
        attributesTwo.put("optional", "on");
        attributesTwo.put("type", "java.lang.String");
        formalMultiParam.setAttributes(attributesTwo);
        
        final ParameterInfo[] formalParams = new ParameterInfo[]{formalSingleParam , formalMultiParam};
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);
    }
    
    @Test
    public void getTokens_quoted_arg() {
        final String QUOTE_DELIM="\"";
        String cmdLine="<run-with-env> -e R_LIBS -e "+QUOTE_DELIM+"R_LIBS_USER=' '"+QUOTE_DELIM;
        assertEquals("getTokens('"+cmdLine+"')", 
                Arrays.asList("<run-with-env>", "-e", "R_LIBS", "-e", "R_LIBS_USER=' '"), 
                ValueResolver.getTokens(cmdLine));
    }

    @Test
    public void getTokens_emptyString() {
        String cmdLine="";
        assertEquals("getTokens('"+cmdLine+"')", 
                Arrays.asList(), 
                ValueResolver.getTokens(cmdLine));
    }
    
    @Test
    public void testListModeCmdWithChoice()
    {
        String paramName = "multi.choice";
        ParameterInfo formalParam=new ParameterInfo();
        formalParam.setName(paramName);
        formalParam.setValue(choiceSpec1);

        HashMap<String,String> attributes=new HashMap<String,String>();
        attributes.put("default_value", "value2");
        attributes.put("name", paramName);
        attributes.put("optional", "on");
        attributes.put("type", "java.lang.String");
        attributes.put("listMode", "cmd");
        formalParam.setAttributes(attributes);

        JobInput jobInput=new JobInput();
        jobInput.addValue(paramName, "value2");
        jobInput.addValue(paramName, "value3");
        jobInput.addValue(paramName, "actual5");

        List<String> cmdList = ValueResolver.getCmdListValues(jobInput.getParam(paramName), formalParam);
        List<String> expectedList = new ArrayList<String>();
        expectedList.add("value2,value3,actual5");

        assertEquals(expectedList, cmdList);
    }

    @Test
    public void testListModeCmdText()
    {
        String paramName = "multi.text";

        HashMap<String,String> attributes=new HashMap<String,String>();
        attributes.put("listMode", "cmd");

        ParameterInfo formalParam=new ParameterInfo();
        formalParam.setName(paramName);
        formalParam.setAttributes(attributes);

        JobInput jobInput=new JobInput();
        jobInput.addValue(paramName, "textval1");
        jobInput.addValue(paramName, "textval2");
        jobInput.addValue(paramName, "textval3");

        List<String> cmdList = ValueResolver.getCmdListValues(jobInput.getParam(paramName), formalParam);

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("textval1,textval2,textval3");

        assertEquals(expectedList, cmdList);
    }

    @Test
    /*
     * Test using listMode=cmd with listModeSep=:
     */
    public void testListModeCmdCustomSepText()
    {
        String paramName = "multi.text.custom.sep";

        HashMap<String,String> attributes=new HashMap<String,String>();
        attributes.put("listMode", "cmd");
        attributes.put("listModeSep", ":");

        ParameterInfo formalParam=new ParameterInfo();
        formalParam.setName(paramName);
        formalParam.setAttributes(attributes);

        JobInput jobInput=new JobInput();
        jobInput.addValue(paramName, "textval1");
        jobInput.addValue(paramName, "textval2");
        jobInput.addValue(paramName, "textval3");

        List<String> cmdList = ValueResolver.getCmdListValues(jobInput.getParam(paramName), formalParam);

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("textval1:textval2:textval3");

        assertEquals(expectedList, cmdList);
    }

    @Test
    /*
     * Tests listMode=cmd_opt
     * cmd_opt should do <prefix> val1 <prefix> val2 <prefix> val3
     */
    public void testListModeCmdOptChoice()
    {
        String paramName = "multi.choice.cmd_opt";

        HashMap<String,String> attributes=new HashMap<String,String>();
        attributes.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--input=");
        attributes.put("name", paramName);
        attributes.put("optional", "on");
        attributes.put("type", "java.lang.String");
        attributes.put("listMode", "cmd_opt");

        ParameterInfo formalParam=new ParameterInfo();
        formalParam.setName(paramName);
        formalParam.setValue(choiceSpec1);
        formalParam.setAttributes(attributes);

        JobInput jobInput=new JobInput();
        jobInput.addValue(paramName, "value1");
        jobInput.addValue(paramName, "value4");
        jobInput.addValue(paramName, "actual5");

        final List<String> cmdList = ValueResolver.getCmdListValues(jobInput.getParam(paramName), formalParam);

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("--input=value1");
        expectedList.add("--input=value4");
        expectedList.add("--input=actual5");

        assertEquals(expectedList, cmdList);
    }

    
    /**
     * Test resolving a command line which contains a parameter with listmode=cmd and multiple values set
     */
    @Test
    public void testResolveCmdLineListModeCmdWithMultiValues()
    {
        String singleValueParam = "single.param";
        String multiValueParam = "multivalue.param";

        final String cmdLine="echo <"+singleValueParam+ "> <"+ multiValueParam + ">";

        ParameterInfo formalSingleParam=new ParameterInfo();
        formalSingleParam.setName(singleValueParam);

        formalSingleParam.setValue(choiceSpec2);
        HashMap<String,String> attributesOne=new HashMap<String,String>();
        attributesOne.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--input ");
        attributesOne.put("optional", "on");
        attributesOne.put("type", "java.lang.String");
        formalSingleParam.setAttributes(attributesOne);

        ParameterInfo formalMultiParam=new ParameterInfo();
        formalMultiParam.setName(multiValueParam);
        HashMap<String,String> attributesTwo=new HashMap<String,String>();
        attributesTwo.put("numValues", "0+");
        attributesTwo.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--multi ");
        attributesTwo.put("listMode", "cmd");
        attributesTwo.put("optional", "on");
        attributesTwo.put("type", "java.lang.String");
        formalMultiParam.setAttributes(attributesTwo);

        final ParameterInfo[] formalParams = new ParameterInfo[]{formalSingleParam , formalMultiParam};
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);

        String singleValueParamValue = "oneValue";
        JobInput jobInput=new JobInput();
        jobInput.addValue(singleValueParam, singleValueParamValue);

        jobInput.addValue(multiValueParam, "value4");
        jobInput.addValue(multiValueParam, "actual5");

        final Map<String,String> setupProps=new HashMap<String,String>();
        setupProps.put(singleValueParam, singleValueParamValue);

        final GpContext jobContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();

        final List<String> actual=ValueResolver.resolveValue((GpConfig) null, jobContext, cmdLine, setupProps, paramInfoMap);
        List<String> expected = new ArrayList<String>();
        expected.add("echo");
        expected.add("--input");
        expected.add("oneValue");
        expected.add("--multi");
        expected.add("value4,actual5");

        assertEquals(expected, actual);
    }

    
    /**
     * Test resolving a command line which contains a parameter with listmode=cmd_opt and multiple values set
     */
    @Test
    public void testResolveCmdLineListModeCmdOptWithMultiValues()
    {
        String singleValueParam = "single.param";
        String multiValueParam = "multivalue.param";

        final String cmdLine="echo <"+singleValueParam+ "> <"+ multiValueParam + ">";

        ParameterInfo formalSingleParam=new ParameterInfo();
        formalSingleParam.setName(singleValueParam);

        formalSingleParam.setValue(choiceSpec2);
        HashMap<String,String> attributesOne=new HashMap<String,String>();
        attributesOne.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--input ");
        attributesOne.put("optional", "on");
        attributesOne.put("type", "java.lang.String");
        formalSingleParam.setAttributes(attributesOne);

        ParameterInfo formalMultiParam=new ParameterInfo();
        formalMultiParam.setName(multiValueParam);
        formalMultiParam.setValue(choiceSpec1);
        HashMap<String,String> attributesTwo=new HashMap<String,String>();
        attributesTwo.put("numValues", "0+");
        attributesTwo.put("listMode", "cmd_opt");
        attributesTwo.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--multi=");
        attributesTwo.put("optional", "on");
        attributesTwo.put("type", "java.lang.String");
        formalMultiParam.setAttributes(attributesTwo);

        final ParameterInfo[] formalParameters = new ParameterInfo[]{formalSingleParam , formalMultiParam};
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParameters);

        String singleValueParamValue = "oneValue";
        JobInput jobInput=new JobInput();
        jobInput.addValue(singleValueParam, singleValueParamValue);

        jobInput.addValue(multiValueParam, "value4");
        jobInput.addValue(multiValueParam, "actual5");

        final Map<String,String> setupProps=new HashMap<String,String>();
        setupProps.put(singleValueParam, singleValueParamValue);

        final GpContext jobContext=new GpContext.Builder()
                .jobInput(jobInput)
                .build();

        final List<String> actual=ValueResolver.resolveValue((GpConfig) null, jobContext, cmdLine, setupProps, paramInfoMap);
        List<String> expected = new ArrayList<String>();
        expected.add("echo");
        expected.add("--input");
        expected.add("oneValue");
        expected.add("--multi=value4");
        expected.add("--multi=actual5");

        assertEquals(expected, actual);
    }
    
    /**
     * Test resolving a command line which contains a parameter with listmode=cmd and one value set
     */
    @Test
    public void testResolveCmdLineListModeCmdOptWithSingleValue()
    {
        String singleValueParam = "single.param";
        String multiValueParam = "multivalue.param";

        final String cmdLine="echo <"+singleValueParam+ "> <"+ multiValueParam + ">";

        ParameterInfo formalSingleParam = new ParameterInfo();
        formalSingleParam.setName(singleValueParam);
        HashMap<String,String> attributesOne = new HashMap<String,String>();
        attributesOne.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--input ");
        attributesOne.put("name", singleValueParam);
        attributesOne.put("optional", "on");
        attributesOne.put("type", "java.lang.String");
        formalSingleParam.setAttributes(attributesOne);

        ParameterInfo formalMultiParam = new ParameterInfo();
        HashMap<String,String> attributesTwo = new HashMap<String,String>();
        formalMultiParam.setName(multiValueParam);
        attributesTwo.put("numValues", "0+");
        attributesTwo.put("listMode", "cmd_opt");
        attributesTwo.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--multi=");
        attributesTwo.put("name", multiValueParam);
        attributesTwo.put("optional", "on");
        attributesTwo.put("type", "java.lang.String");
        formalMultiParam.setAttributes(attributesTwo);

        final ParameterInfo[] formalParameters = new ParameterInfo[]{formalSingleParam , formalMultiParam};
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParameters);

        String singleValueParamValue = "singleValue";
        JobInput jobInput=new JobInput();
        jobInput.addValue(singleValueParam, singleValueParamValue);

        jobInput.addValue(multiValueParam, "onlyValue");

        final Map<String,String> setupProps = new HashMap<String,String>();
        setupProps.put(singleValueParam, singleValueParamValue);

        final GpContext jobContext=new GpContext.Builder()
                .jobInput(jobInput)
                .build();

        final List<String> actual=ValueResolver.resolveValue((GpConfig) null, jobContext, cmdLine, setupProps, paramInfoMap);

        List<String> expected = new ArrayList<String>();
        expected.add("echo");
        expected.add("--input");
        expected.add("singleValue");
        expected.add("--multi=onlyValue");

        assertEquals(expected, actual);
    }
    
    @Test
    public void substituteValue_no_job_input_value_entry() {
        ParameterInfo pinfo = new ParameterInfo();
        pinfo.setName("output.filename");
        HashMap<String, String> attrs = new HashMap<String, String>();
        pinfo.setAttributes(attrs);
        attrs.put("listMode", "cmd");
        //attributesTwo.put(GPConstants.PARAM_INFO_PREFIX[GPConstants.PARAM_INFO_NAME_OFFSET], "--multi=");
        attrs.put("name", multiValueParam);
        //attributesTwo.put("optional", "on");
        attrs.put("type", "java.lang.String");
        attrs.put("default_value", "<input.file_basename>.cp.<input.file_extension>");
        
        final ParameterInfo[] formalParams = new ParameterInfo[]{pinfo};
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);

        final GpContext jobContext=new GpContext.Builder().build();
        Map<String,String> dict=new HashMap<String,String>();
        dict.put("output.filename", "<input.file_basename>.cp.<input.file_extension>");
        final List<String> expected=Arrays.asList(new String[]{ "<input.file_basename>.cp.<input.file_extension>" });
        final List<String> actual=ValueResolver.substituteValue((GpConfig) null, jobContext, "<output.filename>", dict, paramInfoMap);
        
        assertEquals("substitue(<output.filename>)", expected, actual);
    }
    
    @Test
    public void substituteValue_cmdOpt_prefix() throws Exception {
        final String pname="input.param";
        final ParameterInfo pinfo = new ParameterInfoBuilder()
            .name(pname)
            .numValues("0+")
            .listMode(ListMode.CMD_OPT)
            .optional(true)
            .defaultValue("")
            .prefixWhenSpecified("--input-param=")
        .build();
        
        JobInput jobInput=new JobInput();
        jobInput.addValue(pname, "A VALUE");
        jobInput.addValue(pname, "B VALUE");
       
        final GpContext jobContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        Map<String,String> dict=new HashMap<String,String>();
        final ParameterInfo[] formalParams = new ParameterInfo[]{pinfo};
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);
        
        final List<String> actual=ValueResolver.substituteValue((GpConfig) null, jobContext, "<input.param>", dict, paramInfoMap);
        assertEquals("substitue(<input.param>)", 
                Arrays.asList("--input-param=A VALUE", "--input-param=B VALUE"), 
                actual);
    }
    
    @Test
    public void substituteValue_cmdOpt_prefixWithTrailingSpace() throws Exception {
        final String pname="input.param";
        final ParameterInfo pinfo = new ParameterInfoBuilder()
            .name(pname)
            .numValues("0+")
            .listMode(ListMode.CMD_OPT)
            .optional(true)
            .defaultValue("")
            .prefixWhenSpecified("-i ")
        .build();
        
        JobInput jobInput=new JobInput();
        jobInput.addValue(pname, "A VALUE");
        jobInput.addValue(pname, "B VALUE");

        final GpContext jobContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        Map<String,String> dict=new HashMap<String,String>();
        final ParameterInfo[] formalParams = new ParameterInfo[]{pinfo};
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);
        
        final List<String> actual=ValueResolver.substituteValue((GpConfig) null, jobContext, "<input.param>", dict, paramInfoMap);
        assertEquals("substitue(<input.param>)", 
                Arrays.asList("-i", "A VALUE", "-i", "B VALUE"), 
                actual);
    }

    /**
     * Test resolving a command line which contains a parameter with listmode=cmd and multiple values set
     */
    @Test
    public void substituteValue_cmd_prefix() throws Exception {
        final String pname="input.param";
        final ParameterInfo pinfo = new ParameterInfoBuilder()
            .name(pname)
            .numValues("0+")
            .listMode(ListMode.CMD)
            .optional(true)
            .defaultValue("")
            .prefixWhenSpecified("-m")
        .build();
        
        JobInput jobInput=new JobInput();
        jobInput.addValue(pname, "A VALUE");
        jobInput.addValue(pname, "B VALUE");

        final GpContext jobContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        Map<String,String> dict=new HashMap<String,String>();
        final ParameterInfo[] formalParams = new ParameterInfo[]{pinfo};
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);
        final List<String> actual=ValueResolver.substituteValue((GpConfig) null, jobContext, "<input.param>", dict, paramInfoMap);
        assertEquals("substitue(<input.param>)", 
                Arrays.asList("-mA VALUE,B VALUE"), 
                actual);

    }    

    /**
     * Test resolving a command line which contains a parameter with listmode=cmd and multiple values set
     */
    @Test
    public void substituteValue_cmd_prefixWithTrailingSpace() throws Exception {
        final String pname="input.param";
        final ParameterInfo pinfo = new ParameterInfoBuilder()
            .name(pname)
            .numValues("0+")
            .listMode(ListMode.CMD)
            .optional(true)
            .defaultValue("")
            .prefixWhenSpecified("--multi ")
        .build();
        
        JobInput jobInput=new JobInput();
        jobInput.addValue(pname, "A VALUE");
        jobInput.addValue(pname, "B VALUE");

        final GpContext jobContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        Map<String,String> dict=new HashMap<String,String>();
        final ParameterInfo[] formalParams = new ParameterInfo[]{pinfo};
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);
        final List<String> actual=ValueResolver.substituteValue((GpConfig) null, jobContext, "<input.param>", dict, paramInfoMap);
        assertEquals("substitue(<input.param>)", 
                Arrays.asList("--multi", "A VALUE,B VALUE"), 
                actual);
    }

    /**
     * parameterized test substiteValue <env-arch>
     */
    protected void do_env_arch_test(final String arg, final String env_arch, final String expected) { 
        final GpConfig gpConfig=Demo.gpConfig();
        final GpContext jobContext=mock(GpContext.class);
        when(gpConfig.getGPProperty(jobContext, "env-arch")).thenReturn(env_arch);
        final Map<String,String> dict=Collections.emptyMap();
        final Map<String,ParameterInfoRecord> parameterInfoMap = Collections.emptyMap();
        assertEquals("substitute('"+arg+"'), env-arch="+env_arch,
            Arrays.asList(expected),
            ValueResolver.substituteValue(gpConfig, jobContext, arg, dict, parameterInfoMap));
    }
    
    /**
     * test substituteValue for <env-arch> variations
     */
    @Test
    public void substituteValue_env_arch() {
        // test case: '-a <env-arch>'        
        do_env_arch_test("-a <env-arch>", "rhel6", "-a rhel6");
        do_env_arch_test("-a <env-arch>", null, "-a ");
        do_env_arch_test("-a <env-arch>", "", "-a "); // empty
        
        // test case: '-A<env-arch>'
        do_env_arch_test("-A<env-arch>", "rhel6", "-Arhel6");
        do_env_arch_test("-A<env-arch>", null, "-A");
        do_env_arch_test("-A<env-arch>", "", "-A");

        // test case: '-e GP_EVN_ARCH=<env-arch>'
        do_env_arch_test("-e GP_ENV_ARCH=<env-arch>", "rhel6", "-e GP_ENV_ARCH=rhel6");
        do_env_arch_test("-e GP_ENV_ARCH=<env-arch>", null, "-e GP_ENV_ARCH=");
        do_env_arch_test("-e GP_ENV_ARCH=<env-arch>", "", "-e GP_ENV_ARCH=");
    }

    /**
     * test built-in <wrapper-script> substitution
     */
    @Test
    public void substituteValue_wrapper_arg() {
        final String expected_wrapper_dir=new File(FileUtil.resourcesDir, "wrapper_scripts").getAbsolutePath();
        
        // setup requires an fq path to the resourcesDir and webappDir
        final GpConfig gpConfig=new GpConfig.Builder()
            .resourcesDir(FileUtil.getResourcesDir())
            .webappDir(FileUtil.getWebappDir())
        .build();
        
        final GpContext gpContext=Demo.serverContext;
        final Map<String,String> dict=null;
        final Map<String,ParameterInfoRecord> parameterInfoMap=null;

        assertEquals("sanity-check: gpConfig.getValue(\"wrapper-scripts\")",
            expected_wrapper_dir,
            gpConfig.getValue(gpContext, "wrapper-scripts").toString());

        assertEquals( "sanity-check: substituteSubToken(\"<wrapper-scripts>\")",
            Arrays.asList(expected_wrapper_dir), 
            ValueResolver.substituteSubToken(gpConfig, gpContext, 
                new CmdLineSubToken("<wrapper-scripts>"), 
                dict, parameterInfoMap));

        assertEquals( "sanity-check: substituteSubToken(\"<resources>\")",
            Arrays.asList(FileUtil.resourcesDir.toString()), 
            ValueResolver.substituteSubToken(gpConfig, gpContext, 
                new CmdLineSubToken("<resources>"), 
                dict, parameterInfoMap));
        
        assertEquals( "substituteValue(\"<wrapper-scripts>\")",
            Arrays.asList(expected_wrapper_dir), 
            ValueResolver.substituteValue(
                gpConfig, gpContext, 
                "<wrapper-scripts>", 
                dict, parameterInfoMap));

        assertEquals( "resolveValue(\"<wrapper-scripts>\")",
            Arrays.asList(new File(FileUtil.resourcesDir, "wrapper_scripts").getAbsolutePath()), 
            ValueResolver.resolveValue(gpConfig, gpContext, "<wrapper-scripts>"));
    }


}

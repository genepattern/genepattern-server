package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
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

        ParameterInfoRecord parameterInfoRecord = new ParameterInfoRecord(formalParam);
        List<String> cmdList = ValueResolver.getSubstitutedValues(jobInput.getParam(paramName), parameterInfoRecord);

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

        ParameterInfoRecord parameterInfoRecord = new ParameterInfoRecord(formalParam);
        List<String> cmdList = ValueResolver.getSubstitutedValues(jobInput.getParam(paramName), parameterInfoRecord);

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

        ParameterInfoRecord parameterInfoRecord = new ParameterInfoRecord(formalParam);
        List<String> cmdList = ValueResolver.getSubstitutedValues(jobInput.getParam(paramName), parameterInfoRecord);

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

        final ParameterInfoRecord parameterInfoRecord = new ParameterInfoRecord(formalParam);
        final List<String> cmdList = ValueResolver.getSubstitutedValues(jobInput.getParam(paramName), parameterInfoRecord);

        List<String> expectedList = new ArrayList<String>();
        expectedList.add("--input=value1");
        expectedList.add("--input=value4");
        expectedList.add("--input=actual5");

        assertEquals(expectedList, cmdList);
    }

    @Test
    /*
     * Test resolving a command line which contains a parameter with listmode=cmd and multiple values set
     */
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

        GpConfig gpConfig=new GpConfig.Builder().build();
        final GpContext jobContext=new GpContext.Builder()
                .jobInput(jobInput)
                .build();

        final List<String> actual=ValueResolver.resolveValue(gpConfig, jobContext, cmdLine, setupProps, paramInfoMap);
        List<String> expected = new ArrayList<String>();
        expected.add("echo");
        expected.add("--input");
        expected.add("oneValue");
        expected.add("--multi");
        expected.add("value4,actual5");

        assertEquals(expected, actual);
    }

    @Test
    /*
     * Test resolving a command line which contains a parameter with listmode=cmd_opt and multiple values set
     */
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

        final ParameterInfo[] formalParams = new ParameterInfo[]{formalSingleParam , formalMultiParam};
        final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);

        String singleValueParamValue = "oneValue";
        JobInput jobInput=new JobInput();
        jobInput.addValue(singleValueParam, singleValueParamValue);

        jobInput.addValue(multiValueParam, "value4");
        jobInput.addValue(multiValueParam, "actual5");

        final Map<String,String> setupProps=new HashMap<String,String>();
        setupProps.put(singleValueParam, singleValueParamValue);

        GpConfig gpConfig=new GpConfig.Builder().build();
        final GpContext jobContext=new GpContext.Builder()
                .jobInput(jobInput)
                .build();

        final List<String> actual=ValueResolver.resolveValue(gpConfig, jobContext, cmdLine, setupProps, paramInfoMap);
        List<String> expected = new ArrayList<String>();
        expected.add("echo");
        expected.add("--input");
        expected.add("oneValue");
        expected.add("--multi=value4");
        expected.add("--multi=actual5");

        assertEquals(expected, actual);
    }

    @Test
    /*
     * Test resolving a command line which contains a parameter with listmode=cmd and one value set
     */
    public void testResolveCmdLineListModeCmdOptWithSingleValue()
    {
        final String cmdLine="echo <"+singleValueParam+ "> <"+ multiValueParam + ">";

        //final ParameterInfo[] formalParams = new ParameterInfo[]{formalSingleParam , formalMultiParam};
        //final Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);


        final String singleValueParamValue = "singleValue";
        final JobInput jobInput=new JobInput();
        jobInput.addValue(singleValueParam, singleValueParamValue);

        jobInput.addValue(multiValueParam, "onlyValue");

        final Map<String,String> setupProps = new HashMap<String,String>();
        setupProps.put(singleValueParam, singleValueParamValue);

        GpConfig gpConfig=new GpConfig.Builder().build();
        final GpContext jobContext=new GpContext.Builder()
                .jobInput(jobInput)
                .build();

        final List<String> actual=ValueResolver.resolveValue(gpConfig, jobContext, cmdLine, setupProps, paramInfoMap);

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

        final GpConfig gpConfig=new GpConfig.Builder().build();
        final GpContext jobContext=new GpContext.Builder().build();
        Map<String,String> dict=new HashMap<String,String>();
        dict.put("output.filename", "<input.file_basename>.cp.<input.file_extension>");
        final List<String> expected=Arrays.asList(new String[]{ "<input.file_basename>.cp.<input.file_extension>" });
        final List<String> actual=ValueResolver.substituteValue(gpConfig, jobContext, "<output.filename>", dict, paramInfoMap);
        
        assertEquals("substitue(<output.filename>)", expected, actual);
    }
    
}

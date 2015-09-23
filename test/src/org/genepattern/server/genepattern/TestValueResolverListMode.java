package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.junitutil.ParameterInfoBuilder;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.ParamListHelper.ListMode;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Before;
import org.junit.Test;

/**
 * test cases for the listMode=CMD and CMD_OPT flags.
 * @author pcarr
 *
 */
public class TestValueResolverListMode {

    private Map<String,ParameterInfoRecord>  paramInfoMap;
    private JobInput jobInput;
    private GpConfig gpConfig;
    private GpContext jobContext;
    private Map<String,String> dict;
    
    // text param with numValues=0+
    private static final String textListName="text.list";
    private ParameterInfoBuilder textList;
    private ParameterInfo textListInfo;

    // text param with numValues=0..1
    private static final String textFieldName="text.field";
    private ParameterInfoBuilder textField;
    private ParameterInfo textFieldInfo;
    
    @Before
    public void setUp() throws Exception {
        dict=new HashMap<String,String>();
        dict.put(textFieldName, "A VALUE");

        jobInput=new JobInput();
        jobInput.addValue(textListName, "A VALUE");
        jobInput.addValue(textListName, "B VALUE");
        
        gpConfig=new GpConfig.Builder().build();
        jobContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        
        textList = new ParameterInfoBuilder()
            .listMode(ListMode.CMD)
            .name(textListName)
            .numValues("0+")
            .optional(true)
            .defaultValue("");
        
        textField = new ParameterInfoBuilder()
            .name(textFieldName)
            .numValues("0..1")
            .optional(true)
            .defaultValue("");
    }
    
    protected void initPinfo() {
        textListInfo=textList.build();
        textFieldInfo=textField.build();
        final ParameterInfo[] formalParams = new ParameterInfo[]{textFieldInfo, textListInfo};
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);        
    }

    // tests for single valued text param
    @Test
    public void singleValue() {
        initPinfo();
        final String arg="<"+textFieldName+">";

        assertEquals("substituteValue("+arg+")", 
                Arrays.asList("A VALUE"),
                ValueResolver.substituteValue(gpConfig, jobContext, arg, dict, paramInfoMap));
    }

    @Test
    public void singleValue_with_prefix() {
        textField.prefixWhenSpecified("-i");
        initPinfo();
        final String arg="<"+textFieldName+">";

        assertEquals("substituteValue("+arg+")", 
                Arrays.asList("-iA VALUE"),
                ValueResolver.substituteValue(gpConfig, jobContext, arg, dict, paramInfoMap));
    }

    @Test
    public void singleValue_with_prefix_split() {
        textField.prefixWhenSpecified("-i ");
        initPinfo();
        final String arg="<"+textFieldName+">";

        assertEquals("substituteValue("+arg+")", 
                Arrays.asList("-i", "A VALUE"),
                ValueResolver.substituteValue(gpConfig, jobContext, arg, dict, paramInfoMap));
    }
    
    /**
     * Test a substitution like "-f<text.param>"
     */
    @Test
    public void singleValue_inString() {
        initPinfo();
        final String arg="-f<"+textFieldName+">";
        assertEquals("substituteValue("+arg+")", 
                Arrays.asList("-fA VALUE"),
                ValueResolver.substituteValue(gpConfig, jobContext, arg, dict, paramInfoMap));
    }

    /**
     * Test a substitution like "BEFORE<text.param>" with prefix_when_specified
     * Note: this test is descriptive, not prescriptive. We may want to change 
     * the rules for handling prefixes within complex substitutions.
     */
    @Test
    public void singleValue_inString_with_prefix() {
        textField.prefixWhenSpecified("-f");
        initPinfo();
        
        final String arg="BEFORE<"+textFieldName+">";
        assertEquals("substituteValue("+arg+")", 
                Arrays.asList("BEFORE-fA VALUE"),
                ValueResolver.substituteValue(gpConfig, jobContext, arg, dict, paramInfoMap));
    }

    /**
     * Test a substitution like "BEFORE<text.param>" with prefix_when_specified with trailing space
     * Note: this test is descriptive, not prescriptive. We may want to change 
     * the rules for handling prefixes within complex substitutions.
     * 
     * Note: this test-case is a BUG, pre-existing in GP.
     * TODO: fix this bug
     */
    @Test
    public void singleValue_inString_with_prefix_split() {
        textField.prefixWhenSpecified("-f ");
        initPinfo();
        
        final String arg="BEFORE<"+textFieldName+">";
        
        // should be "BEFORE", "-f", "A VALUE"
        assertEquals("substituteValue("+arg+")", 
                Arrays.asList("-f", "BEFOREA VALUE"),
                ValueResolver.substituteValue(gpConfig, jobContext, arg, dict, paramInfoMap));
    }

    /**
     * Test a substitution like "BEFORE <text.param> MIDDLE <text.param> AFTER"
     */
    @Test
    public void singleValue_complex_sub_no_prefix() {
        initPinfo();
        final String arg="BEFORE <"+textFieldName+"> MIDDLE <"+textFieldName+"> AFTER";
        assertEquals("substituteValue("+arg+")", 
                Arrays.asList("BEFORE A VALUE MIDDLE A VALUE AFTER"),
                ValueResolver.substituteValue(gpConfig, jobContext, arg, dict, paramInfoMap));
    }
   
    // tests for multi valued text param
    @Test
    public void substitueValue_cmd() {
        initPinfo();
        assertEquals("substitue(<"+textListName+">), listMode=CMD, no prefix", 
                Arrays.asList("A VALUE,B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+textListName+">", dict, paramInfoMap));
    }

    @Test
    public void substitueValue_cmd_listModeSep() {
        textList.listModeSep("&");
        initPinfo();
        assertEquals("substitue(<"+textListName+">), custom listModeSep", 
                Arrays.asList("A VALUE&B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+textListName+">", dict, paramInfoMap));
    }

    @Test
    public void substitueValue_cmd_listModeSepAsSpace() {
        textList.listModeSep(" ");
        initPinfo();
        assertEquals("substitue(<"+textListName+">), custom listModeSep", 
                Arrays.asList("A VALUE B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+textListName+">", dict, paramInfoMap));
    }

    @Test
    public void substitueValue_cmd_prefix() {
        textList.prefixWhenSpecified("-i");
        initPinfo();
        assertEquals("substitue(<"+textListName+">), prefix with no trailing space", 
                Arrays.asList("-iA VALUE,B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+textListName+">", dict, paramInfoMap));
    }
    
    @Test
    public void substitueValue_cmd_prefix_trailing_space() {
        textList.prefixWhenSpecified("-i ");
        initPinfo();
        assertEquals("substitue(<"+textListName+">), prefix with trailing space", 
                Arrays.asList("-i", "A VALUE,B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+textListName+">", dict, paramInfoMap));
    }

    @Test
    public void substituteValue_cmdOpt() throws Exception {
        textList.listMode(ListMode.CMD_OPT);
        initPinfo(); 
        assertEquals("substitue(<"+textListName+">), listMode=CMD_OPT, no prefix", 
                Arrays.asList("A VALUE", "B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+textListName+">", dict, paramInfoMap));
    }

    @Test
    public void substituteValue_cmdOpt_prefix() throws Exception {
        textList.listMode(ListMode.CMD_OPT)
            .prefixWhenSpecified("--input-param=");
        initPinfo(); 
        assertEquals("substitue(<"+textListName+">), prefix with no trailing space", 
                Arrays.asList("--input-param=A VALUE", "--input-param=B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+textListName+">", dict, paramInfoMap));
    }

    
}

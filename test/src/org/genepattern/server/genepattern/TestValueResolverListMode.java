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
    private static final String pname="input.param";
    private Map<String,ParameterInfoRecord>  paramInfoMap;
    private JobInput jobInput;
    private GpConfig gpConfig;
    private GpContext jobContext;
    private Map<String,String> dict=new HashMap<String,String>();
    
    private ParameterInfoBuilder builder;
    private ParameterInfo pinfo;
    
    @Before
    public void setUp() throws Exception {
        jobInput=new JobInput();
        jobInput.addValue(pname, "A VALUE");
        jobInput.addValue(pname, "B VALUE");
        
        gpConfig=new GpConfig.Builder().build();
        jobContext=new GpContext.Builder()
            .jobInput(jobInput)
        .build();
        
        builder = new ParameterInfoBuilder()
            .listMode(ListMode.CMD)
            .name(pname)
            .numValues("0+")
            .optional(true)
            .defaultValue("");
    }
    
    protected void initPinfo() {
        pinfo=builder.build();
        final ParameterInfo[] formalParams = new ParameterInfo[]{pinfo};
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(formalParams);
    }

    @Test
    public void substitueValue_cmd() {
        initPinfo();
        assertEquals("substitue(<"+pname+">), listMode=CMD, no prefix", 
                Arrays.asList("A VALUE,B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+pname+">", dict, paramInfoMap));
    }

    @Test
    public void substitueValue_cmd_listModeSep() {
        builder.listModeSep("&");
        initPinfo();
        assertEquals("substitue(<"+pname+">), custom listModeSep", 
                Arrays.asList("A VALUE&B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+pname+">", dict, paramInfoMap));
    }

    @Test
    public void substitueValue_cmd_listModeSepAsSpace() {
        builder.listModeSep(" ");
        initPinfo();
        assertEquals("substitue(<"+pname+">), custom listModeSep", 
                Arrays.asList("A VALUE B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+pname+">", dict, paramInfoMap));
    }

    @Test
    public void substitueValue_cmd_prefix() {
        builder.prefixWhenSpecified("-i");
        initPinfo();
        assertEquals("substitue(<"+pname+">), prefix with no trailing space", 
                Arrays.asList("-iA VALUE,B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+pname+">", dict, paramInfoMap));
    }
    
    @Test
    public void substitueValue_cmd_prefix_trailing_space() {
        builder.prefixWhenSpecified("-i ");
        initPinfo();
        assertEquals("substitue(<"+pname+">), prefix with trailing space", 
                Arrays.asList("-i", "A VALUE,B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+pname+">", dict, paramInfoMap));
    }

    @Test
    public void substituteValue_cmdOpt() throws Exception {
        builder.listMode(ListMode.CMD_OPT);
        initPinfo(); 
        assertEquals("substitue(<"+pname+">), listMode=CMD_OPT, no prefix", 
                Arrays.asList("A VALUE", "B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+pname+">", dict, paramInfoMap));
    }

    @Test
    public void substituteValue_cmdOpt_prefix() throws Exception {
        builder.listMode(ListMode.CMD_OPT)
            .prefixWhenSpecified("--input-param=");
        initPinfo(); 
        assertEquals("substitue(<"+pname+">), prefix with no trailing space", 
                Arrays.asList("--input-param=A VALUE", "--input-param=B VALUE"), 
                ValueResolver.substituteValue(gpConfig, jobContext, "<"+pname+">", dict, paramInfoMap));
    }

}

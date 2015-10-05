package org.genepattern.server.job.input;

import static org.junit.Assert.*;

import org.genepattern.junitutil.ParameterInfoBuilder;
import org.genepattern.server.job.input.ParamListHelper.ListMode;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Test;

/**
 * Test cases for handling listMode=CMD et cetera in manifest file.
 * @author pcarr
 *
 */
public class TestParamListHelperInitListMode {
    @Test
    public void isCmdLineList_default() {
        ParameterInfo formalParam=new ParameterInfoBuilder().build();
        assertEquals("isCmdLineList(formalParam), default", false, ParamListHelper.isCmdLineList(formalParam));
        assertEquals("isCmdLineList(formalParam, listMode), default", false, 
                ParamListHelper.isCmdLineList(formalParam, ParamListHelper.initListMode(formalParam)));
    }
    
    @Test
    public void isCmdLineList_numValuesNotAcceptsList() throws Exception {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .numValues("0..1")
        .build();
        assertEquals("isCmdLineList(), numValues=0..1, listMode not set", false, ParamListHelper.isCmdLineList(formalParam));
        assertEquals("isCmdLineList(formalParam, listMode), numValues=0..1, listMode not set", false, 
                ParamListHelper.isCmdLineList(formalParam, ParamListHelper.initListMode(formalParam)));
    }

    @Test
    public void isCmdLineList_numValuesAcceptsList_listModeNotSet() throws Exception {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .numValues("0+")
        .build();
        assertEquals("isCmdLineList(), numValues=0+, listMode not set", false, ParamListHelper.isCmdLineList(formalParam));
        assertEquals("isCmdLineList(formalParam, listMode), numValues=0+, listMode not set", false, 
                ParamListHelper.isCmdLineList(formalParam, ParamListHelper.initListMode(formalParam)));
    }
    
    @Test
    public void isCmdLineList_listModeCmd_only() throws Exception {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .listMode(ListMode.CMD)
        .build();
        assertEquals("isCmdLineList(), numValues=<default>, listMode=CMD", false, ParamListHelper.isCmdLineList(formalParam));
        assertEquals("isCmdLineList(formalParam, listMode), numValues=<default>, listMode=CMD", false, 
                ParamListHelper.isCmdLineList(formalParam, ParamListHelper.initListMode(formalParam)));
    }

    @Test
    public void isCmdLineList_listModeCmdOpt_only() throws Exception {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .listMode(ListMode.CMD_OPT)
        .build();
        assertEquals("isCmdLineList(), numValues=<default>, listMode=CMD", false, ParamListHelper.isCmdLineList(formalParam));
        assertEquals("isCmdLineList(formalParam, listMode), numValues=<default>, listMode=CMD", false, 
                ParamListHelper.isCmdLineList(formalParam, ParamListHelper.initListMode(formalParam)));
    }

    @Test
    public void isCmdLineList_numValuesAcceptsList_listModeCmd() throws Exception {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .numValues("0+")
            .listMode(ListMode.CMD)
        .build();
        assertEquals("isCmdLineList(), numValues=0+, listMode=CMD", true, ParamListHelper.isCmdLineList(formalParam));
        assertEquals("isCmdLineList(formalParam, listMode), numValues=0+, listMode=CMD", true, 
                ParamListHelper.isCmdLineList(formalParam, ParamListHelper.initListMode(formalParam)));
    }
    
    @Test
    public void isCmdLineList_numValuesAcceptsList_listModeCmdOpt() throws Exception {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .numValues("0+")
            .listMode(ListMode.CMD_OPT)
        .build();
        assertEquals("isCmdLineList(), numValues=0+, listMode=CMD_OPT", true, ParamListHelper.isCmdLineList(formalParam));
        assertEquals("isCmdLineList(formalParam, listMode), numValues=0+, listMode=CMD_OPT", true, 
                ParamListHelper.isCmdLineList(formalParam, ParamListHelper.initListMode(formalParam)));
    }

    @Test
    public void getListModeSpec_initFromListMode_enum() {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .listMode(ListMode.LIST)
        .build();
        assertEquals(ListMode.LIST.name(), ParamListHelper.getListModeSpec(formalParam));
    }

    @Test
    public void getListModeSpec_init_fromProperty() {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .property("listMode", ListMode.LIST.name())
        .build();
        assertEquals(ListMode.LIST.name(), ParamListHelper.getListModeSpec(formalParam));
    }

    @Test
    public void getListModeSpec_empty() {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .property("listMode", "")
        .build();
        assertEquals("", ParamListHelper.getListModeSpec(formalParam));
    }

    @Test
    public void getListModeSpec_notSet() {
        ParameterInfo formalParam=new ParameterInfoBuilder().build();
        assertNotNull("expecting non-null formalParam.attributes from builder", formalParam.getAttributes());
        assertEquals(null, ParamListHelper.getListModeSpec(formalParam));
    }

    @Test
    public void getListModeSpec_nullParamInfoAttributes() {
        ParameterInfo formalParam=new ParameterInfo();
        assertEquals("expecting null formalParam.attributes by default", null, formalParam.getAttributes());
        assertEquals(null, ParamListHelper.getListModeSpec(formalParam));
    }

    @Test
    public void getListModeSpec_nullArg() {
        ParameterInfo formalParam=null;
        assertEquals(null, ParamListHelper.getListModeSpec(formalParam));
    }
    
    @Test
    public void hasListMode_null_record() {
        ParameterInfoRecord record=null;
        assertFalse("null ParameterInfoRecord", ParamListHelper.hasListMode(record));
    }
    
    @Test
    public void hasListMode_null_formalParam() {
        ParameterInfo pinfo=null;
        ParameterInfoRecord record=new ParameterInfoRecord(pinfo);
        assertFalse("null ParameterInfoRecord.formalParam", ParamListHelper.hasListMode(record));
    }
    
    @Test
    public void hasListMode_from_record_not_set() {
        ParameterInfo formalParam=new ParameterInfoBuilder().build();
        ParameterInfoRecord record=new ParameterInfoRecord(formalParam);
        assertFalse("formalParam.listMode not set", ParamListHelper.hasListMode(record));        
    }

    @Test
    public void hasListMode_from_record() {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .listMode(ListMode.LIST)
        .build();
        ParameterInfoRecord record=new ParameterInfoRecord(formalParam);
        assertTrue("formalParam.listMode is set", ParamListHelper.hasListMode(record));        
    }
    
    @Test
    public void initListMode() {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .listMode(ListMode.LIST_INCLUDE_EMPTY)
        .build();
        assertEquals(
                "custom listMode set in manifest",
                ListMode.LIST_INCLUDE_EMPTY, ParamListHelper.initListMode(formalParam));
    }
    
    @Test
    public void initListMode_not_set() {
        ParameterInfo formalParam=new ParameterInfoBuilder().build();
        assertEquals(
                "when no entry in manifest, default to listMode=LIST",
                ListMode.LIST, ParamListHelper.initListMode(formalParam));
    }

    @Test
    public void initListMode_fromEmptyString() {
        ParameterInfo formalParam=new ParameterInfoBuilder().build();
        assertEquals(
                "when listMode= (empty string) in manifest, default to listMode=LIST",
                ListMode.LIST, ParamListHelper.initListMode(formalParam));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void initListMode_invalid() {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .property("listMode", "UNKNOWN")
        .build();
        assertEquals(
                "when listMode= (empty string) in manifest, default to listMode=LIST",
                ListMode.LIST, ParamListHelper.initListMode(formalParam));
    }
    
    @Test
    public void initListMode_null_record() {
        ParameterInfoRecord record=null;
        assertEquals(
                "when pInfoRecord is null, use default value",
                ListMode.LIST, ParamListHelper.initListMode(record)); 
    }
    
    @Test
    public void initListMode_fromRecord_formalParam_isNull() {
        ParameterInfo formalParam=new ParameterInfoBuilder().build();
        ParameterInfoRecord record=new ParameterInfoRecord(formalParam);
        assertEquals(
                "when pInfoRecord.formal is null, use default value",
                ListMode.LIST, ParamListHelper.initListMode(record)); 
    }

    @Test
    public void initListMode_fromRecord_not_set() {
        ParameterInfo formalParam=new ParameterInfoBuilder().build();
        ParameterInfoRecord record=new ParameterInfoRecord(formalParam);
        assertEquals(
                "when pInfoRecord.formal.listMode not set, use default value",
                ListMode.LIST, ParamListHelper.initListMode(record)); 
    }

    @Test
    public void initListMode_fromRecord() {
        ParameterInfo formalParam=new ParameterInfoBuilder()
            .listMode(ListMode.CMD_OPT)
        .build();
        ParameterInfoRecord record=new ParameterInfoRecord(formalParam);
        assertEquals(
                "when pInfoRecord.formal.listMode not set, use default value",
                ListMode.CMD_OPT, ParamListHelper.initListMode(record)); 
    }

}

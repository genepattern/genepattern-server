package org.genepattern.server.job.input;

import java.util.Map;

import org.genepattern.junitutil.TaskLoader;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.server.job.input.ParamListHelper.ListMode;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for initializing numValues from a ParameterInfo.
 * @author pcarr
 *
 */
public class TestIsCreateFilelist {
    private static final ParameterInfo createFilelistParam(final String numValues, final ListMode listMode) {
        final boolean optional=false;
        final ParameterInfo pinfo=ParameterInfoUtil.initFileParam("input.files", "Demo filelist parameter", optional);
        if (numValues != null) {
            pinfo.getAttributes().put(NumValues.PROP_NUM_VALUES, numValues);
        }
        if (listMode != null) {
            pinfo.getAttributes().put(NumValues.PROP_LIST_MODE, listMode.name());
        }
        return pinfo;
    }

    private static TaskInfo taskInfo;
    private static Map<String,ParameterInfoRecord> paramInfoMap;
    private static Context jobContext;

    final static private String userId="test";
    final static private String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00006:0.7";
    final static private String ftpFile="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct";


    @BeforeClass
    static public void beforeClass() {
        final TaskLoader taskLoader=new TaskLoader();
        taskLoader.addTask(TestJobInputHelper.class, "TestMultiInputFile_v0.7.zip");
        taskInfo = taskLoader.getTaskInfo(lsid);
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        jobContext=ServerConfiguration.Context.getContextForUser(userId);

    }
    
    @Test
    public void testRequiredFile() {
        final String paramName="requiredFile";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        
        final JobInput jobInput = new JobInput();
        jobInput.addValue(paramName, ftpFile);
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(jobContext, record, param);

        Assert.assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
    }
    
    @Test
    public void testOptionalFile() {
        final String paramName="optionalFile";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        final JobInput jobInput = new JobInput();
        jobInput.addValue(paramName, ftpFile);
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(jobContext, record, param);

        Assert.assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
    }
    
    @Test
    public void testInputListEmptyValue() {
        final String paramName="inputList";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        final JobInput jobInput = new JobInput();
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(jobContext, record, param);

        Assert.assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
    }
    
    /*
     * test cases
     * 
     * 7) accepts a file list, actual num value is 2, mode is ..
     * 8) accepts a file list, actual num value is 2, mode is ..
     * 9) accepts a file list, actual num value is 2, mode is ..

     * 
     */
    
    /**
     *  accepts a file list, actual num values is 0, mode is LEGACY
     */
    @Test
    public void testNoValuesLegacyMode() {
        doTest(false, 0, ParamListHelper.ListMode.LEGACY);
    }
    /**
     *  accepts a file list, actual num values is 0, mode is LIST
     */
    @Test
    public void testNoValuesListMode() {
        doTest(false, 0, ParamListHelper.ListMode.LIST);
    }
    /**
     *  accepts a file list, actual num values is 0, mode is LIST_INCLUDE_EMPTY
     */
    @Test
    public void testNoValuesListIncludeEmptyMode() {
        doTest(true, 0, ParamListHelper.ListMode.LIST_INCLUDE_EMPTY);
    }
    /**
     *  accepts a file list, actual num values is 1, mode is LEGACY
     */
    @Test
    public void testOneValueLegacyMode() {
        doTest(false, 1, ParamListHelper.ListMode.LEGACY);
    }
    /**
     *  accepts a file list, actual num values is 1, mode is LIST
     */
    @Test
    public void testOneValuesListMode() {
        doTest(true, 1, ParamListHelper.ListMode.LIST);
    }
    /**
     *  accepts a file list, actual num values is 1, mode is LIST_INCLUDE_EMPTY
     */
    @Test
    public void testOneValueListIncludeEmptyMode() {
        doTest(true, 1, ParamListHelper.ListMode.LIST_INCLUDE_EMPTY);
    }
    /**
     *  accepts a file list, multi input values, mode is LEGACY
     */
    @Test
    public void testMultiValuesLegacyMode() {
        doTest(true, 2, ParamListHelper.ListMode.LEGACY);
    }
    /**
     *  accepts a file list, multi input values, mode is LIST
     */
    @Test
    public void testMultiValuesListMode() {
        doTest(true, 3, ParamListHelper.ListMode.LIST);
    }
    /**
     *  accepts a file list, multi input values, mode is LIST_INCLUDE_EMPTY
     */
    @Test
    public void testMultiValuesListIncludeEmptyMode() {
        doTest(true, 4, ParamListHelper.ListMode.LIST_INCLUDE_EMPTY);
    }
    
    private void doTest(final boolean expectedCreateFilelist, final int actualNumValues, final ParamListHelper.ListMode mode) {
        final ParameterInfo formalParam=createFilelistParam("0+", mode);
        final ParameterInfoRecord record=new ParameterInfoRecord(formalParam);
        final JobInput jobInput = new JobInput();
        for(int i=0; i<actualNumValues; ++i) {
            jobInput.addValue(formalParam.getName(), "arg_"+i);
        }
        final Param param=jobInput.getParam(formalParam.getName());
        final ParamListHelper plh = new ParamListHelper(jobContext, record, param);
        Assert.assertEquals("isCreateFilelist", expectedCreateFilelist, plh.isCreateFilelist());
    }

}

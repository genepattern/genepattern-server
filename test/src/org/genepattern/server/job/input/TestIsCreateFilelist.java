/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.TaskLoader;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.jobinput.ParameterInfoUtil;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for initializing numValues from a ParameterInfo.
 * @author pcarr
 *
 */
public class TestIsCreateFilelist {
    private static TaskLoader taskLoader;
    private static Map<String,ParameterInfoRecord> paramInfoMap;
    private static GpContext jobContext;

    final static private String userId = "test";
    private String otherUser = "other";

    final static private String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00006:0.7";
    final static private String ftpFile="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct";
    
    @BeforeClass
    static public void beforeClass() throws IOException {
        taskLoader=new TaskLoader();
        taskLoader.addTask(TestJobInputHelper.class, "TestMultiInputFile_v0.7.zip");
        taskLoader.addTask(TestJobInputHelper.class, "TestPassByReference_v0.1.zip");

        final TaskInfo taskInfo = taskLoader.getTaskInfo(lsid);
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        jobContext=new GpContext.Builder().userId(userId).build();
    }

    protected GpConfig initGpConfig() throws IOException {
        final GpConfig gpConfig=new GpConfig.Builder()
            .webappDir(new File("./website"))
        .build();
        return gpConfig;
    }

    @Test
    public void testRequiredFile() throws Exception {
        final String paramName="requiredFile";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        
        final JobInput jobInput = new JobInput();
        jobInput.addValue(paramName, ftpFile);

        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(
                DbUtil.getTestDbSession(), 
                initGpConfig(), 
                jobContext, record, jobInput, param, false);

        assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
    }
    
    @Test
    public void testOptionalFile() throws Exception {
        final String paramName="optionalFile";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        final JobInput jobInput = new JobInput();
        jobInput.addValue(paramName, ftpFile);
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(
                DbUtil.getTestDbSession(), 
                initGpConfig(), 
                jobContext, record, jobInput, param, false);

        assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
    }
    
    @Test
    public void testInputListEmptyValue() throws Exception {
        final String paramName="inputList";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        final JobInput jobInput = new JobInput();
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(
                DbUtil.getTestDbSession(), 
                initGpConfig(), 
                jobContext, record, jobInput, param, false);

        assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
    }
    
    /**
     *  accepts a file list, actual num values is 0, mode is LEGACY
     * @throws ExecutionException 
     */
    @Test
    public void testNoValuesLegacyMode() throws Exception {
        doTest(false, 0, ListMode.LEGACY);
    }
    /**
     *  accepts a file list, actual num values is 0, mode is LIST
     * @throws ExecutionException 
     */
    @Test
    public void testNoValuesListMode() throws Exception {
        doTest(false, 0, ListMode.LIST);
    }
    /**
     *  accepts a file list, actual num values is 0, mode is LIST_INCLUDE_EMPTY
     * @throws ExecutionException 
     */
    @Test
    public void testNoValuesListIncludeEmptyMode() throws Exception {
        doTest(true, 0, ListMode.LIST_INCLUDE_EMPTY);
    }
    /**
     *  accepts a file list, actual num values is 1, mode is LEGACY
     * @throws ExecutionException 
     */
    @Test
    public void testOneValueLegacyMode() throws Exception {
        doTest(false, 1, ListMode.LEGACY);
    }
    /**
     *  accepts a file list, actual num values is 1, mode is LIST
     * @throws ExecutionException 
     */
    @Test
    public void testOneValuesListMode() throws Exception {
        doTest(true, 1, ListMode.LIST);
    }
    /**
     *  accepts a file list, actual num values is 1, mode is LIST_INCLUDE_EMPTY
     * @throws ExecutionException 
     */
    @Test
    public void testOneValueListIncludeEmptyMode() throws Exception {
        doTest(true, 1, ListMode.LIST_INCLUDE_EMPTY);
    }
    /**
     *  accepts a file list, multi input values, mode is LEGACY
     * @throws ExecutionException 
     */
    @Test
    public void testMultiValuesLegacyMode() throws Exception {
        doTest(true, 2, ListMode.LEGACY);
    }
    /**
     *  accepts a file list, multi input values, mode is LIST
     * @throws ExecutionException 
     */
    @Test
    public void testMultiValuesListMode() throws Exception {
        doTest(true, 3, ListMode.LIST);
    }
    /**
     *  accepts a file list, multi input values, mode is LIST_INCLUDE_EMPTY
     * @throws ExecutionException 
     */
    @Test
    public void testMultiValuesListIncludeEmptyMode() throws Exception {
        doTest(true, 4, ListMode.LIST_INCLUDE_EMPTY);
    }

    /**
     *  accepts a file list with urls provided instead of server file paths
     */
    @Test
    public void testCreateFileListUrlMode() throws Exception
    {
        final String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00010:0.1";
        final TaskInfo taskInfo = taskLoader.getTaskInfo(lsid);
        Map<String,ParameterInfoRecord> paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);

        final String paramName="file.list.file";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);

        final String internalURL="users/"+ otherUser + "/filename_test/all_aml_test.cls";
         final JobInput jobInput = new JobInput();
        jobInput.addValue(paramName, ftpFile);
        jobInput.addValue(paramName, "<GenePatternURL>"+internalURL);
        jobInput.addValue(paramName, "http://127.0.0.1:8080/gp/"+internalURL);
        jobInput.addValue(paramName, "http://127.0.0.1:8080/gp/data//Shared/tutorial/all_aml_train.gct");
        jobInput.addValue(paramName, "/Shared/tutorial/all_aml_train.gct");
        jobInput.addValue(paramName, "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct");
        
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(
                DbUtil.getTestDbSession(), 
                initGpConfig(), 
                jobContext, record, jobInput, param, false);

        assertTrue(paramName + " accepts list" , plh.acceptsList());
        assertTrue(paramName+".isCreateFilelist", plh.isCreateFilelist());

        //final boolean saveToDb=false;
        //plh.updatePinfoValue(saveToDb);
        plh.updatePinfoValue();

        assertEquals(paramName + ".Url mode external url", 
                ftpFile, 
                (String)plh.parameterInfoRecord.getActual().getAttributes().get("values_0"));

        assertEquals(paramName + ".Url <GenePatternURL> substitution", 
                // expected
                "http://127.0.0.1:8080/gp/"+internalURL,
                //internalFile.getUrl(gpConfig).toExternalForm(),
                // actual
                (String)plh.parameterInfoRecord.getActual().getAttributes().get("values_1"));

        assertEquals(paramName + ".Url internalURL, http://127.0.0.1:8080/gp/", 
                // expected
                "http://127.0.0.1:8080/gp/"+internalURL,
                 // actual
                (String)plh.parameterInfoRecord.getActual().getAttributes().get("values_2"));

        assertEquals(paramName + ".Url internalURL to server file path", 
                // expected
                "http://127.0.0.1:8080/gp/data//Shared/tutorial/all_aml_train.gct",
                 // actual
                (String)plh.parameterInfoRecord.getActual().getAttributes().get("values_3"));

        assertEquals(paramName + ".Url internalURL to server file path, no URL prefix", 
                // expected
                "http://127.0.0.1:8080/gp/data//Shared/tutorial/all_aml_train.gct",
                 // actual
                (String)plh.parameterInfoRecord.getActual().getAttributes().get("values_4"));

        assertEquals(paramName + ".Url external url to ftp file", 
                // expected
                "ftp://gpftp.broadinstitute.org/example_data/datasets/all_aml/all_aml_test.gct",
                 // actual
                (String)plh.parameterInfoRecord.getActual().getAttributes().get("values_5"));
    }

    private void doTest(final boolean expectedCreateFilelist, final int actualNumValues, final ListMode mode) throws Exception {
        final ParameterInfo formalParam=ParameterInfoUtil.initFilelistParam("input.files", "Demo filelist parameter", "0+", mode);
        final ParameterInfoRecord record=new ParameterInfoRecord(formalParam);
        final JobInput jobInput = new JobInput();
        for(int i=0; i<actualNumValues; ++i) {
            jobInput.addValue(formalParam.getName(), "arg_"+i);
        }
        final Param param=jobInput.getParam(formalParam.getName());
        final ParamListHelper plh = new ParamListHelper(
                DbUtil.getTestDbSession(), 
                initGpConfig(), 
                jobContext, record, jobInput, param, false);
        assertEquals("isCreateFilelist", expectedCreateFilelist, plh.isCreateFilelist());
    }

}

package org.genepattern.server.job.input;

import java.util.Map;

import org.genepattern.junitutil.TaskLoader;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInput.ParamId;
import org.genepattern.server.rest.ParameterInfoRecord;
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
    

//
//    @Test
//    public void numValuesNotSet() {
//        doTest(false, false, 1, 1);
//    }
//    
//    @Test
//    public void emptyString() {
//        pinfo.getAttributes().put("numValues", "");
//        doTest(false, false, 1, 1);
//    }
//
//    @Test
//    public void optionalNumValuesNotSet() {
//        pinfo.getAttributes().put("optional", "on");
//        doTest(true, false, 0, 1);
//    }
//    
//    @Test
//    public void one() {
//        pinfo.getAttributes().put("numValues", "1");
//        doTest(false, false, 1, 1);
//    }
//
//    @Test
//    public void two() {
//        pinfo.getAttributes().put("numValues", "2");
//        doTest(false, true, 2, 2);
//    }
//    
//    @Test
//    public void zeroPlus() {
//        pinfo.getAttributes().put("numValues", "0+");
//        doTest(true, true, 0, null);
//    }
//    
//    @Test
//    public void onePlus() {
//        pinfo.getAttributes().put("numValues", "1+");
//        doTest(false, true, 1, null);
//    }
//    
//    @Test
//    public void twoPlus() {
//        pinfo.getAttributes().put("numValues", "2+");
//        doTest(false, true, 2, null);
//    }
//    
//    @Test
//    public void zeroThroughOne() {
//        pinfo.getAttributes().put("numValues", "0..1");
//        doTest(true, false, 0, 1);
//    }
//    
//    @Test
//    public void zeroThroughTwo() {
//        pinfo.getAttributes().put("numValues", "0..2");
//        doTest(true, true, 0, 2);
//    }
//    
//    @Test
//    public void oneThroughTwo() {
//        pinfo.getAttributes().put("numValues", "1..2");
//        doTest(false, true, 1, 2);
//    }
//    
//    //what about when optional and numValues conflict?
//    //... numValues takes precedence
//    @Test
//    public void optional_NumValues_1() {
//        pinfo.getAttributes().put("optional", "on");
//        pinfo.getAttributes().put("numValues", "1");
//        doTest(false, false, 1, 1);
//    }
//    
//    @Test
//    public void optional_NumValues_4() {
//        pinfo.getAttributes().put("optional", "on");
//        pinfo.getAttributes().put("numValues", "4");
//        doTest(false, true, 4, 4);
//    }
//
//    @Test
//    public void nullAttributes() {
//        pinfo.setAttributes(null);
//        doTest(false, false, 1, 1);
//    }
//    
//    //some bogus test cases
//    @Test
//    public void syntaxError_invalidSeparator() {
//        final String numValues="1-2";
//        try {
//            pinfo.getAttributes().put("numValues", numValues);
//            doTest(false, false, 1, 1);
//            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
//        }
//        catch (IllegalArgumentException e) {
//            //expected
//        }
//    }
//
//    @Test
//    public void syntaxError_invalidNumber() {
//        final String numValues="one";
//        try {
//            pinfo.getAttributes().put("numValues", numValues);
//            doTest(false, false, 1, 1);
//            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
//        }
//        catch (IllegalArgumentException e) {
//            //expected
//        }
//    }
//    
//    @Test
//    public void error_lessThanZero() {
//        final String numValues="-1";
//        try {
//            pinfo.getAttributes().put("numValues", numValues);
//            doTest(false, false, 1, 1);
//            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
//        }
//        catch (IllegalArgumentException e) {
//            //expected
//        }
//    }
//
//    @Test
//    public void error_zero() {
//        final String numValues="0";
//        try {
//            pinfo.getAttributes().put("numValues", numValues);
//            doTest(false, false, 1, 1);
//            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
//        }
//        catch (IllegalArgumentException e) {
//            //expected
//        }
//    }
//    
//    @Test
//    public void error_minGreaterThanMax() {
//        final String numValues="5..1";
//        try {
//            pinfo.getAttributes().put("numValues", numValues);
//            doTest(false, false, 1, 1);
//            Assert.fail("expecting IllegalArgumentException for numValues="+numValues);
//        }
//        catch (IllegalArgumentException e) {
//            //expected
//        }
//    }

}

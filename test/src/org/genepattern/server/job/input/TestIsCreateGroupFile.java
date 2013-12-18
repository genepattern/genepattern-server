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
 * jUnit tests for {@link ParamListHelper#isCreateGroupFile()}
 * @author pcarr
 *
 */
public class TestIsCreateGroupFile {
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
        Assert.assertFalse(paramName+".isCreateGroupFile", plh.isCreateGroupFile());
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
        Assert.assertFalse(paramName+".isCreateGroupFile", plh.isCreateGroupFile());
    }
    
    @Test
    public void testInputListEmptyValue() {
        final String paramName="inputList";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        final JobInput jobInput = new JobInput();
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(jobContext, record, param);

        Assert.assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
        Assert.assertFalse(paramName+".isCreateGroupFile", plh.isCreateGroupFile());
    }

}

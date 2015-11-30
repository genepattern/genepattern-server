/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.genepattern.junitutil.DbUtil;
import org.genepattern.junitutil.TaskLoader;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.ParamId;
import org.genepattern.server.rest.ParameterInfoRecord;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * jUnit tests for {@link ParamListHelper#isCreateGroupFile()}
 * @author pcarr
 *
 */
public class TestIsCreateGroupFile {
    private HibernateSessionManager mgr;
    private static TaskInfo taskInfo;
    private static Map<String,ParameterInfoRecord> paramInfoMap;
    private static GpContext jobContext;

    final static private String userId="test";
    final static private String lsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00006:0.7";
    final static private String ftpFile="ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct";
    
    private GpConfig gpConfig;


    
    @BeforeClass
    @SuppressWarnings("deprecation")
    static public void beforeClass() {
        final TaskLoader taskLoader=new TaskLoader();
        taskLoader.addTask(TestJobInputHelper.class, "TestMultiInputFile_v0.7.zip");
        taskInfo = taskLoader.getTaskInfo(lsid);
        paramInfoMap=ParameterInfoRecord.initParamInfoMap(taskInfo);
        jobContext=GpContext.getContextForUser(userId);
    }
    
    @Before
    public void setUp() throws ExecutionException {
        gpConfig=new GpConfig.Builder().build();
        mgr=DbUtil.getTestDbSession();
    }
    
    @Test
    public void testRequiredFile() {
        final String paramName="requiredFile";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        
        final JobInput jobInput = new JobInput();
        jobInput.addValue(paramName, ftpFile);
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(mgr, gpConfig, jobContext, record, jobInput, param, false);

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
        ParamListHelper plh = new ParamListHelper(mgr, gpConfig, jobContext, record, jobInput, param, false);

        Assert.assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
        Assert.assertFalse(paramName+".isCreateGroupFile", plh.isCreateGroupFile());
    }
    
    @Test
    public void testInputListEmptyValue() {
        final String paramName="inputList";
        final ParameterInfoRecord record=paramInfoMap.get(paramName);
        final JobInput jobInput = new JobInput();
        final Param param=jobInput.getParam(new ParamId(paramName));
        ParamListHelper plh = new ParamListHelper(mgr, gpConfig, jobContext, record, jobInput, param, false);

        Assert.assertFalse(paramName+".isCreateFilelist", plh.isCreateFilelist());
        Assert.assertFalse(paramName+".isCreateGroupFile", plh.isCreateGroupFile());
    }

}

package org.genepattern.server.job.input;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.config.ServerConfiguration.Context;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit tests for the JobConfigParams helper class.
 * @author pcarr
 *
 */
public class TestJobConfigParams {

    @Test
    public void testDefault() {
        final String userId="test_user";
        final File zipFile=FileUtil.getDataFile("modules/ComparativeMarkerSelection_v9.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final Context taskContext=ServerConfiguration.Context.getContextForUser(userId);
        taskContext.setTaskInfo(taskInfo);
        
        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(taskContext);
        
        InputParamGroup inputParamGroup=jobConfigParams.getInputParamGroup();
        Assert.assertEquals("Advanced/Job Configuration", inputParamGroup.getName());
        Assert.assertEquals("num custom params", 6, inputParamGroup.getParameters().size());
    }

}

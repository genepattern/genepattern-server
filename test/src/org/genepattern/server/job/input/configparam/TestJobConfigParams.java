package org.genepattern.server.job.input.configparam;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.junitutil.TaskUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.job.input.configparam.JobConfigParams;
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
    public void testNullGpConfig() {
        final GpConfig gpConfig=null;
        final GpContext gpContext=new GpContextFactory.Builder().build();
        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, gpContext);
        Assert.assertNull("When gpConfig is null, return null", jobConfigParams);
    }
    
    @Test
    public void testDefault() {
        final String userId="test_user";
        final File zipFile=FileUtil.getDataFile("modules/ComparativeMarkerSelection_v9.zip");
        final TaskInfo taskInfo=TaskUtil.getTaskInfoFromZip(zipFile);
        final GpContext taskContext=new GpContextFactory.Builder()
            .userId(userId)
            .taskInfo(taskInfo)
            .build();

        final GpConfig gpConfig=new GpConfig.Builder().build();
        JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, taskContext);
        Assert.assertNull("by default, the jobConfigParams should be null", jobConfigParams);
    }

}

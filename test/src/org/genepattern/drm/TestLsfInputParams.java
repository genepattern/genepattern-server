package org.genepattern.drm;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpContextFactory;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.job.input.configparam.JobConfigParams;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test custom LSF executor.inputParams.
 * @author pcarr
 *
 */
public class TestLsfInputParams {    
    @Test
    public void testLsfExtraInputParams() {
        final File resourcesDir=FileUtil.getSourceDir(this.getClass());
        final GpServerProperties serverProperties=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .addCustomProperty("executor.inputParams", "lsf_executorInputParams.yaml")
            .build();
        final GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(serverProperties)
            .build();
        final GpContext gpContext=new GpContextFactory.Builder()
            .build();
        final JobConfigParams lsfParams=JobConfigParams.initJobConfigParams(gpConfig, gpContext);
        
        Assert.assertNotNull("expecting non-null 'executor.inputParams'", lsfParams);
        Assert.assertEquals("lsfParams.size", 4, lsfParams.getParams().size());
        Assert.assertEquals("lsfParams[0]._displayName", "project", lsfParams.getParams().get(0)._getDisplayName());
        Assert.assertEquals("lsfParams[0].name", "lsf.project", lsfParams.getParams().get(0).getName());
        Assert.assertEquals("lsfParams[1].name", "lsf.queue", lsfParams.getParams().get(1).getName());
        Assert.assertEquals("lsfParams[2].name", "lsf.max.memory", lsfParams.getParams().get(2).getName());
        Assert.assertEquals("lsfParams[3].name", "lsf.cpu.slots", lsfParams.getParams().get(3).getName());
    }

}

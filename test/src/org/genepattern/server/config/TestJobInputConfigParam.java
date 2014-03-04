package org.genepattern.server.config;

import java.io.File;

import org.genepattern.drm.TestDrmJobSubmission;
import org.genepattern.junitutil.ConfigUtil;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.job.input.JobInput;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * junit tests for getting a user-supplied job configuration parameter.
 * @author pcarr
 *
 */
public class TestJobInputConfigParam {
    private static final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    private GpConfig gpConfig;

    @Before
    public void before() throws Throwable {
        File resourcesDir=FileUtil.getSourceDir(this.getClass());
        File configYaml=FileUtil.getSourceFile(TestDrmJobSubmission.class, "drm_test.yaml");
        this.gpConfig=new GpConfig.Builder()
            .resourcesDir(resourcesDir)
            .configFile(configYaml)
            .build();
        if (this.gpConfig.hasInitErrors()) {
            throw this.gpConfig.getInitializationErrors().get(0);
        }
        ConfigUtil.setUserGroups(this.getClass(), "userGroups.xml");
    }
    
    @Test
    public void testJobInput() {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(cleLsid);
        jobInput.addValue("input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct");
        
        //add config params
        jobInput.addValue("drm.queue", "userCustomQueue");
        jobInput.addValue("drm.memory", "28gb");
        
        GpContext jobContext=new GpContextFactory.Builder()
            .jobInput(jobInput)
            .build();
        
        Assert.assertEquals("Get 'drm.queue' from jobInput", "userCustomQueue", gpConfig.getGPProperty(jobContext, "drm.queue"));
        Assert.assertEquals("Get 'drm.memory' from jobInput", "28gb", gpConfig.getGPProperty(jobContext, "drm.memory"));
    }

}

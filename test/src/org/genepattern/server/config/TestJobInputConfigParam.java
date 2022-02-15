/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
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
    private JobInput jobInput;
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
        ConfigUtil.setUserGroups(this.getClass(), "drm_test_userGroups.xml");
        jobInput=new JobInput();
        jobInput.setLsid(cleLsid);
        jobInput.addValue("input.filename", "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_train.gct");
    }
    
    @Test
    public void testJobInput() { 
        //add config params
        jobInput.addValue("job.queue", "userCustomQueue");
        jobInput.addValue("job.memory", "28gb");
        
        GpContext jobContext=new GpContext.Builder()
            .jobInput(jobInput)
            .build();
        
        Assert.assertEquals("Get 'job.queue' from jobInput", "userCustomQueue", gpConfig.getGPProperty(jobContext, "job.queue"));
        Assert.assertEquals("Get 'job.memory' from jobInput", "28gb", gpConfig.getGPProperty(jobContext, "job.memory"));
    }
    
    @Test
    public void customSgeQueueName() {
        jobInput.addValue("sge.queueName", "SGE_GENEPATTERN_QUEUE");
        GpContext jobContext=new GpContext.Builder()
            .jobInput(jobInput)
            .build();
        Assert.assertEquals("Expected 'sge.queueName' from jobInput", "SGE_GENEPATTERN_QUEUE", gpConfig.getGPProperty(jobContext, "sge.queueName", null)); 
    }

}

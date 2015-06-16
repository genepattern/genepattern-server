/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.GpServerProperties;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.server.job.input.configparam.JobConfigParams;
import org.genepattern.webservice.ParameterInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test custom LSF job.inputParams.
 * @author pcarr
 *
 */
public class TestLsfInputParams {    
    @Test
    public void testLsfExtraInputParams() {
        final File resourcesDir=FileUtil.getSourceDir(this.getClass());
        final GpServerProperties serverProperties=new GpServerProperties.Builder()
            .resourcesDir(resourcesDir)
            .addCustomProperty("job.inputParams", "lsfInputParams.yaml")
            .build();
        final GpConfig gpConfig=new GpConfig.Builder()
            .serverProperties(serverProperties)
            .build();
        final GpContext gpContext=new GpContext.Builder()
            .build();
        final JobConfigParams lsfParams=JobConfigParams.initJobConfigParams(gpConfig, gpContext);
        
        Assert.assertNotNull("expecting non-null 'job.inputParams'", lsfParams);
        Assert.assertEquals("lsfParams.size", 4, lsfParams.getParams().size());
        Assert.assertEquals("lsfParams[0]._displayName", "project", lsfParams.getParams().get(0)._getDisplayName());
        Assert.assertEquals("lsfParams[0].name", "lsf.project", lsfParams.getParams().get(0).getName());
        Assert.assertEquals("lsfParams[1].name", "lsf.queue", lsfParams.getParams().get(1).getName());
        Assert.assertEquals("lsfParams[2].name", "lsf.max.memory", lsfParams.getParams().get(2).getName());
        Assert.assertEquals("lsfParams[3].name", "lsf.cpu.slots", lsfParams.getParams().get(3).getName());
        
        
        //test choice drop-down with labels
        ParameterInfo lsfMaxMemory=lsfParams.getParam("lsf.max.memory");
        Assert.assertEquals("lsf.max.memory.defaultValue", "2", lsfMaxMemory.getDefaultValue());
        ChoiceInfo choiceInfo=ChoiceInfoHelper.initChoiceInfo(lsfMaxMemory);    
        Assert.assertEquals("small (1 Gb)", choiceInfo.getChoices().get(0).getLabel());
        Assert.assertEquals("1", choiceInfo.getChoices().get(0).getValue());
    }

}

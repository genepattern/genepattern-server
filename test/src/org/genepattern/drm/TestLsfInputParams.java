/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.drm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.genepattern.server.job.input.choice.ChoiceInfoHelper;
import org.genepattern.server.job.input.configparam.JobConfigParams;
import org.genepattern.webservice.ParameterInfo;
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
        final GpConfig gpConfig=mock(GpConfig.class);
        final GpContext gpContext=mock(GpContext.class);
        
        when(gpConfig.getResourcesDir()).thenReturn(resourcesDir);
        when(gpConfig.getGPProperty(gpContext, JobRunner.PROP_JOB_INPUT_PARAMS)).thenReturn("lsfInputParams.yaml");

        final JobConfigParams lsfParams=JobConfigParams.initJobConfigParams(gpConfig, gpContext);
        
        assertNotNull("expecting non-null 'job.inputParams'", lsfParams);
        assertEquals("lsfParams.size", 4, lsfParams.getParams().size());
        assertEquals("lsfParams[0]._displayName", "project", lsfParams.getParams().get(0)._getDisplayName());
        assertEquals("lsfParams[0].name", "lsf.project", lsfParams.getParams().get(0).getName());
        assertEquals("lsfParams[1].name", "lsf.queue", lsfParams.getParams().get(1).getName());
        assertEquals("lsfParams[2].name", "lsf.max.memory", lsfParams.getParams().get(2).getName());
        assertEquals("lsfParams[3].name", "lsf.cpu.slots", lsfParams.getParams().get(3).getName());
        
        
        //test choice drop-down with labels
        ParameterInfo lsfMaxMemory=lsfParams.getParam("lsf.max.memory");
        assertEquals("lsf.max.memory.defaultValue", "2", lsfMaxMemory.getDefaultValue());
        ChoiceInfo choiceInfo=ChoiceInfoHelper.initChoiceInfo(lsfMaxMemory);    
        assertEquals("small (1 Gb)", choiceInfo.getChoices().get(0).getLabel());
        assertEquals("1", choiceInfo.getChoices().get(0).getValue());
    }

}

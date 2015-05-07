/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.configparam;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.job.input.choice.Choice;
import org.genepattern.server.job.input.choice.ChoiceInfo;
import org.junit.Assert;
import org.junit.Test;

/**
 * junit tests for serializing/deserializing a JobConfigParams instance from a file.
 * @author pcarr
 *
 */
public class TestJobConfigParamsParser {
    
    @Test
    public void testParser() throws Exception {
        File in=FileUtil.getSourceFile(this.getClass(), "executor_input_params.yaml");
        JobConfigParams params=JobConfigParamsParserYaml.parse(in);
        
        Assert.assertEquals("group.name", "Advanced/Job Configuration", params.getInputParamGroup().getName());
        Assert.assertEquals("group.description", "Set job configuration parameters", params.getInputParamGroup().getDescription());
        Assert.assertEquals("group.hidden", true, params.getInputParamGroup().isHidden());
        Assert.assertEquals("group.numParams", 6, params.getInputParamGroup().getParameters().size());
        List<String> expected = Arrays.asList("job.queue", "job.memory", "job.walltime", "job.nodeCount", "job.cpuCount", "job.extraArgs");
        Assert.assertThat("group.parameters", params.getInputParamGroup().getParameters(), is(expected));
        
        List<Choice> choices=ChoiceInfo.getDeclaredChoices(params.getParams().get(1));
        Assert.assertEquals("job.memory.choice.label", "512mb",  choices.get(0).getLabel());
        Assert.assertEquals("job.memory.choice.value", "0.5gb", choices.get(0).getValue());
    }

}

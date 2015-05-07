/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server;

import org.genepattern.server.JobInfoWrapper.InputFile;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestJobInfoWrapperInputFile {
    int jobNumber=0;
    TaskInfo taskInfo;
    JobInfo jobInfo;
    String servletContextPath="/gp";
    ParameterInfo param=Mockito.mock(ParameterInfo.class);

    @Before
    public void setUp() {
        taskInfo=null;
        jobInfo=new JobInfo();
        jobInfo.setJobNumber(jobNumber);
    }
    
    @Test
    public void testInternalLink() {
        final String value="<GenePatternURL>users/admin/filename_test/all_aml_test.cls";
        Mockito.when(param.getValue()).thenReturn(value);
        Mockito.when(param.isInputFile()).thenReturn(true);
        
        InputFile inputFile = new InputFile(taskInfo, jobInfo, servletContextPath, param.getValue(), param);
        Assert.assertEquals("inputFile.displayValue",
                "all_aml_test.cls",
                inputFile.getDisplayValue());
    }

    @Test
    public void testInternalLinkWithEncodedCharacters() {
        final String value="<GenePatternURL>users/admin/filename_test/all%20%2B%20aml%20test.cls";
        Mockito.when(param.getValue()).thenReturn(value);
        Mockito.when(param.isInputFile()).thenReturn(true);
        
        InputFile inputFile = new InputFile(taskInfo, jobInfo, servletContextPath, param.getValue(), param);
        Assert.assertEquals("inputFile.displayValue",
                "all + aml test.cls",
                inputFile.getDisplayValue());
    }

}

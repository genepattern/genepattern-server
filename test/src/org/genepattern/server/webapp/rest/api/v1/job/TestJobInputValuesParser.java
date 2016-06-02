/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.genepattern.junitutil.FileUtil;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.Param;
import org.junit.Test;

public class TestJobInputValuesParser {
    final String GP_URL="http:127.0.0.1:8080/gp";
    final String userId="test";
    final String PREFIX=GP_URL+"/users/"+userId;
    
    @Test
    public void emptyString() throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final File jsonFile=FileUtil.getSourceFile(this.getClass(), "teststep_emptyValue.json");
        JobInputValues jobInputValues=mapper.readValue(jsonFile, JobInputValues.class);
        JobInput jobInput=JobInputValues.parseJobInput(jobInputValues);
        
        assertEquals("getParam('errorMessage')", null, jobInput.getParam("errorMessage"));
        assertEquals("getParam('input0')", null, jobInput.getParam("input0"));
        assertEquals("getValue('exitCode')", "0", jobInput.getParam("exitCode").getValues().get(0).getValue());
        
        // from { "name": "emptyListParam", "values": [ ] } 
        assertEquals("getParam('emptyListParam')", null, jobInput.getParam("emptyListParam"));
        // from { "name": "nullParam", "values": [ null ] }
        assertEquals("getValue('nullParam')", null, 
                jobInput.getParam("nullParam")
                    .getValues()
                    .get(0)
                    .getValue() 
        );
    }
    
    @Test
    public void fileGroupParam_singleValue() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile=FileUtil.getSourceFile(this.getClass(), "cuffdiff_singleValue.json");
        JobInputValues jobInputValues=mapper.readValue(jsonFile, JobInputValues.class);
        JobInput jobInput = JobInputValues.parseJobInput(jobInputValues);
        assertEquals("Expected num values", 1, jobInput.getParamValues("aligned.files").size());
    }
    
    @Test
    public void fileGroupParam_multipleValues_noGroups() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile=FileUtil.getSourceFile(this.getClass(), "cuffdiff_multipleValues_noGroups.json");
        JobInputValues jobInputValues=mapper.readValue(jsonFile, JobInputValues.class);
        JobInput jobInput = JobInputValues.parseJobInput(jobInputValues);
        assertEquals("Expected num values", 3, jobInput.getParamValues("aligned.files").size());
    }
    
    @Test
    public void fileGroupParam_multipleValues_twoGroups() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        File jsonFile=FileUtil.getSourceFile(this.getClass(), "cuffdiff_multipleValues_twoGroups.json");
        JobInputValues jobInputValues=mapper.readValue(jsonFile, JobInputValues.class);
        JobInput jobInput = JobInputValues.parseJobInput(jobInputValues);
        Param alignedFiles=jobInput.getParam("aligned.files");
        
        assertEquals("numGroups", 2,  alignedFiles.getGroups().size());
    }
}

package org.genepattern.server.webapp.rest.api.v1.job;

import static org.junit.Assert.assertEquals;

import java.io.File;

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

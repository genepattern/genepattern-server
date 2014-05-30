package org.genepattern.server.webapp.rest.api.v1.job;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.webapp.rest.api.v1.Rel;
import org.genepattern.server.webapp.rest.api.v1.job.GetJobException;
import org.genepattern.server.webapp.rest.api.v1.job.GetPipelineJobLegacy;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestGetPipelineJobLegacy {
    
    private final String GP_URL="http://127.0.0.1:8080/gp";
    private final boolean includeOutputFiles=true;
    private final int jobNumber=0;

    private JobInfo jobInfo;
    private ParameterInfo executionLog;
    private ParameterInfo outputFile;
    private ParameterInfo stderr;
    private ParameterInfo stdout;

    @Before
    public void setUp() {
        Date submittedDate=new Date();
        Date completedDate=new Date();
        this.jobInfo=new JobInfo();
        jobInfo.setJobNumber(jobNumber);
        jobInfo.setTaskName("ConvertLineEndings");
        jobInfo.setDateCompleted(completedDate);
        jobInfo.setDateSubmitted(submittedDate);
        jobInfo.setStatus(JobStatus.FINISHED);
        //init output file
        outputFile = mock(ParameterInfo.class);
        when(outputFile.isOutputFile()).thenReturn(true);
        when(outputFile._isStderrFile()).thenReturn(false);
        when(outputFile.getName()).thenReturn("all_aml_test.gct");
        //init stdout
        stdout = mock(ParameterInfo.class);
        when(stdout.isOutputFile()).thenReturn(true);
        when(stdout._isStdoutFile()).thenReturn(false);
        when(stdout.getName()).thenReturn("stdout.txt");
        //init stderr
        stderr = mock(ParameterInfo.class);
        when(stderr.isOutputFile()).thenReturn(true);
        when(stderr._isStderrFile()).thenReturn(true);
        when(stderr.getName()).thenReturn("stderr.txt");
        //init executionLog
        executionLog = mock(ParameterInfo.class);
        when(executionLog.isOutputFile()).thenReturn(true);
        when(executionLog.getName()).thenReturn("gp_execution_log.txt");
        
        jobInfo.setParameterInfoArray(new ParameterInfo[]{ outputFile, stdout, stderr, executionLog});
    }
    
    /**
     * In gp 3.8.1 REST API, the execution log is here:
     * <pre>
       status: {
           "executionLogLocation": ""
       }
     * </pre>
     */
    @Test
    public void testExecutionLog_v_3_8_1() throws GetJobException, JSONException {
        
        
        final JSONObject jobObj=GetPipelineJobLegacy.initJsonObject(GP_URL, jobInfo, includeOutputFiles);
        Assert.assertEquals("status.executionLogLocation",
                GP_URL+"/jobResults/"+jobNumber+"/gp_execution_log.txt",
                jobObj.getJSONObject("status").get("executionLogLocation")
                );
    }
    
    /**
     * In gp 3.8.2 REST API, the execution log is also here
     * <pre>
       logFiles: [
           { link: "http://127.0.0.1:8080/gp/jobResults/0/gp_execution_log.txt",
             rel: "gp_log_file",
             name: "gp_execution_log.txt" }
       ]
     * </pre>
     */
    @Test
    public void testExecutionLog_v_3_8_2() throws GetJobException, JSONException {
        final JSONObject jobObj=GetPipelineJobLegacy.initJsonObject(GP_URL, jobInfo, includeOutputFiles);
        Assert.assertEquals("logFiles[0].href",
                GP_URL+"/jobResults/"+jobNumber+"/gp_execution_log.txt",
                jobObj.getJSONArray("logFiles")
                    .getJSONObject(0)
                    .getJSONObject("link")
                    .getString("href")
                );
        
        Assert.assertEquals("logFiles[0].name",
                "gp_execution_log.txt",
                jobObj.getJSONArray("logFiles")
                    .getJSONObject(0)
                    .getJSONObject("link")
                    .getString("name")
                );  

        Assert.assertEquals("logFiles[0].rel",
                Rel.gp_logFile.name(),
                jobObj.getJSONArray("logFiles")
                    .getJSONObject(0)
                    .getJSONObject("link")
                    .getString("rel")
                );
    }
    
    @Test
    public void testStderrLocation() throws GetJobException, JSONException {
        final JSONObject jobObj=GetPipelineJobLegacy.initJsonObject(GP_URL, jobInfo, includeOutputFiles);
        Assert.assertEquals("status.stderrLocation",
                GP_URL+"/jobResults/"+jobNumber+"/stderr.txt",
                jobObj.getJSONObject("status")
                    .getString("stderrLocation")
                );

    }
    

}

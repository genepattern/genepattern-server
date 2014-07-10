package org.genepattern.drm.impl.lsf.core;

import static org.junit.Assert.*;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.Memory;
import org.genepattern.drm.impl.lsf.core.LsfStatusChecker.LsfBjobsParserLogOutputStream;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestLsfStatusChecker {
    private int gpJobNo=0;
    private DrmJobRecord jobRecord;

    /*
     * Example bjobs -W output
     */    
    final String[] exampleOutput= {
            "JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME  PROJ_NAME CPU_USED MEM SWAP PIDS START_TIME FINISH_TIME",
            "1583669 gpdev   RUN   genepattern gpint01     node1450    57940      07/09-11:39:51 default    033:23:02.00 6395   6910   1065,1066,1069,1070 07/09-11:39:52 -"
    };
    
    @Before
    public void setUp() {
        jobRecord=new DrmJobRecord.Builder(gpJobNo)
        .build();
    }
    
    @Test
    public void parseRunningJob() {
        //expected date
        DateTime expectedSubmitTime=new DateTime("2014-07-09T11:39:51");
        DateTime expectedStartTime=new DateTime("2014-07-09T11:39:52");
        long expectedCpuUsage= (33L*60L*60L*1000L) + (23L*60*1000L) + (2L*1000L) +  570L;
        Memory expectedMemUsage=Memory.fromString("6395 mb");
        
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1583669 gpdev   RUN   genepattern gpint01     node1450    57940      07/09-11:39:51 default    033:23:02.57 6395   6910   1065,1066,1069,1070 07/09-11:39:52 -");
        assertEquals("drmJobId", "1583669", jobStatus.getDrmJobId());
        assertEquals("jobState", DrmJobState.RUNNING, jobStatus.getJobState());
        assertEquals("submitTime", expectedSubmitTime.toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", expectedStartTime.toDate(), jobStatus.getStartTime());
        assertEquals("endTime", null, jobStatus.getEndTime());
        assertEquals("cpuUsage", expectedCpuUsage, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", expectedMemUsage, jobStatus.getMemory());
    }
    
    @Test
    public void parseOutput() {
        final int logLevel=0;
        LsfBjobsParserLogOutputStream parser=new LsfBjobsParserLogOutputStream();
        for(int i=0; i<exampleOutput.length; ++i) {
            parser.processLine(exampleOutput[i], logLevel);
        }
        assertEquals("results.size", 1, parser.getResults().size());
        assertEquals("results[0].extJobId", "1583669", parser.getResults().get(0).getDrmJobId());
        assertEquals("results[0].extJobId", DrmJobState.RUNNING, parser.getResults().get(0).getJobState());
    }
    
    
    @Ignore // ignored because it only works on a system where the 'bjobs' command is installed and 
            // with an actual test pending. 
    @Test
    public void queuedJob() throws LsfCmdException {
        LsfStatusChecker c=new LsfStatusChecker();
        DrmJobStatus jobStatus=c.getStatus(jobRecord);
        
        Assert.assertTrue("", jobStatus.getJobState().is(DrmJobState.IS_QUEUED));
    }
    

}

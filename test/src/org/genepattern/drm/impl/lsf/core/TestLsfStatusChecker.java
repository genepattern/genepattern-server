package org.genepattern.drm.impl.lsf.core;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.Memory;
import org.genepattern.junitutil.FileUtil;
import org.joda.time.DateTime;
import org.junit.Before;
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
    
    /*
     * More example output
JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME  PROJ_NAME CPU_USED MEM SWAP PIDS START_TIME FINISH_TIME
1696268 gpdev   RUN   genepattern gpint01     node1457    66364      07/10-10:31:29 default    000:00:00.00 3      39     23412 07/10-10:31:31 - 
1696266 gpdev   PEND  genepattern gpint01        -        66363      07/10-10:31:01 default    000:00:00.00 0      0       -  -  - 
1696270 gpdev   PEND  genepattern gpint01        -        66365      07/10-10:31:56 default    000:00:00.00 0      0       -  -  - 
-bash:gpint01:~ 1002 $ bjobs -W
JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME  PROJ_NAME CPU_USED MEM SWAP PIDS START_TIME FINISH_TIME
1696268 gpdev   RUN   genepattern gpint01     node1457    66364      07/10-10:31:29 default    000:00:00.00 3      39     23412 07/10-10:31:31 - 
1696266 gpdev   PEND  genepattern gpint01        -        66363      07/10-10:31:01 default    000:00:00.00 0      0       -  -  - 
-bash:gpint01:~ 1003 $ bjobs -W 1696270
JOBID   USER    STAT  QUEUE      FROM_HOST   EXEC_HOST   JOB_NAME   SUBMIT_TIME  PROJ_NAME CPU_USED MEM SWAP PIDS START_TIME FINISH_TIME
1696270 gpdev   DONE  genepattern gpint01     node1456    66365      07/10-10:31:56 default    000:00:00.45 3      39     18696 07/10-10:32:00 07/10-10:32:01

     */
    
    @Before
    public void setUp() {
        jobRecord=new DrmJobRecord.Builder(gpJobNo)
        .build();
    }
    
    @Test
    public void parseRunningJob() throws InterruptedException {
        //expected date
        DateTime expectedSubmitTime=new DateTime("2014-07-09T11:39:51");
        DateTime expectedStartTime=new DateTime("2014-07-09T11:39:52");
        long expectedCpuUsage= (33L*60L*60L*1000L) + (23L*60*1000L) + (2L*1000L) +  570L;
        Memory expectedMemUsage=Memory.fromString("6395 mb");
        
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1583669 gpdev   RUN   genepattern gpint01     node1450    57940      07/09-11:39:51 default    033:23:02.57 6395   6910   1065,1066,1069,1070 07/09-11:39:52 - ");
        assertEquals("drmJobId", "1583669", jobStatus.getDrmJobId());
        assertEquals("jobState", DrmJobState.RUNNING, jobStatus.getJobState());
        assertEquals("submitTime", expectedSubmitTime.toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", expectedStartTime.toDate(), jobStatus.getStartTime());
        assertEquals("endTime", null, jobStatus.getEndTime());
        assertEquals("cpuUsage", expectedCpuUsage, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", expectedMemUsage, jobStatus.getMemory());
    }
    
    @Test
    public void parsePendingJob() throws InterruptedException  {
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1696266 gpdev   PEND  genepattern gpint01        -        66363      07/10-10:31:01 default    000:00:00.00 0      0       -  -  - ");
        assertEquals("jobState", DrmJobState.QUEUED, jobStatus.getJobState());
        assertEquals("drmJobId", "1696266", jobStatus.getDrmJobId());
        assertEquals("submitTime", new DateTime("2014-07-10T10:31:01").toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", null, jobStatus.getStartTime());
        assertEquals("endTime", null, jobStatus.getEndTime());
        assertEquals("cpuUsage", 0, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", 0, jobStatus.getMemory().getNumBytes());
    }
    
    @Test
    public void parseCancelledPendingJob() throws InterruptedException  {
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1696266 gpdev   EXIT  genepattern gpint01        -        66363      07/10-10:31:01 default    000:00:00.00 0      0       -  -  07/10-10:46:11");
        assertEquals("jobState", DrmJobState.ABORTED, jobStatus.getJobState());
        assertEquals("drmJobId", "1696266", jobStatus.getDrmJobId());
        assertEquals("submitTime", new DateTime("2014-07-10T10:31:01").toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", null, jobStatus.getStartTime());
        assertEquals("endTime", new DateTime("2014-07-10T10:46:11").toDate(), jobStatus.getEndTime());
        assertEquals("cpuUsage", 0, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", 0, jobStatus.getMemory().getNumBytes());
    }
    
    @Test
    public void parseCancelledRunningJob() throws InterruptedException  {
        File lsfLogFile=FileUtil.getDataFile("jobResults/cancelledJob/.lsf.out");
        DrmJobStatus jobStatus=LsfBjobsParser.parseAsJobStatus(
                "1702585 gpdev   EXIT  genepattern gpint01     node1459    66373      07/10-12:31:23 default    000:00:02.40 32     2550   25751,25758,25760 07/10-12:31:24 07/10-12:32:15",
                lsfLogFile);
        assertEquals("jobState", DrmJobState.CANCELLED, jobStatus.getJobState());
        assertEquals("drmJobId", "1702585", jobStatus.getDrmJobId());
        assertEquals("submitTime", new DateTime("2014-07-10T12:31:23").toDate(), jobStatus.getSubmitTime());
        assertEquals("startTime", new DateTime("2014-07-10T12:31:24").toDate(), jobStatus.getStartTime());
        assertEquals("endTime", new DateTime("2014-07-10T12:32:15").toDate(), jobStatus.getEndTime());
        assertEquals("cpuUsage", 2400, jobStatus.getCpuTime().getTime());
        assertEquals("memUsage", Memory.fromString("32mb"), jobStatus.getMemory());
    }
    
    @Test
    public void checkStatus() throws Exception {
        File lsfLogFile=new File(".lsf.out");  // doesn't exist, should be ignored
        CmdRunner cmdRunner = new CmdRunner() {
            @Override
            public List<String> runCmd(List<String> cmd) throws CmdException {
                // skip the first line of output
                return Arrays.asList(new String[] { exampleOutput[1] });
            }
        };
        LsfStatusChecker statusChecker=new LsfStatusChecker(jobRecord, lsfLogFile, cmdRunner);
        statusChecker.checkStatus();
        DrmJobStatus jobStatus=statusChecker.getStatus();
        assertEquals(DrmJobState.RUNNING, jobStatus.getJobState());
    }
    
}

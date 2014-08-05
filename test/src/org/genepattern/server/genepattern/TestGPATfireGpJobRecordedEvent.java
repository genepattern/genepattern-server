package org.genepattern.server.genepattern;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.events.GpJobRecordedEvent;
import org.genepattern.server.executor.events.JobEventBus;
import org.genepattern.server.job.status.Status;
import org.genepattern.webservice.JobInfo;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.Subscribe;

public class TestGPATfireGpJobRecordedEvent {
    //private EventBus eventBus;
    private Integer gpJobNo=1;
    private JobInfo jobInfo;
    private String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    private Date submittedDate=new DateTime("2014-07-30T02:45:00").toDate();
    private Date completedDate=new DateTime("2014-07-30T14:45:00").toDate();
    
    private GpJobRecordedListener listener;
    
    static class GpJobRecordedListener {
        public String taskLsid=null;
        public Status jobStatus=null;
        public boolean isInPipeline=false;

        @Subscribe
        public void onGpJobRecorded(final GpJobRecordedEvent evt) {
            this.taskLsid=evt.getTaskLsid();
            this.jobStatus=evt.getJobStatus();
            this.isInPipeline=evt.getIsInPipeline();
        }
    }
    
    @Before
    public void setUp() {
        //eventBus=new EventBus();
        listener=new GpJobRecordedListener();
        //eventBus.register(listener);
        JobEventBus.instance().register(listener);
        jobInfo=mock(JobInfo.class);
        when(jobInfo.getJobNumber()).thenReturn(gpJobNo);
        when(jobInfo.getStatus()).thenReturn(JobStatus.FINISHED);
        when(jobInfo.getTaskLSID()).thenReturn(cleLsid);
        when(jobInfo.getDateSubmitted()).thenReturn(submittedDate);
        when(jobInfo.getDateCompleted()).thenReturn(completedDate);
        when(jobInfo._getParentJobNumber()).thenReturn(-1);
    }
    
    @Test
    public void handleGpJobRecordedEvent_nullJrj() {
        GenePatternAnalysisTask.fireGpJobRecordedEvent(null, jobInfo);
        assertEquals("after fire event, taskLsid", cleLsid, listener.taskLsid);
        assertEquals("dateCompletedInGp", completedDate, listener.jobStatus.getDateCompletedInGp());
        assertEquals("dateSubmittedTpGp", submittedDate, listener.jobStatus.getDateSubmittedToGp());
        assertEquals("isFinished", true, listener.jobStatus.getIsFinished());
        assertEquals("isInPipeline", false, listener.isInPipeline);
    }
    
    @Test
    public void handleGpJobRecordedEvent_withJrj() {
        JobRunnerJob jrj=mock(JobRunnerJob.class);
        GenePatternAnalysisTask.fireGpJobRecordedEvent(jrj, jobInfo);
        assertEquals("after fire event, taskLsid", cleLsid, listener.taskLsid);
        assertEquals("dateCompletedInGp", completedDate, listener.jobStatus.getDateCompletedInGp());
        assertEquals("dateSubmittedTpGp", submittedDate, listener.jobStatus.getDateSubmittedToGp());
        assertEquals("isFinished", true, listener.jobStatus.getIsFinished());
        assertEquals("isInPipeline", false, listener.isInPipeline);
    }
    
    @Test
    public void handleGpJobRecordedEvent_isInPipeline() {
        when(jobInfo._getParentJobNumber()).thenReturn(37);
        GenePatternAnalysisTask.fireGpJobRecordedEvent(null, jobInfo);
        assertEquals("isInPipeline", true, listener.isInPipeline);
    }
}

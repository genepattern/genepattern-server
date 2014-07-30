package org.genepattern.server.genepattern;

import java.util.Date;

import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.executor.events.GpJobRecordedEvent;
import org.genepattern.server.executor.events.JobEventBus;
import org.genepattern.server.job.status.Status;
import org.genepattern.webservice.JobInfo;
import org.joda.time.DateTime;
import org.junit.Assert;
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

        @Subscribe
        public void onGpJobRecorded(final GpJobRecordedEvent evt) {
            this.taskLsid=evt.getTaskLsid();
            this.jobStatus=evt.getJobStatus();
        }
    }
    
    @Before
    public void setUp() {
        //eventBus=new EventBus();
        listener=new GpJobRecordedListener();
        //eventBus.register(listener);
        JobEventBus.instance().register(listener);
        jobInfo=new JobInfo();
        jobInfo.setJobNumber(gpJobNo);
        jobInfo.setStatus(JobStatus.FINISHED);
        jobInfo.setTaskLSID(cleLsid);
        jobInfo.setDateSubmitted(submittedDate);
        jobInfo.setDateCompleted(completedDate);
    }
    
    @Test
    public void handleGpJobRecordedEvent() {
        GenePatternAnalysisTask.fireGpJobRecordedEvent(jobInfo);
        Assert.assertEquals("after fire event, taskLsid", cleLsid, listener.taskLsid);
        Assert.assertEquals("dateCompletedInGp", completedDate, listener.jobStatus.getDateCompletedInGp());
        Assert.assertEquals("dateSubmittedTpGp", submittedDate, listener.jobStatus.getDateSubmittedToGp());
    }
}

package org.genepattern.server.executor.drm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.junitutil.DbUtil;
import org.genepattern.server.executor.drm.dao.JobRunnerJob;
import org.genepattern.server.executor.events.JobCompletedEvent;
import org.genepattern.server.executor.events.JobStartedEvent;
import org.genepattern.server.job.status.Status;
import org.junit.Before;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class TestJobExecutor {
    private EventBus eventBus;
    private JobExecutor jobExecutor;
    private DrmLookup jobLookupTable;
    private final Integer gpJobNo=1;
    private final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    private DrmJobStatus jobStatus;
    
    private JobStartedListener jobStartedListener;
    static class JobStartedListener {
        private int count=0;
        @Subscribe
        public void recordJobStartedEvent(JobStartedEvent evt) {
            ++count;
        }
        public int getCount() {
            return count;
        }
    }
    private JobCompletedListener jobCompletedListener;
    static class JobCompletedListener {
        private int count=0;
        private Status jobStatus=null;
        @Subscribe
        public void onJobCompletedEvent(JobCompletedEvent evt) {
            ++count;
            this.jobStatus=evt.getJobStatus();
        }
        public int getCount() {
            return count;
        }
        public Status getJobStatus() {
            return jobStatus;
        }
    }
    
    @Before
    public void setUp() throws Exception {
        DbUtil.initDb();
        List<DrmJobRecord> runningDrmJobs=Collections.emptyList();
        jobLookupTable=mock(DrmLookup.class);
        when(jobLookupTable.getRunningDrmJobRecords()).thenReturn(runningDrmJobs);
        
        eventBus=new EventBus();
        jobStartedListener=new JobStartedListener();
        jobCompletedListener=new JobCompletedListener();
        eventBus.register(jobStartedListener);
        eventBus.register(jobCompletedListener);
        
        jobExecutor=new JobExecutor(eventBus);
        jobExecutor.setJobLookupTable(jobLookupTable);
        jobExecutor.start();
        
        jobStatus=mock(DrmJobStatus.class);
    }

    /**
     * make sure to fire job started event after it transitions from PENDING -> RUNNING
     */
    @Test
    public void fireJobStartedEvent() {
        assertEquals("before events, count", 0, jobStartedListener.getCount());
        // cause a new JobStartedEvent to occur
        Status prevStatus=null;
        Status newStatus=null;
        jobExecutor.fireJobStartedEvent(cleLsid, prevStatus, newStatus);
        assertEquals("after event, count", 1, jobStartedListener.getCount());
    }
    
    /**
     * Make sure to fire job started event after it transitions from PENDING -> RUNNING
     */
    @Test
    public void fireJobStartedEvent_onUpdateStatus() {
        assertEquals("before events, count", 0, jobStartedListener.getCount());
        
        // update status, presumably from null to PENDING
        when(jobStatus.getJobState()).thenReturn(DrmJobState.QUEUED);
        jobExecutor.updateStatus(gpJobNo, cleLsid, jobStatus);
        assertEquals("after PENDING event, count", 0, jobStartedListener.getCount());
        
        // cause a new JobStartedEvent to occur
        when(jobStatus.getJobState()).thenReturn(DrmJobState.RUNNING);
        jobExecutor.updateStatus(gpJobNo, cleLsid, jobStatus);
        
        assertEquals("after RUNNING event, count", 1, jobStartedListener.getCount());
    }
    
    @Test 
    public void fireJobCompletedEvent() {
        JobRunnerJob existingJobRunnerJob=mock(JobRunnerJob.class);
        assertEquals("before events", 0, jobCompletedListener.getCount());
        when(jobStatus.getJobState()).thenReturn(DrmJobState.RUNNING);
        jobExecutor.updateStatus(gpJobNo, cleLsid, jobStatus);
        assertEquals("after RUNNING event, count", 0, jobCompletedListener.getCount());
        when(jobStatus.getJobState()).thenReturn(DrmJobState.DONE);
        jobExecutor.updateStatus(gpJobNo, cleLsid, existingJobRunnerJob, jobStatus);
        assertEquals("after DONE event, count", 1, jobCompletedListener.getCount());
        assertEquals("expected jobState", DrmJobState.DONE, jobCompletedListener.getJobStatus().getJobState());
    }

}

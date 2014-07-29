package org.genepattern.server.executor.drm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.server.executor.events.JobStartedEvent;
import org.genepattern.server.job.status.Status;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

public class TestJobExecutor {
    EventBus eventBus;
    JobExecutor jobExecutor;
    DrmLookup jobLookupTable;
    private final String cleLsid="urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00002:2";
    
    JobStartedListener jobStartedListener;
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
    
    @Before
    public void setUp() {
        List<DrmJobRecord> runningDrmJobs=Collections.emptyList();
        jobLookupTable=mock(DrmLookup.class);
        when(jobLookupTable.getRunningDrmJobRecords()).thenReturn(runningDrmJobs);
        
        eventBus=new EventBus();
        jobStartedListener=new JobStartedListener();
        eventBus.register(jobStartedListener);
        
        jobExecutor=new JobExecutor(eventBus);
        jobExecutor.setJobLookupTable(jobLookupTable);
        jobExecutor.start();
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
     * TODO: implement this test, need some way to mock the DB calls that are embedded in the updateStatus call
     * make sure to fire job started event after it transitions from PENDING -> RUNNING
     */
    @Ignore @Test
    public void fireJobStartedEvent_onUpdateStatus() {
        DrmJobRecord jobRecord=mock(DrmJobRecord.class);
        DrmJobStatus jobStatus=mock(DrmJobStatus.class);
        jobExecutor.updateStatus(jobRecord, jobStatus);
        
        assertEquals("before events, count", 0, jobStartedListener.getCount());
        // cause a new JobStartedEvent to occur
        Status prevStatus=null;
        Status newStatus=null;
        jobExecutor.fireJobStartedEvent(cleLsid, prevStatus, newStatus);
        assertEquals("after event, count", 1, jobStartedListener.getCount());
    }

}

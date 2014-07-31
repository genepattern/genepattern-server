package org.genepattern.server.executor.drm.dao;

import static org.junit.Assert.*;

import java.util.Date;

import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestJobRunnerJob {
    @Before
    public void setUp() {
    }

    /**
     * When jobStatus.queueId is null, make sure to keep the existing queueId.
     */
    @Test
    public void preserveExistingQueueId() {
        JobRunnerJob existing=new JobRunnerJob.Builder()
            .queueId("genepattern_long")
        .build();
        final String extJobId="EXT_0";
        DrmJobStatus jobStatus=new DrmJobStatus.Builder(extJobId, DrmJobState.QUEUED)
            .queueId(null)
        .build();
        
        JobRunnerJob update = new JobRunnerJob.Builder(existing).drmJobStatus(jobStatus).build();
        assertEquals("expected queueId", "genepattern_long", update.getQueueId());
    }
    
    @Ignore @Test
    public void preserveExistingSubmitTime() {
        Date submitTime=new DateTime("2014-07-09T11:39:51").toDate();
        JobRunnerJob existing=new JobRunnerJob.Builder()
            .submitTime(submitTime)
        .build();
        final String extJobId="EXT_0";
        DrmJobStatus jobStatus=new DrmJobStatus.Builder(extJobId, DrmJobState.QUEUED)
            .submitTime(null)
        .build();
        
        JobRunnerJob update = new JobRunnerJob.Builder(existing).drmJobStatus(jobStatus).build();
        assertEquals("expected submitTime", submitTime, update.getSubmitTime());
    }
}

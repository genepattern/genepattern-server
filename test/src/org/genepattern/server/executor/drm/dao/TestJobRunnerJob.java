/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.drm.dao;

import static org.junit.Assert.*;

import java.util.Date;

import org.genepattern.drm.DrmJobState;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.Memory;
import org.genepattern.drm.Walltime;
import org.joda.time.DateTime;
import org.junit.Before;
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
    
    @Test
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

    @Test
    public void preserveExistingStartTime() {
        Date startTime=new DateTime("2014-07-09T11:39:51").toDate();
        JobRunnerJob existing=new JobRunnerJob.Builder()
            .startTime(startTime)
        .build();
        final String extJobId="EXT_0";
        DrmJobStatus jobStatus=new DrmJobStatus.Builder(extJobId, DrmJobState.QUEUED)
            .startTime(null)
        .build();
        
        JobRunnerJob update = new JobRunnerJob.Builder(existing).drmJobStatus(jobStatus).build();
        assertEquals("expected startTime", startTime, update.getStartTime());
    }

    @Test
    public void preserveExistingEndTime() {
        Date endTime=new DateTime("2014-07-09T11:39:51").toDate();
        JobRunnerJob existing=new JobRunnerJob.Builder()
            .endTime(endTime)
        .build();
        final String extJobId="EXT_0";
        DrmJobStatus jobStatus=new DrmJobStatus.Builder(extJobId, DrmJobState.QUEUED)
            .endTime(null)
        .build();
        
        JobRunnerJob update = new JobRunnerJob.Builder(existing).drmJobStatus(jobStatus).build();
        assertEquals("expected endTime", endTime, update.getEndTime());
    }
    
    @Test
    public void preserveRequestedMemory() {
        final Memory requestedMemory=Memory.fromString("2 Gb");
        final JobRunnerJob existing=new JobRunnerJob.Builder()
            .requestedMemory(requestedMemory)
        .build();
        
        final JobRunnerJob update = new JobRunnerJob.Builder(existing).build();
        assertEquals("requestedMemory", (Long) requestedMemory.getNumBytes(), update.getRequestedMemory());
    }
    
    @Test
    public void preserveRequestedCpuCount() {
        final Integer cpuCount=4;
        final JobRunnerJob existing=new JobRunnerJob.Builder()
            .requestedCpuCount(cpuCount)
        .build();
        
        final JobRunnerJob update = new JobRunnerJob.Builder(existing).build();
        assertEquals("requestedCpuCount", cpuCount, update.getRequestedCpuCount());
    }

    @Test
    public void preserveRequestedNodeCount() {
        final Integer nodeCount=4;
        final JobRunnerJob existing=new JobRunnerJob.Builder()
            .requestedNodeCount(nodeCount)
        .build();
        
        final JobRunnerJob update = new JobRunnerJob.Builder(existing).build();
        assertEquals("requestedNodeCount", nodeCount, update.getRequestedNodeCount());
    }

    @Test
    public void preserveRequestedWalltime() {
        final String walltimeStr="7-00:00:00";
        final JobRunnerJob existing=new JobRunnerJob.Builder()
            .requestedWalltime(walltimeStr)
        .build();
        
        final JobRunnerJob update = new JobRunnerJob.Builder(existing).build();
        assertEquals("requestedWalltime", walltimeStr, update.getRequestedWalltime());
    }
    
    @Test
    public void preserveRequestedQueue() {
        final String queue="my_queue";
        final JobRunnerJob existing=new JobRunnerJob.Builder()
            .requestedQueue(queue)
        .build();
        
        final JobRunnerJob update = new JobRunnerJob.Builder(existing).build();
        assertEquals("requestedQueue", queue, update.getRequestedQueue());
    }

}

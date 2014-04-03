package org.genepattern.server.executor.drm;

import java.util.List;

import org.genepattern.drm.DrmJobRecord;
import org.genepattern.drm.DrmJobStatus;
import org.genepattern.drm.DrmJobSubmission;

/**
 * API calls for recording the lookup table between GenePattern jobIds and external jobIds.
 * 
 * This is so the lookup table can be persisted to an external system such
 * as a database or a file system.
 * 
 * You may need to create multiple instances of this interface, one for each specific JobRunner
 * instance. Otherwise, there will be no way to return the list of running jobs on a per-JobRunner
 * basis.
 * 
 * @author pcarr
 *
 */
public interface DrmLookup {
    /**
     * Get the list of drm jobIds from the lookup table for which
     * the job status has not been recorded as completed.
     * Usually this corresponds to running jobs, but it can also include
     * completed jobs which have not yet been sent a callback to the gp server.
     * 
     * @return
     */
    List<DrmJobRecord> getRunningDrmJobRecords();

    /**
     * Get the DrmJobRecord for the given GenePattern job id.
     * @param jobInfo
     * @return
     */
    DrmJobRecord lookupJobRecord(final Integer gpJobNo);
    
    /**
     * Insert a record into the DB for a given GenePattern job;
     * this allows us to store a gp job id which has not yet been mapped to 
     * a drm job id.
     * 
     * @param jobInfo
     */
    void insertJobRecord(final DrmJobSubmission jobSubmission);
    
    /**
     * Update the record for a job.
     * 
     * @param gpJobId, the GenePattern job id.
     * @param drmJobStatus, the current status as reported by the external JobRunner.
     */
    //void updateJobStatus(final Integer gpJobNo, final DrmJobStatus drmJobStatus);
    void updateJobStatus(final DrmJobRecord drmJobRecord, final DrmJobStatus drmJobStatus);
}
package org.genepattern.server.executor.drm;

import java.util.List;

import org.genepattern.webservice.JobInfo;

/**
 * API calls for recording the lookup table between GenePattern jobIds
 * and drm jobIds.
 * 
 * This is so the lookup table can be persisted to an external system such
 * as a database or a file system.
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
    List<String> getRunningDrmJobIds();

    /**
     * Get the drmJob id for the given GenePattern job.
     * @param jobInfo
     * @return
     */
    String lookupDrmJobId(final JobInfo jobInfo);

    /**
     * Get the GenePattern jobId for the given drm job.
     * @param drmJobId
     * @return
     */
    String lookupGpJobId(final String drmJobId);

    /**
     * Initialize a record into the DB for a given GenePattern job;
     * this allows us to store a gp job id which has not yet been mapped to 
     * a drm job id.
     * 
     * @param jobInfo
     */
    void initDrmRecord(final JobInfo jobInfo);

    /**
     * Update the lookup table to map the given drm jobId to the given GenePattern jobId.
     * @param drmJobId
     * @param jobInfo
     */
    void recordDrmId(final String drmJobId, final JobInfo jobInfo);
}
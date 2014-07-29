package org.genepattern.server.executor.events;

import org.genepattern.server.job.status.Status;

/**
 * Superclass for all DrmJobState change events in GenePattern.
 * 
 * From the Google Guava One-Minute Guide,
 *     "Events are automatically dispatched to listeners of any supertype, 
 *      allowing listeners for interface types or "wildcard listeners" for Object."
 * @author pcarr
 *
 */
public class JobStatusEvent {
    protected final String lsid;
    protected final Status jobStatus;
    
    public JobStatusEvent(final String lsid, final Status jobStatus) {
        this.lsid=lsid;
        this.jobStatus=jobStatus;
    }
    
    /**
     * Get the lsid of the job.
     */
    public String getJobLsid() {
        return lsid;
    }
    
    /**
     * Get the current status of the job.
     * @return
     */
    public Status getJobStatus() {
        return jobStatus;
    }

}

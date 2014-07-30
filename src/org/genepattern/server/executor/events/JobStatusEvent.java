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
    protected final String taskLsid;
    protected final Status jobStatus;
    
    public JobStatusEvent(final String taskLsid, final Status jobStatus) {
        this.taskLsid=taskLsid;
        this.jobStatus=jobStatus;
    }
    
    /**
     * Get the lsid of the job.
     */
    public String getTaskLsid() {
        return taskLsid;
    }
    
    /**
     * Get the current status of the job.
     * @return
     */
    public Status getJobStatus() {
        return jobStatus;
    }

}

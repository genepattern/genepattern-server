package org.genepattern.drm;

/**
 * Based on the DRMAA state model (http://www.ogf.org/documents/GFD.194.pdf).
 * Implemented as a hierarchical enum.
 * @author pcarr
 *
 */
public enum DrmJobState {
    /** The job status cannot be determined. This is a permanent issue, not being solvable by asking again for the job state. */
    UNDETERMINED(null),
    /** The outer status of a GenePattern job. */
    GP_STATUS(null),
      /** The job is pending in the GenePattern queue, it has not been submitted to an external queuing system */
      GP_PENDING(GP_STATUS, "Pending in the GenePattern queue, it has not been submitted to an external queuing system"),
      /** The job has been submitted to the external queuing system */
      GP_PROCESSING(GP_STATUS, "Submitted from GenePattern to the external queuing system"),
      /** The job has completed and it's status is recorded in the GenePattern database */
      GP_FINISHED(GP_STATUS, "Completed and status is recorded in the GenePattern database"),
    /** Outer state for a job which has been added to the queue, use one of the nested states.  */
    IS_QUEUED(null),
      /** The job is queued or being scheduled and executed. */
      QUEUED(IS_QUEUED, "The job is queued or being scheduled and executed"),
      /** The job has been placed on hold by the system, the administrator, or the submitting user. */
      QUEUED_HELD(IS_QUEUED, "The job has been placed on hold by the system, the administrator, or the submitting user"),
    /** Outer state for a job which has been started, use one of the nested states when creating new job status instances. */
    STARTED(null),
      /** The job is running on an execution host. */
      RUNNING(STARTED, "The job is running on an execution host"),
      /** The job has been suspended by the user, the system or the administrator. */
      SUSPENDED(STARTED, "The job has been suspended by the user, the system or the administrator"),
      /** The job was re-queued by the system, and is eligible to run. */
      REQUEUED(STARTED, "The job was re-queued by the system, and is eligible to run"),
      /** The job was re-queued by the system, and is currently placed on hold by the system, the administrator, or the submitting user. */
      REQUEUED_HELD(STARTED, "The job was re-queued by the system, and is currently placed on hold by the system, the administrator, or the submitting user"),
    /** Outer state for a completed job, use one of the nested states. */
    TERMINATED(null),
      /** The job finished without an error. */
      DONE(TERMINATED, "The job finished without an error"),
      /** The job exited abnormally before finishing, for example if the command exited with a non-zero exit code or by exceeding a resource usage limit. */
      FAILED(TERMINATED, "The job exited abnormally before finishing, for example if the command exited with a non-zero exit code or by exceeding a resource usage limit"),
        /** The job was cancelled by the user before entering the running state. */
        ABORTED(FAILED, "The job was cancelled by the user before entering the running state"),
        /** The job was cancelled by the user after entering the running state. */
        CANCELLED(FAILED, "The job was cancelled by the user after entering the running state")
    ;
    
    private final DrmJobState parent;
    private final String description;
    private DrmJobState(DrmJobState parent) {
        this.parent=parent;
        this.description=name();
    }
    private DrmJobState(DrmJobState parent, String description) {
        this.parent=parent;
        this.description=description;
    }

    /**
     * So that DONE.is(TERMINATED) == true ...
     * So that UNDETERMINED.is(STARTED) == false ...
     * 
     * @param other
     * @return
     */
    public boolean is(DrmJobState other) {
        if (other==null) {
            return false;
        }
        for (DrmJobState jobState = this; jobState != null; jobState = jobState.parent) {
            if (other==jobState) {
                return true;
            }
        }
        return false;
    }
    
    public String getDescription() {
        return description;
    }
}
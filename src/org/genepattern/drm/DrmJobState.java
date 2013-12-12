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
    /** Outer state, use one of the nested states.  */
    IS_QUEUED(null),
      /** The job is queued or being scheduled and executed. */
      QUEUED(IS_QUEUED),
      /** The job has been placed on hold by the system, the administrator, or the submitting user. */
      QUEUED_HELD(IS_QUEUED),
    /** Outer state for a job which has been started, use one of the nested states when creating new job status instances. */
    STARTED(null),
      /** The job is running on an execution host. */
      RUNNING(STARTED),
      /** The job has been suspended by the user, the system or the administrator. */
      SUSPENDED(STARTED),
      /** The job was re-queued by the system, and is eligible to run. */
      REQUEUED(STARTED),
      /** The job was re-queued by the system, and is currently placed on hold by the system, the administrator, or the submitting user. */
      REQUEUED_HELD(STARTED),
    /** Outer state for a completed job, use one of the nested states. */
    TERMINATED(null),
      /** The job finished without an error. */
      DONE(TERMINATED),
      /** The job exited abnormally before finishing, for example if the command exited with a non-zero exit code or by exceeding a resource usage limit. */
      FAILED(TERMINATED),
        /** The job was cancelled by the user before entering the running state. */
        ABORTED(FAILED),
        /** The job was cancelled by the user after entering the running state. */
        CANCELLED(FAILED)
    ;
    
    private final DrmJobState parent;
    private DrmJobState(DrmJobState parent) {
        this.parent=parent;
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
}
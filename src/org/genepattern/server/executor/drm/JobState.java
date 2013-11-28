package org.genepattern.server.executor.drm;

/**
 * Based on the DRMAA state model (http://www.ogf.org/documents/GFD.194.pdf).
 * Implemented as a hierarchical enum.
 * @author pcarr
 *
 */
public enum JobState {
    /** The job status cannot be determined. This is a permanent issue, not being solvable by asking again for the job state. */
    UNDETERMINED(null),
    IS_QUEUED(null),
      /** The job is queued for being scheduled and executed. */
      QUEUED(IS_QUEUED),
      /** The job has been placed on hold by the system, the administrator, or the submitting user. */
      QUEUED_HELD(IS_QUEUED),
    STARTED(null),
      /** The job is running on an execution host. */
      RUNNING(STARTED),
      /** The job has been suspended by the user, the system or the administrator. */
      SUSPENDED(STARTED),
      /** The job was re-queued by the DRM system, and is eligible to run. */
      REQUEUED(STARTED),
      /** The job was re-queued by the DRM system, and is currently placed on hold by the system, the administrator, or the submitting user. */
      REQUEUED_HELD(STARTED),
    TERMINATED(null),
      /** The job finished without an error. */
      DONE(TERMINATED),
      /** The job exited abnormally before finishing. */
      FAILED(TERMINATED)
    ;
    
    private final JobState parent;
    private JobState(JobState parent) {
        this.parent=parent;
    }

    /**
     * So that DONE.is(TERMINATED) == true ...
     * So that UNDETERMINED.is(STARTED) == false ...
     * 
     * @param other
     * @return
     */
    public boolean is(JobState other) {
        if (other==null) {
            return false;
        }
        for (JobState jobState = this; jobState != null; jobState = jobState.parent) {
            if (other==jobState) {
                return true;
            }
        }
        return false;
    }
}
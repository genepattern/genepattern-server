package org.genepattern.server.executor.events;

import org.genepattern.server.job.status.Status;


public class JobStatusChangedEvent extends JobStatusEvent {
    protected final Status prevJobStatus;

    public JobStatusChangedEvent(Status prevJobStatus, Status curJobStatus) {
        super(curJobStatus);
        this.prevJobStatus=prevJobStatus;
    }

    /**
     * Can be null if the previous status was unknown.
     * @return
     */
    public Status getPrevJobStatus() {
        return prevJobStatus;
    }
}

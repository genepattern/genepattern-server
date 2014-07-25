package org.genepattern.server.executor.events;

import org.genepattern.server.job.status.Status;

public class JobStatusUpdateEvent extends JobStatusEvent {
    public JobStatusUpdateEvent(Status jobStatus) {
        super(jobStatus);
    }
}

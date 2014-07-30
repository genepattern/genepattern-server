package org.genepattern.server.executor.events;

import org.genepattern.server.job.status.Status;

/**
 * Fire this event after a job has completed and it's output files have been recorded to the database.
 * @author pcarr
 *
 */
public class GpJobRecordedEvent extends JobStatusEvent {

    public GpJobRecordedEvent(final String lsid, final Status jobStatus) {
        super(lsid, jobStatus);
    }

}

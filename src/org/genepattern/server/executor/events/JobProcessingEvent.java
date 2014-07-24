package org.genepattern.server.executor.events;

import javax.swing.event.ChangeEvent;

/**
 * Event to be called when a job is completed
 *
 * @author Thorin Tabor
 */
public class JobProcessingEvent extends ChangeEvent {

    /**
     * Constructs a JobCompletionEvent object.
     *
     * @param jobId - the job ID of the job being completed
     */
    public JobProcessingEvent(Integer jobId) {
        super(jobId);
    }
}

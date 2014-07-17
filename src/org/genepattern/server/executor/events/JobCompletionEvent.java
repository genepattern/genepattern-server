package org.genepattern.server.executor.events;

import javax.swing.event.ChangeEvent;

/**
 * Event to be called when a job is completed
 *
 * @author Thorin Tabor
 */
public class JobCompletionEvent extends ChangeEvent {

    /**
     * Constructs a JobCompletionEvent object.
     *
     * @param jobId - the job ID of the job being completed
     */
    public JobCompletionEvent(Integer jobId) {
        super(jobId);
    }
}

/*******************************************************************************
 * Copyright (c) 2003-2022 Regents of the University of California and Broad Institute. All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.events;

import org.genepattern.server.job.status.Status;


public class JobStatusChangedEvent extends JobStatusEvent {
    protected final Status prevJobStatus;

    public JobStatusChangedEvent(final String lsid, final Status prevJobStatus, final Status curJobStatus) {
        super(lsid, curJobStatus);
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

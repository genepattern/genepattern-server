/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.events;

import org.genepattern.server.job.status.Status;


/**
 * This event is fired after the job is started on the external queuing system,
 * presumably when it transitioned from the PENDING to the RUNNING state.
 * Can also be fired if it goes directly to the RUNNING state after adding it to the external
 * queue.
 * 
 * @author pcarr
 *
 */
public class JobStartedEvent extends JobStatusChangedEvent {
    public JobStartedEvent(final String lsid, final Status prevJobStatus, final Status curJobStatus) {
        super(lsid, prevJobStatus, curJobStatus);
    }
}

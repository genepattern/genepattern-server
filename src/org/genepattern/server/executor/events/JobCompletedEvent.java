/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.events;

import org.genepattern.server.job.status.Status;

/**
 * This event is fired after the job has finished running on the queue.
 * @author pcarr
 *
 */
public class JobCompletedEvent extends JobStatusChangedEvent  {
    public JobCompletedEvent(final String lsid, final Status prevJobStatus, final Status curJobStatus) {
        super(lsid, prevJobStatus, curJobStatus);
    }
}

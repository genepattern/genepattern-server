/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.events;

import org.genepattern.server.job.status.Status;

/**
 * Fire this event after a job is added to the internal genepattern queue.
 * @author pcarr
 *
 */
public class GpJobAddedEvent extends JobStatusEvent {

    public GpJobAddedEvent(final String lsid, final Status jobStatus) {
        super(lsid, jobStatus);
    }

}

/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.listener;


import org.apache.log4j.Logger;
import org.genepattern.server.executor.events.GpJobAddedEvent;

import com.google.common.eventbus.Subscribe;

/**
 * Listen for job status events in the genepattern system.
 * @author pcarr
 *
 */
public class GpJobStatusListener {
    private static final Logger log = Logger.getLogger(GpJobStatusListener.class);

    // Uncomment this method to listen to all events
//    @Subscribe
//    public void logEvent(Object event) {
//        log.debug("event: "+event);
//    }
    
    @Subscribe
    public void onGpJobAddedEvent(GpJobAddedEvent event) {
        log.debug("job added: "+event.getTaskLsid()+", "+event.getJobStatus());
    }

}

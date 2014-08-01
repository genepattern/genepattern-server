package org.genepattern.server.executor.listener;


import org.apache.log4j.Logger;

import com.google.common.eventbus.Subscribe;

/**
 * Listen for job status events in the genepattern system.
 * @author pcarr
 *
 */
public class GpJobStatusListener {
    private static final Logger log = Logger.getLogger(GpJobStatusListener.class);

    @Subscribe
    public void on(Object any) {
        log.debug("event: "+any);
    }

}

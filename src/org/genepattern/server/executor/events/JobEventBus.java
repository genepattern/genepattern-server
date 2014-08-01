package org.genepattern.server.executor.events;

import com.google.common.eventbus.EventBus;

import org.apache.log4j.Logger;
import org.genepattern.server.congestion.CongestionListener;
import org.genepattern.server.executor.listener.GpJobStatusListener;

/**
 * A pub-sub event bus for job execution-related events
 *
 * @author Thorin Tabor
 */
public class JobEventBus {
    private static final Logger log = Logger.getLogger(JobEventBus.class);

    private static EventBus eventBus = initEventBus();
    private static EventBus initEventBus() {
        EventBus eventBus = new EventBus();
        eventBus.register(new CongestionListener());
        if (log.isDebugEnabled()) {
            eventBus.register(new GpJobStatusListener());
        }
        return eventBus;
    }

    /**
     * Global singleton EventBus for all GenePattern event handling.
     * @return
     */
    public static EventBus instance() {
        return eventBus;
    }
}

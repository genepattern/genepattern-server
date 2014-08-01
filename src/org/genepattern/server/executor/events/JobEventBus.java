package org.genepattern.server.executor.events;

import com.google.common.eventbus.EventBus;
import org.genepattern.server.congestion.CongestionListener;

/**
 * A pub-sub event bus for job execution-related events
 *
 * @author Thorin Tabor
 */
public class JobEventBus {
    private static EventBus eventBus = initEventBus();
    private static EventBus initEventBus() {
        EventBus eventBus = new EventBus();
        eventBus.register(new CongestionListener());
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

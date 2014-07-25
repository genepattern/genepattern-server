package org.genepattern.server.executor.events;

import com.google.common.eventbus.EventBus;
import org.genepattern.server.congestion.CongestionListener;

/**
 * A pub-sub event bus for job execution-related events
 *
 * @author Thorin Tabor
 */
public class JobEventBus {
    private static EventBus singleton = null;

    public static EventBus instance() {
        if (singleton == null) {
            singleton = new EventBus();

            // Lazily initialize listener
            CongestionListener.init();
        }

        return singleton;
    }
}

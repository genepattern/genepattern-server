package org.genepattern.server.purger;

/**
 * Interface for managing the background process(es) for purging user data from the system.
 * 
 * @author pcarr
 *
 */
public interface Purger {
    /**
     * Usually called on server restart, for example from the StartupServlet.
     */
    void start();

    /**
     * Usually called after a change to the configuration, for example from the ServerSettingsBean.
     */
    void restart();

    /**
     * Usually called when shutting down the GP server, for example from the StartupServlet#destroy method.
     */
    void stop();
}

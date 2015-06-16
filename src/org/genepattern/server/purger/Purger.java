/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.purger;

/**
 * Interface for managing the background process(es) for purging user data from the system.
 * 
 * @author pcarr
 *
 */
public interface Purger {
    public static final String PROP_PURGE_JOBS_AFTER = "purgeJobsAfter";
    public static final int PURGE_JOBS_AFTER_DEFAULT = -1;
    public static final String PROP_PURGE_TIME = "purgeTime";
    public static final String PURGE_TIME_DEFAULT = "23:00";

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

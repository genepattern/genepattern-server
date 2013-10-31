package org.genepattern.server.purger;

import org.apache.log4j.Logger;
import org.genepattern.server.purger.impl02.JobPurger02;

/**
 * Default implementation of the file purger, circa GP <= 3.7.2.
 * Refactored from method calls in the StartupServlet.
 * 
 * @author pcarr
 *
 */
public class DefaultPurgerImpl02 implements Purger {
    private static final Logger log = Logger.getLogger(DefaultPurgerImpl02.class);

    @Override
    public void start() {
        log.info("Starting purger...");
        JobPurger02.startJobPurger();
        log.info("started");
    }

    @Override
    public void restart() {
        log.info("Restarting purger...");
        start();
    }

    @Override
    public void stop() {
        log.info("Stopping purger...");
        JobPurger02.stopJobPurger();
    }

}

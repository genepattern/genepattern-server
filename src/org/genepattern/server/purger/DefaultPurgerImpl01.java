package org.genepattern.server.purger;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.genepattern.server.process.JobPurger;

/**
 * Default implementation of the file purger, circa GP <= 3.7.2.
 * Refactored from method calls in the StartupServlet.
 * 
 * @author pcarr
 *
 */
public class DefaultPurgerImpl01 implements Purger {
    private static final Logger log = Logger.getLogger(DefaultPurgerImpl01.class);
    private Thread thread;

    @Override
    public void start() {
        log.info("Starting purger...");
        final Properties props=System.getProperties();
        this.thread=JobPurger.startJobPurger(props);
        log.info("started");
    }

    @Override
    public void restart() {
        log.info("Restarting purger...");
        start();
    }

    @Override
    public void stop() {
        //no-op method, the JobPurger creates a daemon thread which will naturally be destroyed
        //when the JVM is terminated.
        log.info("Stopping purger...");
        if (thread == null) {
            log.debug("thread is null");
        }
    }

}

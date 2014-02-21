/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2011) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.purger.impl02;

import java.util.Date;
import java.util.Timer;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.process.JobPurgerUtil;
import org.genepattern.server.purger.Purger;

/**
 * Refactored from original (GP <= 3.7.2 JobPurger implementation) and updated to
 * allow for per-user and per-group customization of the 'purgeJobsAfter' property.
 * The integer value for the purgeJobsAfter determines the number of days to keep jobResults and jobInput files
 * before deleting them from the server.
 * 
 * @author pcarr
 *
 */
public class JobPurger02 {
    private static Logger log = Logger.getLogger(JobPurger02.class);

    //singleton
    private static final JobPurger02 instance = new JobPurger02();

    public static void startJobPurger() {
        instance.start();
    }
    
    public static void stopJobPurger() {
        instance.stop();
    }

    private Timer timer = null;
    private Purger02 purger02=null;

    //private constructor, use singleton instance
    private JobPurger02() {
    }
    
    private void start() {
        log.debug("starting purger");
        if (purger02 != null) {
            purger02.cancel();
        } 
        if (timer != null) {
            timer.cancel();
        }
        timer=new Timer(true);
        final Date now=new Date();
        final GpContext serverContext=GpContext.getServerContext();
        final String purgeTime=ServerConfigurationFactory.instance().getGPProperty(serverContext, Purger.PROP_PURGE_TIME, Purger.PURGE_TIME_DEFAULT);
        final Date nextPurgeTime=JobPurgerUtil.getNextPurgeTime(now, purgeTime);
        final long MILLISECONDS_IN_A_DAY = 24 * 60 * 60 * 1000;
        final Purger02 purger02 = new Purger02();
        timer.scheduleAtFixedRate(purger02, nextPurgeTime, MILLISECONDS_IN_A_DAY);
    }
    
    private void stop() {
        if (purger02 != null) {
            purger02.cancel();
        }
        if (timer != null) {
            timer.cancel();
        }
    }
}

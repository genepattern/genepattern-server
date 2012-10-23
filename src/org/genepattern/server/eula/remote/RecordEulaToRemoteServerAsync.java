package org.genepattern.server.eula.remote;

import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;

/**
 * POST eula to remote server, asynchronously, by using a background thread.
 * 
 * @author pcarr
 *
 */
public class RecordEulaToRemoteServerAsync implements RecordEula {
    final static private Logger log = Logger.getLogger(RecordEulaToRemoteServerAsync.class);

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        log.debug("creating new thread...");
        
        //create a new thread for each POST
        Runnable r = new Runnable() {

            //@Override
            public void run() {
                log.debug("running thread...");
                RecordEulaToRemoteServer record = new RecordEulaToRemoteServer();
                try {
                    record.recordLicenseAgreement(userId, eula);
                }
                catch (Exception e) {
                    //ignore
                }
            }
        };        
        Thread t = new Thread(r);
        log.debug("starting thread...");
        t.start();
    }

    //@Override
    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        throw new Exception("Not implemented!");
    }

    //@Override
    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        throw new Exception("Not implemented!");
    }

}

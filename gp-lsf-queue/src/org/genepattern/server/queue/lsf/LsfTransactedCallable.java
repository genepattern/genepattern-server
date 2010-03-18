package org.genepattern.server.queue.lsf;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.mit.broad.core.Main;
import edu.mit.broad.core.lsf.LsfJob;
import edu.mit.broad.core.lsf.LsfWrapper;

/**
 * Based on code in BroadCore for managing hibernate transactions.
 */
public class LsfTransactedCallable implements Callable<LsfJob> {
    private static Logger log = Logger.getLogger(LsfTransactedCallable.class);
    private LsfJob lsfJobIn = null;
    private LsfJob lsfJobOut = null;
    
    public LsfTransactedCallable(LsfJob lsfJob) {
        this.lsfJobIn = lsfJob;
    }

    /**
     * Starts a transaction, invokes runInTransaction and then commits or
     * rolls back the transaction as appropriate.
     */
    public final LsfJob call() {
        Transaction tx = null;
        Session session = null;
        try {
            try {
                // First, try using the "current" session, which works best if you are not using JTA transactions
                //tx = Main.getInstance().getHibernateSession().beginTransaction();
                Session mySession = Main.getInstance().getHibernateSession();
                if (mySession == null) {
                    log.error("Main.getInstance().getHibernateSession returns null!");
                }
                if (mySession != null) {
                    tx = mySession.beginTransaction();
                }
            }
            catch (Throwable the) {
                // But if that blows up...then try using a temporary session to initiate
                // the transaction, then closing that session so it doesn't leak...
                log.debug("Initial attempt to get transaction threw an exception: "+the.getLocalizedMessage(), the);
                session = Main.getInstance().getHibernateSessionFactory().openSession();
                tx = session.beginTransaction();
                session.close();
            }
            runInTransaction();
            tx.commit();
        }
        catch (Throwable t) {
            log.error("Exception running transacted code.", t);

            try { if (tx != null) {
                tx.rollback();
            } }
            catch (Throwable t2) {
                log.fatal("Error while attempting to rollback transaction.", t2);
            }
        }
        return lsfJobOut;
    }

    /**
     * Override this method to do something different inside of the transaction.
     */
    public void runInTransaction() throws Exception {
        LsfWrapper lsfWrapper = new LsfWrapper();
        lsfJobOut = lsfWrapper.dispatchLsfJob(lsfJobIn);
    }
}
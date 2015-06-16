/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.executor.lsf;

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
    public final LsfJob call() throws LsfTransactionException, LsfJobSubmissionException {
        Transaction tx = null;
        Session session = null;
        try {
            try {
                // First, try using the "current" session, which works best if you are not using JTA transactions
                //tx = Main.getInstance().getHibernateSession().beginTransaction();
                Session mySession = Main.getInstance().getHibernateSession();
                if (mySession == null) {
                    throw new LsfTransactionException("Main.getInstance().getHibernateSession returns null!");
                } 
                tx = mySession.beginTransaction();
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
        catch (LsfJobSubmissionException t) {
            //separate DB connection errors from job submission errors
            log.error("Exception running transacted code.", t);
            try { 
                if (tx != null) {
                    tx.rollback();
                } 
            }
            catch (Throwable t2) {
                log.fatal("Error while attempting to rollback transaction.", t2);
            }
            throw t;
        }
        catch (Throwable t) {
            throw new LsfTransactionException("Exception running transacted code.", t);
        }
        return lsfJobOut;
    }

    /**
     * Override this method to do something different inside of the transaction.
     */
    public void runInTransaction() throws LsfJobSubmissionException {
        try {
            LsfWrapper lsfWrapper = new LsfWrapper();
            lsfJobOut = lsfWrapper.dispatchLsfJob(lsfJobIn);
        }
        catch (Throwable t) {
            throw new LsfJobSubmissionException(t);
        }
    }

    /**
     * For errors related to DB connections, transactions, updates, et cetera. 
     * @author pcarr
     */
    public static class LsfTransactionException extends Exception {
        public LsfTransactionException(String message) {
            super(message);
        }
        public LsfTransactionException(Throwable t) {
            this("DB error related to LSF integration: "+t.getLocalizedMessage(), t);
        }
        public LsfTransactionException(String message, Throwable t) {
            super(message,t);
        }
    }
    
    /**
     * For errors occurring while attempting to submit a job to the LSF queue.
     * @author pcarr
     *
     */
    public static class LsfJobSubmissionException extends Exception {
        public LsfJobSubmissionException(Throwable t) {
            super("Error while submitting job to the LSF queue: "+t.getLocalizedMessage(), t);
        }        
    }


}
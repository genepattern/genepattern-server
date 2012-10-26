package org.genepattern.server.eula;

import java.util.Date;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.dao.RecordEulaToDb;
import org.genepattern.server.eula.remote.PostToBroad;
import org.genepattern.server.eula.remote.RecordEulaToRemoteServerAsync;


/**
 * The default method for recording EULA in the local GP server.
 * 
 * It saves a local record, and adds an entry to the queue, for remote record.
 * This is done in a single transaction, so that if we are not able to add the record to the remote queue,
 * the entire transaction will fail.
 * 
 * Note: the actual remote POST is done asynchronously in a different thread.
 * 
 * @author pcarr
 *
 */
public class RecordEulaDefault implements RecordEula {
    final static private Logger log = Logger.getLogger(RecordEulaDefault.class);
    private RecordEulaToDb local;
    private RecordEulaToRemoteServerAsync remote;
    
    public RecordEulaDefault() {
        local=new RecordEulaToDb();
        remote=new RecordEulaToRemoteServerAsync();
    }

    //@Override
    public void recordLicenseAgreement(final String userId, final EulaInfo eula) throws Exception {
        log.debug("recordLicenseAgreement("+userId+","+eula.getModuleLsid()+")");

        //within one transaction,
        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            //1) first, record local record,
            local.recordLicenseAgreement(userId, eula);
            //2) add DB entry to the 'eula_remote_queue'
            local.addToRemoteQueue(userId, eula, PostToBroad.DEFAULT_URL);
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
            else {
                log.debug("committing hibernate transaction, even though it was started before this method");
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            String message="Error recording eula to local GP server: "+t.getLocalizedMessage();
            log.error(message,t);
            HibernateUtil.rollbackTransaction();
            throw new Exception(message);
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        //2) then schedule asynchronous POST of remote record 
        remote.recordLicenseAgreement(userId, eula);
    }

    //@Override
    public boolean hasUserAgreed(final String userId, final EulaInfo eula) throws Exception {
        //delegate to local record
        return local.hasUserAgreed(userId, eula);
    }

    //@Override
    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        //delegate to local record
        return local.getUserAgreementDate(userId, eula);
    }

    //@Override
    public void addToRemoteQueue(final String userId, final EulaInfo eula, final String remoteUrl) throws Exception {
        throw new Exception("Not implemented!");
    }
    
    public void updateRemoteQueue(final String userId, final EulaInfo eula, final String remoteUrl, boolean success, int statusCode, String statusMessage) {
        //1) update eula_remote_queue table
        //2) insert into eula_remote_log table
    }

}

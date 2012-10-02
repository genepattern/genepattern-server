package org.genepattern.server.eula.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Utility class for recording the license agreement to the database.
 * @author pcarr
 *
 */
public class RecordEulaToDb implements RecordEula {
    private static Logger log = Logger.getLogger(RecordEulaToDb.class);

    public boolean hasUserAgreed(String userId, EulaInfo eula) throws Exception {
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        final String lsid=eula.getModuleLsid();
        if (lsid==null || lsid.length()==0) {
            throw new IllegalArgumentException("eula.lsid is not set");
        }
        boolean hasRecord = hasUserAgreedHelper(userId, lsid);
        return hasRecord;
    }

    static public boolean hasUserAgreedHelper(final String userId, final String lsid) 
    throws Exception
    {
        log.debug("userId="+userId+", lsid="+lsid);
        boolean inTransaction = HibernateUtil.isInTransaction();
        log.debug("inTransaction="+inTransaction);
        try {
            String hql = "from "+EulaRecord.class.getName()+" ur where ur.userId = :userId and ur.lsid = :lsid";
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setString("userId", userId);
            query.setString("lsid", lsid);
            List<EulaRecord> records = query.list();
            if (records == null) {
                //TODO: unexpected result
                log.error("Unexpected result: records == null; userId="+userId+", lsid="+lsid);
                return true;
            }
            if (records.size()==0) {
                log.debug("No record in DB");
                return false;
            }
            if (records.size()==1) {
                log.debug("Found record in DB");
                return true;
            }
            log.error("Found multiple records in DB: "+records.size()+ "; userId="+userId+", lsid="+lsid);
            return true;
        }
        catch (Throwable t) {
            //TODO: decide what to do when there are DB errors
            String errorMessage="DB error checking for eula record; userId="+userId+", lsid="+lsid;
            log.error(errorMessage, t);
            throw new Exception(errorMessage+"; "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    
    public void recordLicenseAgreement(final String userId, final String lsid) 
    throws Exception
    { 
        final boolean isInTransaction = HibernateUtil.isInTransaction();
        EulaRecord record = new EulaRecord();
        record.setUserId(userId);
        record.setLsid(lsid);

        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().save( record );
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error recording license agreement to db: "+t.getLocalizedMessage());
        }
    }

}

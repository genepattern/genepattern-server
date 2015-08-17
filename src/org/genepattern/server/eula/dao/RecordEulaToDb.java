/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.eula.dao;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.eula.EulaInfo;
import org.genepattern.server.eula.RecordEula;
import org.hibernate.Query;
import org.hibernate.Session;

/**
 * Use Hibernate to record the license agreement locally to the GP database.
 * 
 * @author pcarr
 */
public class RecordEulaToDb implements RecordEula {
    final static private Logger log = Logger.getLogger(RecordEulaToDb.class);
    
    private final HibernateSessionManager mgr;

    public RecordEulaToDb(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }

    public boolean hasUserAgreed(String userId, EulaInfo eula) throws Exception {
        Date userAgreementDate = getUserAgreementDate(userId, eula);
        return userAgreementDate != null;
    } 

    public void recordLicenseAgreement(final String userId, final EulaInfo eula)  throws Exception { 
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        final String lsid=eula.getModuleLsid();
        final boolean isInTransaction = mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            Date userAgreementDate = getUserAgreementDate(userId, lsid);
            if (userAgreementDate != null) {
                log.warn("Found duplicate record, userId="+userId+", lsid="+lsid+", date="+userAgreementDate);
                return;
            }
            EulaRecord record = new EulaRecord();
            record.setUserId(userId);
            record.setLsid(lsid);
            mgr.getSession().save( record );
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Exception e) {
            log.error("Error recording licenseAgreenet, userId="+userId+", lsid="+lsid, e);
            mgr.rollbackTransaction();
            //some of the hibernate exceptions are convoluted, therefore throw the cause if possible
            if (e.getCause() != null) {
                Throwable t = e.getCause();
                if (t instanceof Exception) {
                    throw (Exception) t;
                }
                throw e;
            }
            throw e;
        }
        finally {
            if (!isInTransaction) {
                //make sure to close the connection, if we find a duplicate record
                mgr.closeCurrentSession();
            }
        }
    }

    public Date getUserAgreementDate(final String userId, final EulaInfo eula) throws Exception {
        if (eula==null) {
            throw new IllegalArgumentException("eula==null");
        }
        return getUserAgreementDate(userId, eula.getModuleLsid());
    }
    
    private Date getUserAgreementDate(final String userId, final String lsid) throws Exception {
        EulaRecord eulaRecord = getEulaRecord(userId, lsid);
        if (eulaRecord != null) {
            return eulaRecord.getDateRecorded();
        }
        return null;
    }
    
    private EulaRecord getEulaRecord(final String userId, final String lsid) throws Exception {
        if (lsid==null || lsid.length()==0) {
            throw new IllegalArgumentException("eula.lsid not set");
        }
        if (userId==null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (userId.length()==0) {
            throw new IllegalArgumentException("userId not set");
        }
        log.debug("getUserAgreementDate, userId="+userId+", lsid="+lsid);
        boolean inTransaction = mgr.isInTransaction();
        log.debug("inTransaction="+inTransaction);
        try {
            String hql = "from "+EulaRecord.class.getName()+" ur where ur.userId = :userId and ur.lsid = :lsid";
            mgr.beginTransaction();
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            query.setString("userId", userId);
            query.setString("lsid", lsid);
            @SuppressWarnings("unchecked")
            List<EulaRecord> records = query.list();
            if (records == null) {
                log.error("Unexpected result: records == null; userId="+userId+", lsid="+lsid);
                return null;
            }
            if (records.size()==0) {
                log.debug("No record in DB");
                return null;
            }
            else if (records.size()==1) {
                log.debug("Found record in DB");
            }
            else {
                //special-case, more than one record
                log.error("Found multiple records in DB: "+records.size()+ "; userId="+userId+", lsid="+lsid+", Using first record");
            }
            return records.get(0);
        }
        catch (Throwable t) {
            String errorMessage="DB error checking for eula record; userId="+userId+", lsid="+lsid;
            log.error(errorMessage, t);
            throw new Exception(errorMessage+"; "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

        
    // methods for adding a remote POST to the queue 
    private EulaRemoteQueue getEulaRemoteQueueEntry(final String userId, final EulaInfo eulaInfo, final String remoteUrl) throws Exception {
        final boolean inTransaction = mgr.isInTransaction();
        log.debug("inTransaction="+inTransaction);
        try {
            mgr.beginTransaction();
            EulaRecord eulaRecord=getEulaRecord(userId, eulaInfo.getModuleLsid());
            if (eulaRecord==null) {
                return null;
            }  
            EulaRemoteQueue.Key key = new EulaRemoteQueue.Key(eulaRecord.getId(), remoteUrl);
            EulaRemoteQueue entry = new EulaRemoteQueue(key);
            return entry;
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public void addToRemoteQueue(final String userId, final EulaInfo eulaInfo, final String remoteUrl) throws Exception {
        final boolean inTransaction = mgr.isInTransaction();
        log.debug("inTransaction="+inTransaction);
        try {
            mgr.beginTransaction();
            EulaRemoteQueue entry = getEulaRemoteQueueEntry(userId, eulaInfo, remoteUrl);
            if (entry==null) {
                // manually enforce foreign key constraint
                throw new Exception("No entry in 'eula_record' for userId="+userId+", lsid="+eulaInfo.getModuleLsid());
            }
            mgr.getSession().saveOrUpdate( entry );
            if (!inTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public void removeFromQueue(final String userId, final EulaInfo eulaInfo, final String remoteUrl) throws Exception {
        final boolean inTransaction = mgr.isInTransaction();
        mgr.beginTransaction();
        log.debug("inTransaction="+inTransaction);
        try {
            mgr.beginTransaction();
            EulaRemoteQueue entry = getEulaRemoteQueueEntry(userId, eulaInfo, remoteUrl);
            if (entry==null) {
                log.error("No entry in 'eula_record' for userId="+userId+", lsid="+eulaInfo.getModuleLsid());
                return;
            }
            mgr.getSession().delete(entry);
            if (!inTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public List<EulaRemoteQueue> getQueueEntries(final String userId, final EulaInfo eulaInfo) throws Exception {
        if (userId==null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (userId.length()==0) {
            throw new IllegalArgumentException("userId not set");
        }
        log.debug("getQueueEntries, userId="+userId+", lsid="+eulaInfo.getModuleLsid());
        boolean inTransaction = mgr.isInTransaction();
        log.debug("inTransaction="+inTransaction);
        try {
            mgr.beginTransaction();
            EulaRecord eulaRecord=getEulaRecord(userId, eulaInfo.getModuleLsid());
            String hql = "from "+EulaRemoteQueue.class.getName()+" q where q.eulaRecordId = :eulaRecordId";
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            query.setLong("eulaRecordId", eulaRecord.getId());
            @SuppressWarnings("unchecked")
            List<EulaRemoteQueue> records = query.list();
            if (records == null) {
                log.error("Unexpected result: records == null; userId="+userId+", lsid="+eulaInfo.getModuleLsid());
                return null;
            }
            if (records.size()==0) {
                log.debug("No record in DB");
                return Collections.emptyList();
            } 
            return records;
        }
        catch (Throwable t) {
            String errorMessage="DB error checking for eula record; userId="+userId+", lsid="+eulaInfo.getModuleLsid();
            log.error(errorMessage, t);
            throw new Exception(errorMessage+"; "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
            
    public List<EulaRemoteQueue> getQueueEntries(final String userId, final EulaInfo eulaInfo, final String remoteUrl) throws Exception {
        if (userId==null) {
            throw new IllegalArgumentException("userId==null");
        }
        if (userId.length()==0) {
            throw new IllegalArgumentException("userId not set");
        }
        log.debug("getQueueEntries, userId="+userId+", lsid="+eulaInfo.getModuleLsid()+", remoteUrl="+remoteUrl);
        boolean inTransaction = mgr.isInTransaction();
        log.debug("inTransaction="+inTransaction);
        try {
            mgr.beginTransaction();
            EulaRecord eulaRecord=getEulaRecord(userId, eulaInfo.getModuleLsid());
            
            
            String hql = "from "+EulaRemoteQueue.class.getName()+" q where q.eulaRecordId = :eulaRecordId and q.remoteUrl = :remoteUrl";
            Session session = mgr.getSession();
            Query query = session.createQuery(hql);
            query.setLong("eulaRecordId", eulaRecord.getId());
            query.setString("remoteUrl", remoteUrl);
            @SuppressWarnings("unchecked")
            List<EulaRemoteQueue> records = query.list();
            if (records == null) {
                log.error("Unexpected result: records == null; userId="+userId+", lsid="+eulaInfo.getModuleLsid());
                return null;
            }
            if (records.size()==0) {
                log.debug("No record in DB");
                return Collections.emptyList();
            } 
            return records;
        }
        catch (Throwable t) {
            String errorMessage="DB error checking for eula record; userId="+userId+", lsid="+eulaInfo.getModuleLsid();
            log.error(errorMessage, t);
            throw new Exception(errorMessage+"; "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        } 
    }
    
    public void updateRemoteQueue(final String userId, final EulaInfo eulaInfo, final String remoteUrl, final boolean success) throws Exception {
        final boolean inTransaction = mgr.isInTransaction();
        log.debug("inTransaction="+inTransaction);
        try {
            mgr.beginTransaction();
            EulaRemoteQueue entry = getEulaRemoteQueueEntry(userId, eulaInfo, remoteUrl);
            if (entry==null) {
                // manually enforce foreign key constraint
                throw new Exception("No entry in 'eula_record' for userId="+userId+", lsid="+eulaInfo.getModuleLsid());
            }
            
            entry.setRecorded(success);
            entry.setDateRecorded(new Date());
            entry.setNumAttempts( entry.getNumAttempts()+1 );
            mgr.getSession().saveOrUpdate( entry );
            if (!inTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}

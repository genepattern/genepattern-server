package org.genepattern.server.taskinstall;


import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.taskinstall.dao.TaskInstall;
import org.hibernate.Query;
import org.hibernate.Session;

public class RecordInstallInfoToDb implements RecordInstallInfo {
    static Logger log = Logger.getLogger(RecordInstallInfoToDb.class);

    @Override
    public void save(final InstallInfo installInfo) 
    throws Exception
    {
        if (installInfo==null) {
            throw new IllegalArgumentException("installInfo==null");
        }

        TaskInstall record=new TaskInstall();
        record.setSourceType(installInfo.getType().name());
        record.setLsid(installInfo.getLsid().toString());
        record.setUserId(installInfo.getUserId());
        record.setDateInstalled(installInfo.getDateInstalled());
        if (installInfo.getRepositoryUrl() != null) {
            record.setRepoUrl(installInfo.getRepositoryUrl().toExternalForm());
        }

        // TODO: save zip file
        // TODO: save prev lsid
        // TODO: save libdir
        //if (installInfo.getLibdir() != null) {
        //    record.setInstallDir(installInfo.getLibdir().getServerFile().toString());
        //}

        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().saveOrUpdate( record );
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error saving InstallInfo for lsid="+installInfo.getLsid(), t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error saving InstallInfo for lsid="+installInfo.getLsid()+": "+t.getLocalizedMessage());
        }
    }
    
    @Override
    public int delete(final String lsid) throws Exception {
        int numDeleted=0;
        boolean inTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            final String hql = "delete "+TaskInstall.class.getName()+" ti where ti.lsid = :lsid";
            final Query query = HibernateUtil.getSession().createQuery( hql );
            query.setString("lsid", lsid);
            numDeleted = query.executeUpdate();
            log.debug("deleted "+numDeleted+" records for lsid="+lsid);
            if (!inTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error deleting record, lsid="+lsid, t);
            HibernateUtil.rollbackTransaction();
            throw new Exception("Error deleting record, lsid="+lsid+": "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return numDeleted;
    }
    
    public TaskInstall query(final String lsid) throws Exception {
        boolean inTransaction=HibernateUtil.isInTransaction();
        TaskInstall record=null;
        try {
            String hql = "from "+TaskInstall.class.getName()+" tir where tir.lsid = :lsid";
            HibernateUtil.beginTransaction();
            Session session = HibernateUtil.getSession();
            Query query = session.createQuery(hql);
            query.setString("lsid", lsid);
            List<TaskInstall> records = query.list();

            if (records == null) {
                log.error("Unexpected result: records == null; lsid="+lsid);
            }
            if (records.size()==0) {
                log.debug("No record for lsid="+lsid);
                record=null;
            }
            else if (records.size()==1) {
                record=records.get(0);
            }
            else {
                log.error("Found more than on record for lsid="+lsid+", records.size="+records.size());
                record=records.get(0);
            }
        }
        finally {
            if (!inTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
        return record;
    }

}

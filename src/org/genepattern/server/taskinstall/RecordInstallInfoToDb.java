package org.genepattern.server.taskinstall;


import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.taskinstall.dao.Category;
import org.genepattern.server.taskinstall.dao.TaskInstall;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

public class RecordInstallInfoToDb implements RecordInstallInfo {
    private static final Logger log = Logger.getLogger(RecordInstallInfoToDb.class);
    
    private final HibernateSessionManager mgr;
    
    public RecordInstallInfoToDb() {
        this(HibernateUtil.instance());
    }
    public RecordInstallInfoToDb(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }    

    @Override
    public void save(final InstallInfo installInfo) throws DbException {
        if (installInfo==null) {
            throw new IllegalArgumentException("installInfo==null");
        }
        boolean inTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();

            TaskInstall record=new TaskInstall();
            record.setSourceType(installInfo.getType().name());
            record.setLsid(installInfo.getLsid().toString());
            record.setUserId(installInfo.getUserId());
            record.setDateInstalled(installInfo.getDateInstalled());
            if (installInfo.getRepositoryUrl() != null) {
                record.setRepoUrl(installInfo.getRepositoryUrl().toExternalForm());
            }
            if (installInfo.getCategories().size()>0) {
                Set<Category> categories=new LinkedHashSet<Category>();
                for(final String name : installInfo.getCategories()) {
                    Category category = (Category) mgr.getSession().createCriteria(Category.class).add(Restrictions.eq("name", name)).uniqueResult();
                    if (category==null) {
                        category = new Category(name);
                        mgr.getSession().saveOrUpdate(category);
                    } 
                    categories.add(category);
                }
                record.setCategories(categories);
            }

            // TODO: save zip file
            // TODO: save prev lsid
            // TODO: save libdir
            //if (installInfo.getLibdir() != null) {
            //    record.setInstallDir(installInfo.getLibdir().getServerFile().toString());
            //}

            mgr.getSession().saveOrUpdate( record );
            if (!inTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error saving InstallInfo for lsid="+installInfo.getLsid(), t);
            mgr.rollbackTransaction();
            throw new DbException("Error saving InstallInfo for lsid="+installInfo.getLsid()+": "+t.getLocalizedMessage(), t);
        }
    }
    
    @Override
    public int delete(final String lsid) throws Exception {
        int numDeleted=0;
        boolean inTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            final String hql = "delete "+TaskInstall.class.getName()+" ti where ti.lsid = :lsid";
            final Query query = mgr.getSession().createQuery( hql );
            query.setString("lsid", lsid);
            numDeleted = query.executeUpdate();
            log.debug("deleted "+numDeleted+" records for lsid="+lsid);
            if (!inTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error deleting record, lsid="+lsid, t);
            mgr.rollbackTransaction();
            throw new Exception("Error deleting record, lsid="+lsid+": "+t.getLocalizedMessage());
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
        return numDeleted;
    }
    
    public TaskInstall query(final String lsid) throws Exception {
        boolean inTransaction=mgr.isInTransaction();
        TaskInstall record=null;
        try {
            String hql = "from "+TaskInstall.class.getName()+" tir where tir.lsid = :lsid";
            mgr.beginTransaction();
            Session session = mgr.getSession();
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
            if (record != null) {
                Hibernate.initialize(record.getCategories());
            }
        }
        finally {
            if (!inTransaction) {
                mgr.closeCurrentSession();
            }
        }
        return record;
    }

}

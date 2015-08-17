/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.plugin.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.plugin.PatchInfo;
import org.hibernate.Query;

public class PatchInfoDao {
    private static final Logger log = Logger.getLogger(PatchInfoDao.class);
    
    private final HibernateSessionManager mgr;
    
    /** @deprecated */
    public PatchInfoDao() {
        this(org.genepattern.server.database.HibernateUtil.instance());
    }
    
    public PatchInfoDao(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }
    
    protected void validatePatchInfo(final PatchInfo patchInfo) throws IllegalArgumentException {
        if (patchInfo==null) {
            throw new IllegalArgumentException("patchInfo==null");
        }
        if (patchInfo.getLsid()==null) {
            throw new IllegalArgumentException("patchInfo.lsid==null");
        }
        if (patchInfo.getPatchLsid()==null) {
            throw new IllegalArgumentException("patchInfo.patchLsid==null");
        }
    }
    
    public PatchInfo selectPatchInfoByLsid(final PatchInfo query) throws IllegalArgumentException, DbException {
        validatePatchInfo(query);
        return selectPatchInfoByLsid(query.getLsid());
    }
    
    /**
     * Get the entry from the patch_info table for the given patchLsid. 
     * 
     * @param patchLsid
     * @return null if there is no matching entry in the table.
     * @throws Exception
     */
    public PatchInfo selectPatchInfoByLsid(final String patchLsid) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            final String hql = "from " + PatchInfo.class.getName() + " pi where pi.lsid = :lsid";
            final Query query = mgr.getSession().createQuery(hql);
            query.setString("lsid", patchLsid);
            @SuppressWarnings("unchecked")
            List<PatchInfo> list = query.list();
            if (list==null || list.isEmpty()) {
                log.debug("no entry in table for patchLsid="+patchLsid);
                return null;
            }
            if (list.size()>1) {
                log.error("More than one entry in patch_info table with lsid="+patchLsid);
            }
            return list.get(0);
        }
        catch (Throwable t) {
            log.error("Unexpected error getting PatchInfo for lsid="+patchLsid, t);
            throw new DbException("Unexpected error getting PatchInfo for lsid="+patchLsid, t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public List<PatchInfo> getInstalledPatches() throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            String hql = "from " + PatchInfo.class.getName() + " pi";
            Query query = mgr.getSession().createQuery(hql);
            @SuppressWarnings("unchecked")
            List<PatchInfo> rval = query.list();
            return rval;
        }
        catch (Throwable t) {
            log.error("Unexpected error selecting installed patches from database: "+t.getLocalizedMessage(), t);
            throw new DbException("Unexpected error selecting installed patches from database: "+t.getLocalizedMessage(), t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public void recordPatch(final PatchInfo patchInfo) throws IllegalArgumentException, DbException {
        validatePatchInfo(patchInfo);
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            PatchInfo existing=selectPatchInfoByLsid(patchInfo.getLsid());
            if (existing!=null) {
                patchInfo.setId(existing.getId());
                // need to evict to avoid problems with implementation of hashCode and equals in the PatchInfo class
                mgr.getSession().evict(existing);
            } 
            mgr.getSession().saveOrUpdate(patchInfo);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error(t);
            throw new DbException("Unexpected error saving patchInfo to database, patchLsid="+patchInfo.getLsid(), t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public boolean removePatch(PatchInfo patchInfo) throws IllegalArgumentException, DbException {
        validatePatchInfo(patchInfo);
        return removePatchByLsid(patchInfo.getLsid());
    }
    
    protected boolean removePatchByLsid(final String patchLsid) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            final String hql = "delete from " + PatchInfo.class.getName() + " pi where pi.lsid = :lsid";
            final Query query = mgr.getSession().createQuery(hql);
            query.setString("lsid", patchLsid);
            int numDeleted=query.executeUpdate();
            log.debug("numDeleted="+numDeleted);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            if (numDeleted==1) {
                return true;
            }
            return false;
        }
        catch (Throwable t) {
            final String message="Unexpected error deleting record from patch_info table for lsid="+patchLsid;
            log.error(message, t);
            throw new DbException(message, t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}

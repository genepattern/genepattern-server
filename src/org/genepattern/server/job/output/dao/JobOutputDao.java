/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.output.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.output.GpFileType;
import org.genepattern.server.job.output.JobOutputFile;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class JobOutputDao {
    private static final Logger log = Logger.getLogger(JobOutputDao.class);

    final int batchSize=100; //flush after each N entries
    
    private final HibernateSessionManager mgr;

    /** @deprecated */
    public JobOutputDao() {
        this(org.genepattern.server.database.HibernateUtil.instance());
    }
    
    public JobOutputDao(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }

    public void recordOutputFile(final JobOutputFile outputFile) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            mgr.getSession().save(outputFile);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public void recordOutputFiles(final List<JobOutputFile> outputFiles) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            int i=0;
            for(final JobOutputFile out : outputFiles) {
                ++i;
                mgr.getSession().save(out);
                if (i%batchSize == 0) {
                    mgr.getSession().flush();
                }
            }
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public List<JobOutputFile> selectOutputFiles(final Integer gpJobNo) {
        boolean includeHidden=false;
        boolean includeDeleted=false;
        return selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
    }

    public List<JobOutputFile> selectOutputFiles(final Integer gpJobNo, boolean includeHidden, boolean includeDeleted) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            Criteria query=mgr.getSession().createCriteria(JobOutputFile.class);
            query=query.add( Restrictions.eq("gpJobNo", gpJobNo ) );
            if (!includeHidden) {
                query=query.add( Restrictions.eq("hidden", false) );
            }
            if (!includeDeleted) {
                query=query.add( Restrictions.eq("deleted", false) );
            }
            @SuppressWarnings("unchecked")
            List<JobOutputFile> out = query.list();
            return out;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public JobOutputFile selectOutputFile(final Integer gpJobNo, final String path) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            Criteria query=mgr.getSession().createCriteria(JobOutputFile.class);
            query=query.add( Restrictions.eq("gpJobNo", gpJobNo ) );
            query=query.add( Restrictions.eq("path", path ) );
            return (JobOutputFile) query.uniqueResult();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public List<JobOutputFile> selectGpExecutionLogs(final Integer gpJobNo) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            Criteria query=mgr.getSession().createCriteria(JobOutputFile.class);
            query=query.add( Restrictions.eq("gpJobNo", gpJobNo ) );
            query=query.add( Restrictions.eq("gpFileType", GpFileType.GP_EXECUTION_LOG.name() ) );
            @SuppressWarnings("unchecked")
            final List<JobOutputFile> rval = query.list();
            return rval;
        }
        catch (Throwable t) {
            final String message="Error querying gp_execution_log for gpJobNo="+gpJobNo;
            log.error(message,t);
            throw new DbException(message+", "+t.getLocalizedMessage());
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public List<JobOutputFile> selectStderrFiles(final Integer gpJobNo) throws DbException {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            Criteria query=mgr.getSession().createCriteria(JobOutputFile.class);
            query=query.add( Restrictions.eq("gpJobNo", gpJobNo ) );
            query=query.add( Restrictions.eq("gpFileType", GpFileType.STDERR.name() ) );
            @SuppressWarnings("unchecked")
            final List<JobOutputFile> rval = query.list();
            return rval;
        }
        catch (Throwable t) {
            final String message="Error querying stderr files for gpJobNo="+gpJobNo;
            log.error(message,t);
            throw new DbException(message+", "+t.getLocalizedMessage());
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public boolean deleteOutputFile(final Integer gpJobNo, final String path) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            JobOutputFile toDel = selectOutputFile(gpJobNo, path);
            if (toDel == null) {
                return false;
            }
            mgr.getSession().delete(toDel);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return true;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }
    
    public boolean setDeleted(final Integer gpJobNo, final String path) {
        final boolean isInTransaction=mgr.isInTransaction();
        mgr.beginTransaction();
        try {
            @SuppressWarnings("unchecked")
            List<JobOutputFile> match=mgr.getSession().createCriteria(JobOutputFile.class)
                    .add( Restrictions.eq("gpJobNo", gpJobNo ) )
                    .add( Restrictions.eq("path", path) )
                    .list();
            if (match.size()==0) {
                log.debug("No match for gpJobNo="+gpJobNo+", path="+path);
                return false;
            }
            if (match.size() > 1) {
                log.debug("Found more than one match for gpJobNo="+gpJobNo+", path="+path);
                return false;
            }
            JobOutputFile out=match.get(0);
            if (out.isDeleted()) {
                log.debug("Already set to deleted for gpJobNo="+gpJobNo+", path="+path);
                return false;
            }
            out.setDeleted(true);
            out.setFileLength(0L); // assume that the file has already been removed from the system
            mgr.getSession().saveOrUpdate(out);
            if (!isInTransaction) {
                mgr.commitTransaction();
            }
            return true;
        }
        catch (Throwable t) {
            log.error("Unexpected error in setDeleted for gpJobNo="+gpJobNo+", path="+path, t);
            return false;
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

}

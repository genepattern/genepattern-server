package org.genepattern.server.job.output.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.output.JobOutputFile;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

public class JobOutputDao {
    private static final Logger log = Logger.getLogger(JobOutputDao.class);

    final int batchSize=100; //flush after each N entries

    public void recordOutputFile(final JobOutputFile outputFile) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            HibernateUtil.getSession().save(outputFile);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    public void recordOutputFiles(final List<JobOutputFile> outputFiles) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            int i=0;
            for(final JobOutputFile out : outputFiles) {
                ++i;
                HibernateUtil.getSession().save(out);
                if (i%batchSize == 0) {
                    HibernateUtil.getSession().flush();
                }
            }
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public List<JobOutputFile> selectOutputFiles(final Integer gpJobNo) {
        boolean includeHidden=false;
        boolean includeDeleted=false;
        return selectOutputFiles(gpJobNo, includeHidden, includeDeleted);
    }

    public List<JobOutputFile> selectOutputFiles(final Integer gpJobNo, boolean includeHidden, boolean includeDeleted) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            Criteria query=HibernateUtil.getSession().createCriteria(JobOutputFile.class);
            query=query.add( Restrictions.eq("gpJobNo", gpJobNo ) );
            if (!includeHidden) {
                query=query.add( Restrictions.eq("hidden", false) );
            }
            if (!includeDeleted) {
                query=query.add( Restrictions.eq("deleted", false) );
            }
            List<JobOutputFile> out = query.list();
            return out;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public JobOutputFile selectOutputFile(final Integer gpJobNo, final String path) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            Criteria query=HibernateUtil.getSession().createCriteria(JobOutputFile.class);
            query=query.add( Restrictions.eq("gpJobNo", gpJobNo ) );
            query=query.add( Restrictions.eq("path", path ) );
            return (JobOutputFile) query.uniqueResult();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public boolean deleteOutputFile(final Integer gpJobNo, final String path) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            JobOutputFile toDel = selectOutputFile(gpJobNo, path);
            if (toDel == null) {
                return false;
            }
            HibernateUtil.getSession().delete(toDel);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            return true;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }
    
    public boolean setDeleted(final Integer gpJobNo, final String path) {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        HibernateUtil.beginTransaction();
        try {
            List<JobOutputFile> match=HibernateUtil.getSession().createCriteria(JobOutputFile.class)
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
            HibernateUtil.getSession().saveOrUpdate(out);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
            return true;
        }
        catch (Throwable t) {
            log.error("Unexpected error in setDeleted for gpJobNo="+gpJobNo+", path="+path, t);
            return false;
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}

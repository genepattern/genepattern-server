package org.genepattern.server.executor.drm.dao;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.database.HibernateUtil;

public class JobRunnerJobDao {
    private static final Logger log = Logger.getLogger(JobRunnerJobDao.class);

    public JobRunnerJob selectJobRunnerJob(final Integer gpJobNo) throws DbException {
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            JobRunnerJob existing = (JobRunnerJob) HibernateUtil.getSession().get(JobRunnerJob.class, gpJobNo);
            return existing;
        }
        catch (Throwable t) {
            log.error("Error getting entry for gpJobNo="+gpJobNo,t);
            throw new DbException("Error getting entry for gpJobNo="+gpJobNo,t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}

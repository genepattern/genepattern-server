package org.genepattern.server.job.comment.dao;

import org.apache.log4j.Logger;
import org.genepattern.server.DbException;
import org.genepattern.server.job.comment.JobComment;
import org.genepattern.server.database.HibernateUtil;
import org.hibernate.Query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by nazaire on 9/30/14.
 */
public class JobCommentDao
{
    private static final Logger log = Logger.getLogger(JobCommentDao.class);

    public void insertJobComment(final JobComment jobComment) {
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().save(jobComment);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error adding comment for gpJobNo="+jobComment.getGpJobNo(), t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    public boolean updateJobComment(JobComment jobComment)
    {
        boolean updated = false;
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            HibernateUtil.getSession().saveOrUpdate(jobComment);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }

            updated = true;
        }
        catch (Throwable t) {
            log.error("Error updating comment", t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return updated;
    }

    public List<JobComment> selectJobComments(final Integer gpJobNo) {
        List<JobComment> jobCommentList = new ArrayList();
        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();

            String hql = "from "+JobComment.class.getName()+" jc where jc.gpJobNo = :gpJobNo ";

            Query query = HibernateUtil.getSession().createQuery( hql );
            query.setInteger("gpJobNo", gpJobNo);
            jobCommentList = query.list();
        }
        catch (Throwable t) {
            log.error("Error getting comments for gpJobNo="+gpJobNo,t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return jobCommentList;
    }

    public boolean deleteJobComment(int id)
    {
        boolean deleted = false;
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();

            JobComment jobComment = (JobComment)HibernateUtil.getSession().get(JobComment.class, Integer.valueOf(id));
            if(jobComment == null)
            {
                //log error and do nothing
                log.error("Error retrieving comment with id="+id);
                return deleted;
            }

            HibernateUtil.getSession().delete(jobComment);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }

            deleted = true;
        }
        catch (Throwable t) {
            log.error("Error deleting comment wih id="+id, t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return deleted;
    }
}

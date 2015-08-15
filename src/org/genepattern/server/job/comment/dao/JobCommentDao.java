/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.comment.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.comment.JobComment;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

/**
 * Created by nazaire on 9/30/14.
 */
public class JobCommentDao
{
    private static final Logger log = Logger.getLogger(JobCommentDao.class);
    private final HibernateSessionManager mgr;

    public JobCommentDao(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }

    public void insertJobComment(final JobComment jobComment) {
        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            mgr.getSession().save(jobComment);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }
        }
        catch (Throwable t) {
            log.error("Error adding comment for gpJobNo="+jobComment.getAnalysisJob().getJobNo(), t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }
    }

    public boolean updateJobComment(JobComment jobComment)
    {
        boolean updated = false;
        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            mgr.getSession().saveOrUpdate(jobComment);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }

            updated = true;
        }
        catch (Throwable t) {
            log.error("Error updating comment", t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        return updated;
    }

    public List<JobComment> selectJobComments(final Integer gpJobNo) {
        List<JobComment> jobCommentList = new ArrayList();
        final boolean isInTransaction=mgr.isInTransaction();
        try {
            mgr.beginTransaction();

            jobCommentList = mgr.getSession().createCriteria(JobComment.class, "jobComment")
                    .createAlias("jobComment.analysisJob", "analysisJob")
                    .add(Restrictions.eq("analysisJob.jobNo", gpJobNo)).addOrder(Order.desc("postedDate")).list();
        }
        catch (Throwable t) {
            log.error("Error getting comments for gpJobNo="+gpJobNo,t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        return jobCommentList;
    }

    public boolean deleteJobComment(int id)
    {
        boolean deleted = false;
        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();

            JobComment jobComment = (JobComment)mgr.getSession().get(JobComment.class, Integer.valueOf(id));
            if(jobComment == null)
            {
                //log error and do nothing
                log.error("Error retrieving comment with id="+id);
                return deleted;
            }

            mgr.getSession().delete(jobComment);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }

            deleted = true;
        }
        catch (Throwable t) {
            log.error("Error deleting comment wih id="+id, t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        return deleted;
    }
}

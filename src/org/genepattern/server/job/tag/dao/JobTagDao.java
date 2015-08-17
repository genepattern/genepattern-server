/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.tag.dao;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.tag.Tag;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nazaire on 10/8/14.
 */
public class JobTagDao
{
    private static final Logger log = Logger.getLogger(JobTagDao.class);
    
    private final HibernateSessionManager mgr;

    /** @deprecated */
    public JobTagDao() {
        this(org.genepattern.server.database.HibernateUtil.instance());
    }

    public JobTagDao(final HibernateSessionManager mgr) {
        this.mgr=mgr;
    }

    public boolean insertJobTag(final JobTag jobTag)
    {
        boolean result = false;
        if (jobTag==null) {
            log.error("No entry to update");
            return false;
        }
        final boolean isInTransaction= mgr.isInTransaction();

        try {
            mgr.beginTransaction();

            //first check if a public tag with the same tag text already exists and it is public to the user
            @SuppressWarnings("unchecked")
            List<Tag> matchingPublicTag = mgr.getSession().createCriteria(Tag.class).add(Restrictions.eq("tag", jobTag.getTagObj().getTag()))
            .add(Restrictions.or(Restrictions.eq("publicTag", true), Restrictions.eq("userId", jobTag.getUserId()))).list();

            //if there are multiple hits even though there shouldn't be then take first one
            if(matchingPublicTag.size() > 0)
            {
                jobTag.setTagObj(matchingPublicTag.get(0));
            }

            mgr.getSession().saveOrUpdate(jobTag);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }

            result = true;
        }
        catch (Throwable t) {

            log.error("Error adding tag for gpJobNo=" + jobTag.getAnalysisJob().getJobNo(), t);
            mgr.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<JobTag> selectJobTags(final int gpJobNo) {
        List<JobTag> jobTagList = new ArrayList<JobTag>();
        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();

            jobTagList = mgr.getSession().createCriteria(JobTag.class, "jobTag")
                    .createAlias("jobTag.analysisJob", "analysisJob")
                    .createAlias("jobTag.tagObj", "tagObj")
                    .add(Restrictions.eq("analysisJob.jobNo", gpJobNo))
                    .addOrder(Order.asc("tagObj.tag")).list();
        }
        catch (Throwable t) {
            log.error("Error getting tags for gpJobNo="+gpJobNo,t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        return jobTagList;
    }

    public JobTag selectJobTagById(final int jobTagId) {
        JobTag jobTag = null;

        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();
            jobTag = (JobTag)mgr.getSession().get(JobTag.class, jobTagId);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }

        }
        catch (Throwable t) {
            log.error("Error getting job tag with id="+jobTagId,t);
        }
        finally {
            if (!isInTransaction) {
                mgr.closeCurrentSession();
            }
        }

        return jobTag;
    }

    public boolean deleteJobTag(final int jobTagId)
    {
        if (jobTagId < 1) {
            log.error("No entry to delete");
            return false;
        }

        boolean deleted = false;
        final boolean isInTransaction= mgr.isInTransaction();
        try {
            mgr.beginTransaction();

            JobTag jobTag = (JobTag)mgr.getSession().get(JobTag.class, jobTagId);
            mgr.getSession().delete(jobTag);

            if (!isInTransaction) {
                mgr.commitTransaction();
            }

            deleted = true;
        }
        catch (Throwable t) {
            log.error("Error deleting tag with jobTagId="+ jobTagId, t);
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


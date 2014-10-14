package org.genepattern.server.job.tag.dao;

import org.apache.log4j.Logger;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.tag.Tag;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nazaire on 10/8/14.
 */
public class JobTagDao
{
    private static final Logger log = Logger.getLogger(JobTagDao.class);

    public void insertJobTag(final JobTag jobTag)
    {
        if (jobTag==null) {
            log.error("No entry to update");
            return;
        }
        final boolean isInTransaction= HibernateUtil.isInTransaction();

        try {
            HibernateUtil.beginTransaction();

            //first check if a public tag with the same tag text already exists and it is public to the user
            List<Tag> matchingPublicTag = HibernateUtil.getSession().createCriteria(Tag.class).add(Restrictions.eq("tag", jobTag.getTagObj().getTag()))
            .add(Restrictions.or(Restrictions.eq("publicTag", true), Restrictions.eq("userId", jobTag.getUserId()))).list();

            //if there are multiple hits even though there shouldn't be then take first one
            if(matchingPublicTag.size() > 0)
            {
                jobTag.setTagObj(matchingPublicTag.get(0));
            }

            HibernateUtil.getSession().save(jobTag);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }
        }
        catch (Throwable t) {

            log.error("Error adding tag for gpJobNo="+jobTag.getGpJobNo(), t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    public List<JobTag> selectJobTags(final int gpJobNo) {
        List<JobTag> jobTagList = new ArrayList();
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();

            jobTagList = HibernateUtil.getSession().createCriteria(JobTag.class)
                    .add(Restrictions.eq("gpJobNo", gpJobNo)).list();
        }
        catch (Throwable t) {
            log.error("Error getting tags for gpJobNo="+gpJobNo,t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return jobTagList;
    }

    public JobTag selectJobTagById(final int jobTagId) {
        JobTag jobTag = null;

        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            jobTag = (JobTag)HibernateUtil.getSession().get(JobTag.class, jobTagId);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }

        }
        catch (Throwable t) {
            log.error("Error getting job tag with id="+jobTagId,t);
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }

        return jobTag;
    }

    public boolean deleteJobTag(final int gpJobNo)
    {
        if (gpJobNo < 1) {
            log.error("No entry to delete");
            return false;
        }

        boolean deleted = false;
        final boolean isInTransaction= HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();

            JobTag jobTag = (JobTag)HibernateUtil.getSession().get(JobTag.class, gpJobNo);
            HibernateUtil.getSession().delete(jobTag);

            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }

            HibernateUtil.getSession().flush();

            deleted = true;
        }
        catch (Throwable t) {
            log.error("Error deleting tag with gpJobNo="+ gpJobNo, t);
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


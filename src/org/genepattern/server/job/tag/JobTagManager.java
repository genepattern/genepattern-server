package org.genepattern.server.job.tag;

import org.apache.log4j.Logger;
import org.genepattern.server.job.tag.dao.JobTagDao;
import org.genepattern.server.tag.Tag;

import java.util.Date;
import java.util.List;

/**
 * Created by nazaire on 10/8/14.
 */
public class JobTagManager
{
    private static Logger log = Logger.getLogger(JobTagManager.class);

    static public void addTag(String userId, int jobNo,String tagText, Date date, boolean isPublic)
    {
        JobTag jobTag = new JobTag();
        jobTag.setGpJobNo(jobNo);
        jobTag.setDateTagged(date);
        jobTag.setUserId(userId);

        Tag tag = new Tag();
        tag.setDateAdded(date);
        tag.setTag(tagText);
        tag.setUserId(userId);
        tag.setPublicTag(isPublic);

        jobTag.setTagObj(tag);

        addTag(jobTag);
    }

    static public boolean addTag(JobTag jobTag)
    {
        JobTagDao jobTagDao  = new JobTagDao();

        return jobTagDao.insertJobTag(jobTag);
    }

    static public List<JobTag> selectAllJobTags(int jobNo)
    {
        JobTagDao jobTagDao  = new JobTagDao();

        return jobTagDao.selectJobTags(jobNo);
    }

    static public boolean deleteTag(int jobTagId)
    {
        JobTagDao jobTagDao  = new JobTagDao();

        return jobTagDao.deleteJobTag(jobTagId);
    }
}

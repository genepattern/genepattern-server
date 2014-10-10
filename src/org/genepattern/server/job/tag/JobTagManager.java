package org.genepattern.server.job.tag;

import org.apache.log4j.Logger;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.job.tag.dao.JobTagDao;

import java.util.List;

/**
 * Created by nazaire on 10/8/14.
 */
public class JobTagManager
{
    private static Logger log = Logger.getLogger(JobTagManager.class);

    static public void addTag(JobTag jobTag)
    {
        JobTagDao jobTagDao  = new JobTagDao();

        jobTagDao.insertJobTag(jobTag);
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

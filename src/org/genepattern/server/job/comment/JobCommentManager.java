package org.genepattern.server.job.comment;

import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.comment.dao.JobCommentDao;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Date;
import java.util.List;

/**
 * Created by nazaire on 10/2/14.
 */
public class JobCommentManager
{
    static public boolean updateJobComment(JobComment jobComment) throws JSONException
    {
        JobCommentDao jobCommentDao  = new JobCommentDao();

        if(jobComment != null && jobComment.getId() >= 0)
        {

            return jobCommentDao.updateJobComment(jobComment);
        }

        return false;
    }

    static public JobComment addJobComment(JobComment jobComment) throws JSONException
    {

        if(jobComment != null && jobComment.getGpJobNo() >= 0 && jobComment.getComment() != null)
        {
            JobCommentDao jobCommentDao  = new JobCommentDao();

            jobCommentDao.insertJobComment(jobComment);
            return jobComment;
        }

        return jobComment;
    }

    static public boolean deleteJobComment(int id) throws JSONException
    {
        boolean result = false;
        if(id >= 0)
        {
            JobCommentDao jobCommentDao  = new JobCommentDao();
            result = jobCommentDao.deleteJobComment(id);
        }

        return result;
    }

    static public List<JobComment> selectAllJobComments(int jobNo)
    {
        JobCommentDao jobCommentDao  = new JobCommentDao();

        return jobCommentDao.selectJobComments(jobNo);
    }

}

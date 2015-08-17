/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.comment;

import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.comment.dao.JobCommentDao;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by nazaire on 10/2/14.
 */
public class JobCommentManager
{
    static public boolean updateJobComment(JobComment jobComment) throws JSONException
    {
        JobCommentDao jobCommentDao  = new JobCommentDao(HibernateUtil.instance());

        if(jobComment != null && jobComment.getId() >= 0)
        {

            return jobCommentDao.updateJobComment(jobComment);
        }

        return false;
    }

    static public JobComment addJobComment(JobComment jobComment) throws JSONException
    {
        if(jobComment != null && jobComment.getAnalysisJob() != null
                && jobComment.getAnalysisJob().getJobNo() >= 0 && jobComment.getComment() != null)
        {
            JobCommentDao jobCommentDao  = new JobCommentDao(HibernateUtil.instance());

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
            JobCommentDao jobCommentDao  = new JobCommentDao(HibernateUtil.instance());
            result = jobCommentDao.deleteJobComment(id);
        }

        return result;
    }

    static public List<JobComment> selectAllJobComments(int jobNo)
    {
        JobCommentDao jobCommentDao  = new JobCommentDao(HibernateUtil.instance());

        return jobCommentDao.selectJobComments(jobNo);
    }

    static public JSONObject createJobCommentBundle(String userId, List<JobComment> jobComments) throws JSONException
    {
        JSONObject jobCommentsResult = new JSONObject();

        JSONArray comments = new JSONArray();
        jobCommentsResult.put("comments", comments);
        for(JobComment jobComment : jobComments)
        {
            comments.put(jobComment.toJson());
        }

        JSONObject user = new JSONObject();
        user.put("user_id", userId);
        user.put("fullname", userId);
        user.put("is_logged_in", true);
        user.put("is_add_allowed", true);
        user.put("is_edit_allowed", true);
        jobCommentsResult.put("user", user);

        jobCommentsResult.put("total_comment", jobComments.size());

        return jobCommentsResult;
    }
}

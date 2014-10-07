package org.genepattern.server.webapp.rest.api.v1.job.comment;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.job.comment.JobComment;
import org.genepattern.server.job.comment.JobCommentManager;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.webapp.rest.api.v1.DateUtil;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by nazaire on 10/1/14.
 */
public class JobCommentsResource
{
    final static private Logger log = Logger.getLogger(JobCommentsResource.class);

    public Response loadComments(
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        try
        {
            final GpContext userContext = Util.getUserContext(request);

            if(jobNo == null)
            {
                throw new Exception("A job number must be specified");
            }

            int gpJobNo = Integer.parseInt(jobNo);
            List<JobComment> jobComments = JobCommentManager.selectAllJobComments(gpJobNo);

            JSONObject jobCommentsResult = createJobCommentBundle(userContext, jobComments);
            JSONObject result = new JSONObject();
            result.put("results", jobCommentsResult);
            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    public Response addComment(
            MultivaluedMap<String,String> multivaluedMap,
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        try
        {
            final GpContext userContext = Util.getUserContext(request);

            JSONObject jobCommentsResult = new JSONObject();
            boolean success = false;

            if( multivaluedMap != null && multivaluedMap.getFirst("text") != null
                    && multivaluedMap.getFirst("text").trim().length() > 0 && jobNo != null
                    && jobNo.length() >0)
            {
                String commentText = multivaluedMap.getFirst("text").trim();
                commentText = StringEscapeUtils.unescapeHtml(commentText);
                int gpJobNo = Integer.parseInt(jobNo);

                int parentId = 0;
                if(multivaluedMap.getFirst("parent_id") != null)
                {
                    parentId = Integer.parseInt(multivaluedMap.getFirst("parent_id"));
                }

                JobComment jobComment = new JobComment();
                jobComment.setGpJobNo(gpJobNo);
                jobComment.setParentId(parentId);
                jobComment.setPostedDate(new Date());
                jobComment.setComment(commentText);
                jobComment.setUserId(userContext.getUserId());
                jobComment = JobCommentManager.addJobComment(jobComment);

                jobCommentsResult = jobCommentJson(jobComment);
                success = true;
            }

            jobCommentsResult.put("success", success);

            return Response.ok().entity(jobCommentsResult.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    public Response editComment(
            MultivaluedMap<String,String> multivaluedMap,
            @PathParam("id") String id,
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        try
        {
            final GpContext userContext = Util.getUserContext(request);

            JSONObject result = new JSONObject();
            boolean success = false;

            if( multivaluedMap != null && multivaluedMap.getFirst("text") != null && jobNo != null
                    && jobNo.length() >0 && id != null && id.length() > 0)
            {
                String commentText = multivaluedMap.getFirst("text");
                int gpJobNo = Integer.parseInt(jobNo);

                JobComment jobComment = new JobComment();

                int commentId = Integer.parseInt(id);
                jobComment.setUserId(userContext.getUserId());
                jobComment.setCommentId(commentId);
                jobComment.setGpJobNo(gpJobNo);
                jobComment.setComment(commentText);
                success = JobCommentManager.updateJobComment(jobComment);
                if(success)
                {
                    result.put("text", commentText);
                }
            }

            result.put("success", success);

            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    public Response deleteComment(
            MultivaluedMap<String,String> multivaluedMap,
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        try
        {
            JSONObject result = new JSONObject();
            boolean success = false;

            if( multivaluedMap != null && multivaluedMap.getFirst("comment_id") != null
                    && multivaluedMap.getFirst("comment_id").length() > 0
                    && jobNo != null && jobNo.length() > 0)
            {
                int id = Integer.parseInt(multivaluedMap.getFirst("comment_id"));

                success = JobCommentManager.deleteJobComment(id);

                if(success)
                {
                    int gpJobNo = Integer.parseInt(jobNo);
                    List<JobComment> jobCommentList = JobCommentManager.selectAllJobComments(gpJobNo);
                    result.put("total_comment", jobCommentList.size());
                }
            }

            result.put("success", success);
            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    private JSONObject jobCommentJson(JobComment jobComment) throws Exception
    {
        JSONObject jb = new JSONObject();
        jb.put("comment_id", jobComment.getId());
        jb.put("parent_id", jobComment.getParentId());
        jb.put("created_by", jobComment.getUserId());
        jb.put("fullname", jobComment.getUserId());
        jb.put("posted_date", DateUtil.toTimeAgoUtc(jobComment.getPostedDate()));
        jb.put("text", jobComment.getComment());
        jb.put("childrens", new JSONArray());

        return jb;
    }

    private JSONObject createJobCommentBundle(GpContext userContext, List<JobComment> jobComments) throws Exception
    {
        JSONObject jobCommentsResult = new JSONObject();

        JSONArray comments = new JSONArray();
        jobCommentsResult.put("comments", comments);
        for(JobComment jobComment : jobComments)
        {
            comments.put(jobCommentJson(jobComment));
        }

        JSONObject user = new JSONObject();
        user.put("user_id", userContext.getUserId());
        user.put("fullname", userContext.getUserId());
        user.put("is_logged_in", true);
        user.put("is_add_allowed", true);
        user.put("is_edit_allowed", true);
        jobCommentsResult.put("user", user);

        jobCommentsResult.put("total_comment", jobComments.size());

        return jobCommentsResult;
    }
}

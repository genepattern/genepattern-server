package org.genepattern.server.webapp.rest.api.v1.job.comment;

import org.apache.log4j.Logger;
import org.genepattern.server.job.comment.JobComment;
import org.genepattern.server.job.comment.dao.JobCommentDao;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

/**
 * Created by nazaire on 10/1/14.
 */
@Path("/"+JobCommentsResource.URI_PATH)
public class JobCommentsResource
{
    final static private Logger log = Logger.getLogger(JobCommentsResource.class);
    final static public String URI_PATH="v1/jobs/comments";

    @GET
    @Path("/load/{jobNo}")
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

            JobCommentDao jobCommentDao  = new JobCommentDao();

            int gpJobNo = Integer.parseInt(jobNo);
            List<JobComment> jobComments = jobCommentDao.selectJobComments(gpJobNo);

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

    @POST
    @Path("/add/{jobNo}")
    public Response addComment(
            MultivaluedMap<String,String> multivaluedMap,
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        try
        {
            final GpContext userContext = Util.getUserContext(request);

            JSONObject result = addOrUpdateComment(userContext, multivaluedMap, jobNo);
            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    @POST
    @Path("/add/{jobNo}/{id}")
    public Response editComment(
            MultivaluedMap<String,String> multivaluedMap,
            @PathParam("id") String id,
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        try
        {
            final GpContext userContext = Util.getUserContext(request);
            JSONObject jobCommentsResult = addOrUpdateComment(userContext, multivaluedMap, jobNo, id);
            JSONObject result = new JSONObject();
            result.put("results", result);
            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    @POST
    @Path("/delete")
    public Response deleteComment(
            MultivaluedMap<String,String> multivaluedMap,
            @Context HttpServletRequest request)
    {
        try
        {
            JSONObject result = new JSONObject();
            result.put("success", false);

            if( multivaluedMap != null && multivaluedMap.getFirst("comment_id") != null
                    && multivaluedMap.getFirst("comment_id").length() > 0)
            {
                int id = Integer.parseInt(multivaluedMap.getFirst("comment_id"));

                JobCommentDao jobCommentDao  = new JobCommentDao();

                boolean success = jobCommentDao.deleteJobComment(id);

                if(success)
                {
                    result.put("success", true);
                }
            }
            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    public JSONObject addOrUpdateComment(GpContext userContext, MultivaluedMap<String,String> multivaluedMap, String jobNo) throws JSONException
    {
        return addOrUpdateComment(userContext,multivaluedMap,jobNo, null);
    }

    public JSONObject addOrUpdateComment(GpContext userContext, MultivaluedMap<String,String> multivaluedMap, String jobNo, String commentId) throws JSONException
    {
        JSONObject result = new JSONObject();
        result.put("success", false);

        if( multivaluedMap != null && multivaluedMap.getFirst("text") != null && jobNo != null
                && jobNo.length() >0 )
        {
            String commentText = multivaluedMap.getFirst("text");

            JobCommentDao jobCommentDao  = new JobCommentDao();

            int gpJobNo = Integer.parseInt(jobNo);

            if(commentId != null)
            {
                int id = Integer.parseInt(commentId);
                jobCommentDao.updateJobComment(id, gpJobNo, commentText);
            }
            else
            {
                JobComment jobComment = new JobComment();

                int parentId = 0;
                if(multivaluedMap.getFirst("parent_id") != null)
                {
                    parentId = Integer.parseInt(multivaluedMap.getFirst("parent_id"));
                }

                jobComment.setGpJobNo(gpJobNo);
                jobComment.setParentId(parentId);
                jobComment.setPostedDate(new Date());
                jobComment.setUserId(userContext.getUserId());
                jobComment.setComment(commentText);
                jobCommentDao.insertJobComment(jobComment);
            }

            result.put("success", true);
            result.put("text", commentText);
        }

        return result;
    }

    private JSONObject createJobCommentBundle(GpContext userContext, List<JobComment> jobComments) throws Exception
    {
        JSONObject jobCommentsResult = new JSONObject();

        JSONArray comments = new JSONArray();
        jobCommentsResult.put("comments", comments);
        for(JobComment jobComment : jobComments)
        {
            JSONObject jb = new JSONObject();
            jb.put("comment_id", jobComment.getId());
            jb.put("parent_id", jobComment.getParentId());
            jb.put("created_by", jobComment.getUserId());
            jb.put("fullname", jobComment.getUserId());
            jb.put("posted_date",jobComment.getPostedDate());
            jb.put("text", jobComment.getComment());
            jb.put("childrens", new JSONArray());

            comments.put(jb);
        }

        JSONObject user = new JSONObject();
        user.put("user_id", userContext.getUserId());
        user.put("is_logged_in", true);
        user.put("is_add_allowed", true);
        user.put("is_edit_allowed", true);
        jobCommentsResult.put("user", user);

        jobCommentsResult.put("total_comment", jobComments.size());

        return jobCommentsResult;
    }

}

package org.genepattern.server.webapp.rest.api.v1.job.tag;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.job.tag.JobTag;
import org.genepattern.server.job.tag.JobTagManager;
import org.genepattern.server.tag.Tag;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.Date;
import java.util.List;

/**
 * Created by nazaire on 10/8/14.
 */
public class JobTagsResource
{
    final static private Logger log = Logger.getLogger(JobTagsResource.class);

    public Response loadTags(
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
            List<JobTag> jobTags = JobTagManager.selectAllJobTags(gpJobNo);

            JSONObject result = new JSONObject();
            JSONObject tags = new JSONObject();
            for(JobTag jobTag : jobTags)
            {
                tags.put(jobTag.getTagObj().getTag(), String.valueOf(jobTag.getId()));
            }

            result.put("tags", tags);
            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }


    public Response addTag(
            @PathParam("jobNo") int jobNo,
            @QueryParam("tagText") String tagText,
            @Context HttpServletRequest request)
    {
        try
        {
            final GpContext userContext = Util.getUserContext(request);

            if(jobNo < 0)
            {
                throw new Exception("A valid job number must be specified");
            }

            Date date = new Date();
            JobTag jobTag = new JobTag();
            jobTag.setGpJobNo(jobNo);
            jobTag.setDate(date);
            jobTag.setUserId(userContext.getUserId());

            Tag tag = new Tag();
            tag.setDate(date);

            tagText = StringEscapeUtils.unescapeHtml(tagText);
            tag.setTag(tagText);
            tag.setUserId(userContext.getUserId());
            tag.setPublicTag(false);

            jobTag.setTagObj(tag);

            JobTagManager.addTag(jobTag);

            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("jobTagId", jobTag.getId());

            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    public Response deleteTag(
            @PathParam("jobNo") int jobNo,
            @QueryParam("jobTagId") int jobTagId,
            @Context HttpServletRequest request)
    {
        try
        {
            JSONObject result = new JSONObject();
            boolean success = false;

            if(jobTagId > 0)
            {
                success = JobTagManager.deleteTag(jobTagId);
            }

            result.put("success", success);
            return Response.ok().entity(result.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }
}

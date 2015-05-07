/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job.tag;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.domain.AnalysisJob;
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
                throw new Exception("Error loading tags: A job number must be specified");
            }

            if(userContext == null || userContext.getUserId() == null || userContext.getUserId().length() == 0)
            {
                throw new Exception("Error loading tags: A user id must be specified");
            }

            int gpJobNo = Integer.parseInt(jobNo);
            JSONObject result = new JSONObject();

            List<JobTag> jobTags = JobTagManager.selectAllJobTags(gpJobNo);
            result.put("tags", JobTagManager.createJobTagBundle(jobTags));

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
            jobTag.setDateTagged(date);
            jobTag.setUserId(userContext.getUserId());

            AnalysisJob analysisJob = new AnalysisJob();
            analysisJob.setJobNo(jobNo);
            jobTag.setAnalysisJob(analysisJob);

            Tag tag = new Tag();
            tag.setDateAdded(date);

            tagText = StringEscapeUtils.escapeHtml(tagText);
            //make tag lowercase
            tagText = tagText.toLowerCase();
            tag.setTag(tagText);
            tag.setUserId(userContext.getUserId());
            tag.setPublicTag(false);

            jobTag.setTagObj(tag);

            boolean success  =JobTagManager.addTag(jobTag);

            JSONObject result = new JSONObject();
            result.put("success", success);
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
            final GpContext userContext = Util.getUserContext(request);

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

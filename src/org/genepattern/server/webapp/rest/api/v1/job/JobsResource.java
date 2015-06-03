/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest.api.v1.job;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.server.*;
import org.genepattern.server.auth.GroupPermission;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.status.JobStatusLoaderFromDb;
import org.genepattern.server.job.status.Status;
import org.genepattern.server.quota.DiskInfo;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApi;
import org.genepattern.server.rest.JobInputApiFactory;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webapp.rest.api.v1.job.comment.JobCommentsResource;
import org.genepattern.server.webapp.rest.api.v1.job.search.JobSearch;
import org.genepattern.server.webapp.rest.api.v1.job.search.SearchQuery;
import org.genepattern.server.webapp.rest.api.v1.job.search.SearchResults;
import org.genepattern.server.webapp.rest.api.v1.job.tag.JobTagsResource;
import org.genepattern.server.webservice.server.Analysis;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.TaskInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * RESTful implementation of the /jobs resource.
 *
 * Example usage, via curl command line:
 * <p>To add a job to the server. This example runs the PreprocessDataset module with an ftp input file.</p>
 * <pre>
 * curl -X POST -u test:test -H "Accept: application/json" -H "Content-type: application/json" 
 *      -d '{"lsid":"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00020:4", 
 *           "params": [
 *               {"name": "input.filename", "values": [
 *                     "ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct"] },
 *               {"name": "threshold.and.filter", "values": [
 *                     "1"] },  
 *           ]
 *          }' 
 *      http://127.0.0.1:8080/gp/rest/v1/jobs
 * </pre>
 *
 * <p>Optionally set the 'groupId' for modules which accept file group parameters.
 * <pre>
 {
 "lsid":<actualLsid>,
 "params": [
 { "name": <paramName>,
 "groupId": <groupName1>,
 "values": [ ... ]
 },
 // repeat the paramName for each grouping of input files
 { "name": <paramName>,
 "groupId": <groupName2>,
 "values": [ ... ]
 }
 ]
 }

 * <pre>
 *
 *
 * <p>To add a batch of jobs to the server, use the 'batchParam' property.</p>
 * <pre>
 {
 "lsid":<actualLsid>,
 "params": [
 { "name": <paramName>,
 "batchParam": <true | false>, //if not set, it means it's not a batch parameter
 "values": [ //list of values, for a file input parameter, if the value is for a directory, then ...
 ]
 },
 {
 }
 ]
 }

 * </pre>
 *
 * @author pcarr
 *
 */
@Path("/"+JobsResource.URI_PATH)
public class JobsResource {
    final static private Logger log = Logger.getLogger(JobsResource.class);
    final static public String URI_PATH="v1/jobs";

    final boolean includeComments = true;
    final boolean includeTags = true;

    ////////////////////////////////////////
    // adding a job
    ////////////////////////////////////////

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addJob(
            final @Context UriInfo uriInfo,
            final @Context HttpServletRequest request,
            final JobInputValues jobInputValues)
    {
        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext jobContext=Util.getUserContext(request);

        try
        {
            //check if the user is above their disk quota
            //first check if the disk quota is or will be exceeded
            DiskInfo diskInfo = DiskInfo.createDiskInfo(ServerConfigurationFactory.instance(), jobContext);

            if(diskInfo.isAboveQuota())
            {
                //disk usage exceeded so do not allow user to run a job
                return Response.status(Response.Status.FORBIDDEN).entity("Disk usage exceeded.").build();
            }
        }
        catch(DbException db)
        {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(db.getMessage())
                            .build()
            );
        }

        final JSONObject rval=new JSONObject();
        try {
            //TODO: add support for batch jobs to REST API
            final JobInput jobInput= JobInputValues.parseJobInput(jobInputValues);
            final boolean initDefault=true;
            final JobInputApi impl = JobInputApiFactory.createJobInputApi(gpConfig, jobContext, initDefault);
            final String jobId = impl.postJob(jobContext, jobInput);
            //JobReceipt receipt=impl.postBatchJob(jobContext, jobInput);
            //TODO: if necessary, add batch details to the JSON representation
            //if (receipt.getJobIds()==null) {
            //    log.error("receipt.jobIds==null");
            //    throw new GpServerException("receipt.jobIds==null");
            //}
            //if (receipt.getJobIds().size()==0) {
            //    throw new GpServerException("number of jobs submitted is 0");
            //}
            //final String jobId=receipt.getJobIds().get(0);
            rval.put("jobId", jobId);

            //set the Location header to the URI of the newly created resource
            final URI uri = uriInfo.getAbsolutePathBuilder().path(jobId).build();
            rval.put("jobUri", uri.toString());
            return Response.created(uri).entity(rval.toString()).build();
        }
        catch (JSONException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(e.getMessage())
                            .build()
            );
        }
        catch (GpServerException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(e.getMessage())
                            .build()
            );
        }
        catch (Throwable t) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(t.getMessage())
                            .build()
            );
        }
    }

    /////////////////////////////////////
    // Job search API
    /////////////////////////////////////

    /**
     * Job search API, default GET response for this resource.
     * Template:
     <pre>
     curl -u {userId}:{password} -X GET {GenePatternURL}rest/v1/jobs
     ?userId={userId}
     &groupId={groupId}
     &batchId={batchId}
     &page={page}
     &pageSize={pageSize}
     &orderBy={jobId | taskName | dateSubmitted | dateComplated | status}, prefix with '-' to reverse order
     &orderFilesBy={name | date | size}, prefix with '-' to reverse order
     </pre>
     * Example query:
     * <pre>
     curl -u test:test -X GET http://127.0.0.1:8080/gp/rest/v1/jobs
     * </pre>
     *
     * @param uriInfo
     * @param request
     * @param userId
     * @param groupId
     * @param batchId
     * @param page
     * @param pageSize
     * @param includeChildren
     * @param includeOutputFiles
     * @param prettyPrint
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJobSearchResults(
            final @Context UriInfo uriInfo,
            final @Context HttpServletRequest request,
            /**
             * optionally, show all jobs (*) or for a specific userId, Note: this is not necessarily the current user.
             */
            final @QueryParam("userId") String userId,
            /**
             * optionally, filter all jobs by groupId
             */
            final @QueryParam("groupId") String groupId,
            /**
             * optionally, filter all jobs by batchId
             */
            final @QueryParam("batchId") String batchId,
            /**
             * optionally, filter all jobs by tag
             */
            final @QueryParam("tag") String tag,
            /**
             * optionally, filter all jobs by comment
             */
            final @QueryParam("comment") String comment,
            /**
             * optionally, set the page number for paged job results, The first page is page 1.
             */
            final @DefaultValue("1") @QueryParam("page") Integer page,
            /**
             * optionally, set the number of items per page.
             */
            final @QueryParam("pageSize") int pageSize,
            /**
             * Optionally set the order of the jobs, default is by jobId.
             * Examples,
             * <pre>
             orderBy=jobId
             orderBy=-jobId
             </pre>
             *
             */
            final @QueryParam("orderBy") String orderBy,
            /**
             * Optionally set the order of the outputFiles for each jobs.
             *     orderFilesBy={name | date | size}, default sort order is by date.
             * By default they are ordered by 'date'.
             * This determines the sort order of files within each job results 'folder'.
             *
             * Examples,
             * <pre>
             # sort by date, descending order
             orderFilesBy=-date
             # sort by name, ascending order
             orderFilesBy=name
             # sort by size, ascending order ('+' is allowed but optional)
             orderFilesBy+name
             * </pre>
             */
            final @QueryParam("orderFilesBy") String orderFilesBy,
            final @DefaultValue("true") @QueryParam("includeChildren") boolean includeChildren,
            final @DefaultValue("true") @QueryParam("includeOutputFiles") boolean includeOutputFiles,
            final @DefaultValue("true") @QueryParam("includePermissions") boolean includePermissions,
            final @DefaultValue("true") @QueryParam("prettyPrint") boolean prettyPrint
    ) {

        final GpConfig gpConfig=ServerConfigurationFactory.instance();
        final GpContext userContext=Util.getUserContext(request);

        try {
            final String gpUrl=UrlUtil.getGpUrl(request);
            final String jobsResourcePath = uriInfo.getBaseUri().toString() + URI_PATH;
            final SearchQuery q = new SearchQuery.Builder(gpConfig, userContext, jobsResourcePath)
                    .userId(userId)
                    .groupId(groupId)
                    .batchId(batchId)
                    .tag(tag)
                    .comment(comment)
                    .pageNum(page)
                    .pageSize(pageSize)
                    .orderBy(orderBy)
                    .orderFilesBy(orderFilesBy)
                    .build();
            final SearchResults searchResults= JobSearch.doSearch(q);
            final List<JobInfo> jobInfoResults=searchResults.getJobInfos();

            //create JSON representation
            GetPipelineJobLegacy getJobImpl = new GetPipelineJobLegacy(gpUrl, jobsResourcePath);

            // Put the job JSON in an array
            JSONArray jobs = new JSONArray();
            for (final JobInfo jobInfo : jobInfoResults) {
                JSONObject jobObject = getJobImpl.getJob(userContext, jobInfo, includeChildren,
                        includeOutputFiles, includePermissions, includeComments, includeTags);
                //decorate with 'self'
                final String self=jobsResourcePath+"/"+jobObject.getString("jobId");
                jobObject.put("self", self);

                //check whether to include comments
                /*if(includeComments)
                {
                    JobCommentsResource jcRes = new JobCommentsResource();
                    jobObject.put("comments", jcRes.loadComments(userContext, jobInfo.getJobNumber()));
                }

                //check whether to include tags
                if(includeTags)
                {
                    JobTagsResource jcRes = new JobTagsResource();
                    jobObject.put("tags", jcRes.loadTags(userContext, jobInfo.getJobNumber()));
                }*/

                jobs.put(jobObject);
            }

            JSONObject jsonObj=new JSONObject();
            jsonObj.put("items", jobs);

            JSONObject nav = searchResults.navigationDetailsToJson();
            jsonObj.put("nav", nav);

            final String jsonStr;
            if (prettyPrint) {
                final int indentFactor=2;
                jsonStr=jsonObj.toString(indentFactor);
            }
            else {
                jsonStr=jsonObj.toString();
            }
            return Response.ok()
                    .entity(jsonStr)
                    .build();

        }
        catch (Throwable t) {
            log.error(t);
            final String message="Error in job search: "+t.getLocalizedMessage();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .build();
        }
    }

    /**
     * Get a JSON object of the job's permissions, including a list of all possible groups to share with
     * and the current permission status of each group.
     *
     * @param request
     * @param jobId
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{jobId}/permissions")
    public Response getJobPermissions(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {
        GpContext jobContext = Util.getJobContext(request, jobId);
        GpContext userContext = Util.getUserContext(request);
        JobInfo jobInfo = jobContext.getJobInfo();

        PermissionsHelper ph = new PermissionsHelper(
                userContext.isAdmin(), //final boolean _isAdmin,
                userContext.getUserId(), // final String _userId,
                jobInfo.getJobNumber(), // final int _jobNo,
                jobInfo.getUserId(), //final String _rootJobOwner,
                jobInfo.getJobNumber()//, //final int _rootJobNo,
        );

        try {
            // Create the user permissions object
            JSONObject userPerms = GetPipelineJobLegacy.permissionsToJson(userContext, ph);

            // Return the response
            return Response.ok().entity(userPerms.toString()).build();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error obtaining permissions").build();
        }
    }

    /**
     * Sets the permissions for a job
     *
     * Takes an array of permissions objects and sets the permissions for each group with a matching ID
     *      Ex: {id: GROUP_ID, read: BOOLEAN, write: BOOLEAN}
     *
     * Returns the job's new set of permissions
     *
     * @param request
     * @param jobId
     * @return
     */
    @PUT
    //@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{jobId}/permissions")
    public Response setJobPermissions(@Context HttpServletRequest request, @PathParam("jobId") String jobId, String payload) {
        GpContext jobContext = Util.getJobContext(request, jobId);
        GpContext userContext = Util.getUserContext(request);
        JobInfo jobInfo = jobContext.getJobInfo();

        PermissionsHelper ph = new PermissionsHelper(
                userContext.isAdmin(), //final boolean _isAdmin,
                userContext.getUserId(), // final String _userId,
                jobInfo.getJobNumber(), // final int _jobNo,
                jobInfo.getUserId(), //final String _rootJobOwner,
                jobInfo.getJobNumber()//, //final int _rootJobNo,
        );

        try {
            // Parse the payload
            JSONArray setPerms = new JSONArray(payload);

            // Set the permissions
            if(!ph.canSetJobPermissions()) {
                throw new Exception("Cannot set permissions on job: " + jobId);
            }
            else {
                // New permissions
                Set<GroupPermission> newPermissions = new HashSet<GroupPermission>();

                List<GroupPermission> perms = ph.getJobResultPermissions(true);

                // Add public to list
                GroupPermission.Permission pubPerm = ph.getPublicAccessPermission();
                perms.add(new GroupPermission("*", pubPerm));

                // Set the permissions for each group
                for (int i = 0; i < setPerms.length(); i++) {
                    JSONObject iPerm = setPerms.getJSONObject(i);

                    // New permission being set, add
                    GroupPermission gp = null;
                    if (iPerm.getBoolean("write")) {
                        gp = new GroupPermission(iPerm.getString("id"), GroupPermission.Permission.READ_WRITE);
                        newPermissions.add(gp);
                    } else if (iPerm.getBoolean("read")) {
                        gp = new GroupPermission(iPerm.getString("id"), GroupPermission.Permission.READ);
                        newPermissions.add(gp);
                    } else {
                        // new GroupPermission(iPerm.getString("id"), GroupPermission.Permission.NONE);
                    }
                }

                ph.setPermissions(newPermissions);
            }

            // Create the JSON Object to return
            JSONObject toReturn = GetPipelineJobLegacy.permissionsToJson(userContext, ph);

            // Return the response
            return Response.ok().entity(toReturn.toString()).build();
        }
        catch (JSONException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error setting permissions").build();
        } catch (Exception e) {
            return Response.status(Response.Status.FORBIDDEN).entity(e.getLocalizedMessage()).build();
        }
    }



    ////////////////////////////////////
    // Getting a job
    ////////////////////////////////////
    /**
     * GET a job, by jobId.
     *
     * Example
     * <pre>
     curl -D headers.txt -u test:test http://127.0.0.1:8080/gp/rest/v1/jobs/9140?includeChildren=true
     * </pre>
     * @param request
     * @param jobId
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{jobId}")
    public Response getJob(
            final @Context UriInfo uriInfo,
            final @Context HttpServletRequest request,
            final @PathParam("jobId") String jobId,
            final @DefaultValue("false") @QueryParam("includePermissions") boolean includePermissions,
            final @DefaultValue("true") @QueryParam("includeChildren") boolean includeChildren,
            final @DefaultValue("true") @QueryParam("includeOutputFiles") boolean includeOutputFiles,
            final @DefaultValue("true") @QueryParam("prettyPrint") boolean prettyPrint
    ) {

        final GpContext jobContext=Util.getJobContext(request, jobId);

        final String gpUrl=UrlUtil.getGpUrl(request);
        final String self=uriInfo.getAbsolutePath().toString();
        final URI baseUri=uriInfo.getBaseUri();
        final String jobsResourcePath=baseUri.toString()+URI_PATH;
        final GetPipelineJobLegacy getJobImpl = new GetPipelineJobLegacy(gpUrl, jobsResourcePath);
        String jsonStr;
        try {

            JSONObject job=null;
            job=getJobImpl.getJob(jobContext, jobContext.getJobInfo(), includeChildren, includeOutputFiles,
                    includePermissions, includeComments, includeTags);
            if (job==null) {
                throw new Exception("Unexpected null return value");
            }
            //decorate with 'self'
            job.put("self", self);
            if (prettyPrint) {
                final int indentFactor=2;
                jsonStr=job.toString(indentFactor);
            }
            else {
                jsonStr=job.toString();
            }

            //for debugging
            if (log.isDebugEnabled()) {
                try {
                    if (job.getJSONObject("status").getBoolean("isFinished")) {
                        if (job.getInt("numOutputFiles")==0) {
                            log.debug("Hmmm ... no output files for a completed job");
                        }
                    }
                }
                catch (Throwable t) {
                    //ignore
                    log.error("Unexpected error in debugging code", t);
                }
            }
        }
        catch (Throwable t) {
            //TODO: customize the response errors, e.g.
            //    404 Not found, when the job with given job_id is no longer in the DB
            //    ?, when job_id is not set
            //    ?, when job_id is invalid, e.g. not an integer
            //    ?, when current user does not have read access to the job
            final String message="Error creating JSON representation for jobId="+jobId+": "+t.getLocalizedMessage();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .build();
        }

        //return the JSON representation of the job
        return Response.ok()
                .entity(jsonStr)
                .build();
    }

    /**
     * GET status.json for the given jobId.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{jobId}/status.json")
    public Response getStatus(
            final @Context UriInfo uriInfo,
            final @Context HttpServletRequest request,
            final @PathParam("jobId") String jobId
    ) {

        final GpContext jobContext=Util.getJobContext(request, jobId);
        try {
            final String gpUrl=UrlUtil.getGpUrl(request);
            final Status status = new JobStatusLoaderFromDb(gpUrl).loadJobStatus(jobContext);

            final JSONObject jsonObj = status.toJsonObj();
            final String jsonStr = jsonObj.toString(2);
            return Response.ok()
                    .entity(jsonStr)
                    .build();

        }
        catch (Throwable t) {
            String errorMessage="Error getting status.json for jobId="+jobId;
            log.error(errorMessage, t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMessage)
                    .build();
        }
    }

    /**
     * Terminate the specified job
     * @param request
     * @param jobId
     * @return
     */
    @DELETE
    @Path("/{jobId}/terminate")
    public Response terminateJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {
        GpContext userContext = Util.getUserContext(request);

        try {
            int intJobId = Integer.parseInt(jobId);
            JobManager.terminateJob(userContext.isAdmin(), userContext.getUserId(), intJobId);
            return Response.ok().entity("Terminated Job: " + intJobId).build();
        }
        catch (Throwable t) {
            log.error("job termination error, jobId="+jobId, t);
            return Response.status(500).entity("Could not terminate job " + jobId + " " + t.getLocalizedMessage()).build();
        }
    }

    /**
     * Get code for the specified job
     * @param request
     * @param response
     * @param jobId
     * @param language
     * @return
     */
    @GET
    @Path("/{jobId}/code")
    public Response jobCode(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("jobId") String jobId, @QueryParam("language") String language) {
        GpContext userContext = Util.getUserContext(request);

        try {
            int jobNumber = Integer.parseInt(jobId);
            JobInfo jobInfo = new AnalysisDAO().getJobInfo(jobNumber);
            AnalysisJob job = new AnalysisJob(userContext.getUserId(), jobInfo);
            String filename = jobId + CodeGeneratorUtil.getFileExtension(language);

            response.setHeader("Content-disposition", "inline; filename=\"" + filename + "\"");
            response.setHeader("Content-Type", "text/plain");
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache
            response.setDateHeader("Expires", 0);
            OutputStream os = response.getOutputStream();

            IAdminClient adminClient = new LocalAdminClient(userContext.getUserId());
            TaskInfo taskInfo = adminClient.getTask(job.getLSID());

            String code = CodeGeneratorUtil.getCode(language, job, taskInfo, adminClient);

            PrintWriter pw = new PrintWriter(os);
            pw.println(code);
            pw.flush();
            os.close();

            return Response.ok().build();
        }
        catch (Exception e) {
            log.error("Error viewing code for job " + jobId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build();
        }
    }

    /**
     * Delete the specified job
     * @param request
     * @param jobId
     * @return
     */
    @DELETE
    @Path("/{jobId}/delete")
    public Response deleteJob(@Context HttpServletRequest request, @PathParam("jobId") String jobId) {
        GpContext userContext = Util.getUserContext(request);

        try {
            String userId = userContext.getUserId();
            boolean isAdmin = userContext.isAdmin();
            int intJobId = Integer.parseInt(jobId);

            List<Integer> deleted = JobManager.deleteJob(isAdmin, userId, intJobId);

            if (deleted.size() > 0) {
                return Response.ok().entity("Deleted Jobs: " + deleted.toString()).build();
            }
            else {
                return Response.status(500).entity("Could not delete job " + jobId).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Delete the a list of jobs
     * @param request
     * @param jobs - a comma separated list of job IDs
     * @return
     */
    @DELETE
    @Path("/delete")
    public Response deleteJobs(@Context HttpServletRequest request, @QueryParam("jobs") String jobs) {
        GpContext userContext = Util.getUserContext(request);

        try {
            // Split the jobs string on commas
            String[] jobStrings = jobs.split(",");
            int[] jobIds = new int[jobStrings.length];

            // Parse individual job IDs to ints
            int index = 0;
            for (String job : jobStrings) {
                int intJobId = Integer.parseInt(job);
                jobIds[index] = intJobId;
                index++;
            }

            // Delete each of the jobs
            String userId = userContext.getUserId();
            boolean isAdmin = userContext.isAdmin();
            List<Integer> deleted = new ArrayList<Integer>();
            for (int id : jobIds) {
                deleted.addAll(JobManager.deleteJob(isAdmin, userId, id));
            }

            if (deleted.size() > 0) {
                return Response.ok().entity("Deleted Jobs: " + deleted.toString()).build();
            }
            else {
                return Response.status(500).entity("Could not delete jobs: " + jobs).build();
            }
        }
        catch (Throwable t) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }

    /**
     * Sets the correct download headers and serves up the zip file for the job
     * @param request
     * @param response
     * @param jobId
     * @return
     */
    @GET
    @Path("/{jobId}/download")
    public Response downloadJob(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("jobId") String jobId) {
        GpContext userContext = Util.getUserContext(request);
        String contextPath = request.getContextPath();
        String cookie = request.getHeader("Cookie");

        try {
            AnalysisDAO dao = new AnalysisDAO();
            int id = Integer.parseInt(jobId);

            JobInfoManager manager = new JobInfoManager();
            JobInfoWrapper wrapper = manager.getJobInfo(cookie, contextPath, userContext.getUserId(), id);

            response.setHeader("Content-Disposition", "attachment; filename=" + jobId + ".zip" + ";");
            response.setHeader("Content-Type", "application/octet-stream");
            response.setHeader("Cache-Control", "no-store");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            OutputStream os = response.getOutputStream();
            JobInfoManager.writeOutputFilesToZipStream(os, wrapper);
            os.close();
        }
        catch (Throwable t) {
            String message = "Error downloading output files for job " + jobId + ": " + t.getLocalizedMessage();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(message).build();
        }

        return Response.ok().build();
    }

    /**
     * Get a JSON List of the JSOn objects for the most recent jobs,
     * as well as the total number of processing jobs for the current user.
     *
     * Response template:
     * <pre>
     {
     numProcessingJobs: 14,
     recentJobs: [ {}, {}, ..., {} ]
     }
     * </pre>
     *
     * @param uriInfo
     * @param request
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/recent")
    public Response getRecentJobs (@Context UriInfo uriInfo, @Context HttpServletRequest request,
                                   @DefaultValue("true") @QueryParam("includeChildren") boolean includeChildren,
                                   @DefaultValue("true") @QueryParam("includeOutputFiles") boolean includeOutputFiles
    ) {
        final GpContext userContext = Util.getUserContext(request);

        try {
            final String gpUrl=UrlUtil.getGpUrl(request);
            // Get the number of recent jobs to show
            UserDAO userDao = new UserDAO();
            Set<UserProp> props = userDao.getUserProps(userContext.getUserId());
            int recentJobsToShow = Integer.parseInt(UserDAO.getPropertyValue(props, UserPropKey.RECENT_JOBS_TO_SHOW, "10"));

            // Get the recent jobs
            AnalysisDAO dao = new AnalysisDAO();
            int numProcessingJobs=dao.getNumProcessingJobsByUser(userContext.getUserId());
            List<JobInfo> recentJobs = dao.getRecentJobsForUser(userContext.getUserId(), recentJobsToShow, Analysis.JobSortOrder.SUBMITTED_DATE);

            // Create the object for getting the job JSON
            URI baseUri = uriInfo.getBaseUri();
            String jobsResourcePath = baseUri.toString() + URI_PATH;
            GetPipelineJobLegacy getJobImpl = new GetPipelineJobLegacy(gpUrl, jobsResourcePath);

            // Put the job JSON in an array
            boolean includePermissions = false;
            JSONArray jobs = new JSONArray();
            for (JobInfo jobInfo : recentJobs) {
                JSONObject jobObject = getJobImpl.getJob(userContext, jobInfo, includeChildren, includeOutputFiles,
                        includePermissions, includeComments, includeTags);
                jobs.put(jobObject);
            }

            // Return the JSON representation of the jobs
            JSONObject jsonObj=new JSONObject();
            jsonObj.put("recentJobs", jobs);
            jsonObj.put("numProcessingJobs", numProcessingJobs);

            final int indentFactor=2;
            String jsonStr=jsonObj.toString(indentFactor);
            return Response.ok().entity(jsonStr).build();
        }
        catch (Throwable t) {
            String message = "Error creating JSON representation for recent jobs: " + t.getLocalizedMessage();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(message).build();
        }
    }

    /**
     * GET children for the given jobId.
     *
     * Example
     * <pre>
     curl -D headers.txt -u test:test http://127.0.0.1:8080/gp/rest/v1/jobs/9140/children
     * </pre>
     * @param request
     * @param jobId
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{jobId}/children")
    public Response getChildren(
            final @Context UriInfo uriInfo,
            final @Context HttpServletRequest request,
            final @PathParam("jobId") String jobId,
            final @DefaultValue("true") @QueryParam("includeOutputFiles") boolean includeOutputFiles
    ) {

        final GpContext userContext=Util.getUserContext(request);
        final String self=uriInfo.getAbsolutePath().toString();
        final String gpUrl=UrlUtil.getGpUrl(request);
        final URI baseUri=uriInfo.getBaseUri();
        final String jobsResourcePath=baseUri.toString()+URI_PATH;
        final GetPipelineJobLegacy getJobImpl = new GetPipelineJobLegacy(gpUrl, jobsResourcePath);
        String jsonStr;
        try {
            boolean includeChildren=true;
            JSONObject children=getJobImpl.getChildren(userContext, jobId, includeChildren, includeOutputFiles);
            if (children==null) {
                throw new Exception("Unexpected null return value");
            }
            //decorate with 'self'
            children.put("href", self);
            jsonStr=children.toString();
        }
        catch (Throwable t) {
            //TODO: customize the response errors, e.g.
            //    404 Not found, when the job with given job_id is no longer in the DB
            //    ?, when job_id is not set
            //    ?, when job_id is invalid, e.g. not an integer
            //    ?, when current user does not have read access to the job
            final String message="Error creating JSON representation for jobId="+jobId+": "+t.getLocalizedMessage();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .build();
        }

        //return the JSON representation of the job
        return Response.ok()
                .entity(jsonStr)
                .build();
    }

    /**
     * Returns a list of job numbers for currently pending or running jobs
     * @param request
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/incomplete")
    public Response isJobRunning(@Context HttpServletRequest request) {
        GpContext userContext = Util.getUserContext(request);
        final String userId = userContext.getUserId();

        final boolean isInTransaction = HibernateUtil.isInTransaction();
        try {
            // Get the map of the latest tasks
            AnalysisDAO analysisDao = new AnalysisDAO();
            List<JobInfo> jobs = analysisDao.getIncompleteJobsForUser(userId);

            // Return the JSON object
            JSONArray jsonArray = new JSONArray();
            for (JobInfo jobInfo : jobs) {
                jsonArray.put(jobInfo.getJobNumber());
            }
            return Response.ok().entity(jsonArray.toString()).build();
        }
        catch (Throwable t) {
            log.error(t);
            String errorMessage = "Error constructing json response for /jobs/incomplete: " + t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

    @GET
    @Path("{jobNo}/comments")
    public Response loadComments(
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        JobCommentsResource res = new JobCommentsResource();
        return res.loadComments(jobNo, request);
    }

    @POST
    @Path("/{jobNo}/comments/add")
    public Response addComment(
            MultivaluedMap<String,String> multivaluedMap,
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        JobCommentsResource res = new JobCommentsResource();
        return res.addComment(multivaluedMap, jobNo, request);
    }

    @POST
    @Path("/{jobNo}/comments/add/{id}")
    public Response editComment(
            MultivaluedMap<String,String> multivaluedMap,
            @PathParam("id") String id,
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        JobCommentsResource res = new JobCommentsResource();
        return res.editComment(multivaluedMap, id, jobNo, request);
    }

    @POST
    @Path("/{jobNo}/comments/delete")
    public Response deleteComment(
            MultivaluedMap<String,String> multivaluedMap,
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        JobCommentsResource res = new JobCommentsResource();
        return res.deleteComment(multivaluedMap, jobNo, request);
    }

    @GET
    @Path("{jobNo}/tags")
    public Response loadTags(
            @PathParam("jobNo") String jobNo,
            @Context HttpServletRequest request)
    {
        JobTagsResource res = new JobTagsResource();
        return res.loadTags(jobNo, request);
    }

    @POST
    @Path("/{jobNo}/tags/add")
    public Response addTag(
            @PathParam("jobNo") int jobNo,
            @QueryParam("tagText") String tagText,
            @Context HttpServletRequest request)
    {
        JobTagsResource res = new JobTagsResource();
        return res.addTag(jobNo, tagText, request);
    }

    @POST
    @Path("/{jobNo}/tags/delete")
    public Response deleteTag(
            @PathParam("jobNo") int jobNo,
            @QueryParam("jobTagId") int jobTagId,
            @Context HttpServletRequest request)
    {
        JobTagsResource res = new JobTagsResource();
        return res.deleteTag(jobNo, jobTagId, request);
    }
}

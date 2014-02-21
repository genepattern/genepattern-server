package org.genepattern.server.webapp.rest.api.v1.job;

import java.net.URI;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApi;
import org.genepattern.server.rest.JobInputApiFactory;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.user.UserProp;
import org.genepattern.server.user.UserPropKey;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webapp.rest.api.v1.job.JobInputValues.Param;
import org.genepattern.server.webservice.server.Analysis;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
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
 * <p>To add a batch of job to the server, use the 'isBatchParam' property.</p>
 * <pre>
   {
     "lsid":<actualLsid>,
     "params": [
       { "name": <paramName>,
         "isBatch": <true | false>, //if not set, it means it's not a batch parameter
         "batchFilter": //if set, this can be used to define a glob patter for matching input files in the given directory
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
        final GpContext jobContext=Util.getUserContext(request);
        
        final JSONObject rval=new JSONObject();
        try {
            //TODO: add support for batch jobs to REST API
            final JobInput jobInput=parseJobInput(jobInputValues);
            final boolean initDefault=true;
            final JobInputApi impl = JobInputApiFactory.createJobInputApi(jobContext, initDefault);
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
    
    private JobInput parseJobInput(final JobInputValues jobInputValues) {
        JobInput jobInput=new JobInput();
        jobInput.setLsid(jobInputValues.lsid);
        for(final Param param : jobInputValues.params) {
            for(final String value : param.values) {
                jobInput.addValue(param.name, value, param.batchParam);
            }
        }
        return jobInput;
    }
    
    ////////////////////////////////////
    // Getting a job
    ////////////////////////////////////
    /**
     * GET a job, by jobId.
     * 
     * Example
     * <pre>
       curl -D headers.txt -u test:test http://127.0.0.1:8080/gp/rest/v1/jobs/9140
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
            final @PathParam("jobId") String jobId
    ) {
        
        final GpContext userContext=Util.getUserContext(request);
        
        final String self=uriInfo.getAbsolutePath().toString();
        final URI baseUri=uriInfo.getBaseUri();
        final String jobsResourcePath=baseUri.toString()+URI_PATH;
        final GetPipelineJobLegacy getJobImpl = new GetPipelineJobLegacy(jobsResourcePath);
        String jsonStr;
        try {
            //TODO: make this a configurable flag
            final boolean includeChildren=true;
            JSONObject job=null;
            job=getJobImpl.getJob(userContext, jobId, includeChildren);
            if (job==null) {
                throw new Exception("Unexpected null return value");
            }
            //decorate with 'self'
            job.put("self", self);
            jsonStr=job.toString();
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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/recent")
    public Response getRecentJobs (@Context UriInfo uriInfo, @Context HttpServletRequest request) {
        GpContext userContext = Util.getUserContext(request);

        try {
            // Get the number of recent jobs to show
            UserDAO userDao = new UserDAO();
            Set<UserProp> props = userDao.getUserProps(userContext.getUserId());
            int recentJobsToShow = Integer.parseInt(UserDAO.getPropertyValue(props, UserPropKey.RECENT_JOBS_TO_SHOW, "10"));

            // Get the recent jobs
            AnalysisDAO dao = new AnalysisDAO();
            List<JobInfo> recentJobs = dao.getRecentJobsForUser(userContext.getUserId(), recentJobsToShow, Analysis.JobSortOrder.SUBMITTED_DATE);

            // Create the object for getting the job JSON
            URI baseUri = uriInfo.getBaseUri();
            String jobsResourcePath = baseUri.toString() + URI_PATH;
            GetPipelineJobLegacy getJobImpl = new GetPipelineJobLegacy(jobsResourcePath);

            // Put the job JSON in an array
            JSONArray jobs = new JSONArray();
            for (JobInfo jobInfo : recentJobs) {
                JSONObject jobObject = getJobImpl.getJob(jobInfo, true);
                jobs.put(jobObject);
            }

            // Return the JSON representation of the jobs
            return Response.ok().entity(jobs.toString()).build();
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
            final @PathParam("jobId") String jobId
    ) {
        
        final GpContext userContext=Util.getUserContext(request);
        final String self=uriInfo.getAbsolutePath().toString();
        final URI baseUri=uriInfo.getBaseUri();
        final String jobsResourcePath=baseUri.toString()+URI_PATH;
        final GetPipelineJobLegacy getJobImpl = new GetPipelineJobLegacy(jobsResourcePath);
        String jsonStr;
        try {
            JSONObject children=getJobImpl.getChildren(userContext, jobId);
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

}

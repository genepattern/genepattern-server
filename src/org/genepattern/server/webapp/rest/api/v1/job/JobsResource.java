package org.genepattern.server.webapp.rest.api.v1.job;

import java.net.URI;

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
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApiImpl;
import org.genepattern.server.webapp.rest.api.v1.job.JobInputValues.Param;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;
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
 * @author pcarr
 *
 */
@Path("/v1/jobs")
public class JobsResource {
    final static private Logger log = Logger.getLogger(JobsResource.class);
    
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
        final ServerConfiguration.Context jobContext=TasksResource.getUserContext(request);
        
        final JSONObject rval=new JSONObject();
        try {
            final JobInput jobInput=parseJobInput(jobInputValues);
            final JobInputApiImpl impl = new JobInputApiImpl();
            final String jobId = impl.postJob(jobContext, jobInput);
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
                jobInput.addValue(param.name, value);
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
        
        final ServerConfiguration.Context userContext=TasksResource.getUserContext(request);
        JSONObject job=null;
        GetJob getJobImpl = new GetJobLegacy();
        String jsonStr;
        try {
            job=getJobImpl.getJob(userContext, jobId);
            if (job==null) {
                throw new Exception("Unexpected null return value");
            }
            //decorate with 'self'
            final String self=uriInfo.getAbsolutePath().toString();
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

}

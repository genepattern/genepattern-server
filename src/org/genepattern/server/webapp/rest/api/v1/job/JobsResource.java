package org.genepattern.server.webapp.rest.api.v1.job;

import java.io.File;
import java.net.URI;
import java.net.URL;

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
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.dm.jobresult.JobResultFile;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobInputApiImpl;
import org.genepattern.server.webapp.rest.api.v1.job.JobInputValues.Param;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
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
            rval.put("lsid", jobInputValues.lsid);
            final JobInput jobInput=parseJobInput(jobInputValues);
            final JobInputApiImpl impl = new JobInputApiImpl();
            final String jobId = impl.postJob(jobContext, jobInput);
            rval.put("jobId", jobId);
            
            //set the Location header to the URI of the newly created resource
            final URI uri = uriInfo.getAbsolutePathBuilder().path(jobId).build();
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
        JSONObject job;
        GetJob getJobImpl = new GetJobLegacy();
        try {
            job=getJobImpl.getJob(userContext, jobId);
            
            //decorate with 'self'
            final String self=uriInfo.getAbsolutePath().toString();
            job.put("self", self);
        }
        catch (Throwable t) {
            //TODO: customize the response errors, e.g.
            //    404 Not found, when the job with given job_id is no longer in the DB
            //    ?, when job_id is not set
            //    ?, when job_id is invalid, e.g. not an integer
            //    ?, when current user does not have read access to the job
            
            String errorMessage="Error getting job with jobId="+jobId+
                    ": "+t.getLocalizedMessage();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(errorMessage)
                    .build();
        }
        String content=null;
        if (job!=null) {
            content=job.toString();
        }
        if (content==null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating JSON representation for jobId="+jobId)
                    .build();
        }

        //return the JSON representation of the job
        return Response.ok()
                .entity(content)
                .build();
    }
    

    static class GetJobException extends Exception {
        public GetJobException() {
        }
        public GetJobException(final String message) {
            super(message);
        }
    }
    static interface GetJob {
        JSONObject getJob(final ServerConfiguration.Context userContext, final String jobId)
        throws GetJobException;
    }
    
    static class JobRecord extends JSONObject {
        public JobRecord(final String jobId) throws JSONException {
            this.put("job_id", jobId);
        }
    }
    
    static class GetJobLegacy implements GetJob {
        /**
         * helper method which indicates if the job has completed processing.
         */
        public static boolean isFinished(final JobInfo jobInfo) {
            if ( JobStatus.FINISHED.equals(jobInfo.getStatus()) ||
                    JobStatus.ERROR.equals(jobInfo.getStatus()) ) {
                return true;
            }
            return false;        
        }
        
        public static boolean hasError(final JobInfo jobInfo) {
            return JobStatus.ERROR.equals(jobInfo.getStatus());
        }
        
        public static URL getStderrLocation(final JobInfo jobInfo) throws Exception {
            for(ParameterInfo pinfo : jobInfo.getParameterInfoArray()) {
                if (pinfo._isStderrFile()) {
                    //construct URI to the file
                    //Hint: the name of the parameter is the name of the file (e.g. name=stderr.txt)
                    //      the value of the parameter includes the jobId (e.g. 2137/stderr.txt)
                    String name=pinfo.getName();
                    JobResultFile stderr=new JobResultFile(jobInfo, new File(name));
                    return stderr.getUrl();
                }
            }
            return null;
        }
        
        public JSONObject getJob(final ServerConfiguration.Context userContext, final String jobId) 
        throws GetJobException
        {
            if (userContext==null) {
                throw new IllegalArgumentException("userContext==null");
            }
            if (userContext.getUserId()==null || userContext.getUserId().length() == 0) {
                throw new IllegalArgumentException("userContext.userId not set");
            }
            
            JobInfo jobInfo=null;
            //expecting jobId to be an integer
            int jobNo;
            try {
                jobNo=Integer.parseInt(jobId);
            }
            catch (NumberFormatException e) {
                throw new GetJobException("Expecting an integer value for jobId="+jobId+" :"
                        +e.getLocalizedMessage());
            }
            if (jobNo<0) {
                throw new GetJobException("Invalid jobNo="+jobNo+" : Can't be less than 0.");
            }
            final boolean isInTransaction=HibernateUtil.isInTransaction();
            try {
                AnalysisDAO analysisDao = new AnalysisDAO();
                jobInfo = analysisDao.getJobInfo(jobNo);
            }
            catch (Throwable t) {
                log.error("Error initializing jobInfo for jobId="+jobId, t);
            }
            finally {
                if (!isInTransaction) {
                    HibernateUtil.closeCurrentSession();
                }
            }
            
            //manually create a JSONObject representing the job
            JSONObject job = new JSONObject();
            try {
                job.put("jobId", jobId);
                
                //init jobStatus
                JSONObject jobStatus = new JSONObject();
                boolean isFinished=isFinished(jobInfo);
                jobStatus.put("isFinished", isFinished);
                boolean hasError=hasError(jobInfo);
                jobStatus.put("hasError", hasError);
                URL stderr=null;
                try {
                    stderr=getStderrLocation(jobInfo);
                }
                catch (Throwable t) {
                    log.error("Error getting stderr file for jobId="+jobId, t);
                }
                if (stderr != null) {
                    //TODO: come up with a standard JSON representation of a gp job result file
                    jobStatus.put("stderrLocation", stderr.toExternalForm());
                }
                
                job.put("status", jobStatus);
            }
            catch (JSONException e) {
                log.error("Error initializing JSON representation for jobId="+jobId, e);
                throw new GetJobException("Error initializing JSON representation for jobId="+jobId+
                        ": "+e.getLocalizedMessage());
            }
            return job;
        }
    }
    
    //proposed model for JobStatus
    /*
     jobStatus: {
         isFinished: Boolean
         hasError: Boolean
         status: String [ Pending | Processing | Finished ]
         exitCode: Integer
         stderr: URI to file
         //optionally, expand contents of stderr and return
     }
     
     */



}

package org.genepattern.server.webapp.rest.api.v1.job;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.domain.JobStatus;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;
import org.genepattern.server.webservice.server.dao.AnalysisDAO;
import org.genepattern.webservice.JobInfo;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Rest API calls to GET a job record from the server.
 * 
 * @author pcarr
 *
 */
@Path("/v1/job")
public class JobResource {
    final static private Logger log = Logger.getLogger(JobResource.class);

    /**
     * GET a job, by jobId.
     * 
     * Example
     * <pre>
       curl -D headers.txt -u test:test http://127.0.0.1:8080/gp/rest/v1/job/9140
     * </pre>
     * @param request
     * @param jobId
     * @return
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{jobId}")
    public Response getJob(
            final @Context HttpServletRequest request,
            final @PathParam("jobId") String jobId
    ) {
        
        final ServerConfiguration.Context userContext=TasksResource.getUserContext(request);
        JSONObject job;
        GetJob getJobImpl = new GetJobLegacy();
        try {
            job=getJobImpl.getJob(userContext, jobId);
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
        
        
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
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
                boolean isFinished=isFinished(jobInfo);
                job.put("isFinished", isFinished);
                if (isFinished) {
                    boolean hasError=hasError(jobInfo);
                    job.put("hasError", hasError);
                }
            }
            catch (JSONException e) {
                log.error("Error initializing JSON representation for jobId="+jobId, e);
                throw new GetJobException("Error initializing JSON representation for jobId="+jobId+
                        ": "+e.getLocalizedMessage());
            }
            return job;
        }
    }
}

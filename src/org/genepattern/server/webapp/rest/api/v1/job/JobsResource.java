package org.genepattern.server.webapp.rest.api.v1.job;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
 *      http://127.0.0.1:8080/gp/rest/jobs
 * </pre>
 * 
 * @author pcarr
 *
 */
@Path("/jobs")
public class JobsResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON) 
    @Produces(MediaType.APPLICATION_JSON)
    public Response addJob(@Context HttpServletRequest request, JobInputValues jobInputValues) {
        final ServerConfiguration.Context jobContext=TasksResource.getUserContext(request);
        
        final JSONObject rval=new JSONObject();
        try {
            rval.put("lsid", jobInputValues.lsid);
            final JobInput jobInput=parseJobInput(jobInputValues);
            final JobInputApiImpl impl = new JobInputApiImpl();
            final String jobId = impl.postJob(jobContext, jobInput);
            rval.put("jobId", jobId);
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
        
        return Response.ok().entity(rval.toString()).build();
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

}

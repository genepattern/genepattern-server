package org.genepattern.server.webapp.rest.api.v1.task;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.Responses;

/**
 * RESTful implementation of the /task resource.
 * 
 * Example usage, via curl command line.
   <p>To get the task_info for a given task name:
   <pre>
   curl -u test:test http://127.0.0.1:8080/gp/rest/v1/tasks/ComparativeMarkerSelection
   </pre>
   <p>Or by task lsid:
   <pre>
   curl -u test:test http://127.0.0.1:8080/gp/rest/v1/tasks/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9
   </pre>
   
   This returns a JSON representation of the task, for example,
   <pre>
{
"self":"http://127.0.0.1:8080/gp/rest/v1/tasks/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00007:0.1",
"name":"TestJavaWrapper",
"lsid":"urn:lsid:broad.mit.edu:cancer.software.genepattern.module.test.analysis:00007:0.1",
"params":[
  {"text": {
     "attributes": {
       "default_value":"",
       "optional":"on",
       "prefix_when_specified":"--text=",
       "type":"java.lang.String",
       "fileFormat":""}}},
       
   ...
   
  {"file":{
    "attributes":{
      "default_value":"",
      "optional":"on",
      "prefix_when_specified":"--file=",
      "MODE":"IN",
      "type":"java.io.File",
      "TYPE":"FILE",
      "fileFormat":""}}}
]
}

   </pre>
 *
 * The value of the lower-case 'type' attribute should be used when testing for the type of input parameter.
 * Here are some example values:
   <pre>
"type":"java.io.File"
"type":"java.lang.String"
"type":"java.lang.Integer"
"type":"java.lang.Float"
"type":"DIRECTORY"
"type":"PASSWORD"
   </pre>
 *   
 * 
 * @author pcarr
 *
 */
@Path("/v1/tasks")
public class TasksResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{taskNameOrLsid}")
    public Response getTask(
            final @Context UriInfo uriInfo,
            final @PathParam("taskNameOrLsid") String taskNameOrLsid,
            final @Context HttpServletRequest request
            ) {
        ServerConfiguration.Context userContext=Util.getUserContext(request);
        final String userId=userContext.getUserId();
        TaskInfo taskInfo = null;
        try {
            taskInfo=getTaskInfo(taskNameOrLsid, userId);
        }
        catch (Throwable t) {
            return Responses.notFound().entity(t.getLocalizedMessage()).build();
        }
        if(taskInfo == null) {
            String errorMessage="No task with task id: " + taskNameOrLsid + " found " + "for user " + userId;
            return Responses.notFound().entity(errorMessage).build();
        }
        
        //form a JSON response, from the given taskInfo
        JSONObject jsonObj=null;
        try {
            jsonObj=new JSONObject();
            URI baseUri=uriInfo.getBaseUri();
            String self=baseUri.toURL().toExternalForm();
            if (!self.endsWith("/")) {
                self += "/";
            }
            self += "v1/tasks/" + taskInfo.getLsid();
            jsonObj.put("self", self);
            jsonObj.put("name", taskInfo.getName());
            jsonObj.put("lsid", taskInfo.getLsid());
            JSONArray paramsJson=new JSONArray();
            for(ParameterInfo pinfo : taskInfo.getParameterInfoArray()) {
                final JSONObject attributesJson = new JSONObject();
                for(final Object key : pinfo.getAttributes().keySet()) {
                    final Object value = pinfo.getAttributes().get(key);
                    if (value != null) {
                        attributesJson.put(key.toString(), value.toString());
                    }
                }
                final JSONObject attrObj = new JSONObject();
                attrObj.put("attributes", attributesJson);
                final JSONObject paramJson = new JSONObject();
                paramJson.put(pinfo.getName(), attrObj);
                paramsJson.put(paramJson);
            }
            jsonObj.put("params", paramsJson);
        }
        catch (Throwable t) {
            final String errorMessage="Error constructing json response for task="+taskNameOrLsid+": "+
                    t.getLocalizedMessage();
            return Response.serverError().entity(errorMessage).build();
        }
        return Response.ok().entity(jsonObj.toString()).build();        
    }
    
    private TaskInfo getTaskInfo(final String taskLSID, final String username) 
    throws WebServiceException 
    {
        return new LocalAdminClient(username).getTask(taskLSID);
    }
}

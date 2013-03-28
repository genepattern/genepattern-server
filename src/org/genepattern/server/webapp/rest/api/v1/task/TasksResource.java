package org.genepattern.server.webapp.rest.api.v1.task;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineDependencyHelper;
import org.genepattern.modules.ModuleJSON;
import org.genepattern.modules.ParametersJSON;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONArray;
import org.json.JSONException;
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
   curl -u test:test http://127.0.0.1:8080/gp/rest/v1/tasks/ComparativeMarkerSelection
   </pre>
   curl -u test:test http://127.0.0.1:8080/gp/rest/v1/tasks/urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9
   
 * 
 * @author pcarr
 *
 */
@Path("/v1/tasks")
public class TasksResource {
    final static private Logger log = Logger.getLogger(TasksResource.class);
    private static class TaskNotFoundException extends Exception {
        public TaskNotFoundException(String message) {
            super(message);
        }
        public TaskNotFoundException(String message, Throwable t) {
            super(message, t);
        }
    }

    @Context
    UriInfo uriInfo;
    @Context
    Request request;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{lsid}")
    public Response getTask(@PathParam("lsid") String lsid, @Context HttpServletRequest request) {
        //Note: not using the userContext instance, but the method does throw an exception if
        //    there is not a current user
        ServerConfiguration.Context userContext=Util.getUserContext(request);

        JSONObject responseObject = null;
        try {
            responseObject=initTaskResponse(lsid, userContext.getUserId());
        }
        catch (TaskNotFoundException e) {
            return Responses.notFound().entity(e.getLocalizedMessage()).build();
        }
        catch (WebApplicationException e) {
            //send this along to the calling method, to formulate an HTTP error response
            throw e;
        }
        catch (JSONException e) {
            return Response.serverError()
                    .entity("Error formatting JSON response for lsid="+lsid+": "+e.getLocalizedMessage())
                    .build();
        }
        catch (Throwable t) {
            return Response.serverError()
                    .entity("Error getting task for lsid="+lsid+": "+t.getLocalizedMessage())
                    .build();
        }
        return Response.ok().entity(responseObject.toString()).build();
    }

    private JSONObject initTaskResponse(final String lsid, final String username)
    throws TaskNotFoundException, JSONException
    {
        TaskInfo taskInfo = null;
        try {
            taskInfo=getTaskInfo(lsid, username);
        }
        catch (Throwable t) {
            throw new TaskNotFoundException("Error initializing task lsid=" + lsid + " for user " + username, t);
        }
        if(taskInfo == null) {
            throw new TaskNotFoundException("No task with task id: " + lsid + " found " + "for user " + username);
        }
        
        List<String> moduleVersions=null;
        try {
            moduleVersions=getModuleVersions(taskInfo);
        }
        catch (MalformedURLException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error initializing moduleVersions for task=" + lsid)
                    .build()
                );
        }
        
        ModuleJSON moduleObject = new ModuleJSON(taskInfo, null);
        moduleObject.put("lsidVersions", new JSONArray(moduleVersions));

        //check if user is allowed to edit the module
        boolean createModuleAllowed = AuthorizationHelper.createModule(username);
        boolean editable = createModuleAllowed && taskInfo.getUserId().equals(username)
                && LSIDUtil.getInstance().isAuthorityMine(taskInfo.getLsid());
        moduleObject.put("editable", editable);

        //check if the user is allowed to view the module
        boolean isViewable = true;

        //check if the module has documentation
        boolean hasDoc = true;

        File[] docFiles = null;
        try {
            LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);
            docFiles = taskIntegratorClient.getDocFiles(taskInfo);

            if(docFiles == null || docFiles.length == 0) {
                hasDoc = false;
            }
        }
        catch (WebServiceException e) {
            log.error("Error getting doc files.", e);
        }
        moduleObject.put("hasDoc", hasDoc);

        //if this is a pipeline check if there are any missing dependencies
        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
        String taskType = tia.get(GPConstants.TASK_TYPE);
        boolean isPipeline = "pipeline".equalsIgnoreCase(taskType);
        if(isPipeline && PipelineDependencyHelper.instance().getMissingDependenciesRecursive(taskInfo).size() != 0)
        {
            moduleObject.put("missing_tasks", true);
        }
        else
        {
            moduleObject.put("missing_tasks", false);                        
        }
        JSONObject responseObject = new JSONObject();
        responseObject.put(ModuleJSON.KEY, moduleObject);

        JSONArray parametersObject = getParameterList(taskInfo.getParameterInfoArray());
        responseObject.put(ParametersJSON.KEY, parametersObject);
        
        return responseObject;
    }

    private TaskInfo getTaskInfo(final String taskLSID, final String username) 
    throws WebServiceException 
    {
        return new LocalAdminClient(username).getTask(taskLSID);
    }

    private List<String> getModuleVersions(final TaskInfo taskInfo) throws MalformedURLException
    {
        LSID taskLSID = new LSID(taskInfo.getLsid());
        String taskNoLSIDVersion = taskLSID.toStringNoVersion();

        ArrayList<String> moduleVersions = new ArrayList<String>();
        TaskInfo[] tasks = TaskInfoCache.instance().getAllTasks();
        for(int i=0;i<tasks.length;i++)
        {
            TaskInfoAttributes tia = tasks[i].giveTaskInfoAttributes();
            String lsidString = tia.get(GPConstants.LSID);
            LSID lsid = new LSID(lsidString);
            String lsidNoVersion = lsid.toStringNoVersion();
            if(taskNoLSIDVersion.equals(lsidNoVersion))
            {
                moduleVersions.add(lsidString);
            }
        }
        return moduleVersions;
    }

    private JSONArray getParameterList(ParameterInfo[] pArray)
    {
        JSONArray parametersObject = new JSONArray();

        for(int i =0;i < pArray.length;i++)
        {
            ParametersJSON parameter = new ParametersJSON(pArray[i]);
            parametersObject.put(parameter);
        }

        return parametersObject;
    }
}

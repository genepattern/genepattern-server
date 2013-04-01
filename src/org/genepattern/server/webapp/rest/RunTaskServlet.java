package org.genepattern.server.webapp.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineDependencyHelper;
import org.genepattern.modules.ModuleJSON;
import org.genepattern.modules.ParametersJSON;
import org.genepattern.modules.ResponseJSON;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.domain.Lsid;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInput.ParamValue;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.NumValues;
import org.genepattern.server.job.input.ParamListHelper;
import org.genepattern.server.rest.JobInputApiImpl;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
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

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Jan 10, 2013
 * Time: 9:41:34 PM
 * To change this template use File | Settings | File Templates.
 */
@Path("/RunTask")
public class RunTaskServlet extends HttpServlet
{
    public static Logger log = Logger.getLogger(RunTaskServlet.class);
    /*public static final String UPLOAD = "/upload";
    public static final String RUN = "/run";
    */

    /**
	 * Inject details about the URI for this request
	 */
	@Context
    UriInfo uriInfo;

    /**
     * Helper method for initializing the 'fileFormat' from a given file name.
     * Usually we shouldn't need to call this method because both '_file=' and
     * '_format=' request parameters are set from the send-to menu.
     * 
     * @param _fileParam
     * @return
     */
    private String getType(final String _fileParam) {
        int idx=_fileParam.lastIndexOf(".");
        if (idx<0) {
            log.debug("file has no extension: "+_fileParam);
            return "";
        }
        if (idx==_fileParam.length()-1) {
            log.debug("file ends with '.': "+_fileParam);
            return "";
        }
        return _fileParam.substring(idx+1);
    }

    /**
     * Helper method for initializing the values for the job input form.
     * Set initial values for the parameters for the following cases:
     * 
     * 1) a reloaded job
     * 2) values set in request parameters, when linking from the protocols page
     * 3) send to module, from the context menu for a file
     * 
     * @param pInfoArray, the list of formal parameters, from the TaskInfo object
     * @param reloadedValues, the values from the original job, if this is a job reload request
     * @param _fileParam, the input file value, if this is from a send-to module request
     * @param _formatParam, the input file type, if this is from a send-to module request
     * @param parameterMap, the HTTP request parameters, if this is from protocols page link
     * 
     * @return
     * @throws JSONException
     */
    private JSONObject getIntialValues(
            ParameterInfo[] pInfoArray, //the formal input parameters
            final JobInput reloadedValues, 
            final String _fileParam,
            String _formatParam,
            final Map<String,String[]> parameterMap
    )
    throws JSONException, Exception
    {
        JSONObject values = new JSONObject();
        
        for(ParameterInfo pinfo : pInfoArray) {
            final String pname=pinfo.getName();
            //1) initialize from default values
            final List<String> defaultValues=ParamListHelper.getDefaultValues(pinfo);
            if (defaultValues != null) {
                values.put(pname, defaultValues);
            }
            
            //2) if it's a reloaded job, use that
            if (reloadedValues != null) {
                if (reloadedValues.hasValue(pname)) {
                    List<String> fromReload=new ArrayList<String>();
                    for(ParamValue pval : reloadedValues.getParamValues(pname)) {
                        fromReload.add(pval.getValue());
                    }
                    values.put(pname, fromReload);
                }
            }

            //3) if there's a matching request parameter, use that
            if (parameterMap.containsKey(pname)) {
                List<String> fromRequestParam=new ArrayList<String>();
                for(String requestParam : parameterMap.get(pname)) {
                    fromRequestParam.add(requestParam);
                }
                values.put(pname, fromRequestParam);
            }
            
            //validate numValues
            NumValues numValues=ParamListHelper.initNumValues(pinfo);
            if (numValues.getMax() != null) {
                try {
                    JSONArray jsonValues=values.getJSONArray(pname);
                    if (jsonValues != null && jsonValues.length() > numValues.getMax()) {
                        //this is an error: more input values were specified than
                        //this parameter allows so throw an exception
                        throw new Exception(" Error: " + jsonValues.length() + " input values were specified for " +
                                pname + " but a maximum of " + numValues.getMax() + " is allowed. ");
                    }
                }
                catch (JSONException e) {
                    log.error("server error validating numValues from JSONArray: "+e.getLocalizedMessage(), e);
                }
            }
        }
        
        //special-case for send-to module from file
        if (_fileParam != null && _fileParam.length() != 0) {
            if (_formatParam == null || _formatParam.length() == 0) {
                log.error("_format request parameter is not set, _file="+_fileParam);
                _formatParam=getType(_fileParam);
            }
            
            //find the first parameter which matches the type of the file
            for(ParameterInfo pinfo : pInfoArray) {
                List<String> fileFormats=ParamListHelper.getFileFormats(pinfo);
                if (pinfo != null) {
                    if (fileFormats.contains(_formatParam)) {
                        //we found the first match
                        List<String> fromFile=new ArrayList<String>();
                        fromFile.add(_fileParam);
                        values.put(pinfo.getName(), fromFile);
                    }
                }
            }
        }
        
        return values;
    }

    @GET
    @Path("/load")
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadModule(
            @QueryParam("lsid") String lsid, 
            @QueryParam("reloadJob") String reloadJobId, 
            @QueryParam("_file") String sendFromFile,
            @QueryParam("_format") String sendFromFormat,
            @Context HttpServletRequest request)
    {
        try
        {
            String username = (String) request.getSession().getAttribute("userid");

            if (username == null)
            {
                throw new Exception("User not logged in");
            }

            ServerConfiguration.Context context = ServerConfiguration.Context.getContextForUser(username);
            JobInput reloadJobInput = null;

            if (lsid == null && reloadJobId == null)
            {
                throw new Exception ("No lsid or job number to reload received");
            }

            if(reloadJobId != null && !reloadJobId.equals(""))
            {
                //This is a reloaded job
                reloadJobInput= ParamListHelper.getInputValues(context, reloadJobId);

                String reloadedLsidString = reloadJobInput.getLsid();

                //check if lsid is null
                if(lsid == null)
                {
                    lsid = reloadedLsidString;
                }
                else
                {
                    //warn the user if the reloaded job lsid and given lsid do not match
                    //but continue execution
                    Lsid reloadLsid = new Lsid(reloadedLsidString);
                    Lsid givenLsid = new Lsid(lsid);
                    if(reloadLsid.getLsidNoVersion().equals(givenLsid.getLsidNoVersion()))
                    {
                        log.warn("The given lsid " + givenLsid.getLsidNoVersion() + " does not match " +
                                "the lsid of the reloaded job " + reloadLsid.getLsidNoVersion());
                    }
                }

            }

            //check if lsid is still null
            if(lsid == null)
            {
                throw new Exception ("No lsid  received");
            }

            TaskInfo taskInfo = getTaskInfo(lsid, username);

            if(taskInfo == null)
            {
                throw new Exception("No task with task id: " + lsid + " found " +
                        "for user " + username);
            }

            ModuleJSON moduleObject = new ModuleJSON(taskInfo, null);
            moduleObject.put("lsidVersions", new JSONArray(getModuleVersions(taskInfo)));

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

                if(docFiles == null || docFiles.length == 0)
                {
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


            //set initial values for the parameters for the following cases:
            //   1) a reloaded job
            //   2) values set in request parameters, when linking from the protocols page
            //   3) send to module, from the context menu for a file
            String _fileParam=null;
            String _formatParam=null;
            final Map<String,String[]> parameterMap=request.getParameterMap();
            if (parameterMap.containsKey("_file")) {
                _fileParam=parameterMap.get("_file")[0];
                if (parameterMap.containsKey("_format")) {
                    _formatParam=parameterMap.get("_format")[0];
                }
            } 
            JSONObject initialValues=getIntialValues(
                    taskInfo.getParameterInfoArray(), 
                    reloadJobInput, 
                    _fileParam, 
                    _formatParam, 
                    parameterMap);

            responseObject.put("initialValues", initialValues);

            return Response.ok().entity(responseObject.toString()).build();
        }
        catch(Exception e)
        {
            String message = "An error occurred while loading the module with lsid: \"" + lsid + "\"";
            if(e.getMessage() != null)
            {
                message = e.getMessage();
            }
            log.error(message);

            if(message.contains("You do not have the required permissions"))
            {
                throw new WebApplicationException(
                Response.status(Response.Status.FORBIDDEN)
                    .entity(message)
                    .build()
                );
            }
            else
            {
                throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(message)
                    .build()
                );
            }
        }
	}

    @POST
    @Path("/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(
        @FormDataParam("ifile") InputStream uploadedInputStream,
        @FormDataParam("ifile") FormDataContentDisposition fileDetail,
        @FormDataParam("paramName") final String paramName,
        @FormDataParam("index") final int index,        
        @Context HttpServletRequest request)
    {
        try
        {
            String username = (String) request.getSession().getAttribute("userid");
            if (username == null)
            {         
                throw new Exception("User not logged in");
            }

            ServerConfiguration.Context jobContext=ServerConfiguration.Context.getContextForUser(username);

            JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
            GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, paramName, fileDetail.getFileName());

            // save it
            writeToFile(uploadedInputStream, gpFilePath.getServerFile().getCanonicalPath());
            fileUtil.updateUploadsDb(gpFilePath);

            String output = "File uploaded to : " + gpFilePath.getServerFile().getCanonicalPath();
            log.error(output);

            log.error(gpFilePath.getUrl().toExternalForm());
            ResponseJSON result = new ResponseJSON();
            result.addChild("location",  gpFilePath.getUrl().toExternalForm());
            return Response.ok().entity(result.toString()).build();
        }
        catch(Exception e)
        {
            String message = "An error occurred while uploading the file \"" + fileDetail.getFileName() + "\"";
            if(e.getMessage() != null)
            {
                message = message + ": " + e.getMessage();
            }
            log.error(message);

            throw new WebApplicationException(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity(message)
                    .build()
            );
        }
    }

    @POST
    @Path("/addJob")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addJob(
        JobSubmitInfo jobSubmitInfo,
        @Context HttpServletRequest request)
    {
        try
        {
            String username = (String) request.getSession().getAttribute("userid");
            if (username == null)
            {
                throw new Exception("User not logged in");
            }

            JobInput jobInput = new JobInput();
            jobInput.setLsid(jobSubmitInfo.getLsid());

            JSONObject parameters = new JSONObject(jobSubmitInfo.getParameters());
            Iterator<String> paramNames = parameters.keys();
            while(paramNames.hasNext())
            {
                String parameterName = paramNames.next();
                //JSONArray valueList = new JSONArray((String)parameters.get(parameterName));
                JSONArray valueList;
                Object val=parameters.get(parameterName);
                if (val instanceof JSONArray) {
                    valueList=(JSONArray) val;
                }
                else {
                    valueList = new JSONArray((String)parameters.get(parameterName));
                }
                for(int v=0; v<valueList.length();v++)
                {
                    jobInput.addValue(parameterName, valueList.getString(v));
                }
            }

            ServerConfiguration.Context jobContext=ServerConfiguration.Context.getContextForUser(username);

            JobInputApiImpl impl = new JobInputApiImpl();
            String jobId = impl.postJob(jobContext, jobInput);

            ResponseJSON result = new ResponseJSON();
            result.addChild("jobId", jobId);

            return Response.ok(result.toString()).build();

            //JSONObject result = new JSONObject(((String)jobSubmitInfo.getParameters());
            //String r2 = (String)result.get("input.file");
            //new JSONArray(r2);
        }
        catch(Exception e)
        {
            String message = "An error occurred while submitting the job";
            if(e.getMessage() != null)
            {
                message = message + ": " + e.getMessage();
            }
            log.error(message);

            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .build()
            );
        }
    }

    // save uploaded file to new location
    private void writeToFile(InputStream uploadedInputStream,
        String uploadedFileLocation) {

        try {
            OutputStream out = new FileOutputStream(new File(
                    uploadedFileLocation));
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(new File(uploadedFileLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

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

    private ArrayList getModuleVersions(TaskInfo taskInfo) throws Exception
    {
        LSID taskLSID = new LSID(taskInfo.getLsid());
        String taskNoLSIDVersion = taskLSID.toStringNoVersion();

        ArrayList moduleVersions = new ArrayList();
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

    private TaskInfo getTaskInfo(String taskLSID, String username) throws WebServiceException
    {
        return new LocalAdminClient(username).getTask(taskLSID);
    }
}

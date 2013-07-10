package org.genepattern.server.webapp.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

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
import org.genepattern.codegenerator.CodeGeneratorUtil;
import org.genepattern.data.pipeline.GetIncludedTasks;
import org.genepattern.modules.ModuleJSON;
import org.genepattern.modules.ParametersJSON;
import org.genepattern.modules.ResponseJSON;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInput.Param;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.LoadModuleHelper;
import org.genepattern.server.job.input.ReloadJobHelper;
import org.genepattern.server.repository.SourceInfo;
import org.genepattern.server.repository.SourceInfoLoader;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webapp.jsf.JobBean;
import org.genepattern.server.webapp.jsf.UIBeanHelper;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.AnalysisJob;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.ClientResponse;
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
            String userId = (String) request.getSession().getAttribute("userid");
            if (userId == null) {
                throw new Exception("User not logged in");
            }
            if (lsid == null && reloadJobId == null) {
                throw new Exception ("No lsid or job number to reload received");
            }

            //Note: we have a helper method to initialize the userId,
            //    see org.genepattern.server.webapp.rest.api.v1.Util#getUserContext 
            ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(userId);
            final boolean isAdmin = AuthorizationHelper.adminServer(userId);
            userContext.setIsAdmin(isAdmin);
            
            JobInput reloadJobInput = null;

            if(reloadJobId != null && !reloadJobId.equals(""))
            {
                //This is a reloaded job
                ReloadJobHelper reloadJobHelper=new ReloadJobHelper(userContext);
                reloadJobInput = reloadJobHelper.getInputValues(reloadJobId);

                String reloadedLsidString = reloadJobInput.getLsid();

                //check if lsid is null
                if(lsid == null)
                {
                    lsid = reloadedLsidString;
                }
                else
                {
                    if (log.isDebugEnabled()) {
                        log.debug("reloadedLsidString="+reloadedLsidString);
                        log.debug("lsid="+lsid);
                        if (!reloadedLsidString.equals(lsid)) {
                            //warn if the reloaded job lsid and given lsid do not match
                            //but continue execution
                            log.warn("The given lsid " + lsid + " does not match " +
                                    "the lsid of the reloaded job " + reloadedLsidString);
                        }
                    }
                }
            }

            //check if lsid is still null
            if(lsid == null)
            {
                throw new Exception ("No lsid  received");
            }

            final TaskInfo taskInfo = getTaskInfo(lsid, userId);

            if(taskInfo == null)
            {
                throw new Exception("No task with task id: " + lsid + " found " +
                        "for user " + userId);
            }

            final ModuleJSON moduleObject = new ModuleJSON(taskInfo, null);
            final SortedSet<LSID> moduleLsidVersions=getModuleVersions(userContext, taskInfo);
            final JSONArray lsidVersions=new JSONArray();
            for(final LSID moduleLsidVersion : moduleLsidVersions) {
                lsidVersions.put(moduleLsidVersion.toString());
            }
            moduleObject.put("lsidVersions", lsidVersions);

            //check if user is allowed to edit the task
            final boolean editable=isEditable(userContext, taskInfo);
            moduleObject.put("editable", editable);

            //check if the module has documentation
            boolean hasDoc = true;

            File[] docFiles = null;
            try {
                LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userId);
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
            final boolean isPipeline=taskInfo.isPipeline();
            if (isPipeline) {
                // check for missing dependencies
                // hint, all of the work is done in the constructor, including initialization of the 
                //    dependent tasks and missing task lsids
                GetIncludedTasks getDependentTasks = new GetIncludedTasks(userContext, taskInfo);
                if (getDependentTasks.getMissingTaskLsids().size()>0) {
                    moduleObject.put("missing_tasks", true);
                    if (log.isDebugEnabled()) {
                        for(final LSID missingTaskLsid : getDependentTasks.getMissingTaskLsids()) {
                            final String str=missingTaskLsid.toString();
                            log.debug("missingTaskLsid: "+str);
                        }
                    }
                }
                else {
                    moduleObject.put("missing_tasks", false); 
                }
                
                final Set<TaskInfo> privateTasks=getDependentTasks.getPrivateTasks();
                if (privateTasks != null && privateTasks.size()>0) {
                    log.debug("current user, '"+userContext.getUserId()+"', doesn't have permission to run one of the dependent tasks");
                    JSONArray privateTasksObj=new JSONArray();
                    for(final TaskInfo privateTask : getDependentTasks.getPrivateTasks()) {
                        final String message=privateTask.getName()+", "+privateTask.getLsid();
                        log.debug(message);
                        final JSONObject entry=new JSONObject();
                        entry.put("name", privateTask.getName());
                        entry.put("lsid", privateTask.getLsid());
                        entry.put("userId", privateTask.getUserId());
                        privateTasksObj.put(entry);
                    }
                    moduleObject.put("private_tasks", privateTasksObj);
               }
            }

            //get module source and quality info
            JSONObject sourceInfoObj = new JSONObject();

            SourceInfoLoader sourceInfoLoader = SourceInfo.getSourceInfoLoader(userContext);
            SourceInfo sourceInfo = sourceInfoLoader.getSourceInfo(taskInfo);
            if(sourceInfo.getShowSourceInfo())
            {
                if(sourceInfo.getLabel() != null)
                {
                    sourceInfoObj.put("label", sourceInfo.getLabel());
                }
                if(sourceInfo.getIconImgSrc() != null)
                {
                    sourceInfoObj.put("iconUrl", sourceInfo.getIconImgSrc());
                }
                if(sourceInfo.getBriefDescription() != null)
                {
                    sourceInfoObj.put("briefDesc", sourceInfo.getBriefDescription());
                }
                if(sourceInfo.getFullDescription() != null)
                {
                    sourceInfoObj.put("fullDesc", sourceInfo.getFullDescription());
                }
                moduleObject.put("source_info", sourceInfoObj);
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
            LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext);
            JSONObject initialValues=loadModuleHelper.getInitialValuesJson(
                    lsid,
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
    @Consumes({MediaType.MULTIPART_FORM_DATA, MediaType.APPLICATION_OCTET_STREAM})
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

            if (log.isDebugEnabled()) {
                final String output = "File uploaded to : " + gpFilePath.getServerFile().getCanonicalPath();
                log.debug(output);
                log.debug(gpFilePath.getUrl().toExternalForm());
            }

            ResponseJSON result = new ResponseJSON();
            result.addChild("location",  gpFilePath.getUrl().toExternalForm());
            return Response.ok().entity(result.toString()).build();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            String message = "An error occurred while uploading the file \"" + fileDetail.getFileName() + "\"";
            if(e.getMessage() != null)
            {
                message = message + ": " + e.getMessage();
            }
            log.error(message,e);

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

            final ServerConfiguration.Context userContext=ServerConfiguration.Context.getContextForUser(username);
            final JobInputHelper jobInputHelper=new JobInputHelper(userContext, jobSubmitInfo.getLsid());

            JSONObject parameters = new JSONObject(jobSubmitInfo.getParameters());
            Iterator<String> paramNames = parameters.keys();
            while(paramNames.hasNext())
            {
                String parameterName = paramNames.next();
                boolean isBatch = isBatchParam(jobSubmitInfo, parameterName);
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
                    if (isBatch) {
                        jobInputHelper.addBatchDirectory(parameterName, valueList.getString(v));
                    }
                    else {
                        jobInputHelper.addValue(parameterName, valueList.getString(v));
                    }
                }
            }

            final List<JobInput> batchInputs;
            // experimental, when deduceBatch is true, it means ignore the 'Single' or 'Batch' selection from the end user
            //    instead automatically set batch inputs when the input value is a directory (instead of a file)
            final boolean deduceBatchValues=false;
            jobInputHelper.setDeduceBatchValues(deduceBatchValues);
            batchInputs=jobInputHelper.prepareBatch();
            final JobReceipt receipt=jobInputHelper.submitBatch(batchInputs);

            
            //TODO: if necessary, add batch details to the JSON representation
            String jobId="-1";
            if (receipt.getJobIds().size()>0) {
                jobId=receipt.getJobIds().get(0);
            }
            ResponseJSON result = new ResponseJSON();
            result.addChild("jobId", receipt.getJobIds().get(0));
            if (receipt.getBatchId() != null && receipt.getBatchId().length()>0) {
                result.addChild("batchId", receipt.getBatchId());
                request.getSession().setAttribute(JobBean.DISPLAY_BATCH, receipt.getBatchId());
            }
            return Response.ok(result.toString()).build();
        }
        catch (GpServerException e) {
            String message = "An error occurred while submitting the job";
            if(e.getMessage() != null)
            {
                message = message + ": " + e.getMessage();
            }
            return Response.status(ClientResponse.Status.FORBIDDEN).entity(message).build();
        }        
        catch(Throwable t)
        {
            String message = "An error occurred while submitting the job";
            if(t.getMessage() != null)
            {
                message = message + ": " + t.getMessage();
            }
            log.error(message);

            throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .build()
            );
        }
    }
    
    /**
     * Given the submitted job info and a param name, determine if the provided parameter is a batch parameter
     * @param jobSubmitInfo
     * @param name
     * @return
     */
    private boolean isBatchParam(JobSubmitInfo jobSubmitInfo, String name) {
        List<String> batches = jobSubmitInfo.getBatchParams();
        return batches.contains(name);
    }

    /**
     * Get the GP client code for the given task, copied from JobBean#getTaskCode().
     * Requires a logged in user, and valid 'lsid' query parameter or a valid 'reloadJob' query parameter.
     * The lsid can be the full lsid or the name of a module.
     * 
     * To test from curl,
     * <pre>
       curl -u <username:password> <GenePatternURL>/rest/RunTask/viewCode?
           lsid=<lsid>,
           reloadJob=<reloadJobId>,
           language=[ 'Java' | 'R' | 'MATLAB' ], if not set, default to 'Java',
           <pname>=<pvalue>
     * </pre>
     * 
     * Example 1: get Java code for ComparativeMarkerSelection (v.9)
     * <pre>
       curl -u test:**** "http://127.0.0.1:8080/gp/rest/RunTask/viewCode?language=Java&lsid=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9" 
     * </pre>
     * Example 2: by taskName
     * <pre>
       curl -u test:**** "http://127.0.0.1:8080/gp/rest/RunTask/viewCode?language=Java&lsid=ComparativeMarkerSelection" 
     * </pre>
     * Example 3: initialize the input.filename
     * <pre>
       curl -u test:**** "http://127.0.0.1:8080/gp/rest/RunTask/viewCode?language=Java&lsid=urn:lsid:broad.mit.edu:cancer.software.genepattern.module.analysis:00044:9&input.filename=ftp://ftp.broadinstitute.org/pub/genepattern/datasets/all_aml/all_aml_test.gct" 
     * </pre>
     * Example 4: from a reloaded job
     * <pre>
       curl -u test:**** "http://127.0.0.1:8080/gp/rest/RunTask/viewCode?language=Java&reloadJob=9948" 
     * </pre> 
     * 
     * Note: I had to wrap the uri in double-quotes to deal with the '&' character.
     * 
     * Note: If you prefer to use use cookie-based authentication. This command logs in and
     * saves the session cookie to the file 'cookies.txt'
     * <pre>
       curl -c cookies.txt "<GenePatternURL>/login?username=<username>&password=<password>"
       </pre>
     *
     * Use the '-b cookies.txt' on subsequent calls.       
     * 
     * @param lsid, the full lsid or taskName of the module or pipeline
     * @param language, the programming language client, e.g. 'Java', 'R', or 'MATLAB'
     * @return
     */
    @GET
    @Path("/viewCode")
    @Produces(MediaType.APPLICATION_JSON)
    public Response viewCode(
            @QueryParam("language") String language,
            @QueryParam("lsid") String lsid,
            final @QueryParam("reloadJob") String reloadJob, 
            final @QueryParam("_file") String _fileParam,
            final @QueryParam("_format") String _formatParam,
            final @Context HttpServletRequest request
    ) {

        String userId = (String) request.getSession().getAttribute("userid");
        final ServerConfiguration.Context userContext=ServerConfiguration.Context.getContextForUser(userId);
        JobInput reloadJobInput=null;
        if (reloadJob != null && !reloadJob.equals("")) {
            //This is a reloaded job
            try {
                ReloadJobHelper reloadJobHelper=new ReloadJobHelper(userContext);
                reloadJobInput = reloadJobHelper.getInputValues(reloadJob);
            }
            catch (Exception e) {
                log.error("Error initializing from reloadJob="+reloadJob, e);
                return Response.serverError().entity(e.getLocalizedMessage()).build();
            }
        }
        if (lsid==null || lsid.length()==0) {
            if (reloadJobInput != null) {
                lsid=reloadJobInput.getLsid();
            }
        }
        if (lsid==null || lsid.length()==0) { 
            //400, Bad Request
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing required request parameter, 'lsid'").build();
        }
        if (language==null || language.length()==0) {
            log.debug("Missing request parameter, setting 'language=Java'");
            language="Java";
        }
        JSONObject content=new JSONObject();
        try {
            IAdminClient adminClient = new LocalAdminClient(userId);
            TaskInfo taskInfo = adminClient.getTask(lsid);
            if (taskInfo == null) { 
                return Response.status(Response.Status.NOT_FOUND).entity("Module not found, lsid="+lsid).build();
            }
            
            ParameterInfo[] parameters = taskInfo.getParameterInfoArray();
            
            
            ParameterInfo[] jobParameters=null;
            if (parameters != null) {
                //JobInput initialValues=ParamListHelper.getInitialValues(
                //        lsid, parameters, reloadJobInput, _fileParam, _formatParam, request.getParameterMap());
                LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext);
                JobInput initialValues=loadModuleHelper.getInitialValues(
                        lsid, parameters, reloadJobInput, _fileParam, _formatParam, request.getParameterMap());

                
                jobParameters = new ParameterInfo[parameters.length];
                int i=0;
                for(ParameterInfo pinfo : parameters) {
                    final String id=pinfo.getName();
                    String value=null;
                    if (initialValues.hasValue(id)) {
                        Param p = initialValues.getParam(pinfo.getName());
                        int numValues=p.getNumValues();
                        if (numValues==0) {
                        }
                        else if (numValues==1) {
                            value=p.getValues().get(0).getValue();
                        }
                        else {
                            //TODO: can't initialize from a list of values
                            log.error("can't initialize from a list of values, lsid="+lsid+
                                    ", pname="+id+", numValues="+numValues);
                        }
                    }
                    jobParameters[i++] = new ParameterInfo(id, value, "");
                }
            }

            JobInfo jobInfo = new JobInfo(-1, -1, null, null, null, jobParameters, userContext.getUserId(), lsid, taskInfo.getName());
            boolean isVisualizer = TaskInfo.isVisualizer(taskInfo.getTaskInfoAttributes());
            AnalysisJob job = new AnalysisJob(UIBeanHelper.getServer(), jobInfo, isVisualizer);
            String code=CodeGeneratorUtil.getCode(language, job, taskInfo, adminClient);
            content.put("code", code);
            return Response.ok().entity(content.toString()).build();
        }
        catch (Throwable t) {
            //String errorMessage=;
            log.error("Error getting code.", t);
            try {
                content.put("error", "Error getting code: "+t.getLocalizedMessage());
                return Response.serverError().entity(content).build();
            }
            catch (JSONException e) {
                log.error(e);
            }
            return Response.serverError().build();
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
            parameter.addNumValues(pArray[i]);
            parametersObject.put(parameter);
        }

        return parametersObject;
    }

    /**
     * Get the set of LSID for all versions of this task which are installed on the server.
     * The LSID are ordered by the natural ordering as implemented in the LSID class,
     * which is in reverse order of the LSID version.
     * 
     * @param userContext, must have valid userId, and should have isAdmin set
     * @param taskInfo
     * @return
     * @throws Exception
     */
    private SortedSet<LSID> getModuleVersions(final ServerConfiguration.Context userContext, final TaskInfo taskInfo) throws Exception
    {
        final LSID taskLSID = new LSID(taskInfo.getLsid());
        final String taskNoLSIDVersion = taskLSID.toStringNoVersion();
        final SortedSet<LSID> moduleVersions = new TreeSet<LSID>();
        
        final List<TaskInfo> allVersions=TaskInfoCache.instance().getAllVersions(userContext, taskLSID);
        for(final TaskInfo version : allVersions) {
            final LSID lsid=new LSID(version.getLsid());
            final String lsidNoVersion = lsid.toStringNoVersion();
            if(taskNoLSIDVersion.equals(lsidNoVersion))
            {
                moduleVersions.add(lsid);
            }
        }
        return moduleVersions;
    }

    private TaskInfo getTaskInfo(String taskLSID, String username) throws WebServiceException
    {
        return new LocalAdminClient(username).getTask(taskLSID);
    }
    
    /**
     * Check if the user is allowed to edit the module or pipeline.
     * @param userContext
     * @return
     */
    private boolean isEditable(final ServerConfiguration.Context userContext, final TaskInfo taskInfo) {
        if (userContext == null) {
            log.error("userContext == null");
            return false;
        }
        if (userContext.getUserId()==null) {
            log.error("userContext.userId == null");
            return false;
        }
        if (userContext.getUserId().length()==0) {
            log.error("userContext.userId not set");
            return false;
        }
        //can only edit your own task
        final boolean isMine=taskInfo.getUserId().equals(userContext.getUserId());
        if (!isMine) {
            return false;
        }
        //can only edit modules or pipelines created on this gp server
        final boolean isAuthorityMine = LSIDUtil.getInstance().isAuthorityMine(taskInfo.getLsid());
        if (!isAuthorityMine) {
            return false;
        } 
        final boolean isPipeline=taskInfo.isPipeline();
        if (!isPipeline) {
            final boolean createModuleAllowed = AuthorizationHelper.createModule(userContext.getUserId());
            final boolean editable = createModuleAllowed && isMine && isAuthorityMine;
            return editable;
        }
        else {
            final boolean createPipelineAllowed = AuthorizationHelper.createPipeline(userContext.getUserId());
            boolean editable = createPipelineAllowed && isMine && isAuthorityMine;
            return editable;
        }
    }
}

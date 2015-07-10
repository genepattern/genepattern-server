/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.webapp.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
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
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.PipelineUtil;
import org.genepattern.modules.ModuleJSON;
import org.genepattern.modules.ParametersJSON;
import org.genepattern.modules.ResponseJSON;
import org.genepattern.server.DbException;
import org.genepattern.server.JobInfoManager;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.server.cm.CategoryUtil;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.tasklib.TasklibPath;
import org.genepattern.server.eula.LibdirLegacy;
import org.genepattern.server.eula.LibdirStrategy;
import org.genepattern.server.job.JobInfoLoaderDefault;
import org.genepattern.server.job.comment.JobComment;
import org.genepattern.server.job.comment.JobCommentManager;
import org.genepattern.server.job.input.GroupId;
import org.genepattern.server.job.input.JobInput;
import org.genepattern.server.job.input.JobInputFileUtil;
import org.genepattern.server.job.input.JobInputHelper;
import org.genepattern.server.job.input.LoadModuleHelper;
import org.genepattern.server.job.input.Param;
import org.genepattern.server.job.input.configparam.JobConfigParams;
import org.genepattern.server.job.tag.JobTagManager;
import org.genepattern.server.quota.DiskInfo;
import org.genepattern.server.repository.SourceInfo;
import org.genepattern.server.repository.SourceInfoLoader;
import org.genepattern.server.rest.GpServerException;
import org.genepattern.server.rest.JobReceipt;
import org.genepattern.server.webapp.jsf.AuthorizationHelper;
import org.genepattern.server.webapp.jsf.JobBean;
import org.genepattern.server.webapp.rest.api.v1.Util;
import org.genepattern.server.webapp.rest.api.v1.task.TasksResource;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            @DefaultValue("true") @QueryParam("prettyPrint") boolean prettyPrint,
            @DefaultValue("true") @QueryParam("includeChildren") boolean includeChildren,
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
            final boolean initIsAdmin=true;
            final GpConfig gpConfig=ServerConfigurationFactory.instance();
            final GpContext userContext = GpContext.getContextForUser(userId, initIsAdmin);

            JobInput reloadJobInput = null;
            if (reloadJobId != null && !reloadJobId.equals("")) {
                //This is a reloaded job
                final GpContext reloadJobContext=GpContext.createContextForJob(Integer.parseInt(reloadJobId));
                reloadJobInput = reloadJobContext.getJobInput();
                final String reloadedLsidString = reloadJobInput.getLsid();

                //check if lsid is null
                if(lsid == null) {
                    lsid = reloadedLsidString;
                }
                else {
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
            if(lsid == null) {
                throw new Exception ("No lsid  received");
            }

            final TaskInfo taskInfo = getTaskInfo(lsid, userId);

            if(taskInfo == null) {
                throw new Exception("No task with lsid=" + lsid + " found for user=" + userId);
            }
            userContext.setTaskInfo(taskInfo);

            final ModuleJSON moduleObject = new ModuleJSON(taskInfo, null);
            //check for EULA
            JSONObject eulaObject=TasksResource.getPendingEulaForModuleJson(request, userContext, taskInfo);
            if (eulaObject != null) {
                moduleObject.put("eula", eulaObject);
            }

            final SortedSet<LSID> moduleLsidVersions=getModuleVersions(userContext, taskInfo);
            final JSONArray lsidVersions=new JSONArray();
            for(final LSID moduleLsidVersion : moduleLsidVersions) {
                lsidVersions.put(moduleLsidVersion.toString());
            }
            moduleObject.put("lsidVersions", lsidVersions);

            //check if there is a hidden beta version of the module available
            LSID selectedTaskVersionLSID = new LSID(taskInfo.getLsid());
            LSID latestTaskVersionLSID = new LSID((String)lsidVersions.get(0));
            if(!selectedTaskVersionLSID.getVersion().equals(latestTaskVersionLSID.getVersion()))
            {
                TaskInfo latestTaskInfo = getTaskInfo(latestTaskVersionLSID.toString(), userId);
                if(isHiddenBetaVersion(latestTaskInfo))
                {
                    moduleObject.put("betaVersion", lsidVersions.get(0));
                }
            }

            //check if user is allowed to edit the task
            final boolean editable=isEditable(userContext, taskInfo);
            moduleObject.put("editable", editable);

            //check if the module has documentation
            boolean hasDoc = true;

            File[] docFiles = null;
            File[] allFiles = null;
            try {
                LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userId);
                docFiles = taskIntegratorClient.getDocFiles(taskInfo);
                allFiles = taskIntegratorClient.getAllFiles(taskInfo);

                if(docFiles == null || docFiles.length == 0)
                {
                    hasDoc = false;
                }
            }
            catch (WebServiceException e) {
                log.error("Error getting doc files.", e);
            }
            moduleObject.put("hasDoc", hasDoc);

            // Add list of all files
            JSONArray fileList = new JSONArray();

            if (allFiles != null) {
                for (File i : allFiles) {
                    fileList.put(i.getName());
                }
            }

            moduleObject.put("allFiles", fileList);

            //if this is a pipeline check if there are any missing dependencies
            final boolean isPipeline=taskInfo.isPipeline();
            if (isPipeline) {
                // check for missing dependencies
                // hint, all of the work is done in the constructor, including initialization of the
                //    dependent tasks and missing task lsids
                JSONArray missingTasksList = new JSONArray();

                PipelineModel model = PipelineUtil.getPipelineModel(lsid);
                boolean pipelineWithMissingTasks = PipelineUtil.isMissingTasks(model, userId);

                if (pipelineWithMissingTasks) {
                    LinkedHashMap<LSID, PipelineUtil.MissingTaskRecord> missingTasks = PipelineUtil.getMissingTasks(model, userContext.getUserId());

                    for(LSID missingLsid : missingTasks.keySet()) {
                        String taskName = missingTasks.get(missingLsid).getName();
                        //SortedSet<LSID> installedVersions = missingTasks.get(missingLsid).getInstalledVersions();

                        if (log.isDebugEnabled())
                        {
                            log.debug("missingTaskLsid: "+ missingLsid.toString());
                        }

                        JSONObject missingTaskObj = new JSONObject();
                        missingTaskObj.put("name", taskName);
                        missingTaskObj.put("version",missingLsid.getVersion());
                        missingTaskObj.put("lsid", missingLsid.toStringNoVersion());

                        SortedSet<LSID> installedVers = missingTasks.get(missingLsid).getInstalledVersions();
                        JSONArray installedVersions = new JSONArray();
                        for(LSID installedLsid: installedVers) {

                            installedVersions.put(installedLsid.getVersion());
                        }
                        missingTaskObj.put("installedVersions", installedVersions);
                        missingTasksList.put(missingTaskObj);
                    }
                }
                else {
                }

                moduleObject.put("missing_tasks", missingTasksList);

                GetIncludedTasks getDependentTasks = new GetIncludedTasks(userContext, taskInfo);
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

            JSONArray parametersArray = getParameterList(request, taskInfo);
            responseObject.put(ParametersJSON.KEY, parametersArray);

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

            final LoadModuleHelper loadModuleHelper=new LoadModuleHelper(userContext);
            final JobInput initialValues=loadModuleHelper.getInitialValues(lsid,
                    taskInfo.getParameterInfoArray(),
                    reloadJobInput,
                    _fileParam,
                    _formatParam,
                    parameterMap);

            final JSONObject initialValuesJson=LoadModuleHelper.asJsonV2(initialValues);
            responseObject.put("initialValues", initialValuesJson);

            //check if there are any batch parameters
            Set<Param> batchParams = initialValues.getBatchParams();
            Set<String> batchParamNames = new HashSet<String>();
            if(batchParams != null && batchParams.size() > 0)
            {
                Iterator<Param> batchIt = batchParams.iterator();
                while(batchIt.hasNext())
                {
                    String batchParamName = batchIt.next().getParamId().getFqName();
                    batchParamNames.add(batchParamName);
                }

                responseObject.put("batchParams", batchParamNames);
            }

            //add parameter grouping info (i.e advanced parameters
            //check if there are any user defined groups
            final LibdirStrategy libdirStrategy = new LibdirLegacy();
            final TasklibPath filePath = new TasklibPath(libdirStrategy, taskInfo, "paramgroups.json");
            JSONArray paramGroupsJson = loadModuleHelper.getParameterGroupsJson(taskInfo, filePath.getServerFile());
            final JobConfigParams jobConfigParams=JobConfigParams.initJobConfigParams(gpConfig, userContext);
            if (jobConfigParams != null) {
                final JSONObject jobConfigGroupJson=jobConfigParams.getInputParamGroup().toJson();
                paramGroupsJson.put(jobConfigGroupJson);
                for(final ParameterInfo jobConfigParameterInfo : jobConfigParams.getParams()) {
                    JSONObject jsonObj=RunTaskServlet.initParametersJSON(request, taskInfo, jobConfigParameterInfo);
                    parametersArray.put(jsonObj);
                }
            }

            // Add children
            if (includeChildren) {
                TaskInfoAttributes tia = taskInfo.getTaskInfoAttributes();
                String serializedModel = tia.get(GPConstants.SERIALIZED_MODEL);
                if (serializedModel != null && serializedModel.length() > 0) {
                    PipelineModel model = PipelineModel.toPipelineModel(serializedModel);

                    JSONArray children = new JSONArray();
                    for (JobSubmission js : model.getTasks()) {
                        try {
                            TaskInfo childTask = TaskInfoCache.instance().getTask(js.getLSID());
                            JSONObject childObject = TasksResource.createTaskObject(childTask, request, true, true, true);
                            TasksResource.applyJobSubmission(childObject, js);
                            children.put(childObject);
                        }
                        catch (TaskLSIDNotFoundException e) {
                            // Task is not installed
                            JSONObject childObject = TasksResource.createTaskNotFoundObject(js);
                            children.put(childObject);
                        }
                    }

                    moduleObject.put("children", children);
                }
            }

            moduleObject.put("parameter_groups", paramGroupsJson);
            responseObject.put(ModuleJSON.KEY, moduleObject);

            final String jsonStr;
            if (prettyPrint) {
                final int indentFactor=2;
                jsonStr=responseObject.toString(indentFactor);
            }
            else {
                jsonStr=responseObject.toString();
            }
            return Response.ok().entity(jsonStr).build();
        }
        catch(Throwable t)
        {
            String message = "An error occurred while loading the module with lsid: \"" + lsid + "\"";
            if(t.getMessage() != null)
            {
                message = t.getMessage();
            }
            log.error(message, t);

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

            GpContext jobContext=GpContext.getContextForUser(username);

            JobInputFileUtil fileUtil = new JobInputFileUtil(jobContext);
            final String fileName=fileDetail.getFileName();
            log.debug("fileName="+fileName);
            GpFilePath gpFilePath=fileUtil.initUploadFileForInputParam(index, paramName, fileName);

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
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addJob(
        JobSubmitInfo jobSubmitInfo,
        @Context HttpServletRequest request)
    {
        final GpConfig gpConfig = ServerConfigurationFactory.instance();
        final GpContext userContext = Util.getUserContext(request);

        if (checkDiskQuota(userContext))
            return Response.status(Response.Status.FORBIDDEN).entity("Disk usage exceeded.").build();

        return addJob(gpConfig, userContext, jobSubmitInfo, request);
    }

    @POST
    @Path("/launchJsViewer")
    @Produces(MediaType.APPLICATION_JSON)
    public Response launchJsViewer(
            JobSubmitInfo jobSubmitInfo,
            @Context HttpServletRequest request)
    {
        final GpConfig gpConfig = ServerConfigurationFactory.instance();
        final GpContext userContext = Util.getUserContext(request);

        if (checkDiskQuota(userContext))
            return Response.status(Response.Status.FORBIDDEN).entity("Disk usage exceeded.").build();

        return launchJsViewer(gpConfig, userContext, jobSubmitInfo, request);
    }

    /**
     * Added this in 3.8.1 release to enable additional job configuration input parameters.
     * @param jobSubmitInfo
     * @param request
     * @return
     */
    private Response launchJsViewer(final GpConfig gpConfig, final GpContext userContext, final JobSubmitInfo jobSubmitInfo, final HttpServletRequest request) {
        if (jobSubmitInfo==null || jobSubmitInfo.getLsid()==null || jobSubmitInfo.getLsid().length()==0) {
            return handleError("No lsid received");
        }
        try {
            final JobInputHelper jobInputHelper = new JobInputHelper(gpConfig, userContext, jobSubmitInfo.getLsid());
            final JSONObject parameters = new JSONObject(jobSubmitInfo.getParameters());
            TaskInfo taskInfo = getTaskInfo(jobSubmitInfo.getLsid(), userContext.getUserId());

            for (final Iterator<?> iter = parameters.keys(); iter.hasNext(); ) {
                final String parameterName = (String) iter.next();
                boolean isBatch = isBatchParam(jobSubmitInfo, parameterName);

                //batch is not allowed for javascript visualizers
                if(isBatch)
                {
                    throw new Exception("Batching of javascript visualizers is not supported");
                }

                //verify this is a javascript visualizer
                if(taskInfo == null || !TaskInfo.isJavascript(taskInfo.getTaskInfoAttributes()))
                {
                    throw new Exception("Error: The selected task" + taskInfo.getName()
                            + " is not a Javascript visualizer.");
                }

                JSONArray valueList;
                final JSONArray groupInfos = parameters.getJSONArray(parameterName);
                for (int i = 0; i < groupInfos.length(); i++) {
                    final JSONObject groupInfo = groupInfos.getJSONObject(i);
                    final String groupName = groupInfo.getString("name");
                    final GroupId groupId;
                    if (groupName == null || groupName.length() == 0) {
                        groupId = GroupId.EMPTY;
                    } else {
                        groupId = new GroupId(groupName);
                    }
                    final Object values = groupInfo.get("values");
                    if (values instanceof JSONArray) {
                        valueList = (JSONArray) values;
                    } else {
                        valueList = new JSONArray((String) parameters.get(parameterName));
                    }
                    for (int v = 0; v < valueList.length(); v++) {
                        final String value = valueList.getString(v);
                        if (isBatch) {
                            //TODO: implement support for groupId with batch values
                            jobInputHelper.addBatchValue(parameterName, value);
                        } else {
                            jobInputHelper.addValue(parameterName, value, groupId);
                        }
                    }
                }
            }

            final List<JobInput> batchInputs;
            batchInputs = jobInputHelper.prepareBatch();
            final JobReceipt receipt = jobInputHelper.submitBatch(batchInputs);

            //TODO: if necessary, add batch details to the JSON representation
            final ResponseJSON result = new ResponseJSON();
            final String jobId;
            if (receipt.getJobIds().size() > 0) {
                jobId = receipt.getJobIds().get(0);
            } else {
                jobId = "-1";
            }
            result.addChild("jobId", jobId);

            try {
                JobInfo jobInfo = new JobInfoLoaderDefault().getJobInfo(userContext, jobId);
                String launchUrl = JobInfoManager.generateLaunchURL(taskInfo, jobInfo);
                result.addChild("launchUrl", launchUrl);
            }
            catch (Exception e) {
                log.error(e);
                throw new Exception("Could not generate launch url found for Javascript visualizer: " + taskInfo.getName()
                + e.getMessage());
            }

            int gpJobNo = Integer.parseInt(jobId);
            //check if there was a comment specified for job and add it to database
            if(jobSubmitInfo.getComment() != null && jobSubmitInfo.getComment().length() > 0)
            {
                JobComment jobComment = new JobComment();
                jobComment.setUserId(userContext.getUserId());
                jobComment.setComment(jobSubmitInfo.getComment());

                org.genepattern.server.domain.AnalysisJob analysisJob = new org.genepattern.server.domain.AnalysisJob();
                analysisJob.setJobNo(gpJobNo);
                jobComment.setAnalysisJob(analysisJob);

                JobCommentManager.addJobComment(jobComment);
            }

            //check if there were tags specified for this job and add it to database
            List<String> tags = jobSubmitInfo.getTags();
            if(tags != null && tags.size() > 0)
            {
                Date date = new Date();
                for(String tag: tags)
                {
                    JobTagManager.addTag(userContext.getUserId(), gpJobNo, tag, date, false);
                }
            }

            return Response.ok(result.toString()).build();
        }
        catch (GpServerException e) {
            String message = "An error occurred while submitting the job";
            if(e.getMessage() != null) {
                message = message + ": " + e.getMessage();
            }
            return Response.status(Response.Status.FORBIDDEN).entity(message).build();
        }
        catch(Throwable t) {
            String message = "An error occurred while submitting the job";
            if(t.getMessage() != null) {
                message = message + ": " + t.getMessage();
            }
            log.error(message, t);
            return handleError(message);
        }
    }

    private boolean checkDiskQuota(GpContext userContext) {
        try
        {
            //check if the user is above their disk quota
            //first check if the disk quota is or will be exceeded
            DiskInfo diskInfo = DiskInfo.createDiskInfo(ServerConfigurationFactory.instance(), userContext);

            if(diskInfo.isAboveQuota())
            {
                //disk usage exceeded so do not allow user to run a job
                return true;
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
        return false;
    }

    /**
     * Added this in 3.8.1 release to enable additional job configuration input parameters.
     * @param jobSubmitInfo
     * @param request
     * @return
     */
    private Response addJob(final GpConfig gpConfig, final GpContext userContext, final JobSubmitInfo jobSubmitInfo, final HttpServletRequest request) {
        if (jobSubmitInfo==null || jobSubmitInfo.getLsid()==null || jobSubmitInfo.getLsid().length()==0) {
            return handleError("No lsid received");
        }
        try {
            final JobInputHelper jobInputHelper = new JobInputHelper(gpConfig, userContext, jobSubmitInfo.getLsid());
            final JSONObject parameters = new JSONObject(jobSubmitInfo.getParameters());

            for (final Iterator<?> iter = parameters.keys(); iter.hasNext(); ) {
                final String parameterName = (String) iter.next();
                boolean isBatch = isBatchParam(jobSubmitInfo, parameterName);
                JSONArray valueList;
                final JSONArray groupInfos = parameters.getJSONArray(parameterName);
                for (int i = 0; i < groupInfos.length(); i++) {
                    final JSONObject groupInfo = groupInfos.getJSONObject(i);
                    final String groupName = groupInfo.getString("name");
                    final GroupId groupId;
                    if (groupName == null || groupName.length() == 0) {
                        groupId = GroupId.EMPTY;
                    } else {
                        groupId = new GroupId(groupName);
                    }
                    final Object values = groupInfo.get("values");
                    if (values instanceof JSONArray) {
                        valueList = (JSONArray) values;
                    } else {
                        valueList = new JSONArray((String) parameters.get(parameterName));
                    }
                    for (int v = 0; v < valueList.length(); v++) {
                        final String value = valueList.getString(v);
                        if (isBatch) {
                            //TODO: implement support for groupId with batch values
                            jobInputHelper.addBatchValue(parameterName, value);
                        } else {
                            jobInputHelper.addValue(parameterName, value, groupId);
                        }
                    }
                }
            }

            final List<JobInput> batchInputs;
            batchInputs = jobInputHelper.prepareBatch();
            final JobReceipt receipt = jobInputHelper.submitBatch(batchInputs);

            //TODO: if necessary, add batch details to the JSON representation
            final ResponseJSON result = new ResponseJSON();
            final String jobId;
            if (receipt.getJobIds().size() > 0) {
                jobId = receipt.getJobIds().get(0);
            } else {
                jobId = "-1";
            }
            result.addChild("jobId", jobId);
            if (receipt.getBatchId() != null && receipt.getBatchId().length() > 0) {
                result.addChild("batchId", receipt.getBatchId());
                request.getSession().setAttribute(JobBean.DISPLAY_BATCH, receipt.getBatchId());
            }

            List<String> jobIds = receipt.getJobIds();

            //check if there was a comment specified for job and add it to database
            if(jobSubmitInfo.getComment() != null && jobSubmitInfo.getComment().length() > 0)
            {
                for(String gpJobId : jobIds)
                {
                    int gpJobNo = Integer.parseInt(gpJobId);

                    JobComment jobComment = new JobComment();
                    jobComment.setUserId(userContext.getUserId());
                    jobComment.setComment(jobSubmitInfo.getComment());

                    org.genepattern.server.domain.AnalysisJob analysisJob = new org.genepattern.server.domain.AnalysisJob();
                    analysisJob.setJobNo(gpJobNo);
                    jobComment.setAnalysisJob(analysisJob);

                    JobCommentManager.addJobComment(jobComment);
                }
            }

            //check if there were tags specified for this job and add it to database
            List<String> tags = jobSubmitInfo.getTags();
            if(tags != null && tags.size() > 0)
            {
                Date date = new Date();

                for(String gpJobId : jobIds) {
                    int gpJobNo = Integer.parseInt(gpJobId);

                    for (String tag : tags) {
                        JobTagManager.addTag(userContext.getUserId(), gpJobNo, tag, date, false);
                    }
                }
            }

            return Response.ok(result.toString()).build();
        }
        catch (GpServerException e) {
            String message = "An error occurred while submitting the job";
            if(e.getMessage() != null) {
                message = message + ": " + e.getMessage();
            }
            return Response.status(Response.Status.FORBIDDEN).entity(message).build();
        }
        catch(Throwable t) {
            String message = "An error occurred while submitting the job";
            if(t.getMessage() != null) {
                message = message + ": " + t.getMessage();
            }
            log.error(message, t);
            return handleError(message);
        }
    }

    private Response handleError(final String errorMessage) throws WebApplicationException {
        throw new WebApplicationException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(errorMessage)
                .build()
        );
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
        final GpContext userContext=GpContext.getContextForUser(userId);
        JobInput reloadJobInput=null;
        if (reloadJob != null && !reloadJob.equals("")) {
            //This is a reloaded job
            try {
                final GpContext reloadJobContext=GpContext.createContextForJob(Integer.parseInt(reloadJob));
                reloadJobInput=reloadJobContext.getJobInput();
            }
            catch (Throwable t) {
                log.error("Error initializing from reloadJob="+reloadJob, t);
                return Response.serverError().entity(t.getLocalizedMessage()).build();
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
            String code = CodeGeneratorUtil.getTaskCode(language, lsid, userContext, reloadJobInput, _fileParam, _formatParam, request.getParameterMap());
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

    private JSONArray getParameterList(final HttpServletRequest request, final TaskInfo taskInfo)
    {
        final ParameterInfo[] pArray=taskInfo.getParameterInfoArray();
        final JSONArray parametersObject = new JSONArray();
        for(final ParameterInfo pinfo : pArray) {
            final ParametersJSON parameter = initParametersJSON(request, taskInfo, pinfo);
            parametersObject.put(parameter);
        }
        return parametersObject;
    }

    private static ParametersJSON initParametersJSON(final HttpServletRequest request, final TaskInfo taskInfo, final ParameterInfo pinfo) {
        // don't initialize the drop-down menu; instead wait for the web client to make a callback
        final boolean initDropdown=false;
        final ParametersJSON parameter = new ParametersJSON(pinfo);
        parameter.addNumValues(pinfo);
        parameter.addGroupInfo(pinfo);
        parameter.initChoice(request, taskInfo, pinfo, initDropdown);
        return parameter;
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
    private SortedSet<LSID> getModuleVersions(final GpContext userContext, final TaskInfo taskInfo) throws Exception
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
    private boolean isEditable(final GpContext userContext, final TaskInfo taskInfo) {
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

    private boolean isHiddenBetaVersion(TaskInfo taskInfo)
    {
        String taskQuality = taskInfo.getTaskInfoAttributes().get("quality");

        if (taskQuality.equalsIgnoreCase("development")) {
            return true;
        }

        return false;
    }
}

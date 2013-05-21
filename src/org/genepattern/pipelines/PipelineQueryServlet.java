/*
 * Copyright 2012 The Broad Institute, Inc.
 * SOFTWARE COPYRIGHT NOTICE
 * This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
 */

package org.genepattern.pipelines;

import static org.genepattern.util.GPConstants.SERIALIZED_MODEL;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineDependencyHelper;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.data.pipeline.PipelineUtil;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.executor.pipeline.PipelineException;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.webapp.PipelineCreationHelper;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet for handling the server-side calls necessary for the new Pipeline Designer
 * @author tabor
 */
public class PipelineQueryServlet extends HttpServlet {
	private static final long serialVersionUID = 8270613493170496154L;
	public static Logger log = Logger.getLogger(PipelineQueryServlet.class);
	
	public static final String LIBRARY = "/library";       // path for calling the REST-like library call
	public static final String SAVE = "/save";             // path for calling the REST-like save call
	public static final String LOAD = "/load";             // path for calling the REST-like load call
	public static final String UPLOAD = "/upload";         // path for calling the REST-like upload call
	public static final String DEPENDENTS = "/dependents"; // path for calling the REST-like dependents call
	
	public static final String DSL_FIRST = "1st Output";
	public static final String DSL_SECOND = "2nd Output";
	public static final String DSL_THIRD = "3rd Output";
	public static final String DSL_FOURTH = "4th Output";
	public static final String DSL_SCATTER = "Scatter Each Output";
	public static final String DSL_GATHER = "File List of All Outputs";
	
	public static final String MANIFEST_FIRST = "1";
    public static final String MANIFEST_SECOND = "2";
    public static final String MANIFEST_THIRD = "3";
    public static final String MANIFEST_FOURTH = "4";
    public static final String MANIFEST_SCATTER = "?scatter&filter=*";
    public static final String MANIFEST_GATHER = "?filelist&filter=*";
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String action = request.getPathInfo();
		
		// Route to the appropriate action, returning an error if unknown
		if (LIBRARY.equals(action)) {
		    constructLibrary(request, response);
		}
		else if (SAVE.equals(action)) {
		    savePipeline(request, response);
		}
		else if (LOAD.equals(action)) {
            loadPipeline(request, response);
        }
		else if (UPLOAD.equals(action)) {
		    uploadFile(request, response);
		}
		else if (DEPENDENTS.equals(action)) {
            updateDependents(request, response);
        }
		else {
		    sendError(response, "Routing error for " + action);
		}
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) {
		doGet(request, response);
	}
	
	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response) {
	    doGet(request, response);
	}
	
	private void write(HttpServletResponse response, Object content) {
	    this.write(response, content.toString());
	}
	
	/**
	 * Write a string back to the response
	 * @param response
	 * @param content
	 */
	private void write(HttpServletResponse response, String content) {
	    PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.println(content);
            writer.flush();
        }
        catch (IOException e) {
            log.error("Error writing to the response in PipelineQueryServlet: " + content);
            e.printStackTrace();
        }
        finally {
            if (writer != null) writer.close();
        }
	}
	
	/**
	 * Send an error as JSON back with the response
	 * @param response
	 * @param message
	 */
	public void sendError(HttpServletResponse response, String message) {
	    ResponseJSON error = new ResponseJSON();
	    error.addError("ERROR: " + message);
	    this.write(response, error);
	}
	
	/**
	 * Handles the actual transfer of an uploaded file.
	 * @param from
	 * @param to
	 * @throws IOException
	 */
	private void transferUpload(FileItem from, File to) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        
        try {
            is = from.getInputStream();
            os = new BufferedOutputStream(new FileOutputStream(to, true));
            final int BUFSIZE = 2048;
            final byte buf[] = new byte[BUFSIZE];
            int n;
            while ((n = is.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
        finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
	
	public void updateDependents(HttpServletRequest request, HttpServletResponse response) {
	    // Test the session
        String username = (String) request.getSession().getAttribute("userid");
        if (username == null) {
            sendError(response, "No GenePattern session found.  Please log in.");
            return;
        }
        
        // Make sure the update object was passed to the server
        String updates = request.getParameter("updates");
        if (updates == null) {
            log.error("Unable to update the dependent pipelines");
            sendError(response, "Unable to update the dependent pipelines");
            return;
        }
        
        // Transform the update object string into a JSON object
        Set<TaskInfo> changed = new HashSet<TaskInfo>();
        try {
            JSONObject updateObject = new JSONObject(updates);
            JSONArray updateListJSON = updateObject.getJSONArray("updateList");
            String oldLsid = updateObject.getString("oldLsid");
            String newLsid = updateObject.getString("newLsid");
            
            for (int i = 0; i < updateListJSON.length(); i++) {
                String lsidToUpdate = updateListJSON.getString(i);
                changed.addAll(recursiveDependencyUpdate(lsidToUpdate, oldLsid, newLsid, username));
            }
        }
        catch (Throwable e) {
            log.error("Unable to read the list of pipelines to update");
            sendError(response, "Unable to read the list of pipelines to update");
            return;
        }
        
        // Respond to the client
        ResponseJSON message = new ResponseJSON();
        message.addMessage("Pipeline Dependencies Updated");
        if (changed != null && changed.size() > 0) {
            message.addChild("changed", makeDependentsObject(username, changed));
        }
        this.write(response, message);
	}
	
	public Set<TaskInfo> recursiveDependencyUpdate(String lsidToUpdate, String oldLsid, String newLsid, String username) throws Throwable {
	    TaskInfo toUpdateTaskInfo = TaskInfoCache.instance().getTask(lsidToUpdate);
	    TaskInfo newTaskInfo = TaskInfoCache.instance().getTask(newLsid);
	    PipelineModel toUpdateModel = null;
	    String newUpdateLsid = null;
	    Set<TaskInfo> changed = new HashSet<TaskInfo>();
	    
	    // Get a list of the pipelines dependent on this one (useful for recursion later)
        Set<TaskInfo> dependents = PipelineDependencyHelper.instance().getDependentPipelines(toUpdateTaskInfo);
	    
	    synchronized(this) {
    	    try {
                toUpdateModel = PipelineUtil.getPipelineModel(toUpdateTaskInfo);

                // Do the changes here
                for (JobSubmission job : toUpdateModel.getTasks()) {
                    if (job.getLSID().equals(oldLsid)) {
                        job.setLSID(newLsid);
                        job.setTaskInfo(newTaskInfo);
                    }
                }
                
                // Save the pipeline
                PipelineCreationHelper controller = new PipelineCreationHelper(toUpdateModel);
                controller.generateLSID();
                newUpdateLsid = controller.generateTask();
                
                // Copy the files to the new TaskLib
                copyFilesToNewTaskLib(toUpdateTaskInfo.getLsid(), toUpdateTaskInfo.getName(), new ArrayList<String>(), toUpdateTaskInfo, newUpdateLsid, username);
            }
            catch (Exception e) {
                throw new PipelineException("Unable to obtain a PipelineModel from the TaskInfo");
            }
	    }
        
        // Recurse for all pipeline dependent on the old toUpdateTaskInfo
        //Set<TaskInfo> dependents = toUpdateModel.getDependentPipelines();
        if (dependents != null) {
            for (TaskInfo info : dependents) {
                Set<TaskInfo> called = recursiveDependencyUpdate(info.getLsid(), toUpdateTaskInfo.getLsid(), newUpdateLsid, username);
                changed.addAll(called);
            }
        }
        
        changed.add(toUpdateTaskInfo);
        return changed;
	}

	/**
	 * Handles uploading a file from the Pipeline Designer client to the server
     * Saves the file in a temp directory until the pipeline is saved.
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
    public void uploadFile(HttpServletRequest request, HttpServletResponse response) {
	    RequestContext reqContext = new ServletRequestContext(request);
        if (FileUploadBase.isMultipartContent(reqContext)) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<FileItem> postParameters = upload.parseRequest(reqContext);
                
                for (FileItem i : postParameters) {
                    // Only read the submitted files
                    if (!i.isFormField()) {
                        // Store in a temp directory until the pipeline is saved
                        String username = (String) request.getSession().getAttribute("userid");
                        
                        if (username == null) {
                            throw new Exception("No valid session detected.  Please log in again.");
                        }

                        ServerConfiguration.Context userContext = ServerConfiguration.Context.getContextForUser(username);
                        File fileTempDir = ServerConfiguration.instance().getTemporaryUploadDir(userContext);
                        File uploadedFile = new File(fileTempDir, i.getName());
                        
                        transferUpload(i, uploadedFile);
                        
                        // Return a success response
                        ResponseJSON message = new ResponseJSON();
                        message.addChild("location", uploadedFile.getCanonicalPath());
                        this.write(response, message);
                    }
                }
            }
            catch (Exception e) {
                sendError(response, "Exception retrieving the uploaded file:\n" + e.getLocalizedMessage());
                return;
            }
            catch (Throwable t) {
                sendError(response, "Exception retrieving the uploaded file:\n" + t.getLocalizedMessage());
                return;
            }
        }
        else {
            sendError(response, "Unable to find uploaded file");
        }
	}
	
	/**
	 * Load a pipeline using the requested lsid and return a representation of it in JSON 
	 * to the Pipeline Designer client
	 * @param request
	 * @param response
	 */
	@SuppressWarnings("unchecked")
    public void loadPipeline(HttpServletRequest request, HttpServletResponse response) {
	    String lsid = request.getParameter("lsid");
	    
	    if (lsid == null) {
	        sendError(response, "No lsid received");
	        return;
	    }
	    
	    TaskInfo info = null;
	    try {
	        info = TaskInfoCache.instance().getTask(lsid);
	    }
	    catch (Throwable t) {
	        sendError(response, "Unable to load the selected pipeline");
	        return;
	    }
	    
	    String username = (String) request.getSession().getAttribute("userid");
	    if (!"public".equals(info.getTaskInfoAttributes().get("privacy")) && !info.getUserId().equals(username)) {
	        sendError(response, "Access Denied");
            return;
	    }
	    
	    PipelineModel pipeline = null;
	    try {
            pipeline = PipelineModel.toPipelineModel(info.getTaskInfoAttributes().get(SERIALIZED_MODEL));
            pipeline.setLsid(info.getLsid());
            ParameterInfo[] params = info.getParameterInfoArray();
            
            for (ParameterInfo i : params) {
                String runTimePrompt = (String) i.getAttributes().get("runTimePrompt");
                if ("1".equals(runTimePrompt)) {
                    ParameterInfo modelParam = pipeline.getInputParameters().get(i.getName());
                    String altName = (String) i.getAttributes().get("altName");
                    String altDescription = (String) i.getAttributes().get("altDescription");
                    
                    modelParam.getAttributes().put("altName", altName);
                    modelParam.getAttributes().put("altDescription", altDescription);
                }
            }
        }
        catch (Exception e) {
            sendError(response, "Exception loading pipeline");
            return;
        }

        ResponseJSON responseObject = new ResponseJSON();
        PipelineJSON pipelineObject = new PipelineJSON(username, pipeline, info);
        ResponseJSON modulesObject = createModuleList(pipeline);
        ResponseJSON pipesObject = PipeJSON.createPipeList(pipeline.getTasks(), this);
        ResponseJSON filesObject = createFileList(pipeline);
        
        responseObject.addChild(PipelineJSON.KEY, pipelineObject);
        responseObject.addChild(ModuleJSON.KEY, modulesObject);
        responseObject.addChild(PipeJSON.KEY, pipesObject);
        responseObject.addChild(FileJSON.KEY, filesObject);
        
        this.write(response, responseObject);
	}
	
	/**
	 * Returns a list of modules that are upstream from the specified module (must be executed 
	 * in the pipeline before the given module, provided the list of pipes between the modules 
	 * in the pipeline)
	 * @param id
	 * @param pipesObject
	 * @param moduleMap
	 * @return
	 * @throws JSONException
	 */
	private List<ModuleJSON> getUpstreamModules(Integer id, PipeJSON[] pipesObject, Map<Integer, ModuleJSON> moduleMap) throws JSONException {
	    List<ModuleJSON> inputs = new ArrayList<ModuleJSON>();
	    for (PipeJSON pipe : pipesObject) {
	        if (pipe.getInputModule().equals(id)) {
	            inputs.add(moduleMap.get(pipe.getOutputModule()));
	        }
	    }
	    
	    return inputs;
	}
	
	/**
	 * Places a given module in the list of modules to be executed in the pipeline.
	 * Used by the algorithm that determines module order in a given pipeline.
	 * @param moduleList
	 * @param moduleMap
	 * @param module
	 * @param pipesObject
	 * @return
	 * @throws Exception
	 */
	private List<ModuleJSON> placeModuleInList(List<ModuleJSON> moduleList, Map<Integer, ModuleJSON> moduleMap, 
	        ModuleJSON module, PipeJSON[] pipesObject) throws Exception {   
	    
	    // Get the list of modules that provide input this this module
	    List<ModuleJSON> inputModules = getUpstreamModules(module.getId(), pipesObject, moduleMap);
	    
	    // For each upstream module place it in the list ahead of this one and recurse if not already in
	    for (ModuleJSON m : inputModules) {
	        // Get the index of this module in the list
            int currentIndex = moduleList.indexOf(module);
	        
	        // Found upstream module already in the list
	        if (moduleList.contains(m)) {
	            // Get the index of the input module in the list
	            int inputIndex = moduleList.indexOf(m);
	            // If the input module comes after the current module, throw an error
	            if (inputIndex >= currentIndex) {
	                throw new Exception("Loop found in pipeline model");
	            }
	            else {
	                // Otherwise, move on
	                // continue;
	            }
	        }
	        else {
	            // If not found add it to the list before the current and recurse
	            moduleList.add(currentIndex, m);
	            moduleList = placeModuleInList(moduleList, moduleMap, m, pipesObject);
	        }
	    }
	    return moduleList;
	}
	
	/**
	 * Transforms the graph of modules in a pipeline into a list of modules, giving an execution 
	 * order to those modules based on dependencies.
	 * @param modulesObject
	 * @param pipesObject
	 * @return
	 * @throws Exception
	 */
	private List<ModuleJSON> transformGraph(ModuleJSON[] modulesObject, PipeJSON[] pipesObject) throws Exception {
	    // Build a map of module IDs to module objects
	    Map<Integer, ModuleJSON> moduleMap = new HashMap<Integer, ModuleJSON>();
	    for (ModuleJSON mod : modulesObject) {
	        moduleMap.put(mod.getId(), mod);
        }
	    
	    // Recursively build the list
	    List<ModuleJSON> moduleList = new LinkedList<ModuleJSON>();
	    for (ModuleJSON module : modulesObject) {
	        // Add the module to the end of the list if it's not already in the list
	        if (!moduleList.contains(module)) {
	            moduleList.add(module);
	        }
	        
	        moduleList = placeModuleInList(moduleList, moduleMap, module, pipesObject);
        }
	    
	    return moduleList;
	}
	
	/**
	 * Return a blank lsid if necessary
	 * @param lsid
	 * @return
	 */
	private String blankLsidIfNecessary(String lsid) {
	    if (lsid.length() < 10) {
	        return "";
	    }
	    else {
	        return lsid;
	    }
	}
	
	private void setPipelineInfo(PipelineModel model, PipelineJSON pipelineObject) throws JSONException {
	    model.setName(pipelineObject.getName());
	    model.setDescription(pipelineObject.getDescription());
	    model.setAuthor(pipelineObject.getAuthor());
	    model.setVersion(pipelineObject.getVersionComment());
	    model.setPrivacy(pipelineObject.getPrivacy().equals(GPConstants.PRIVATE));
	    
	    String lsid = blankLsidIfNecessary(pipelineObject.getLsid());
	    model.setLsid(lsid);
	}
	
	@SuppressWarnings("rawtypes")
    private ParameterInfo[] copyModuleParams(ParameterInfo[] taskParams) throws WebServiceException {
        ParameterInfo[] newParams = new ParameterInfo[taskParams.length];

        for (int i = 0; i < taskParams.length; i++) {
            ParameterInfo taskParam = new ParameterInfo();
            taskParam.setName(taskParams[i].getName());
            taskParam.setDescription(taskParams[i].getDescription());
            taskParam.setLabel(taskParams[i].getLabel());
            taskParam.setValue(taskParams[i].getDefaultValue());                        // set to the default by default
            taskParam.setAttributes((HashMap) taskParams[i].getAttributes().clone());   // is a shallow copy OK here?

            newParams[i] = taskParam;
        }
        return newParams;
    }
	
	@SuppressWarnings("unchecked")
    private ParameterInfo setParameter(String pName, String value, ParameterInfo[] taskParams, List<String> promptWhenRun) throws Exception {
        for (ParameterInfo param : taskParams) {
            if (pName.equalsIgnoreCase(param.getName())) {
                param.setValue(value);
                if (promptWhenRun != null) {
                    param.getAttributes().put("altName", promptWhenRun.get(0));
                    param.getAttributes().put("altDescription", promptWhenRun.get(1));
                    param.getAttributes().put(PipelineModel.RUNTIME_PARAM, "1");
                }
                else {
                    param.getAttributes().put("altName", param.getName());
                    param.getAttributes().put("altDescription", param.getDescription());
                }
                return param;
            }
        }
        throw new Exception("Parameter " + pName + " was not found");
    }
    
    private List<String> toPWRList(JSONArray pwr) throws JSONException {
        if (pwr == null) return null;

        List<String> toReturn = new ArrayList<String>();
        toReturn.add((String) pwr.get(0));
        toReturn.add((String) pwr.get(1));
        return toReturn;
    }
	
	@SuppressWarnings("unchecked")
    private void setModuleInfo(PipelineModel model, List<ModuleJSON> modulesList) throws Exception {
	    IAdminClient adminClient = new LocalAdminClient(model.getUserID());
        Map<String, TaskInfo> taskCatalog = adminClient.getTaskCatalogByLSID();
	    
        int taskNum = 1;
	    for (ModuleJSON module : modulesList) {
	        TaskInfo taskInfo = taskCatalog.get(module.getLsid());
	        TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
	        ParameterInfo[] moduleParams = copyModuleParams(taskInfo.getParameterInfoArray());
	        boolean[] promptWhenRun = new boolean[moduleParams.length];
	        
	        // Set all the parameter values
	        for (int i = 0; i < module.getInputs().length(); i++) {
	            InputJSON input = (InputJSON) module.getInputs().getJSONObject(i);
	            JSONArray pwr = input.getPromptWhenRun();
	            promptWhenRun[i] = pwr != null;
	            if (pwr != null) {
	                ParameterInfo pInfo = setParameter(input.getName(), taskInfo.getParameterInfoArray()[i].getValue(), moduleParams, toPWRList(pwr));
	                pInfo.getAttributes().put("name", taskInfo.getName() + taskNum + "." + input.getName());
	                model.addInputParameter(taskInfo.getName() + taskNum + "." + input.getName(), pInfo);
	            }
	            else {
                    // Special case for file paths
                    if (taskInfo.getParameterInfoArray()[i].getAttributes().get("type").equals("java.io.File") && !input.getValue().equals("")) {
                        String filePath = null;
                        // Check for values that already embed the <GenePatternURL>
                        if (input.getValue().startsWith("<GenePatternURL>")) {
                            filePath = input.getValue();
                        }
                        else if (input.getValue().startsWith("http://")) {  // Check for external HTTP URLs
                            filePath = input.getValue();
                        }
                        else if (input.getValue().startsWith("ftp://")) {  // Check for external FTP URLs
                            filePath = input.getValue();
                        }
                        else {
                            filePath = "<GenePatternURL>getFile.jsp?task=<LSID>&file=" + URLEncoder.encode(input.getValue(), "UTF-8");
                        }
                        moduleParams[i].getAttributes().put("runTimePrompt", null);
                        setParameter(input.getName(), filePath, moduleParams, null);
                    }
                    else {
                        moduleParams[i].getAttributes().put("runTimePrompt", null);
                        setParameter(input.getName(), input.getValue(), moduleParams, null);
                    }
	            }
	        }
	        
	        JobSubmission js = new JobSubmission(taskInfo.getName(), taskInfo.getDescription(), tia.get(GPConstants.LSID), moduleParams, promptWhenRun, 
	                TaskInfo.isVisualizer(tia), taskInfo);
	        model.addTask(js);
	        taskNum++;
	    }
	}
	
	private int findIndexWithId(int id, List<ModuleJSON> modulesList) throws JSONException {
	    for (int i = 0; i < modulesList.size(); i++) {
	        if (modulesList.get(i).getId().equals(id)) {
	            return i;
	        }
	    }
	    return -1;
	}
	
	@SuppressWarnings("unchecked")
    private void setPipesInfo(PipelineModel model, List<ModuleJSON> modulesList, PipeJSON[] pipesObject) throws JSONException {
        for (PipeJSON pipe : pipesObject) {
            // Read the pipe
            Integer outputId = pipe.getOutputModule();
            Integer inputId = pipe.getInputModule();
            String value = pipe.getOutputPort();
            String param = pipe.getInputPort();
            
            // Unescape the value
            value = StringEscapeUtils.unescapeHtml(value);
            
            // Translate dsl to manifest
            value = dslToManifest(value);
            
            // Find the index of input and output
            Integer outputIndex = findIndexWithId(outputId, modulesList);
            Integer inputIndex = findIndexWithId(inputId, modulesList);
            
            // Set inheriting the value on the input
            JobSubmission inputModule = model.getTasks().get(inputIndex);
            Vector<ParameterInfo> params = inputModule.getParameters();
            for (ParameterInfo pi : params) {
                if (pi.getName().equals(param)) {
                    pi.getAttributes().put(PipelineModel.INHERIT_TASKNAME, outputIndex.toString());
                    pi.getAttributes().put(PipelineModel.INHERIT_FILENAME, value);
                    pi.setValue("");
                }
            }
        }
    }
	
	/**
	 * Transforms the representation of a selection in the manifest to its representation in the 
	 * Pipeline Designer's domain specific language (DSL)
	 * @param manifest representation
	 * @return dsl representation
	 */
	protected String manifestToDsl(String manifest) {
	    if (MANIFEST_FIRST.equals(manifest)) return DSL_FIRST;
	    else if (MANIFEST_SECOND.equals(manifest)) return DSL_SECOND;
	    else if (MANIFEST_THIRD.equals(manifest)) return DSL_THIRD;
	    else if (MANIFEST_FOURTH.equals(manifest)) return DSL_FOURTH;
	    else if (MANIFEST_SCATTER.equals(manifest)) return DSL_SCATTER;
	    else if (MANIFEST_GATHER.equals(manifest)) return DSL_GATHER;
	    else return manifest;
    }
	
	/**
     * Transforms the representation of a selection in the Pipeline Designer's domain specific 
     * language (DSL) to its representation in the manifest
     * @param dsl representation
     * @return manifest representation
     */
	private String dslToManifest(String dsl) {
	    if (DSL_FIRST.equals(dsl)) return MANIFEST_FIRST;
        else if (DSL_SECOND.equals(dsl)) return MANIFEST_SECOND;
        else if (DSL_THIRD.equals(dsl)) return MANIFEST_THIRD;
        else if (DSL_FOURTH.equals(dsl)) return MANIFEST_FOURTH;
        else if (DSL_SCATTER.equals(dsl)) return MANIFEST_SCATTER;
        else if (DSL_GATHER.equals(dsl)) return MANIFEST_GATHER;
        else return dsl;
	}
	
	/**
	 * Handle the call to save a pipeline
	 * @param request
	 * @param response
	 */
	public void savePipeline(HttpServletRequest request, HttpServletResponse response) {
	    // Test the session
	    String username = (String) request.getSession().getAttribute("userid");
	    if (username == null) {
	        sendError(response, "No GenePattern session found.  Please log in.");
	        return;
	    }
	    
	    String bundle = request.getParameter("bundle");
	    if (bundle == null) {
	        log.error("Unable to retrieved the saved pipeline");
	        sendError(response, "Unable to save the pipeline");
	        return;
	    }
	    
        // Extract the right json from the saved bundle
	    PipelineJSON pipelineObject;
        ModuleJSON[] modulesObject;
	    PipeJSON[] pipesObject;
	    Map<String, FileJSON> filesObject;
	    try {
	        JSONObject pipelineJSON = PipelineJSON.parseBundle(bundle);
	        pipelineObject = PipelineJSON.extract(pipelineJSON);
	        modulesObject = ModuleJSON.extract(pipelineJSON);
	        pipesObject = PipeJSON.extract(pipelineJSON);
	        filesObject = FileJSON.extract(pipelineJSON);
	    }
	    catch (Throwable t) {
	        log.error("Error parsing JSON bundle", t);
	        sendError(response, "Unable to save the pipeline: Server error parsing JSON bundle");
	        return;
	    } 
	    
	    try {
	        // Test that the saved pipeline has not been deleted
    	    String lsid = pipelineObject.getLsid();
    	    if (lsid.length() > 4) { // Test if the lsid is not blank or zero
    	        TaskInfo info = TaskInfoCache.instance().getTask(lsid);
    	        if (info == null) throw new Exception("TaskInfo is null");
    	        
    	        // Clone pipeline is necessary
    	        String oldUser = info.getUserId();
    	        if (!username.equals(oldUser)) {
    	            pipelineObject.setLsid(""); // Set to Clone
    	        }
    	    }
	    }
	    catch (Throwable t) {
            log.error("Error loading older version of saved pipeline", t);
            sendError(response, "Unable to save the pipeline: Unable to locate the older versions of the pipeline being saved.");
            return;
        }
	    
	    // Transform the graph of modules and pipes to an ordered list
	    List<ModuleJSON> modulesList = null;
	    try {
            modulesList = transformGraph(modulesObject, pipesObject);
        }
        catch (Throwable t) {
            log.error("Unable to transform the graph to a list", t);
            sendError(response, "Unable to save the pipeline: Server error, Unable to transform the graph to a list");
            return;
        }
        
        // Build up the pipeline model to save
        String newLsid = null;
        Set<TaskInfo> dependents = null;
        
        synchronized(this) {
            PipelineModel model = null;
            PipelineCreationHelper controller = null;
            try {
                model = new PipelineModel();
                model.setUserID(username); 
                
                // if there is a license, add it to the model
                String license = pipelineObject.getLicense();
                if (license != null && license.length() > 0) {
                    model.setLicense(license);
                } 
                
                // if there is doc, add it to the model
                String doc = pipelineObject.getDocumentation();
                if (doc == null) {
                    // if there is no declared doc, use an empty string
                    doc = "";
                }
                model.setDocumentation(doc);
                model.setGenePatternVersion(System.getProperty("genepattern.version", "unknown"));
                model.setCreationDate(new Date());
                
                setPipelineInfo(model, pipelineObject);
    
                controller = new PipelineCreationHelper(model);
                controller.generateLSID();
    
                setModuleInfo(model, modulesList);
                setPipesInfo(model, modulesList, pipesObject);
            }
            catch (Throwable t) {
                log.error("Unable to build the pipeline model", t);
                sendError(response, "Unable to save the pipeline: Server error, Unable to build the pipeline model");
                return;
            }
            
            // Generate a task from the pipeline model and install it
            try {
                newLsid = controller.generateTask();
            }
            catch (TaskInstallationException e) {
                log.error("Unable to install the pipeline:" + e.getMessage(), e);
                sendError(response, "Unable to save the pipeline: "+e.getMessage());
                return;
            }
        }
        
        TaskInfo oldInfo = null;
        
        // Check for dependent pipelines to prompt for update
        try {
            if (pipelineObject.getLsid().length() > 0) { 
                oldInfo = TaskInfoCache.instance().getTask(pipelineObject.getLsid());
                dependents = PipelineDependencyHelper.instance().getDependentPipelines(oldInfo);
            }
        }
        catch (Exception e) {
            log.error("Unable to retrieve the old taskInfo based on old lsid for: " + newLsid, e);
            sendError(response, "Unable to save uploaded files for the pipeline: " + e.getLocalizedMessage());
            return;
        }

        // Create the new pipeline directory in taskLib and move files
        try {
            File newDir = copyFilesToNewTaskLib(pipelineObject.getLsid(), pipelineObject.getName(), pipelineObject.getFiles(), oldInfo, newLsid, username);
            
            // Create verified files list and purge unnecessary files
            FileCollection verifiedFiles = extractVerifiedFiles(newDir, pipelineObject, filesObject);
            purgeUnnecessaryFiles(newDir, verifiedFiles.getInternal());
            
            PipelineDesignerFile pdFile = new PipelineDesignerFile(newDir);
            pdFile.write(modulesList, verifiedFiles);
        }
        catch (Throwable e) {
            log.error("Unable to retrieve the old taskInfo based on old lsid for: " + newLsid, e);
            sendError(response, "Unable to save uploaded files for the pipeline: " + e.getLocalizedMessage());
            return;
        }

        // Respond to the client
        ResponseJSON message = new ResponseJSON();
        message.addMessage("Pipeline Saved");
        message.addChild("lsid", newLsid);
        if (dependents != null && dependents.size() > 0) {
            message.addChild("dependents", makeDependentsObject(username, dependents));
        }
        this.write(response, message);
	}
	
	private File copyFilesToNewTaskLib(String oldLsid, String name, List<String> files, TaskInfo oldInfo, String newLsid, String username) throws Throwable {
	    try {
            // Create the new pipeline directory in taskLib and move files
            File newDir = null;
            // Existing pipeline being saved
            if (oldLsid.length() > 0) { 
                newDir = this.copySupportFiles(oldInfo.getName(), name, oldInfo.getLsid(), newLsid, username); 
            }
            else { // New pipeline being saved
                newDir = new File(DirectoryManager.getTaskLibDir(name + "." + GPConstants.TASK_TYPE_PIPELINE, newLsid, username));
            }
            this.copyNewFiles(files, newDir);

            return newDir;
        }
        catch (Throwable t) {
            throw t;
        }
	}
	
	private ResponseJSON makeDependentsObject(String username, Set<TaskInfo> dependents) {
	    ResponseJSON dependentsObject = new ResponseJSON();
	    
	    int count = 0;
	    for (TaskInfo dependent : dependents) {
	        if (username.equals(dependent.getUserId())) {
	            dependentsObject.addChild(count, makeDependentObject(dependent));
	            count++;
	        }
	    }
	    
	    return dependentsObject;
	}
	
	private ResponseJSON makeDependentObject(TaskInfo dependent) {
        ResponseJSON dependentObject = new ResponseJSON();
        dependentObject.addChild("name", dependent.getName());
        dependentObject.addChild("lsid", dependent.getLsid());
        
        return dependentObject;
    }
	
	private void purgeUnnecessaryFiles(File taskDir, List<File> necessaryFiles) {
	    for (File file : taskDir.listFiles()) {
	        boolean necessary = false;
	        
	        // Ignore internal pipeline files
	        if (file.getName() == "manifest" || file.getName() == ".pipelineDesigner") {
	            necessary = true;
	            continue; // File is necessary
	        }
	        
	        // Ignore system files
	        if (file.getName() == "Thumbs.db" || file.getName() == ".DS_Store" || file.getName() == "desktop.ini") {
	            necessary = true;
                continue; // File is necessary
            }
	        
	        for (File necessaryFile: necessaryFiles) {
	            if (necessaryFile.equals(file)) {
	                necessary = true;
	                continue; // File is necessary
	            }
	        }
	        
	        // Otherwise, purge the file
	        if (!necessary) {
	            file.delete();
	        } 
	    }
	}
	
	/**
	 * Determines if the path to a file is for a file internal to the GenePattern server
	 * @param path
	 * @return
	 */
	private boolean isInternalFile(String path) {
	    if ((path.contains("<GenePatternURL>") && path.contains("<LSID>")) || path.startsWith("/") || path.contains(":\\")) {
	        return true;
	    }
	    else {
	        return false;
	    }
	}
	
	private FileCollection extractVerifiedFiles(File dir, PipelineJSON pipelineObject, Map<String, FileJSON> files) throws Exception {
	    FileCollection verified = new FileCollection();
	    
	    try {
	        // Handle documentation files
            String doc = pipelineObject.getDocumentation();
            if (!"".equals(doc) && doc != null) {
                verified.doc = new File (dir, doc);
            }
            
            // Handle license files
            String license = pipelineObject.getLicense();
            if (!"".equals(license) && license != null) {
                verified.license = new File (dir, license);
            }
            
            // Handle module input files
            for (FileJSON file : files.values()) {
                if (isInternalFile(file.getPath())) {
                    File systemFile = new File (dir, file.getName());
                    verified.inputFiles.add(systemFile);
                    verified.positions.put(systemFile, new HashMap<String, String>());
                    verified.positions.get(systemFile).put("top", file.getTop());
                    verified.positions.get(systemFile).put("left", file.getLeft());
                }
                else {
                    verified.urls.add(file.getPath());
                    verified.positions.put(file.getPath(), new HashMap<String, String>());
                    verified.positions.get(file.getPath()).put("top", file.getTop());
                    verified.positions.get(file.getPath()).put("left", file.getLeft());
                }
            }
        }
        catch (JSONException e) {
            log.error("Unable to extract files");
            throw new Exception("Unable to extract files: " + e.getLocalizedMessage());
        }
	    
	    return verified;
	}
	
	public void constructLibrary(HttpServletRequest request, HttpServletResponse response) {
	    String username = (String) request.getSession().getAttribute("userid");
	    
	    ResponseJSON listObject = new ResponseJSON();
	    Integer count = 0;
        for (TaskInfo info : TaskInfoCache.instance().getAllTasks()) {
            if ("public".equals(info.getTaskInfoAttributes().get("privacy")) || info.getUserId().equals(username)) {
                ModuleJSON mj = new ModuleJSON(info, username);
                listObject.addChild(count, mj);
                count++;
            }
        }
        
        this.write(response, listObject);
	}
	
	private void copyNewFiles(List<String> files, File copyTo) throws Exception {
	    if (copyTo == null || !copyTo.isDirectory()) {
	        throw new Exception("Attempting to copy files to a location that is not a directory");
	    }
	    
	    for (String path : files) {
	        File file = new File(path);
	        if (!file.exists()) {
	            throw new Exception("Attempting to move file that does not exist: " + path);
	        }

	        // Move file to new directory
	        boolean success = file.renameTo(new File(copyTo, file.getName()));
	        if (!success) {
	            throw new Exception("Unable to move file: " + file.getName());
	        }
	    }
	}
	
	// Method copied directly from makePipeline.jsp
    private File copySupportFiles(String oldTaskName, String newTaskName, String oldLSID, String newLSID, String userID) throws Exception {
        String oldDir = DirectoryManager.getTaskLibDir(oldTaskName + "." + GPConstants.TASK_TYPE_PIPELINE, oldLSID, userID);
        String newDir = DirectoryManager.getTaskLibDir(newTaskName + "." + GPConstants.TASK_TYPE_PIPELINE, newLSID, userID);

        File[] oldFiles = new File(oldDir).listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (!name.endsWith(".old") && !name.equals("version.txt"));
            }
        });
        byte[] buf = new byte[100000];
        int j;
        for (int i = 0; oldFiles != null && i < oldFiles.length; i++) {
            FileInputStream is = new FileInputStream(oldFiles[i]);
            FileOutputStream os = new FileOutputStream(new File(newDir, oldFiles[i].getName()));
            while ((j = is.read(buf, 0, buf.length)) > 0) {
                os.write(buf, 0, j);
            }
            is.close();
            os.close();
        }
        
        return new File(newDir);
    }
    
    @SuppressWarnings("unchecked")
    public static ResponseJSON createFileList(PipelineModel pipeline) {
     // Get the pipeline's directory of files
        File directory = PipelineQueryServlet.getPipelineDirectory(pipeline);
        
        // Read the pipeline designer file and populate list for insertion into json
        PipelineDesignerFile pdFile = new PipelineDesignerFile(directory);
        Map<String, Object> reads = pdFile.read();
        Map<Integer, Map<String, String>> fileReads = (Map<Integer, Map<String, String>>) reads.get("files");
        ResponseJSON listObject = new ResponseJSON();
        
        if (fileReads != null) {   
            Integer idCounter = 0;
            Map<String, String> fileMap = fileReads.get(idCounter);
            while (fileMap != null) {
                if ("file".equals(fileMap.get("type"))) {
                    FileJSON file = new FileJSON(fileMap.get("name"), buildInternalPath(fileMap.get("name")), fileMap.get("top"), fileMap.get("left"));
                    listObject.addChild(idCounter, file);
                    idCounter++;
                }
                else if ("url".equals(fileMap.get("type"))) {
                    FileJSON file = new FileJSON(extractFilename(fileMap.get("name")), fileMap.get("name"), fileMap.get("top"), fileMap.get("left"));
                    listObject.addChild(idCounter, file);
                    idCounter++;
                }
                else {
                    idCounter++;
                }
                fileMap = fileReads.get(idCounter);
            }
        }
        
        return listObject;
    }
    
    private static String buildInternalPath(String name) {
        try {
            return "<GenePatternURL>getFile.jsp?task=<LSID>&file=" + URLEncoder.encode(name, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            log.error("ERROR: Building path for: " + name);
            return name;
        }
    }
    
    private static String extractFilename(String path) {
        String[] parts = path.split("/");
        try {
            return URLDecoder.decode(parts[parts.length - 1], "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            log.error("ERROR: Parsing filename from path: " + path);
            return path;
        }
    }
    
    @SuppressWarnings("unchecked")
    public static ResponseJSON createModuleList(PipelineModel pipeline) {
        // Get the pipeline's directory of files
        File directory = PipelineQueryServlet.getPipelineDirectory(pipeline);
        
        // Read the pipeline designer file and populate list for insertion into json
        PipelineDesignerFile pdFile = new PipelineDesignerFile(directory);
        Map<String, Object> reads = pdFile.read();
        Map<Integer, Map<String, String>> moduleReads = (Map<Integer, Map<String, String>>) reads.get("modules");

        // Get the list of modules
        Vector<JobSubmission> jobs = pipeline.getTasks();
        ResponseJSON listObject = new ResponseJSON();
        Integer idCounter = 0;
        
        for (JobSubmission i : jobs) {
            ModuleJSON module = new ModuleJSON(idCounter, i);
            // If the position list has been populated from the .pipelineDesigner file
            if (idCounter < moduleReads.size()) {
                try {
                    if (moduleReads.get(idCounter).get("top") != null) {
                        module.setTop(moduleReads.get(idCounter).get("top"));
                    }
                    if (moduleReads.get(idCounter).get("left") != null) {
                        module.setLeft(moduleReads.get(idCounter).get("left"));
                    }
                }
                catch (JSONException e) {
                    log.error("ERROR: Attaching pipeline designer location data to module");
                }
            }
            listObject.addChild(idCounter, module);
            idCounter++;
        }
        
        return listObject;
    }
    
    public static File getPipelineDirectory(PipelineModel pipeline) {
        return getPipelineDirectory(pipeline.getName(), pipeline.getLsid(), pipeline.getUserID());
    }
    
    public static File getPipelineDirectory(String name, String lsid, String userid) {
        try {
            return new File(DirectoryManager.getTaskLibDir(name + "." + GPConstants.TASK_TYPE_PIPELINE, lsid, userid));
        }
        catch (MalformedURLException e) {
            log.error("ERROR: Unable to create appropriate path for pipeline directory: " + name + " " + lsid + " " + userid);
            return null;
        }
    }
    
    public class FileCollection {
        public File doc = null;
        public File license = null;
        public List<File> inputFiles = new ArrayList<File>();
        public List<String> urls = new ArrayList<String>();
        public Map<Object, Map<String, String>> positions = new HashMap<Object, Map<String, String>>(); 
        
        public List<File> getInternal() {
            List<File> toReturn = new ArrayList<File>();
            if (doc != null) {
                toReturn.add(doc);
            }
            if (license != null) {
                toReturn.add(license);
            }
            toReturn.addAll(inputFiles);
            return toReturn;
        }
    }
}

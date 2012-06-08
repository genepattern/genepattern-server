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
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.config.ServerConfiguration;
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

public class PipelineQueryServlet extends HttpServlet {
	private static final long serialVersionUID = 8270613493170496154L;
	public static Logger log = Logger.getLogger(PipelineQueryServlet.class);
	
	public static final String LIBRARY = "/library";
	public static final String SAVE = "/save";
	public static final String LOAD = "/load";
	public static final String UPLOAD = "/upload";
	
	public static final String PIPELINE_DESIGNER_FILE = ".pipelineDesigner";
	
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
	
	public void sendError(HttpServletResponse response, String message) {
	    ResponseJSON error = new ResponseJSON();
	    error.addError("ERROR: " + message);
	    this.write(response, error);
	}
	
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
                        File tempDir = ServerConfiguration.instance().getTempDir();
                        File userTempDir = new File(tempDir, username);
                        userTempDir.mkdir();
                        File uploadedFile = new File(userTempDir, i.getName());
                        
                        // Test to see if the file already exists
                        if (uploadedFile.exists()) {
                            throw new Exception("Uploaded file already exists");
                        }
                        
                        transferUpload(i, uploadedFile);
                        
                        // Return a success response
                        ResponseJSON message = new ResponseJSON();
                        message.addChild("location", uploadedFile.getCanonicalPath());
                        this.write(response, message);
                    }
                }
            }
            catch (Exception e) {
                sendError(response, "Exception retrieving the uploaded file");
            }
        }
        else {
            sendError(response, "Unable to find uploaded file");
        }
	}
	
	@SuppressWarnings("unchecked")
    public void loadPipeline(HttpServletRequest request, HttpServletResponse response) {
	    String lsid = request.getParameter("lsid");
	    
	    if (lsid == null) {
	        sendError(response, "No lsid received");
	        return;
	    }

	    TaskInfo info = TaskInfoCache.instance().getTask(lsid);
	    
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
        PipelineJSON pipelineObject = new PipelineJSON(pipeline, info);
        ResponseJSON modulesObject = createModuleList(pipeline);
        ResponseJSON pipesObject = PipeJSON.createPipeList(pipeline.getTasks());
        
        responseObject.addChild(PipelineJSON.KEY, pipelineObject);
        responseObject.addChild(ModuleJSON.KEY, modulesObject);
        responseObject.addChild(PipeJSON.KEY, pipesObject);
        
        this.write(response, responseObject);
	}
	
	private List<ModuleJSON> getUpstreamModules(Integer id, PipeJSON[] pipesObject, Map<Integer, ModuleJSON> moduleMap) throws JSONException {
	    List<ModuleJSON> inputs = new ArrayList<ModuleJSON>();
	    for (PipeJSON pipe : pipesObject) {
	        if (pipe.getInputModule().equals(id)) {
	            inputs.add(moduleMap.get(pipe.getOutputModule()));
	        }
	    }
	    
	    return inputs;
	}
	
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
	
	public void savePipeline(HttpServletRequest request, HttpServletResponse response) {
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
	    JSONObject pipelineJSON = PipelineJSON.parseBundle(bundle);
	    PipelineJSON pipelineObject = PipelineJSON.extract(pipelineJSON);
	    ModuleJSON[] modulesObject = ModuleJSON.extract(pipelineJSON);
	    PipeJSON[] pipesObject = PipeJSON.extract(pipelineJSON);
	    
	    // Transform the graph of modules and pipes to an ordered list
	    List<ModuleJSON> modulesList = null;
	    try {
            modulesList = transformGraph(modulesObject, pipesObject);
        }
        catch (Exception e) {
            log.error("Unable to transform the graph to a list", e);
            sendError(response, "Unable to save the pipeline");
            return;
        }
        
        // Build up the pipeline model to save
        PipelineModel model = null;
        PipelineCreationHelper controller = null;
        try {
            model = new PipelineModel();
            model.setUserID(username);

            setPipelineInfo(model, pipelineObject);

            controller = new PipelineCreationHelper(model);
            controller.generateLSID();

            setModuleInfo(model, modulesList);
            setPipesInfo(model, modulesList, pipesObject);
        }
        catch (Exception e) {
            log.error("Unable to build the pipeline model", e);
            sendError(response, "Unable to save the pipeline");
            return;
        }
        
        // Generate a task from the pipeline model and install it
        String newLsid = null;
        try {
            newLsid = controller.generateTask();
        }
        catch (TaskInstallationException e) {
            log.error("Unable to install the pipeline:" + e.getMessage(), e);
            sendError(response, "Unable to save the pipeline");
            return;
        }
        
        // Create the new pipeline directory in taskLib and move files
        try {
            if (pipelineObject.getLsid().length() > 0) {
                TaskInfo oldInfo = TaskInfoCache.instance().getTask(pipelineObject.getLsid());
                File newDir = this.copySupportFiles(oldInfo.getName(), pipelineObject.getName(), oldInfo.getLsid(), newLsid, username);
                this.copyNewFiles(pipelineObject.getFiles(), newDir);
                this.writePipelineDesignerFile(newDir, modulesList);
            }
            else {
                File newDir = new File(DirectoryManager.getTaskLibDir(pipelineObject.getName() + "." + GPConstants.TASK_TYPE_PIPELINE, newLsid, username));
                this.copyNewFiles(pipelineObject.getFiles(), newDir);
                this.writePipelineDesignerFile(newDir, modulesList);
            }
        }
        catch (Exception e) {
            log.error("Unable to retrieve the old taskInfo based on old lsid for: " + newLsid);
            sendError(response, "Unable to save uploaded files for the pipeline");
            return;
        }
        
        // Respond to the client
        ResponseJSON message = new ResponseJSON();
        message.addMessage("Pipeline Saved");
        message.addChild("lsid", newLsid);
        this.write(response, message);
	}
	
	private void writePipelineDesignerFile(File directory, List<ModuleJSON> modules) throws JSONException {
	    File pdFile = new File(directory, PIPELINE_DESIGNER_FILE);
	    try {
            PrintWriter writer = new PrintWriter(new FileWriter(pdFile));
            int counter = 0;
            
            for (ModuleJSON module : modules) {
                writer.println(counter + " " + module.getTop() + " " + module.getLeft());
                counter++;
            }
            
            writer.close();
        }
        catch (IOException e) {
            log.error("Unable to write to .pipelineDesigner");
        }
	}
	
	public void constructLibrary(HttpServletRequest request, HttpServletResponse response) {
	    String username = (String) request.getSession().getAttribute("userid");
	    
	    ResponseJSON listObject = new ResponseJSON();
	    Integer count = 0;
        for (TaskInfo info : TaskInfoCache.instance().getAllTasks()) {
            if ("public".equals(info.getTaskInfoAttributes().get("privacy")) || info.getUserId().equals(username)) {
                ModuleJSON mj = new ModuleJSON(info);
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
    
    public static ResponseJSON createModuleList(PipelineModel pipeline) {
        // Get the pipeline's directory of files
        File directory = PipelineQueryServlet.getPipelineDirectory(pipeline);
        
        // Read the pipeline designer file and populate list for insertion into json
        File pdFile = new File(directory, PIPELINE_DESIGNER_FILE);
        List<String[]> fileReads = new ArrayList<String[]>();
        if (pdFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(pdFile))));
                String line = null;
                Integer expected = 0;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split(" ");
                    if (parts[0].equals(expected.toString())) {
                        String[] toInsert = new String[2];
                        if (parts.length >= 2) {
                            toInsert[0] = parts[1];
                        }
                        else {
                            toInsert[0] = null;
                        }
                        if (parts.length >= 3) {
                            toInsert[1] = parts[2];
                        }
                        else {
                            toInsert[1] = null;
                        }
                        fileReads.add(toInsert);
                        expected++;
                    }
                }
            }
            catch (Exception e) {
                log.error("ERROR: Reading pipeline designer file on file load");
            }
        }

        // Get the list of modules
        Vector<JobSubmission> jobs = pipeline.getTasks();
        ResponseJSON listObject = new ResponseJSON();
        Integer idCounter = 0;
        
        for (JobSubmission i : jobs) {
            ModuleJSON module = new ModuleJSON(idCounter, i);
            // If the position list has been populated from the .pipelineDesigner file
            if (idCounter < fileReads.size()) {
                try {
                    if (fileReads.get(idCounter)[0] != null) {
                        module.setTop(fileReads.get(idCounter)[0]);
                    }
                    if (fileReads.get(idCounter)[1] != null) {
                        module.setLeft(fileReads.get(idCounter)[1]);
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
}

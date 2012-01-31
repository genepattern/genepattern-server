package org.genepattern.pipelines;

import static org.genepattern.util.GPConstants.SERIALIZED_MODEL;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.genepattern.TaskInstallationException;
import org.genepattern.server.webapp.PipelineCreationHelper;
import org.genepattern.server.webservice.server.local.IAdminClient;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.WebServiceException;
import org.json.JSONException;
import org.json.JSONObject;

public class PipelineQueryServlet extends HttpServlet {
	private static final long serialVersionUID = 8270613493170496154L;
	public static Logger log = Logger.getLogger(PipelineQueryServlet.class);
	
	public static final String LIBRARY = "/library";
	public static final String SAVE = "/save";
	public static final String LOAD = "/load";
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String action = request.getPathInfo();
		
		// Route to the appropriate action, returning an error if unknown
		if (LIBRARY.equals(action)) {
		    constructLibrary(response);
		}
		else if (SAVE.equals(action)) {
		    savePipeline(request, response);
		}
		else if (LOAD.equals(action)) {
            loadPipeline(request, response);
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
	
	public void loadPipeline(HttpServletRequest request, HttpServletResponse response) {
	    String lsid = request.getParameter("lsid");
	    
	    if (lsid == null) {
	        sendError(response, "No lsid received");
	        return;
	    }
	    
	    TaskInfo info = TaskInfoCache.instance().getTask(lsid);
	    PipelineModel pipeline = null;
	    try {
            pipeline = PipelineModel.toPipelineModel((String) info.getTaskInfoAttributes().get(SERIALIZED_MODEL));
            pipeline.setLsid(info.getLsid());
        }
        catch (Exception e) {
            sendError(response, "Exception loading pipeline");
            return;
        }

        ResponseJSON responseObject = new ResponseJSON();
        PipelineJSON pipelineObject = new PipelineJSON(pipeline, info);
        ResponseJSON modulesObject = ModuleJSON.createModuleList(pipeline.getTasks());
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
	                continue;
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
	    model.setVersion(pipelineObject.getVersion().toString());
	    model.setPrivacy(pipelineObject.getPrivacy() == GPConstants.PRIVATE);
	    
	    String lsid = blankLsidIfNecessary(pipelineObject.getLsid());
	    model.setLsid(lsid);
	}
	
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
	
	private ParameterInfo setParameter(String pName, String value, ParameterInfo[] taskParams, boolean promptWhenRun) throws Exception {
        for (int i = 0; i < taskParams.length; i++) {
            ParameterInfo param = taskParams[i];
            if (pName.equalsIgnoreCase(param.getName())){
                param.setValue(value);
                param.getAttributes().put("altName", param.getName());
                param.getAttributes().put("altDescription", param.getDescription());
                if (promptWhenRun) {
                    param.getAttributes().put(PipelineModel.RUNTIME_PARAM, "1");
                }
                return param;
            }
        }
        throw new Exception("Parameter " + pName + " was not found");
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
	            boolean pwr = input.getPromptWhenRun();
	            promptWhenRun[i] = pwr;
	            if (pwr) {
	                ParameterInfo pInfo = setParameter(input.getName(), "", moduleParams, true);
	                model.addInputParameter(taskInfo.getName() + taskNum + "." + input.getName(), pInfo);
	            }
	            else {
	                setParameter(input.getName(), input.getValue(), moduleParams, false);
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
	    
	    String bundle = (String) request.getParameter("bundle");
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
        try {
            model = new PipelineModel();
            model.setUserID(username);
            setPipelineInfo(model, pipelineObject);
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
            PipelineCreationHelper controller = new PipelineCreationHelper(model);
            controller.generateLSID();
            newLsid = controller.generateTask();
            System.out.println("RESULT:::::::::::::::::::: " + newLsid);
        }
        catch (TaskInstallationException e) {
            log.error("Unable to install the pipeline:" + e.getMessage(), e);
            sendError(response, "Unable to save the pipeline");
            return;
        }
        
        // Respond to the client
        ResponseJSON message = new ResponseJSON();
        message.addMessage("Pipeline Saved");
        message.addChild("lsid", newLsid);
        this.write(response, message);
	}
	
	public void constructLibrary(HttpServletResponse response) {
	    ResponseJSON listObject = new ResponseJSON();
	    Integer count = 0;
        for (TaskInfo info : TaskInfoCache.instance().getAllTasks()) {
            ModuleJSON mj = new ModuleJSON(info);
            listObject.addChild(count, mj);
            count++;
        }
        
        this.write(response, listObject);
	}
}

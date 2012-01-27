package org.genepattern.pipelines;

import static org.genepattern.util.GPConstants.SERIALIZED_MODEL;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

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
        }
        catch (Exception e) {
            sendError(response, "Exception loading pipeline");
            return;
        }

        ResponseJSON responseObject = new ResponseJSON();
        PipelineJSON pipelineObject = new PipelineJSON(pipeline);
        ResponseJSON modulesObject = ModuleJSON.createModuleList(pipeline.getTasks());
        
        responseObject.addChild(PipelineJSON.KEY, pipelineObject);
        responseObject.addChild(ModuleJSON.KEY, modulesObject);
        
        this.write(response, responseObject);
	}
	
	// TODO: Implement
	public void savePipeline(HttpServletRequest request, HttpServletResponse response) {
	    Map x = request.getParameterMap();
	    System.out.println(x);
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

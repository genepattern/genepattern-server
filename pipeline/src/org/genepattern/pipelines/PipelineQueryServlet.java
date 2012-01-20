package org.genepattern.pipelines;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;

public class PipelineQueryServlet extends HttpServlet {
	private static final long serialVersionUID = 8270613493170496154L;
	public static Logger log = Logger.getLogger(PipelineQueryServlet.class);
	
	public static final String LIBRARY = "/library";
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) {
		String action = request.getPathInfo();
		
		// Route to the appropriate action, returning an error if unknown
		if (LIBRARY.equals(action)) {
		    constructLibrary(response);
		}
		else {
		    sendError(response, action);
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
	
	public void sendError(HttpServletResponse response, String action) {
	    ListJSON error = new ListJSON();
	    error.addError("Error routing in servlet for: " + action);
	    this.write(response, error);
	}
	
	public void constructLibrary(HttpServletResponse response) {
	    ListJSON listObject = new ListJSON();
        for (TaskInfo info : TaskInfoCache.instance().getAllTasks()) {
            ModuleJSON mj = new ModuleJSON(info);
            listObject.addChild(mj);
        }
        
        this.write(response, listObject);
	}
}

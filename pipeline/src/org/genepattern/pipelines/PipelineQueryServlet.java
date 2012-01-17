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
		    constructLibrary();
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
	
	public void sendError(HttpServletResponse response, String action) {
	    // TODO: Implement returning a JSON error to the client
	    PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.println("Error routing in servlet for: " + action);
            writer.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (writer != null) writer.close();
        }
	}
	
	public void constructLibrary() {
	    // TODO: Construct and return an array of ModuleJSONs
	    for (TaskInfo info : TaskInfoCache.instance().getAllTasks()) {
            ModuleJSON mj = new ModuleJSON(info);
            log.info(mj.toString());
        }
	}
}

package org.genepattern.modules;

import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.TaskInfo;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.Collections;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * based on PipelineQueryServer class in the org.genepattern.pipelines class
 */
public class ModuleQueryServlet extends HttpServlet 
{
    public static Logger log = Logger.getLogger(ModuleQueryServlet.class);

    public static final String MODULE_CATEGORIES = "/categories";

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    {
		String action = request.getPathInfo();

		// Route to the appropriate action, returning an error if unknown
		if (MODULE_CATEGORIES.equals(action)) 
        {
            getModuleCategories(response);
        }
        else
        {
		    sendError(response, "Routing error for " + action);
		}
    }

    @Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
    {
		doGet(request, response);
	}

	@Override
	public void doPut(HttpServletRequest request, HttpServletResponse response)
    {
	    doGet(request, response);
	}

    private void write(HttpServletResponse response, Object content)
    {
        this.write(response, content.toString());
    }

    private void write(HttpServletResponse response, String content)
    {
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

    public void sendError(HttpServletResponse response, String message)
    {
	    ResponseJSON error = new ResponseJSON();
	    error.addError("ERROR: " + message);
	    this.write(response, error);
	}

    public SortedSet<String> getAllCategories() {
        SortedSet<String> categories = new TreeSet<String>(new Comparator<String>() {
            // sort categories alphabetically, ignoring case
            public int compare(String arg0, String arg1) {
                String arg0tl = arg0.toLowerCase();
                String arg1tl = arg1.toLowerCase();
                int rval = arg0tl.compareTo(arg1tl);
                if (rval == 0) {
                    rval = arg0.compareTo(arg1);
                }
                return rval;
            }
        });
        
        for (TaskInfo ti : TaskInfoCache.instance().getAllTasks()) {
            String taskType = ti.getTaskInfoAttributes().get("taskType");
            if (taskType == null || taskType.trim().length() == 0) {
                //ignore null and blank
            }
            else {
                categories.add(taskType);
            }
        }
        return Collections.unmodifiableSortedSet(categories);
    }
    
    public void getModuleCategories(HttpServletResponse response)
    {
        SortedSet<String> categories = null;
        try
        {
            categories = getAllCategories();
        }
        catch (Throwable t)
        {
            log.error("Error listing categories from TaskInfoCache: "+t.getLocalizedMessage());
        }

        ResponseJSON message = new ResponseJSON();
        message.addChild("categories", categories.toString());
        this.write(response, message);
    }
}

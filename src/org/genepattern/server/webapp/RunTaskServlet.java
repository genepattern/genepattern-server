package org.genepattern.server.webapp;

import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.server.config.ServerConfiguration;
import org.genepattern.server.TaskLSIDNotFoundException;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoCache;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.modules.ResponseJSON;
import org.genepattern.modules.ModuleJSON;
import org.genepattern.modules.ParametersJSON;
import org.json.JSONArray;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Comparator;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Jan 10, 2013
 * Time: 9:41:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class RunTaskServlet extends HttpServlet 
{
    public static Logger log = Logger.getLogger(RunTaskServlet.class);
     public static final String UPLOAD = "/upload";
    public static final String RUN = "/run";
    public static final String LOAD = "/load";

    private String lsid = null;

    public void doGet(HttpServletRequest request, HttpServletResponse response)
    {
        String action = request.getPathInfo();

        // Route to the appropriate action, returning an error if unknown
        if (LOAD.equals(action))
        {
            loadModule(request, response);
        }
        else if (UPLOAD.equals(action))
        {
            //uploadFile(request, response);
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

    public void loadModule(HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            String username = (String) request.getSession().getAttribute("userid");

            if (username == null)
            {
                response.sendRedirect("/gp/pages/notFound.jsf");
                return;
            }

            String lsid = request.getParameter("lsid");

            if (lsid == null) {
                sendError(response, "No lsid received");
                return;
            }

            TaskInfo taskInfo = getTaskInfo(lsid);

            ResponseJSON responseObject = new ResponseJSON();

            ModuleJSON moduleObject = new ModuleJSON(taskInfo, null);
            moduleObject.put("lsidVersions", new JSONArray(getModuleVersions(lsid)));

            responseObject.addChild(ModuleJSON.KEY, moduleObject);

            JSONArray parametersObject = getParameterList(taskInfo.getParameterInfoArray());
            responseObject.addChild(ParametersJSON.KEY, parametersObject);
            this.write(response, responseObject);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            log.error(e);

            String message = "";
            if(e.getMessage() != null)
            {
                message = e.getMessage();
            }
            sendError(response, "Error: while loading the module with lsid: " + lsid + " " + message);
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

    private ArrayList getModuleVersions(String taskLSID) throws Exception
    {
        String taskNoLSIDVersion = new LSID(taskLSID).toStringNoVersion();

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

    private TaskInfo getTaskInfo(String taskLSID) throws Exception
    {
        TaskInfo taskInfo = null;
        try
        {
            taskInfo = TaskInfoCache.instance().getTask(taskLSID);
            return taskInfo;
        }
        catch(TaskLSIDNotFoundException e)
        {
            // do nothing check with lsid
        }

        String taskNoLSIDVersion = new LSID(taskLSID).toStringNoVersion();
        SortedSet<String> moduleVersions = new TreeSet<String>(new Comparator<String>() {
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

        if(moduleVersions.size() > 0)
        {
            taskInfo = TaskInfoCache.instance().getTask(moduleVersions.first());
        }

        return taskInfo;
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
            log.error("Error writing to the response in ModuleQueryServlet: " + content);
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
}

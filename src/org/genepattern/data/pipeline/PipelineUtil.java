package org.genepattern.data.pipeline;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;

/**
 * Utility methods for pipeline validation and execution.
 * Moved some methods out of RunPipelineForJsp because we no longer use RunPipelineForJsp.
 * 
 * @author pcarr
 */
public class PipelineUtil {
    /**
     * Collect the command line params from the request and see if they are all present.
     *
     * @param taskInfo, the task info object
     * @param commandLineParams, maps parameter name to value
     */
    public static boolean validateAllRequiredParametersPresent(TaskInfo taskInfo, HashMap commandLineParams) {
        ParameterInfo[] parameterInfoArray = taskInfo.getParameterInfoArray();
        if (parameterInfoArray != null && parameterInfoArray.length > 0) {
            for (int i = 0; i < parameterInfoArray.length; i++) {
                ParameterInfo param = parameterInfoArray[i];
                String key = param.getName();
                Object value = commandLineParams.get(key);
                if (!isOptional(param)) {
                    if (value == null) {
                        return true;
                    } else if (value instanceof String) {
                        String s = (String) value;
                        if ("".equals(s.trim())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    public static boolean isOptional(final ParameterInfo info) {
        final Object optional = info.getAttributes().get("optional");
        return (optional != null && "on".equalsIgnoreCase(optional.toString()));
    }
    
    /**
     * Checks the given pipeline to see if all tasks that it uses are installed
     * on the server. Writes an error message to the given
     * <code>PrintWriter</code> if there are missing tasks.
     *
     * @param model  the pipeline model
     * @param out    the writer
     * @param userID the user id
     * @return <code>true</code> if the pipeline is missing tasks
     */
    public static boolean isMissingTasks(PipelineModel model, java.io.PrintWriter out, String userID) throws Exception {
        boolean isMissingTasks = false;
        List tasks = model.getTasks();
        HashMap<String,LSID> unknownTaskNames = new HashMap<String,LSID>();
        HashMap<String,LSID> unknownTaskVersions = new HashMap<String,LSID>();
        for (int ii = 0; ii < tasks.size(); ii++) {
            JobSubmission js = (JobSubmission) tasks.get(ii);
            TaskInfo formalTask = GenePatternAnalysisTask.getTaskInfo(js.getName(), userID);
            boolean unknownTask = !GenePatternAnalysisTask.taskExists(js.getLSID(), userID);
            boolean unknownTaskVersion = false;
            if (unknownTask) {
                isMissingTasks = true;
                // check for alternate version
                String taskLSIDstr = js.getLSID();
                LSID taskLSID = new LSID(taskLSIDstr);
                String taskLSIDstrNoVer = taskLSID.toStringNoVersion();
                unknownTaskVersion = GenePatternAnalysisTask.taskExists(taskLSIDstrNoVer, userID);
                if (unknownTaskVersion) {
                    unknownTaskVersions.put(js.getName(), taskLSID);
                } else {
                    unknownTaskNames.put(js.getName(), taskLSID);
                }
            }
        }
        if (((unknownTaskNames.size() + unknownTaskVersions.size()) > 0) && (out != null)) {
            out
                    .println(
                    "<font color='red' size=\"+1\"><b>Warning:</b></font><br>The following module versions do not exist on this server. Before running this pipeline you will need to edit the pipeline to use the available module version or install the required modules.");
            out.println("<table width='100%'  border='1'>");
            out
                    .println(
                    "<tr class=\"paleBackground\" ><td> Name </td><td> Required Version</td><td> Available Version</td><td>LSID</td></tr>");
        }
        if (((unknownTaskNames.size() + unknownTaskVersions.size()) > 0) && (out != null)) {
            out.println("<form method=\"post\" action=\"pages/taskCatalog.jsf\">");
        }
        if (unknownTaskNames.size() > 0) {
            for (String name : unknownTaskNames.keySet()) {
                LSID absentlsid = (LSID) unknownTaskNames.get(name);
                out.println("<input type=\"hidden\" name=\"lsid\" value=\"" + absentlsid + "\" /> ");
                out.println("<tr><td>" + name + "</td><td>" + absentlsid.getVersion() + "</td><td></td><td> " +
                        absentlsid.toStringNoVersion() + "</td></tr>");
            }
            
        }
        if (unknownTaskVersions.size() > 0) {
            for (Iterator iter = unknownTaskVersions.keySet().iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                LSID absentlsid = (LSID) unknownTaskVersions.get(name);
                out.println("<input type=\"hidden\" name=\"lsid\" value=\"" + absentlsid + "\" /> ");
                TaskInfo altVersionInfo = GenePatternAnalysisTask.getTaskInfo(absentlsid.toStringNoVersion(), userID);
                Map altVersionTia = altVersionInfo.getTaskInfoAttributes();
                LSID altVersionLSID = new LSID((String) (altVersionTia
                        .get(GPConstants.LSID)));
                out.println("<tr><td>" + name + "</td><td> " + absentlsid.getVersion() + "</td><td>" +
                        altVersionLSID.getVersion() + "</td><td>" + absentlsid.toStringNoVersion() + "</td></tr>");
            }
        }
        
        if ((unknownTaskNames.size() + unknownTaskVersions.size()) > 0) {
            out.println("<tr class=\"paleBackground\" >");
            out.println(
                    "<td colspan='4' align='center' border = 'none'> <a href='pages/importTask.jsf'>Install from zip file </a>");
            out.println(
                    " &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ");
            out.println("<input type=\"hidden\" name=\"checkAll\" value=\"1\"  >");
            out.println("<input type=\"submit\" value=\"Install from repository\"  ></td></form>");
            out.println("</tr>");
            out.println("</table>");
        }
        return isMissingTasks;
    }

    public static boolean isMissingTasks(PipelineModel model, String userID) {
        List tasks = model.getTasks();
        try {
            for (int ii = 0; ii < tasks.size(); ii++) {
                JobSubmission js = (JobSubmission) tasks.get(ii);
                boolean unknownTask = !GenePatternAnalysisTask.taskExists(js
                        .getLSID(), userID);
                if (unknownTask) {
                    return true;
                }
            }
        } catch (OmnigeneException e) {
            return true; 
            // be defensive about running if there is an exception
        }
        return false;
    }
}

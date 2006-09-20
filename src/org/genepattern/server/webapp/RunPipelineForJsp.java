/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp;

import org.apache.commons.fileupload.FileItem;
import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.genepattern.LSIDManager;
import org.genepattern.server.webservice.server.DirectoryManager;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.JobStatus;
import org.genepattern.webservice.OmnigeneException;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;

import java.io.BufferedWriter;
import java.io.File;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RunPipelineForJsp {
    public static int jobID = -1;

    public RunPipelineForJsp() {
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
        java.util.List tasks = model.getTasks();
        HashMap unknownTaskNames = new HashMap();
        HashMap unknownTaskVersions = new HashMap();
        for (int ii = 0; ii < tasks.size(); ii++) {
            JobSubmission js = (JobSubmission) tasks.get(ii);
            TaskInfo formalTask = GenePatternAnalysisTask.getTaskInfo(js
                    .getName(), userID);
            boolean unknownTask = !GenePatternAnalysisTask.taskExists(js
                    .getLSID(), userID);
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
                            "<font color='red' size=\"+1\"><b>Warning:</b></font><br>The following task versions do not exist on this server. Before running this pipeline you will need to edit the pipeline to use the available version or import them.");
            out.println("<table width='100%'  border='1'>");
            out
                    .println(
                            "<tr class=\"paleBackground\" ><td> Name </td><td> Required Version</td><td> Available Version</td><td>LSID</td></tr>");
        }
        if (((unknownTaskNames.size() + unknownTaskVersions.size()) > 0) && (out != null)) {
            out.println("<form method=\"post\" action=\"taskCatalog.jsp\">");
        }
        if (unknownTaskNames.size() > 0) {
            for (Iterator iter = unknownTaskNames.keySet().iterator(); iter
                    .hasNext();) {
                String name = (String) iter.next();
                LSID absentlsid = (LSID) unknownTaskNames.get(name);
                out.println("<input type=\"hidden\" name=\"LSID\" value=\"" + absentlsid + "\" /> ");
                out.println("<tr><td>" + name + "</td><td>" + absentlsid.getVersion() + "</td><td></td><td> " +
                        absentlsid.toStringNoVersion() + "</td></tr>");
            }
        }
        if (unknownTaskVersions.size() > 0) {
            for (Iterator iter = unknownTaskVersions.keySet().iterator(); iter
                    .hasNext();) {
                String name = (String) iter.next();
                LSID absentlsid = (LSID) unknownTaskVersions.get(name);
                out.println("<input type=\"hidden\" name=\"LSID\" value=\"" + absentlsid + "\" /> ");
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
            out
                    .println(
                            "<td colspan='4' align='center' border = 'none'> <a href='addZip.jsp'>Import zip file </a>");
            out
                    .println(
                            " &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; ");
            out
                    .println("<input type=\"hidden\" name=\"checkAll\" value=\"1\"  >");
            out
                    .println("<input type=\"submit\" value=\"install/update from catalog\"  ></td></form>");
            out.println("</tr>");
            out.println("</table>");
        }
        return isMissingTasks;
    }

    public static boolean isMissingTasks(PipelineModel model, String userID) {
        java.util.List tasks = model.getTasks();
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
            return true; // be defensive about running if there is an
            // exception
        }
        return false;
    }

    public static boolean deletePipelineDirAfterRun(String pipelineName) {
        boolean deleteDirAfterRun = false;
        // determine if we want to delete the dir in tasklib after running
        // we do this if there is no pipeline saved by the same name.
        try {
            TaskInfo savedTaskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, null);
        } catch (Exception e) {
            // exception means it isn't in the DB therefore not saved
            deleteDirAfterRun = true;
        }
        return deleteDirAfterRun;
    }

    public static boolean isSavedModel(TaskInfo taskInfo, String pipelineName, String userID) {
        boolean savedPipeline = true;
        // determine if we want a link to the pipeline. Check if one existis in
        // the DB
        // by the same name and that the model matches what we have here
        try {
            TaskInfo savedTaskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, userID);
            Map sTia = savedTaskInfo.getTaskInfoAttributes();
            String savedSerializedModel = (String) sTia
                    .get(GPConstants.SERIALIZED_MODEL);
            Map tia = taskInfo.getTaskInfoAttributes();
            String serializedModel = (String) tia
                    .get(GPConstants.SERIALIZED_MODEL);
            if (!savedSerializedModel.equals(serializedModel)) {
                savedPipeline = false;
            }
        } catch (Exception e) {
            // exception means it isn't in the DB therefore not saved
            savedPipeline = false;
        }
        return savedPipeline;
    }

    public static void stopPipeline(String jobID) throws Exception {
        Process p = null;
        for (int i = 0; i < 10; i++) {
            p = GenePatternAnalysisTask.terminatePipeline(jobID);
            if (p != null) {
                break;
            }
            Thread.currentThread().sleep(1000);
        }
        GenePatternAnalysisTask.updatePipelineStatus(Integer.parseInt(jobID), JobStatus.JOB_ERROR, null);
        if (p != null) {
            p.destroy();
        }
        return;
    }

    public static String[] generatePipelineCommandLine(String name, String jobID, String userID, String baseURL,
                                                       TaskInfo taskInfo, HashMap commandLineParams, File tempDir,
                                                       String decorator) throws Exception {
        String JAVA_HOME = System.getProperty("java.home");
        boolean savedPipeline = isSavedModel(taskInfo, name, userID);
        // these jar files are required to execute
        // gp-full.jar;
        // log4j-1.2.4.jar; xerces.jar;
        // activation.jar; saaj.jar
        // axis.jar; jaxrpc.jar;
        // commons-logging.jar; commons-discovery.jar;
        String tomcatLibDir = System.getProperty("tomcatCommonLib") + "/";
        String webappLibDir = System.getProperty("webappDir") + "/" + "WEB-INF" + "/" + "lib" + "/";
        String resourcesDir = null;
        resourcesDir = new File(System.getProperty("resources"))
                .getAbsolutePath() + "/";
        ArrayList cmdLine = new ArrayList();
        cmdLine.add(JAVA_HOME + File.separator + "bin" + File.separator + "java");
        cmdLine.add("-cp");
        StringBuffer classPath = new StringBuffer();
        classPath.append(tomcatLibDir + "activation.jar" + File.pathSeparator);
        classPath.append(tomcatLibDir + "xerces.jar" + File.pathSeparator);
        classPath.append(tomcatLibDir + "saaj.jar" + File.pathSeparator);
        classPath.append(tomcatLibDir + "jaxrpc.jar" + File.pathSeparator);
        String[] jars = new File(webappLibDir).list();
        for (int i = 0; i < jars.length; i++) {
            classPath.append(webappLibDir + jars[i] + File.pathSeparator);
        }
        cmdLine.add(classPath.toString());
        cmdLine.add("-Ddecorator=" + decorator);
        cmdLine.add("-DjobID=" + jobID);
        cmdLine.add("-Djobs=" + System.getProperty("jobs"));
        cmdLine.add("-DsavedPipeline=" + savedPipeline);
        cmdLine.add("-Domnigene.conf=" + resourcesDir);
        cmdLine.add("-Dgenepattern.properties=" + resourcesDir);
        cmdLine.add("-DGenePatternURL=" + System.getProperty("GenePatternURL"));
        cmdLine.add("-D" + GPConstants.LSID + "=" + (String) taskInfo.getTaskInfoAttributes().get(GPConstants.LSID));
        cmdLine.add("org.genepattern.server.webapp.RunPipeline");

        // -------------------------------------------------------------
        // ------Serialize the pipeline model for the java executor-----
        Map tia = taskInfo.getTaskInfoAttributes();
        String pipelineShortName = name;
        if (pipelineShortName != null) {
            int i = pipelineShortName.indexOf(" ");
            if (i != -1) {
                pipelineShortName = pipelineShortName.substring(0, i);
            }
        }
        String serializedModel = (String) tia.get(GPConstants.SERIALIZED_MODEL);
        File pipeFile = new File(tempDir, pipelineShortName + ".xml");
        BufferedWriter writer = new BufferedWriter(new java.io.FileWriter(pipeFile));
        writer.write(serializedModel);
        writer.flush();
        writer.close();
        // -------------------------------------------------------------
        cmdLine.add(pipeFile.getName());
        cmdLine.add(userID);

        // add any command line parameters
        // first saving the files locally
        for (Iterator iter = commandLineParams.keySet().iterator(); iter
                .hasNext();) {
            String key = (String) iter.next();
            Object obj = commandLineParams.get(key);
            String val = "";
            if (obj instanceof FileItem) {
                FileItem fi = (FileItem) obj;
                java.io.File file = new java.io.File(tempDir, fi
                        .getName());
                fi.write(file);
                try {
                    val = baseURL + "getFile.jsp?task=&file=" +
                            URLEncoder.encode(tempDir.getName() + "/" + file.getName(), "UTF-8");
                } catch (UnsupportedEncodingException uee) {
                }
            } else {
                val = obj.toString();
            }
            cmdLine.add(key + "=" + val);
        }
        return (String[]) cmdLine.toArray(new String[0]);
    }

    public static boolean isOptional(final ParameterInfo info) {
        final Object optional = info.getAttributes().get("optional");
        return (optional != null && "on".equalsIgnoreCase(optional.toString()));
    }

    /**
     * Collect the command line params from the request and see if they are all
     * present
     *
     * @param taskInfo          the task info object
     * @param commandLineParams maps parameter name to value
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

    public static int getJobID() {
        return jobID;
    }

    public static Process runPipeline(TaskInfo taskInfo, String name, String baseURL, String decorator, String userID,
                                      HashMap commandLineParams) throws Exception {
        Map tia = taskInfo.getTaskInfoAttributes();
        String lsid = (String) tia.get(GPConstants.LSID);
        JobInfo jobInfo = GenePatternAnalysisTask.createPipelineJob(userID, "", taskInfo.getName(), lsid);
        jobID = jobInfo.getJobNumber();
        String pipelineShortName = taskInfo.getName();
        if (pipelineShortName != null) {
            int i = pipelineShortName.indexOf(" ");
            if (i != -1) {
                pipelineShortName = pipelineShortName.substring(0, i);
            }
        }
        java.io.File tempDir = java.io.File
                .createTempFile("pipe", null, new java.io.File(System.getProperty("jobs")));
        tempDir.delete();
        tempDir.mkdirs();
        if (decorator == null) {
            decorator = "org.genepattern.server.webapp.RunPipelineLoggingHTMLDecorator";
        }
        boolean deleteDirAfterRun = RunPipelineForJsp
                .deletePipelineDirAfterRun(taskInfo.getName());
        String[] commandLine = RunPipelineForJsp.generatePipelineCommandLine(taskInfo.getName(), "" + jobID, userID,
                baseURL, taskInfo, commandLineParams, tempDir, decorator);

        // spawn the command
        final Process process = Runtime.getRuntime().exec(commandLine, null, tempDir);
        GenePatternAnalysisTask.startPipeline(Integer.toString(jobID), process);
        WaitForPipelineCompletionThread waiter = new WaitForPipelineCompletionThread(process, jobID);
        waiter.start();
        if (deleteDirAfterRun) {
            DeleteUnsavedTasklibDirThread delThread = new DeleteUnsavedTasklibDirThread(taskInfo, process);
            delThread.start();
        }
        return process;
    }

    public static void writePipelineBody(PrintWriter outstr, String pipelineName, PipelineModel model, String userID,
                                         boolean showParams, boolean showLSID, boolean hideButtons) {
        try {
            String paramDisplayStyle = "none";
            String lsidDisplayStyle = "none";
            if (showParams) {
                paramDisplayStyle = "block";
            }
            if (showLSID) {
                lsidDisplayStyle = "block";
            }
            if (pipelineName == null) {
                outstr.println("	Must specify a name parameter");
                return;
            }
            TaskInfo task =
                    new org.genepattern.server.webservice.server.local.LocalAdminClient(userID).getTask(pipelineName);
            if ((task != null) && (model == null)) {
                TaskInfoAttributes tia = task.giveTaskInfoAttributes();
                if (tia != null) {
                    String serializedModel = (String) tia
                            .get(GenePatternAnalysisTask.SERIALIZED_MODEL);
                    if (serializedModel != null && serializedModel.length() > 0) {
                        try {
                            model = PipelineModel
                                    .toPipelineModel(serializedModel);
                        } catch (Throwable x) {
                            x.printStackTrace(System.out);
                        }
                    }
                }
            }
            outstr.println("<script language=\"JavaScript\">");
            outstr.println("var numTasks = " + model.getTasks().size());
            outstr.println("function toggle() {");
            outstr.println("	for(var i = 0; i < numTasks; i++) {");
            outstr.println("		formobj = document.getElementById('id' + i);");
            outstr.println("		var visible = document.form1.togglecb.checked;");
            outstr.println("		if(!visible) {");
            outstr.println("			formobj.style.display = \"none\";");
            outstr.println("		} else {");
            outstr.println("			formobj.style.display = \"block\";");
            outstr.println("		}");
            outstr.println("	}");
            outstr.println("}");
            outstr.println("function toggleLSID() {");
            outstr.println("	for(var i = 0; i < numTasks; i++) {");
            outstr.println("		formobj = document.getElementById('lsid' + i);");
            outstr
                    .println("		var visible = document.form1.togglelsid.checked;");
            outstr.println("		if(!visible) {");
            outstr.println("			formobj.style.display = \"none\";");
            outstr.println("		} else {");
            outstr.println("			formobj.style.display = \"block\";");
            outstr.println("		}");
            outstr.println("	}");
            outstr.println("}");
            outstr.println("function cloneTask(origName, lsid, user) {");
            outstr.println("	while (true) {");
            outstr.println("		suggestedName = \"copyOf\" + origName;");
            outstr
                    .println("		var cloneName = window.prompt(\"Name for cloned pipeline\", suggestedName);");
            outstr
                    .println("		if (cloneName == null || cloneName.length == 0) {");
            outstr.println("			return;");
            outstr.println("		}");
            outstr.println("		if(cloneName.lastIndexOf(\".pipeline\")==-1) {");
            outstr.println("			cloneName = cloneName + \".pipeline\";");
            outstr.println("		}");
            outstr
                    .println(
                            "		window.location = \"saveTask.jsp?clone=1&name=\"+origName+\"&LSID=\" + lsid + \"&cloneName=\" + cloneName + \"&userid=\" + user + \"&pipeline=1\";");
            outstr.println("		break;");
            outstr.println("	}");
            outstr.println("}");
            outstr.println("function runpipeline( url) {");
            outstr.println("		window.location= url;");
            outstr.println("}");
            outstr.println("</script>");
            outstr
                    .println("<link href=\"skin/stylesheet.css\" rel=\"stylesheet\" type=\"text/css\">");
            outstr
                    .println("<link rel=\"SHORTCUT ICON\" href=\"favicon.ico\" >");
            outstr.println("<title>" + model.getName() + "</title></head><body>");
            String displayName = model.getName();
            if (displayName.endsWith(".pipeline")) {
                displayName = displayName.substring(0, displayName.length() - ".pipeline".length());
            }
            outstr.println("<p><font size='+2'><b>" + displayName + "</b></font>");

            // show edit link when task has local authority and either belongs
            // to current user or is public
            String lsid = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
            boolean showEdit = false;
            try {
                LSIDManager manager = LSIDManager.getInstance();
                String authority = manager
                        .getAuthorityType(new org.genepattern.util.LSID(lsid));
                if (authority.equals(LSIDUtil.AUTHORITY_MINE)) {
                    showEdit = task.getTaskInfoAttributes().get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) ||
                            task.getTaskInfoAttributes().get(GPConstants.USERID).equals(userID);
                }
            } catch (Exception e) {
                e.printStackTrace(System.out);
            }
            if (showEdit && (!hideButtons)) {
                String editURL = "pipelineDesigner.jsp?name=" + pipelineName;
                outstr
                        .println(
                                "  <input type=\"button\" value=\"edit\" name=\"edit\" class=\"little\" onclick=\"window.location='" +
                                        editURL + "'\"; />");
            }
            if (!hideButtons) {
                outstr
                        .println(
                                "  <input type=\"button\" value=\"clone...\" name=\"clone\"       class=\"little\" onclick=\"cloneTask('" +
                                        displayName + "', '" + pipelineName + "', '" + userID + "')\"; />");
                if (!RunPipelineForJsp.isMissingTasks(model, userID)) {
                    outstr
                            .println(
                                    "  <input type=\"button\" value=\"run\"      name=\"runpipeline\" class=\"little\" onclick=\"runpipeline('runPipeline.jsp?cmd=run&name=" +
                                            pipelineName + "')\"; />");
                }
            }
            outstr
                    .println("&nbsp;&nbsp;<form name=\"form1\"><input name=\"togglecb\" type=\"checkbox\" ");
            if (showParams) {
                outstr.println("checked");
            }
            outstr.println(" onClick=toggle();>Show Input Parameters</input>");
            outstr.println("<input name=\"togglelsid\" type=\"checkbox\" ");
            if (showLSID) {
                outstr.println("checked");
            }
            outstr.println("onClick=toggleLSID();>Show LSIDs</input></form>");
            try {
                RunPipelineForJsp.isMissingTasks(model, userID);
            } catch (Exception e) {
                outstr
                        .println("An error occurred while processing your request. Please try again.");
                return;
            }
            List tasks = model.getTasks();
            for (int i = 0; i < tasks.size(); i++) {
                JobSubmission js = (JobSubmission) tasks.get(i);
                ParameterInfo[] parameterInfo = js.giveParameterInfoArray();
                int displayNumber = i + 1;
                TaskInfo formalTask = GenePatternAnalysisTask.getTaskInfo(js
                        .getName(), userID);
                TaskInfo ti = GenePatternAnalysisTask.getTaskInfo(js.getLSID(), userID);
                boolean unknownTask = !GenePatternAnalysisTask.taskExists(js
                        .getLSID(), userID);
                boolean unknownTaskVersion = false;
                if (unknownTask) {
                    // check for alternate version
                    String taskLSIDstr = js.getLSID();
                    LSID taskLSID = new LSID(taskLSIDstr);
                    String taskLSIDstrNoVer = taskLSID.toStringNoVersion();
                    unknownTaskVersion = !GenePatternAnalysisTask.taskExists(taskLSIDstrNoVer, userID);
                }
                outstr.print("<p><font size=\"+1\"><a name=\"" + displayNumber + "\"/> " + displayNumber + ". ");
                Map tia = formalTask != null ? formalTask
                        .getTaskInfoAttributes() : null;
                ParameterInfo[] formalParams = formalTask != null ? formalTask
                        .getParameterInfoArray() : null;
                if (formalParams == null) {
                    formalParams = new ParameterInfo[0];
                }
                if (formalTask == null) {
                    outstr.print("<font color='red'>" + js.getName() + "</font></font> is not present on this server.");
                    tia = new HashMap();
                    formalParams = new ParameterInfo[0];
                } else if (!unknownTask) {
                    outstr.print("<a href=\"addTask.jsp?view=1&name=" + js.getLSID() + "\">" + js.getName() +
                            "</a></font> " + StringUtils.htmlEncode(formalTask
                            .getDescription()));
                } else {
                    if (!unknownTaskVersion) {
                        LSID taskLSID = new LSID(js.getLSID());
                        TaskInfo altVersionInfo = GenePatternAnalysisTask
                                .getTaskInfo(taskLSID.toStringNoVersion(), userID);
                        Map altVersionTia = altVersionInfo
                                .getTaskInfoAttributes();
                        LSID altVersionLSID = new LSID((String) (altVersionTia
                                .get(GPConstants.LSID)));
                        outstr
                                .print("<font color='red'>" + js.getName() + "</font></font> This task version <b>(" +
                                        taskLSID.getVersion() +
                                        ")</b> is not present on this server. The version present on this server is <br>");
                        outstr.print("<dd><a href=\"addTask.jsp?view=1&name=" + js.getName() + "\">" + js.getName() +
                                " <b>(" + altVersionLSID.getVersion() + ")</b> </a> " +
                                StringUtils.htmlEncode(formalTask
                                        .getDescription()));
                    } else {
                        outstr
                                .print("<font color='red'>" + js.getName() +
                                        "</font></font> This task is not present on this server");
                    }
                }
                outstr.print("<div id=\"lsid" + i + "\" style=\"display:" + lsidDisplayStyle + ";\">");
                outstr.print("<pre>     " + js.getLSID() + "</pre>");
                outstr.print("</div>");
                outstr.println("<div id=\"id" + i + "\" style=\"display:" + paramDisplayStyle + ";\">"); // XXX
                outstr
                        .println("<table cellspacing='0' width='100%' frame='box'>");
                boolean[] runtimePrompt = js.getRuntimePrompt();
                java.util.Map paramName2FormalParamMap = new java.util.HashMap();
                for (int j = 0; j < formalParams.length; j++) {
                    paramName2FormalParamMap.put(formalParams[j].getName(), formalParams[j]);
                }
                boolean odd = false;
                for (int j = 0; j < formalParams.length; j++) {
                    String paramName = formalParams[j].getName();
                    ParameterInfo formalParam = (ParameterInfo) paramName2FormalParamMap
                            .get(paramName);
                    ParameterInfo informalParam = null;
                    int k;
                    for (k = 0; k < parameterInfo.length; k++) {
                        if (paramName.equals(parameterInfo[k].getName())) {
                            informalParam = parameterInfo[k];
                            break;
                        }
                    }
                    if (informalParam == null) {
                        informalParam = formalParam;
                        k = j;
                    }
                    String value = null;
                    if (formalParam.isInputFile()) {
                        java.util.Map pipelineAttributes = informalParam
                                .getAttributes();
                        String taskNumber = null;
                        if (pipelineAttributes != null) {
                            taskNumber = (String) pipelineAttributes
                                    .get(PipelineModel.INHERIT_TASKNAME);
                        }
                        if (runtimePrompt[k]) {
                            value = "Prompt when run";
                        } else if (taskNumber != null) {
                            String outputFileNumber = (String) pipelineAttributes
                                    .get(PipelineModel.INHERIT_FILENAME);
                            int taskNumberInt = Integer.parseInt(taskNumber
                                    .trim());
                            String inheritedOutputFileName = null;
                            if (outputFileNumber.equals("1")) {
                                inheritedOutputFileName = "1st output";
                            } else if (outputFileNumber.equals("2")) {
                                inheritedOutputFileName = "2nd output";
                            } else if (outputFileNumber.equals("3")) {
                                inheritedOutputFileName = "3rd output";
                            } else if (outputFileNumber.equals("stdout")) {
                                inheritedOutputFileName = "standard output";
                            } else if (outputFileNumber.equals("stderr")) {
                                inheritedOutputFileName = "standard error";
                            }
                            JobSubmission previousTask = (JobSubmission) tasks
                                    .get(taskNumberInt);
                            int displayTaskNumber = taskNumberInt + 1;
                            value = "Use <b>" + inheritedOutputFileName + "</b> from <a href=\"#" + displayTaskNumber +
                                    "\">" + displayTaskNumber + ". " + previousTask.getName() + "</a>";
                        } else {
                            value = informalParam.getValue();
                            try {
                                new java.net.URL(value); // see if parameter
                                // if
                                // a URL
                                value = "<a href=\"" + value + "\">" + value + "</a>";
                            } catch (java.net.MalformedURLException x) {
                                value = StringUtils.htmlEncode(value);
                            }
                        }
                    } else {
                        value = StringUtils
                                .htmlEncode(informalParam.getValue());
                    }
                    paramName = paramName.replace('.', ' ');
                    // outstr.print("<dd>" + paramName);
                    // outstr.println(": " + value);
                    if (odd) {
                        outstr.print("<tr ><td width='25%' align='right'>" + paramName);
                    } else {
                        outstr
                                .print("<tr  class=\"paleBackground\" ><td width='25%' align='right'>" + paramName);
                    }
                    outstr.flush();
                    outstr.print(":</td><td>&nbsp;&nbsp;&nbsp; " + value);
                    outstr.println("</td></tr>");
                    odd = !odd;
                }
                outstr.println("</table>");
                outstr.println("</div>");
            }
            outstr.println("<table cellspacing='0' width='100%' frame='box'>");
            if (!RunPipelineForJsp.isMissingTasks(model, userID)) {
                if (!hideButtons) {
                    outstr
                            .println(
                                    "<table width='100%'><tr><td align='center'><input type=\"button\" value=\"run\"      name=\"runpipeline\" class=\"little\" onclick=\"runpipeline('runPipeline.jsp?cmd=run&name=" +
                                            pipelineName + "')\"; /></td></tr></table>");
                }
            }
        } catch (Exception e) {
            e.printStackTrace(outstr);
        }
    } // end of writePipelineBody

    static class WaitForPipelineCompletionThread extends Thread {
        Process process = null;

        int jobID = -1;

        WaitForPipelineCompletionThread(Process aProcess, int aJobID) {
            process = aProcess;
            jobID = aJobID;
        }

        public void run() {
            try {
                process.waitFor();
                GenePatternAnalysisTask.terminatePipeline(Integer.toString(jobID));
            } catch (Exception e) {
                try {
                    GenePatternAnalysisTask.updatePipelineStatus(jobID, JobStatus.JOB_ERROR, null);
                    GenePatternAnalysisTask.terminatePipeline(Integer
                            .toString(jobID));
                } catch (Exception ee) {
                    // ignore
                }
            }
        }
    }

    static class DeleteUnsavedTasklibDirThread extends Thread {
        TaskInfo taskInfo;

        Process process;

        DeleteUnsavedTasklibDirThread(TaskInfo taskInfo, Process process) {
            this.taskInfo = taskInfo;
            this.process = process;
        }

        public void run() {
            try {
                process.waitFor();
            } catch (InterruptedException ie) {
                // ignore
            }
            try {
                java.io.File fDir = new java.io.File(DirectoryManager
                        .getTaskLibDir(taskInfo));
                // delete the temp files now
                if (fDir.exists()) {
                    java.io.File[] children = fDir.listFiles();
                    for (int i = 0; i < children.length; i++) {
                        System.out.println("Deleting " + children[i].getCanonicalPath());
                        children[i].delete();
                    }
                    System.out.println("Deleting directory " + fDir.getCanonicalPath());
                    fDir.delete();
                }
            } catch (Exception e) {
                // ignore
            }
        } // run
    }
}




<!-- /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ -->


<%@ page
	import="
	java.io.BufferedReader,
	java.io.File,
	java.io.InputStream,
	java.io.InputStreamReader,
	java.io.IOException,
	java.io.OutputStream,
	java.text.DateFormat,
	java.text.ParseException,
	java.text.SimpleDateFormat,
	java.util.Date,
	java.util.Enumeration,
	java.util.GregorianCalendar,
	java.util.Map,
	java.util.HashMap,
	org.genepattern.server.user.User,
	org.genepattern.server.user.UserDAO,
	org.genepattern.webservice.JobStatus,
	org.genepattern.webservice.ParameterInfo,
	org.genepattern.webservice.TaskInfo,
	org.genepattern.util.StringUtils,
	org.genepattern.server.genepattern.GenePatternAnalysisTask,
	org.genepattern.util.GPConstants,org.genepattern.server.indexer.IndexerDaemon,
	org.genepattern.data.pipeline.PipelineModel,
	org.genepattern.server.webapp.RunPipelineForJsp,
	java.net.URLDecoder"
	session="false" language="Java" buffer="1kb"%>
<%
            response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
            response.setDateHeader("Expires", 0);
            response.setHeader("Connection", "Keep-Alive");
            response.setHeader("Keep-Alive", "timeout=1000, max=50");
            response.setHeader("HTTP-Version", "HTTP/1.1");

            HashMap requestParamsAndAttributes = new HashMap();

            for (Enumeration eNames = request.getParameterNames(); eNames.hasMoreElements();) {
                String n = (String) eNames.nextElement();
                requestParamsAndAttributes.put(n, request.getParameter(n));
            }
            for (Enumeration eNames = request.getAttributeNames(); eNames.hasMoreElements();) {
                String n = (String) eNames.nextElement();
                requestParamsAndAttributes.put(n, request.getAttribute(n));
            }

            HashMap commandLineParams = new HashMap();

            int UNDEFINED = -1;
            int jobID = UNDEFINED;
            String userEmail = null;

            try {
                boolean DEBUG = false;
                if (!DEBUG && requestParamsAndAttributes.get("DEBUG") != null)
                    DEBUG = true;

                String userID = (String) request.getAttribute("userID"); // will force login if necessary
                if (userID == null || userID.length() == 0)
                    return; // come back after login

                try {
                    User user = (new UserDAO()).findById(userID);
                    userEmail = user.getEmail();
                    if ((userEmail == null) || (userEmail.length() == 0)) {
                        userEmail = userID;
                    }
                } catch (Exception e) {
                    userEmail = userID;
                }

                String NAME = "name";
                String DOT_PIPELINE = "." + GPConstants.TASK_TYPE_PIPELINE;
                String STOP = "stop";
                String CMD = "cmd";
                String RUN = "run";
                String CODE = "code";
                String EMAIL = "email";
                String JOBID = "jobID";

                String name = (String) requestParamsAndAttributes.get(NAME);
                String command = (String) requestParamsAndAttributes.get(CMD);
                if (command == null)
                    command = RUN;
                String description = null;
                boolean isSaved = (requestParamsAndAttributes.get("saved") == null);
                TaskInfo taskInfo = null;
                String serverPort = System.getProperty("GENEPATTERN_PORT");
                if (!isSaved) {
                    description = (String) requestParamsAndAttributes.get("description");
                    taskInfo = (TaskInfo) requestParamsAndAttributes.get("taskInfo");
                    serverPort = (String) requestParamsAndAttributes.get("serverPort");
                } else if (!command.equals(STOP) && name != null) {
                    taskInfo = GenePatternAnalysisTask.getTaskInfo(URLDecoder.decode(name, "UTF-8"), userID);
                }
                String pipelineName = (name == null ? ("unnamed" + DOT_PIPELINE) : taskInfo.getName());

                String baseURL = request.getScheme() + "://" + request.getServerName() + ":" + serverPort
                        + request.getRequestURI();
                baseURL = baseURL.substring(0, baseURL.lastIndexOf("/") + 1);

                try {
                    if (command.equals(STOP)) {
                        RunPipelineForJsp.stopPipeline((String) requestParamsAndAttributes.get(JOBID));
%>
<script language="Javascript">
		self.window.close();
		</script>
<%
                    return;
                    }

                    /**
                     * Collect the command line params from the request and see if they are all present
                     */

                    ParameterInfo[] parameterInfoArray = taskInfo.getParameterInfoArray();
                    ParameterInfo[] parameters = (ParameterInfo[]) request.getAttribute("parameters");
                    Map<String, String> nameToParameter = new HashMap<String, String>();
                    if (parameters != null) {
                        for (ParameterInfo p : parameters) {
                            nameToParameter.put(p.getName(), p.getValue());
                        }
                    }
                    if (parameterInfoArray != null && parameterInfoArray.length > 0) {
                        // the pipeline needs parameters.  If they are provided, set them 
                        // into the correct place. 
                        for (int i = 0; i < parameterInfoArray.length; i++) {
                            ParameterInfo param = parameterInfoArray[i];
                            String key = param.getName();

                            Object val = requestParamsAndAttributes.get(key); // get non-file param
                            if (val == null) {
                                val = nameToParameter.get(key);
                            }
                            if (val != null) {
                                commandLineParams.put(key, val);
                            }

                        }
                    }

                    // check all required params present and punt if not
                    boolean paramsRequired = RunPipelineForJsp.validateAllRequiredParametersPresent(taskInfo,
                            commandLineParams);
                    if (paramsRequired) {
                        request.setAttribute("name", pipelineName);
                        request.getRequestDispatcher("cannotRunPipeline.jsp").forward(request, response);
                        return;
                    }

                    // set the title
                    String title = "pipeline";
                    if (name != null)
                        title = taskInfo.getName();
                    if (description != null)
                        title = title + ": " + description;

                    if (isSaved) {
                        try {
                            if ((name != null) && (!(name.trim().length() == 0))) {
                                taskInfo = GenePatternAnalysisTask
                                        .getTaskInfo(URLDecoder.decode(name, "UTF-8"), userID);
                            } else {

                                taskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, userID);

                            }
                        } catch (Exception e) {
                        }
                        if (taskInfo == null) {
                            request.setAttribute("name", pipelineName);
                            request.getRequestDispatcher("unknownPipeline.jsp").forward(request, response);
                            return;
                        }
                        description = taskInfo.getDescription();
                    } else { // !isSaved
                        taskInfo = (TaskInfo) requestParamsAndAttributes.get("taskInfo");
                    }

                    String taskName = (String) request.getAttribute("taskName");
%>

<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="css/style.css" rel="stylesheet" type="text/css">
<link href="css/style-jobresults.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">

<title>GenePattern | <%=title.replace('.', ' ')%> Results</title>
<script language="Javascript" src="js/prototype.js"></script>
<script language="Javascript" src="js/commons-validator-1.3.0.js"></script>
<script language="Javascript" src="js/genepattern.js"></script>
<jsp:include page="navbarHead.jsp" />
</head>
<body>
<jsp:include page="navbar.jsp" />
<script language="Javascript">
var outputFileCount = new Array();
</script>
<%
                    out.flush();
                    if (command.equals(RUN)) {
                        String serializedModel = (String) taskInfo.giveTaskInfoAttributes().get(
                                GPConstants.SERIALIZED_MODEL);
                        PipelineModel model = null;
                        int numTasks = 50;
                        try {
                            model = PipelineModel.toPipelineModel(serializedModel);
                            numTasks = model.getTasks().size();
                        } catch (Exception e) {
                            out.println("An error occurred while attempting to run the pipeline.");
                            return;
                        }

                        try {
                            if (RunPipelineForJsp.isMissingTasks(model, new java.io.PrintWriter(out), userID)) {
                                return;
                            }
                        } catch (Exception e) {
                            out.println("An error occurred while processing your request. Please try again.");
                            return;
                        }
                        String decorator = (String) requestParamsAndAttributes.get("decorator");
                        Process process = RunPipelineForJsp.runPipeline(taskInfo, name, baseURL, decorator, userID,
                                commandLineParams);
                        jobID = RunPipelineForJsp.getJobID();

                        // stuff to write view file
                        String jobDir = GenePatternAnalysisTask.getJobDir("" + jobID);
                        File jobDirFile = new File(jobDir);
                        jobDirFile.mkdirs();

                        StringBuffer cc = new StringBuffer();
                        // create threads to read from the command's stdout and stderr streams
                        Thread stdoutReader = copyStream(process.getInputStream(), out, cc, DEBUG, false);
                        Thread stderrReader = copyStream(process.getErrorStream(), out, cc, DEBUG, true);
                        stderrReader.setPriority(stdoutReader.getPriority() - 1);
                        OutputStream stdin = process.getOutputStream();

                        // drain the output and error streams
                        stdoutReader.start();
                        stderrReader.start();
%>
<script language="Javascript">

function checkAll(frm, bChecked) {
	frm = document.forms["results"];
	for (i = 0; i < frm.elements.length; i++) {
		if (frm.elements[i].type != "checkbox") continue;
		frm.elements[i].checked = bChecked;
	}
}
  
// check all the files in the job by its index
function checkAllInTask(idx, taskcb) {
	frm = document.forms["results"];
    var fileCount = outputFileCount[idx];

	for (i = 1; i < (fileCount+1); i++) {
		cb = document.getElementById('outFileCB'+idx+"_"+i);

		cb.checked = taskcb.checked;

	}
}
 

function setEmailNotification(jobId){
		var cb = document.getElementById('emailCheckbox');
		var ue = document.getElementById("userEmail");
		var valid = jcv_checkEmail(ue.value); 
		if (!valid){
			var em = prompt("Email on completion to?:");
			if (em == null){
				cb.checked = false;
				return;
			} else {
				ue.value = em;
				valid = jcv_checkEmail(ue.value); 
				if (!valid){
					cb.checked = false;
					alert(ue.value + ' is not a valid email address');
					return;
				}
			}
		}

 	  	if (cb.checked) {
			requestEmailNotification(ue.value, jobId);
	 	} else {
			cancelEmailNotification(ue.value, jobId);
		}
   }
var numTasks = <%=numTasks%>;

function toggleLogs() {
	var cb = document.getElementById('logCheckbox');
	var visible = cb.checked;
	var frm = document.forms["results"];
    
	for(var i = 0; i < (numTasks + 1); i++) {
		
		divObj = document.getElementById('executionLogRow'+i);
		
		if(!visible) {
			divObj.style.display = "none";
			divObj.visibility=false;
		} else {
			divObj.style.display = "";
			divObj.visibility=true;	
		}


	}
}
   
function openAllTasks(){
	for(var i = 1; i < (numTasks + 1); i++) {
		toggleTask(i, false);
	}
}
function closeAllTasks(){
	for(var i = 1; i < (numTasks + 1); i++) {
		toggleTask(i, true);
	}
}

function setVisibility(anObj, visibility){

	if (visibility == null){
		visibility = anObj.visibility;
	}
	if (visibility == false){
		anObj.style.display = "";
		anObj.visibility=true;
	} else {
		anObj.style.display = "none";
		anObj.visibility=false;
	}

}

function toggleTask(idx, visibility) {
	var idBase = 'fileRow'+idx+"_";
	var fileCount = outputFileCount[idx];

	arrowdown = document.getElementById('downarrow'+idx);
	setVisibility(arrowdown, visibility);	

	arrowright = document.getElementById('rightarrow'+idx);
	setVisibility(arrowright, !visibility);	

	logRow = document.getElementById('executionLogRow'+idx);
	setVisibility(logRow, visibility);	
	
	for(var i = 0; i < (fileCount + 1); i++) {
		
		adiv  = document.getElementById(idBase +i);
		if (adiv != null){
		setVisibility(adiv, visibility);
		}
	}		
	
}	
	
  function deleteCheckedFiles(){
	 var frm = document.forms["results"];
       var really = confirm('Really delete the checked files?');
       if (!really) return;
	cmd = frm.elements['cmdElement'];
	cmd.name="delete";
	cmd.value="true";
 	frm.submit();
	
	cmd.name="cmdElement";
  }
 
 function downloadCheckedFiles(){
	 var frm = document.forms["results"];
	
	cmd = frm.elements['cmdElement'];
	cmd.name="download";
	cmd.value="true";
	frm.submit();
	cmd.name="cmdElement";
    }

</script>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>
		<td valign="top" class="maintasknav" id="maintasknav"><input
			type="checkbox" id="emailCheckbox"
			onclick="setEmailNotification(<%=jobID%>);" value="checkbox" />email
		notification&nbsp;&nbsp;&nbsp;&nbsp; <input type="hidden"
			id="userEmail" value="<%= userEmail %>" /> <input name="showLogs"
			id="logCheckbox" type="checkbox" onclick="toggleLogs()"
			value="showLogs" checked="checked" /> show execution logs</td>
	</tr>
</table>
<table width="100%" border="0" cellpadding="0" cellspacing="0"
	class="barhead-task">
	<tr>
		<td><%=taskInfo.getName()%> Status</td>
	</tr>
</table>

<table width='100%' cellpadding="0">
	<tr>
		<td width="50px"><input name="stopCmd" id="stopCmd" type="button"
			value="Stop..." onclick="stopJob(this, <%= jobID%>)" class="little">
		</td>
		<td>Running <a
			href="viewPipeline.jsp?view=1&name=<%=taskInfo.getTaskInfoAttributes().get("LSID")%>"><%=taskInfo.getName()%>
		</a> as job # <a href="getJobResults.jsp?jobID=<%=jobID%>"><%=jobID%>
		</a> on <%=new Date()%></td>
	</tr>
</table>


<!--span id='output'-->
<%
                        out.flush();
                        // copy the generated code to the stdin of the R process
                        if (DEBUG)
                            out.println("<pre>");

                        // wait for all output so that nothing is left buffered at end of process
                        stdoutReader.join();
                        stderrReader.join();
                        out.flush();

                        // output an extra </table> tag in case the pipeline was stopped, which would leave an open table tag and cause the
                        // supposedly trailing output to come out before the table itself!
                        if (DEBUG)
                            out.println("</pre>");
%>
<!--/span-->

<script language="Javascript">
	document.getElementById("stopCmd").disabled=true;
	document.getElementById("stopCmd").visibility=false;


	
		</script>
<%
                        GregorianCalendar purgeTOD = new GregorianCalendar();

                        try {

                            SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
                            GregorianCalendar gcPurge = new GregorianCalendar();
                            gcPurge.setTime(dateFormat.parse(System.getProperty("purgeTime", "23:00")));
                            purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, gcPurge.get(GregorianCalendar.HOUR_OF_DAY));
                            purgeTOD.set(GregorianCalendar.MINUTE, gcPurge.get(GregorianCalendar.MINUTE));
                        } catch (ParseException pe) {
                            purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, 23);
                            purgeTOD.set(GregorianCalendar.MINUTE, 0);
                        }
                        purgeTOD.set(GregorianCalendar.SECOND, 0);
                        purgeTOD.set(GregorianCalendar.MILLISECOND, 0);
                        int purgeInterval;
                        try {
                            purgeInterval = Integer.parseInt(System.getProperty("purgeJobsAfter", "-1"));
                        } catch (NumberFormatException nfe) {
                            purgeInterval = 7;
                        }
                        purgeTOD.add(GregorianCalendar.DATE, purgeInterval);
                        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
%>

<table with="100%">
	<tr>
		<td class="purge_notice">These job results are scheduled to be
		purged from the server on <%=df.format(purgeTOD.getTime()).toLowerCase()%>
		</td>
	</tr>
	<tr><td><br>
	<a href="pages/index.jsf">Return to Tasks & Pipelines Start</a>
</td></tr>
</table>

<%
                            synchronized (IndexerDaemon.getDaemon().indexLock) {
                            IndexerDaemon.getDaemon().indexLock.notify();
                        }

                    } else {
                        out.println("unknown command: " + command + "<br>");
                    }
                } catch (Throwable e) {
                    out.flush();
                    out.println("<br>");
                    out.println("runPipeline failed: <br>");
                    out.println(e.getMessage());
                    out.println("<br><pre>");
                    e.printStackTrace();
                    out.println("</pre><br>");

                    if (jobID != UNDEFINED) {
                        GenePatternAnalysisTask.updatePipelineStatus(jobID, JobStatus.JOB_ERROR, null);
                        GenePatternAnalysisTask.terminatePipeline(Integer.toString(jobID));
                    }
                    return;
                } finally {
%>

<jsp:include page="footer.jsp" />
</body>
</html>
<%
            }
            } catch (Throwable t) {
                out.println(t);
                out.println("<br><pre>");
                t.printStackTrace();
                out.println("</pre><br>");
                if (jobID != UNDEFINED) {
                    GenePatternAnalysisTask.updatePipelineStatus(jobID, JobStatus.JOB_ERROR, null);
                    GenePatternAnalysisTask.terminatePipeline(Integer.toString(jobID));
                }
            }
%>
<%!Thread copyStream(final InputStream is, final JspWriter out, final StringBuffer cc, final boolean DEBUG,
            final boolean htmlEncode) throws IOException {
        // create thread to read from the a process' output or error stream
        Thread copyThread = new Thread(new Runnable() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                // copy stdout to JspWriter

                try {
                    boolean bNeedsBreak;
                    while ((line = in.readLine()) != null) {
                        if (!DEBUG)
                            if (line.startsWith("> ") || line.startsWith("+ "))
                                continue;
                        if (htmlEncode) {
                            line = StringUtils.htmlEncode(line);
                        }
                        bNeedsBreak = (line.length() > 0 && (line.indexOf("<") == -1 || line.indexOf("<-") != -1) && line
                                .indexOf(">") == -1);
                        out.print(line);
                        if (bNeedsBreak)
                            out.println("<br>");
                        for (int i = 0; i < 8 * 1024; i++)
                            out.print(" ");
                        out.println();
                        out.flush();
                        cc.append(line);
                        if (bNeedsBreak)
                            cc.append("<br>\n");
                    }
                } catch (IOException ioe) {
                    cc.append("</td></tr></table><br>Pipeline terminated before completion.<br>\n");
                }
            }
        }, "runPipelineCopyThread");
        copyThread.setPriority(Thread.currentThread().getPriority() + 1);
        copyThread.setDaemon(true);
        return copyThread;
    }%>

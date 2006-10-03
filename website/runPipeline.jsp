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
	import="java.io.BufferedReader,java.io.File,java.io.FileInputStream,java.io.FileReader,java.io.BufferedWriter,java.io.FileWriter,java.io.InputStream,java.io.InputStreamReader,java.io.IOException,java.io.ObjectInputStream,java.io.OutputStream,java.io.PrintStream,java.net.URLEncoder,java.text.DateFormat,java.text.ParseException,java.text.SimpleDateFormat,java.util.Date,java.util.Enumeration,java.util.GregorianCalendar,java.util.HashMap,java.util.Hashtable,java.util.Iterator,java.util.Map,java.util.StringTokenizer,java.util.TreeMap,java.io.PrintWriter,javax.mail.*,javax.mail.internet.MimeMessage,javax.mail.internet.InternetAddress,org.genepattern.webservice.JobInfo,org.genepattern.webservice.JobStatus,org.genepattern.webservice.ParameterFormatConverter,org.genepattern.webservice.ParameterInfo,org.genepattern.webservice.TaskInfo,org.genepattern.webservice.TaskInfoAttributes,org.genepattern.server.util.AccessManager,org.genepattern.util.StringUtils,org.genepattern.server.genepattern.GenePatternAnalysisTask,org.genepattern.util.GPConstants,org.genepattern.server.indexer.IndexerDaemon,org.genepattern.data.pipeline.PipelineModel,org.genepattern.server.webapp.RunPipelineForJsp,org.genepattern.server.webapp.*,org.genepattern.data.pipeline.*,org.genepattern.server.*,org.genepattern.util.LSID"
	session="false" language="Java" buffer="1kb"%>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache"); // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
	response.setHeader("Connection", "Keep-Alive");
	response.setHeader("Keep-Alive", "timeout=1000, max=50");
	response.setHeader("HTTP-Version", "HTTP/1.1");

	HashMap requestParamsAndAttributes = new HashMap();

	for (Enumeration eNames = request.getParameterNames(); eNames
			.hasMoreElements();) {
		String n = (String) eNames.nextElement();
		requestParamsAndAttributes.put(n, request.getParameter(n));
	}
	for (Enumeration eNames = request.getAttributeNames(); eNames
			.hasMoreElements();) {
		String n = (String) eNames.nextElement();
		requestParamsAndAttributes.put(n, request.getAttribute(n));
	}

	HashMap commandLineParams = new HashMap();

	int UNDEFINED = -1;
	int jobID = UNDEFINED;
	try {
		boolean DEBUG = false;
		if (!DEBUG && requestParamsAndAttributes.get("DEBUG") != null)
			DEBUG = true;

		String userID = (String) request.getAttribute("userID"); // will force login if necessary
		if (userID == null || userID.length() == 0)
			return; // come back after login

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
			description = (String) requestParamsAndAttributes
			.get("description");
			taskInfo = (TaskInfo) requestParamsAndAttributes
			.get("taskInfo");
			serverPort = (String) requestParamsAndAttributes
			.get("serverPort");
		} else if (!command.equals(STOP) && name != null) {
			taskInfo = GenePatternAnalysisTask
			.getTaskInfo(name, userID);
		}
		String pipelineName = (name == null ? ("unnamed" + DOT_PIPELINE)
		: taskInfo.getName());

		String baseURL = request.getScheme() + "://"
		+ request.getServerName() + ":" + serverPort
		+ request.getRequestURI();
		baseURL = baseURL.substring(0, baseURL.lastIndexOf("/") + 1);

		try {
			if (command.equals(STOP)) {
		RunPipelineForJsp
				.stopPipeline((String) requestParamsAndAttributes
				.get(JOBID));
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

			ParameterInfo[] parameterInfoArray = taskInfo
			.getParameterInfoArray();
			if (parameterInfoArray != null
			&& parameterInfoArray.length > 0) {
		// the pipeline needs parameters.  If they are provided, set them 
		// into the correct place. 
		for (int i = 0; i < parameterInfoArray.length; i++) {
			ParameterInfo param = parameterInfoArray[i];
			String key = param.getName();

			Object val = requestParamsAndAttributes.get(key); // get non-file param

			if (val != null) {
				commandLineParams.put(key, val);
			}

		}
			}

			// check all required params present and punt if not
			boolean paramsRequired = RunPipelineForJsp
			.validateAllRequiredParametersPresent(taskInfo,
			commandLineParams);
			if (paramsRequired) {
		request.setAttribute("name", pipelineName);
		request.getRequestDispatcher("cannotRunPipeline.jsp")
				.forward(request, response);
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
			if ((name != null)
			&& (!(name.trim().length() == 0))) {
				taskInfo = GenePatternAnalysisTask.getTaskInfo(
				name, userID);
			} else {
				taskInfo = GenePatternAnalysisTask.getTaskInfo(
				pipelineName, userID);

			}
		} catch (Exception e) {
		}
		if (taskInfo == null) {
			request.setAttribute("name", pipelineName);
			request.getRequestDispatcher("unknownPipeline.jsp")
			.forward(request, response);
			return;
		}
		description = taskInfo.getDescription();
			} else { // !isSaved
		taskInfo = (TaskInfo) requestParamsAndAttributes
				.get("taskInfo");
			}

			String taskName = (String) request.getAttribute("taskName");
%>

<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title><%=title.replace('.', ' ')%></title>

</head>
<body>
<jsp:include page="navbar.jsp"/>
<%
			out.flush();
			if (command.equals(RUN)) {
		String serializedModel = (String) taskInfo
				.giveTaskInfoAttributes().get(
				GPConstants.SERIALIZED_MODEL);
		PipelineModel model = null;
		try {
			model = PipelineModel
			.toPipelineModel(serializedModel);

		} catch (Exception e) {
			out
			.println("An error occurred while attempting to run the pipeline.");
			return;
		}

		try {
			if (RunPipelineForJsp.isMissingTasks(model,
			new java.io.PrintWriter(out), userID)) {
				return;
			}
		} catch (Exception e) {
			out
			.println("An error occurred while processing your request. Please try again.");
			return;
		}
		String decorator = (String) requestParamsAndAttributes
				.get("decorator");
		Process process = RunPipelineForJsp.runPipeline(
				taskInfo, name, baseURL, decorator, userID,
				commandLineParams);
		jobID = RunPipelineForJsp.getJobID();

		// stuff to write view file
		String jobDir = GenePatternAnalysisTask.getJobDir(""
				+ jobID);
		File jobDirFile = new File(jobDir);
		jobDirFile.mkdirs();

		StringBuffer cc = new StringBuffer();
		// create threads to read from the command's stdout and stderr streams
		Thread stdoutReader = copyStream(process
				.getInputStream(), out, cc, DEBUG, false);
		Thread stderrReader = copyStream(process
				.getErrorStream(), out, cc, DEBUG, true);
		stderrReader
				.setPriority(stdoutReader.getPriority() - 1);
		OutputStream stdin = process.getOutputStream();

		// drain the output and error streams
		stdoutReader.start();
		stderrReader.start();
%>
<script language="Javascript">

var pipelineStopped = false;

function stopPipeline(button) {
	var really = confirm('Really stop the pipeline?');
	if (!really) return;
	window.open("runPipeline.jsp?<%= CMD %>=<%= STOP %>&<%= JOBID %>=<%= jobID %>", "_blank", "height=100, width=100, directories=no, menubar=no, statusbar=no, resizable=no");
	pipelineStopped = true;
}

function checkAll(frm, bChecked) {
	frm = document.forms["results"];
	for (i = 0; i < frm.elements.length; i++) {
		if (frm.elements[i].type != "checkbox") continue;
		frm.elements[i].checked = bChecked;
	}
}

var ie4 = (document.all) ? true : false;
var ns4 = (document.layers) ? true : false;
var ns6 = (document.getElementById && !document.all) ? true : false;

function showLayer(lay) {
	if (ie4) {document.all[lay].style.visibility = "visible";}
	if (ns4) {document.layers[lay].visibility = "show";}
	if (ns6) {document.getElementById([lay]).style.display = "block";}
}

function hideLayer(lay) {
	if (ie4) {document.all[lay].style.visibility = "hidden";}
	if (ns4) {document.layers[lay].visibility = "hide";}
	if (ns6) {document.getElementById([lay]).style.display = "none";}
}

window.focus();

if (document.layers) document.captureEvents(Event.KEYPRESS);

function suppressEnterKey(evt) {
	var c = document.layers ? evt.which : document.all ? event.keyCode : evt.keyCode;
	return "\r\n".indexOf(String.fromCharCode(c)) == -1;
}

</script>


<br>
<form name="frm<%= STOP %>"><input name="cmd" type="button"
	value="<%= STOP %>..." onclick="stopPipeline(this)" class="little">
<input type="hidden" name="<%= JOBID %>" value="<%= jobID %>"></form>

<form name="frm<%= EMAIL %>" method="POST" target="_blank"
	action="sendMail.jsp" onsubmit="javascript:return false;">email
notification to: <input name="to" class="little" size="70" value=""
	onkeydown="return suppressEnterKey(event)"> <input
	type="hidden" name="from" value="<%= StringUtils.htmlEncode(userID) %>">
<input type="hidden" name="subject"
	value="<%= taskInfo.getName() %> results for job # <%= jobID %>">
<input type="hidden" name="message"
	value="<html><head><link href='stylesheet.css' rel='stylesheet' type='text/css'><script language='Javascript'>\nfunction checkAll(frm, bChecked) {\n\tfrm = document.forms['results'];\n\tfor (i = 0; i < frm.elements.length; i++) {\n\t\tif (frm.elements[i].type != 'checkbox') continue; \n\t\tfrm.elements[i].checked = bChecked;\n\t}\n}\n</script></head><body>">
</form>
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

			document.frm<%= STOP %>.cmd.disabled = true;
			document.frm<%= STOP %>.cmd.visibility = false;
			var frm = document.frm<%= EMAIL %>;
			frm.to.readonly = true; // no more edits as it is about to be used for addressing
			var to = frm.to.value;
			if (to != "") {
<%
				String output = GenePatternAnalysisTask.replace(cc.toString(), "'", "\\'"); // replace quote with backslash-quote
				output = GenePatternAnalysisTask.replace(output, "\n", "\\n");
				output = GenePatternAnalysisTask.replace(output, "\r", "\\r");
%>
				frm.message.value = frm.message.value + '<%= output %>';
				frm.submit();
			}
		</script>
<%
		GregorianCalendar purgeTOD = new GregorianCalendar();

		try {

			SimpleDateFormat dateFormat = new SimpleDateFormat(
			"HH:mm");
			GregorianCalendar gcPurge = new GregorianCalendar();
			gcPurge.setTime(dateFormat.parse(System
			.getProperty("purgeTime", "23:00")));
			purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, gcPurge
			.get(GregorianCalendar.HOUR_OF_DAY));
			purgeTOD.set(GregorianCalendar.MINUTE, gcPurge
			.get(GregorianCalendar.MINUTE));
		} catch (ParseException pe) {
			purgeTOD.set(GregorianCalendar.HOUR_OF_DAY, 23);
			purgeTOD.set(GregorianCalendar.MINUTE, 0);
		}
		purgeTOD.set(GregorianCalendar.SECOND, 0);
		purgeTOD.set(GregorianCalendar.MILLISECOND, 0);
		int purgeInterval;
		try {
			purgeInterval = Integer.parseInt(System
			.getProperty("purgeJobsAfter", "-1"));
		} catch (NumberFormatException nfe) {
			purgeInterval = 7;
		}
		purgeTOD.add(GregorianCalendar.DATE, purgeInterval);
		DateFormat df = DateFormat.getDateTimeInstance(
				DateFormat.SHORT, DateFormat.SHORT);
%>

<br>
These job results are scheduled to be purged from the server on
<%=df.format(purgeTOD.getTime()).toLowerCase()%>
<br>

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
		GenePatternAnalysisTask.updatePipelineStatus(jobID,
				JobStatus.JOB_ERROR, null);
		GenePatternAnalysisTask.terminatePipeline(Integer
				.toString(jobID));
			}
			return;
		} finally {
%>

<jsp:include page="footer.jsp"/>
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
			GenePatternAnalysisTask.updatePipelineStatus(jobID,
			JobStatus.JOB_ERROR, null);
			GenePatternAnalysisTask.terminatePipeline(Integer
			.toString(jobID));
		}
	}
%>
<%!Thread copyStream(final InputStream is, final JspWriter out,
			final StringBuffer cc, final boolean DEBUG, final boolean htmlEncode)
			throws IOException {
		// create thread to read from the a process' output or error stream
		Thread copyThread = new Thread(new Runnable() {
			public void run() {
				BufferedReader in = new BufferedReader(
						new InputStreamReader(is));
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
						bNeedsBreak = (line.length() > 0
								&& (line.indexOf("<") == -1 || line
										.indexOf("<-") != -1) && line
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
					cc
							.append("</td></tr></table><br>Pipeline terminated before completion.<br>\n");
				}
			}
		}, "runPipelineCopyThread");
		copyThread.setPriority(Thread.currentThread().getPriority() + 1);
		copyThread.setDaemon(true);
		return copyThread;
	}%>

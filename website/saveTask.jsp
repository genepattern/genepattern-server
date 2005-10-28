<%@ page import="java.io.File,
		 java.io.FileInputStream,
		 java.io.FilenameFilter,
		 java.io.FileOutputStream,
		 java.io.FileWriter,
		 java.io.IOException,
		 java.lang.reflect.Constructor,
		 java.net.URLEncoder,
		 java.text.SimpleDateFormat,
		 java.util.Arrays,
		 java.util.Comparator,
		 java.util.Date,
		 java.util.HashMap,
		 java.util.TreeMap,
		 java.util.Vector,
		 java.util.Enumeration,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterInfo,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.server.genepattern.TaskInstallationException,
		 org.genepattern.util.GPConstants,
		 org.genepattern.webservice.OmnigeneException, 
		 org.genepattern.webservice.WebServiceException, 
		 org.genepattern.webservice.WebServiceErrorMessageException, 
		 org.genepattern.server.webservice.*,
		 org.genepattern.server.webapp.*,
		 org.genepattern.data.pipeline.PipelineModel,
		 org.genepattern.server.webservice.server.DirectoryManager,
		 com.jspsmart.upload.*,
		 org.genepattern.util.LSID"
	session="false" contentType="text/html" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String userID= (String)request.getAttribute("userID"); // will force login if necessary
if (userID == null) return; // come back after login
LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);
%>
<jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" />
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >

<title>saved GenePattern task</title>
</head>
<body>	
<%
int i;
StringBuffer log = null;

String logFilename = System.getProperty("taskLog", null);
com.jspsmart.upload.Request requestParameters = null;
StringBuffer sbAttachments = new StringBuffer();
TaskInfo previousTask = null;
String taskName = null;
String forward = null;

try {

// mySmartUpload is from http://www.jspsmart.com/

// Initialization
mySmartUpload.initialize(pageContext);
try { 	mySmartUpload.upload(); 
    } catch (NegativeArraySizeException nase) {
	// ignore
    }
requestParameters = mySmartUpload.getRequest();

/*
out.println("request parameters:<br>");
for (java.util.Enumeration eNames = requestParameters.getParameterNames(); eNames.hasMoreElements(); ) {
	String n = (String)eNames.nextElement();
	out.println(n + "='" + StringUtils.htmlEncode(requestParameters.getParameter(n)) + "'<br>");
}
for (java.util.Enumeration eNames = request.getParameterNames(); eNames.hasMoreElements(); ) {
	String n = (String)eNames.nextElement();
	out.println(n + "='" + StringUtils.htmlEncode(request.getParameter(n)) + "'<br>");
}
out.println("<hr><br>");
*/

taskName = requestParameters.getParameter(GPConstants.NAME);
if (taskName == null ) taskName = request.getParameter(GPConstants.NAME);
if (taskName == null || taskName.trim().length() == 0) {
%>
	<jsp:include page="navbar.jsp"></jsp:include>
	You must specify a task name or LSID.<br>
	<a href="javascript:history.back()">back</a><br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}

// save the file attachments, if any, before installing the task, so that it is immediately ready to run
String lsid = requestParameters.getParameter(GPConstants.LSID);
if (lsid == null ) lsid = request.getParameter(GPConstants.LSID);
String attachmentDir = null;
File dir = null;

// delete task
if (requestParameters.getParameter("delete") != null || request.getParameter("delete") != null) {
	try {
		taskIntegratorClient.deleteTask(lsid);
%>
		<jsp:include page="navbar.jsp"></jsp:include>
		<%= taskName %> has been deleted.  Any running jobs using that task have been stopped.<br>
<%
	} catch (Throwable t) { 
%>
		<jsp:include page="navbar.jsp"></jsp:include>
<%
		out.println(t + " while attempting to delete " + taskName);
	} finally {
%>
		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
<%
	}
	return;
}

// delete support file

if ((requestParameters.getParameter("deleteFiles") != null || request.getParameter("deleteFiles") != null) ||
    (requestParameters.getParameter("deleteSupportFiles") != null || request.getParameter("deleteSupportFiles") != null)) {


	if ((requestParameters.getParameter("deleteSupportFiles") != null && requestParameters.getParameter("deleteSupportFiles").length() > 0) ||
	    (request.getParameter("deleteSupportFiles") != null && request.getParameter("deleteSupportFiles").length() > 0)) {

		String filename = requestParameters.getParameter("deleteFiles");
		if (filename == null) filename = request.getParameter("deleteFiles");

		forward = request.getParameter("forward");
		if (forward== null) forward = "addTask.jsp";


		if (filename != null && !filename.equals("")) {
			try {
				StringBuffer sbURL = request.getRequestURL();
				String queryString = request.getQueryString();
				if (queryString != null) {
					sbURL.append("?");
					sbURL.append(queryString);
				}
				lsid = taskIntegratorClient.deleteFiles(lsid, new String[] { filename });
				if (lsid != null) { 
					response.sendRedirect(forward + "?" + GPConstants.NAME + "=" + lsid);
					return;
				} else { %>
					<jsp:include page="navbar.jsp"></jsp:include>
					Unable to delete <%= filename %> from <%= taskName %> support files.<br>
					<jsp:include page="footer.jsp"></jsp:include>
					</body>
					</html>
<% 				
					return;
				}
			} catch (Throwable t) { 
%>
				<jsp:include page="navbar.jsp"></jsp:include>
				<%= t %> while attempting to delete <%= filename %>
				<br>
				<jsp:include page="footer.jsp"></jsp:include>
				</body>
				</html>
<%
				return;
			}
		}
	} // end of deleteSupportFiles
}

// clone task
if (request.getParameter("clone") != null) {
	String cloneName = request.getParameter("cloneName");
	try {
		StringBuffer sbURL = request.getRequestURL();
		String queryString = request.getQueryString();
		if (queryString != null) {
			sbURL.append("?");
			sbURL.append(queryString);
		}
		lsid = taskIntegratorClient.cloneTask(lsid, cloneName);
	} catch (WebServiceErrorMessageException wseme) {
		Vector vProblems = wseme.getErrors();
		if(vProblems != null && vProblems.size() > 0) {
%>
			<jsp:include page="navbar.jsp"></jsp:include>
			There are some problems with the <%= cloneName %> task that need to be fixed:<br><ul>
<%	
		    	for (Enumeration eProblems = vProblems.elements(); eProblems.hasMoreElements(); ) {
%>
				<li><%= StringUtils.htmlEncode((String)eProblems.nextElement()) %></li>
<%
			}
%>
			</ul><a href="javascript:history.back()">back</a>
<%
			return;
		}
	}
%>
	<jsp:include page="navbar.jsp"></jsp:include>
	Cloned <%= taskName %> as <%= cloneName %>.<br>
	<a href="addTask.jsp?<%= GPConstants.NAME%>=<%=lsid %>">edit <%= cloneName %></a><br>
	<script language="javascript">
	<% if("1".equals(request.getParameter("pipeline"))) {
		%> window.location = "pipelineDesigner.jsp?<%= GPConstants.NAME %>=<%= lsid %>"; <%
	} else { %>
		window.location = "addTask.jsp?<%= GPConstants.NAME %>=<%= lsid %>";
	<%}
	%>
	
	</script>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}

int access_id = requestParameters.getParameter(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) ? GPConstants.ACCESS_PUBLIC : GPConstants.ACCESS_PRIVATE;

String formerName = requestParameters.getParameter(GPConstants.FORMER_NAME);
if (formerName != null && formerName.length() > 0 && !formerName.equals(taskName)) {
	try {
		previousTask = GenePatternAnalysisTask.getTaskInfo(formerName, userID);
	} catch (OmnigeneException oe) {
	}
	if (previousTask != null && !formerName.equalsIgnoreCase(taskName)) {
		// TODO: handle overwrite-by-renaming of some other task
	}

	attachmentDir = DirectoryManager.getTaskLibDir(taskName, lsid, userID);
	dir = new File(attachmentDir);
	dir.delete(); // delete the just created directory

	// renaming task, need to rename taskLib directory for this task
	formerName = requestParameters.getParameter(GPConstants.FORMER_NAME);
	File oldDir = new File(DirectoryManager.getTaskLibDir(formerName, lsid, userID));
	oldDir.renameTo(dir);

	// TODO: check whether this task is involved in any pipelines and if so, alert/offer to rename
	// the task in each pipeline
} else if (formerName != null && formerName.equals(taskName)) {
	try {
		previousTask = GenePatternAnalysisTask.getTaskInfo(formerName, userID);
	} catch (OmnigeneException oe) {
	}
}



// count parameters
int numParameterInfos;
String key = null;
String value = null;
String[] values = null;
for (numParameterInfos = 0; ; numParameterInfos++) {
	key = "p" + numParameterInfos + "_name";
	value = requestParameters.getParameter(key);
	if (value == null || value.length() == 0) break;
}

ParameterInfo[] params = new ParameterInfo[numParameterInfos];
for (i = 0; i < numParameterInfos; i++) {
	ParameterInfo pi = new ParameterInfo(requestParameters.getParameter("p" + i + "_name"), 
					     requestParameters.getParameter("p" + i + "_value"), 
					     requestParameters.getParameter("p" + i + "_description"));
	if (GPConstants.PARAM_INFO_ATTRIBUTES.length > 0) {
		HashMap attributes = new HashMap();
		for (int attributeNum = 0; attributeNum < GPConstants.PARAM_INFO_ATTRIBUTES.length; attributeNum++) {
			String attributeName = (String)GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum][GPConstants.PARAM_INFO_NAME_OFFSET];
			values = requestParameters.getParameterValues("p" + i + "_" + attributeName);
			value = "";
			for (int x=0; values!=null && x < values.length ; x++){
			 	if (x > 0) {
					value = value + GPConstants.PARAM_INFO_CHOICE_DELIMITER;
				}
				value += values[x];
			}
	
			attributes.put(attributeName, value);
		}
		pi.setAttributes(attributes);
		if (pi.getName().indexOf("filename") != -1) {
			pi.setAsInputFile();
			attributes.put(GPConstants.PARAM_INFO_TYPE[GPConstants.PARAM_INFO_TYPE_NAME_OFFSET], GPConstants.PARAM_INFO_TYPE_INPUT_FILE);
		}
	}
	params[i] = pi;
}

TaskInfoAttributes tia = new TaskInfoAttributes();
for (i = 0; i < GPConstants.TASK_INFO_ATTRIBUTES.length; i++) {
	key = GPConstants.TASK_INFO_ATTRIBUTES[i];
	values = requestParameters.getParameterValues(key);
	value = "";
	for (int x=0; values!=null && x < values.length ; x++){
	 	if (x > 0) {
			value = value + GPConstants.PARAM_INFO_CHOICE_DELIMITER;
		}
		value += values[x];
	}
	
	tia.put(key, value);
}

File attachment = null;
String attachmentName = null;
com.jspsmart.upload.File attachedFile = null;

// TODO: get values for access_id from task_access table in database
//
// check if this task already exists (byName) and use updateTask instead of installNewTask if it does
//

access_id = requestParameters.getParameter(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) ? GPConstants.ACCESS_PUBLIC : GPConstants.ACCESS_PRIVATE;

Vector vProblems = null;
try {
	// merge old support files with newly submitted ones!

	javax.activation.DataHandler[] supportFiles = null;
	String[] supportFileNames = null;
	try {
	      supportFiles = taskIntegratorClient.getSupportFiles(lsid);
	      supportFileNames = taskIntegratorClient.getSupportFileNames(lsid);
	} catch(WebServiceException wse) {
	}
   
	lsid = (String)tia.get(GPConstants.LSID);
	lsid = taskIntegratorClient.modifyTask(access_id, 
				requestParameters.getParameter(GPConstants.NAME), 
				requestParameters.getParameter(GPConstants.DESCRIPTION), 
				params,
				tia,
				supportFiles, 
				supportFileNames);

	// make $GenePatternHome/taskLib/<taskName> to store DLLs, etc.

	attachmentDir = DirectoryManager.getTaskLibDir(requestParameters.getParameter(GPConstants.NAME), lsid, userID);
	dir = new File(attachmentDir);

	for (i=0;i<mySmartUpload.getFiles().getCount();i++){
		attachedFile = mySmartUpload.getFiles().getFile(i);
		if (attachedFile.isMissing()) continue;

		try {
			attachmentName = attachedFile.getFileName();
			if (attachmentName.trim().length() == 0) continue;
			attachment = new File(dir, attachmentName);
			if (attachment.exists()) {
				File oldVersion = new File(dir, attachment.getName() + ".old");
				out.println("Replaced " + attachmentName + " (" + attachment.length() + " bytes) in <nobr>" + dir.getPath() + "</nobr>.  Renamed old one to " + oldVersion.getName() + "<br><br>");
				oldVersion.delete(); // delete the previous .old file
				boolean renamed = attachment.renameTo(oldVersion);
				if (!renamed) out.println("failed to rename.<br>");
			}
			
			attachedFile.saveAs(dir.getPath() + File.separator + attachmentName);
			if (sbAttachments.length() > 0) sbAttachments.append(",");
			sbAttachments.append(attachmentName);
	//		out.println("saved " + dir.getPath() + File.separator + attachmentName + " (" + attachedFile.getSize() + " bytes)<br>");
		} catch (SmartUploadException sue) {
		    	throw new Exception("error saving " + dir.getPath() + File.separator + attachmentName + " in " + dir.getPath() +":<br>" + sue.getMessage());
		}
	}

} catch (WebServiceErrorMessageException wseme) {
	vProblems = wseme.getErrors();
}

if (forward == null) {
%>
<jsp:include page="navbar.jsp"></jsp:include>
<%
}

if(vProblems != null && vProblems.size() > 0) {
	if (formerName != null && formerName.length() > 0 && !formerName.equals(taskName)) {
		// renaming task, need to rename taskLib directory for this task
		dir.renameTo(new File(DirectoryManager.getTaskLibDir(formerName, lsid, userID)));
	}

%>
	There are some problems with the <%= requestParameters.getParameter(GPConstants.NAME) %> 
	task description that need to be fixed:<br><ul>
<%	
	for (Enumeration eProblems = vProblems.elements(); eProblems.hasMoreElements(); ) {
%>
		<li><%= StringUtils.htmlEncode((String)eProblems.nextElement()) %></li>
<%
	}
%>
	</ul><a href="javascript:history.back()">back</a><br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}

/*********************************
      begin logging of changes
 *********************************/
if (logFilename != null) {
/* 
log file format:

timeMS dateTime loginId taskType moduleName  manifest supportFilesChanges URLToEditPage URLToZipDownload
76576574 2003-11-03_17:34:11 liefeld "Filter" SelectFeaturesColumns "manifest" "foo.jar,bar.jar" http://elm:8080/gp/addTask.jsp?name=SelectFeaturesColumns http://elm:8080/gp/makeZip.jsp?name=SelectFeaturesColumns
*/

	log = new StringBuffer();
	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-M-d_H:mm");
	Date d = new Date();


	log.append(d.getTime());
	log.append(" ");
	log.append(dateFormat.format(d));
	log.append(" ");
	log.append((String)request.getAttribute("userID"));
	log.append(" ");
	String taskType = tia.get(GPConstants.TASK_TYPE);
	if (taskType == null || taskType.length() == 0) taskType = "[unclassified]";
	log.append("\"");
	log.append(taskType);
	log.append("\" \"");
	log.append(taskName);
	log.append("\"");
	log.append(" \"");

	TaskInfo thisTask = null;
	try {
		thisTask = GenePatternAnalysisTask.getTaskInfo(taskName, userID);
	} catch (OmnigeneException oe) {
	}
	// if manifest changed, list the name of the manifest file
	boolean manifestChange = (previousTask == null || !(thisTask.equals(previousTask)));
	if (manifestChange) {
		log.append(GPConstants.MANIFEST_FILENAME);
	}

	// list all newly added/replaced attachments
	log.append("\" \"");
	log.append(sbAttachments);
	log.append("\" ");

	String baseURL = request.getScheme()+"://" + request.getServerName() + ":" + request.getServerPort() + request.getRequestURI();
	baseURL = baseURL.substring(0, baseURL.lastIndexOf("/")+1);
	log.append(baseURL + "addTask.jsp?" + GPConstants.NAME + "=" + URLEncoder.encode(lsid, "UTF-8"));
	log.append(" ");
	log.append(baseURL + "makeZip.jsp?" + GPConstants.NAME + "=" + URLEncoder.encode(lsid, "UTF-8"));
	log.append("\n");

	if (manifestChange || sbAttachments.length() > 0) {
		FileWriter fwLog = new FileWriter(logFilename, true);
		fwLog.write(log.toString());
		fwLog.close();
	}
}
/*********************************
      end logging of changes
 *********************************/

//if (formerName != null && !formerName.equals("") && !formerName.equals(taskName)) {
//    	ti.deleteTask(formerName, null);
//}

	if (forward != null) {
		response.sendRedirect(forward);
		return;
	}
%>
    Installation of your <a href="addTask.jsp?<%= GPConstants.NAME %>=<%= URLEncoder.encode(lsid, "UTF-8") %>"><%= taskName %></a> task (version <%= new LSID(lsid).getVersion() %>) is complete.<br><br>

    If you write R methods to run tasks, you'll find the R wrapper <a href="taskWrapperGenerator.jsp?<%= GPConstants.NAME %>=<%= URLEncoder.encode(lsid, "UTF-8") %>">here</a>.<br>
    
<hr><h4>Try running <%= taskName %> now!</h4>

<jsp:include page="runTask.jsp" flush="true">
	<jsp:param name="name" value="<%= lsid %>"/>
	<jsp:param name="noEnvelope" value="1"/>
</jsp:include>

<% } catch (Exception e) { %>
	install of <%= taskName %> task failed: <%= e.getMessage() %>
	<br><pre>
<%
	try {
		java.io.PrintWriter pw = new java.io.PrintWriter(out);
	   
		e.printStackTrace(pw);
		pw.close();
	} catch(Throwable t){}
%>
	</pre><br>
	<a href="javascript:history.back()">back</a>
<% } %>
   
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

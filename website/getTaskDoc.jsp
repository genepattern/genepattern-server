<%@ page import="java.io.File,
		 java.io.FilenameFilter,
		 java.io.IOException,
		 java.net.URLEncoder,
		 java.util.Collection,
		 java.util.Hashtable,
		 java.util.Iterator,
		 com.jspsmart.upload.*,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask"
	session="false" language="Java" %><jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" /><% 


response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String userID = GenePatternAnalysisTask.getUserID(request, response); // will force login if necessary
LocalAdminClient adminClient = new LocalAdminClient(userID);
LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID);
String name = request.getParameter("name");
TaskInfo ti;
TaskInfoAttributes tia;

if (name == null) {
%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link href="favicon.ico" rel="shortcut icon">
<title>GenePattern task documentation</title>
<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<table>
<thead>
<tr>
<td><b>name</b></td><td><b>description</b></td>
</tr>
</thead>
<tbody>
<%

Collection tmTasks = adminClient.getTaskCatalog();
String description;
String lsid;
String taskType;
for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
	ti = (TaskInfo)itTasks.next();
	tia = ti.giveTaskInfoAttributes();
	lsid = tia.get(GPConstants.LSID);
	taskType = tia.get(GPConstants.TASK_TYPE);
	description = ti.getDescription();
	if (description == null || description.length() == 0) description = "[no description]";
	boolean isPipeline = taskType.equals(GPConstants.TASK_TYPE_PIPELINE);
%>
	<tr>
	<td valign="top"><a name="<%= ti.getName() %>" href="<%= !isPipeline ? "addTask.jsp" : "pipelineDesigner.jsp" %>?<%= GPConstants.NAME %>=<%= lsid %>&view=1"><%= ti.getName() %><a/></td>
	<td valign="top"><%= GenePatternAnalysisTask.htmlEncode(description) %>
	<br>
<%
	File[] docFiles = taskIntegratorClient.getDocFiles(ti);
	if (docFiles.length == 0) out.println("[no documentation]");
	for (int i = 0; i < docFiles.length; i++) {
%>
		<a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= lsid %>&file=<%= URLEncoder.encode(docFiles[i].getName()) %>"><%= docFiles[i].getName() %></a>
<%
	}
%>
	</td></tr>
<%
}
%>
</tbody>
</table>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
<%
	return;
}
ti = GenePatternAnalysisTask.getTaskInfo(name, userID);
if (ti == null) {
	out.print("No such task: " + name);
	return;
}
tia = ti.giveTaskInfoAttributes();

String filename = request.getParameter("file");
if (filename != null && filename.length() == 0) filename = null;
if (filename == null) {
	File[] docFiles = taskIntegratorClient.getDocFiles(ti);
	if (docFiles.length > 0) {
		filename = docFiles[0].getName();
	}
}
if (filename == null) {
%>
	<html>
	<head>
	<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link href="favicon.ico" rel="shortcut icon">
	<title>GenePattern task documentation</title>
	<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
	</head>
	<body>
	<jsp:include page="navbar.jsp"></jsp:include>
	Sorry, no documentation available for <a href="addTask.jsp?<%= GPConstants.NAME %>=<%= name %>&view=1"><%= ti.getName() %></a>.<br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}
if (filename.indexOf("/") != -1) filename = filename.substring(filename.indexOf("/")+1);
String taskLibDir = GenePatternAnalysisTask.getTaskLibDir(ti.getName(), (String)tia.get(GPConstants.LSID), userID);
int i = filename.lastIndexOf(".");

File in = new File(taskLibDir, filename);
if (!in.exists()) {
%>
	<html>
	<head>
	<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link href="favicon.ico" rel="shortcut icon">
	<title>GenePattern task documentation</title>
	<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
	</head>
	<body>
	<jsp:include page="navbar.jsp"></jsp:include>
	Sorry, no such file <%= filename %> for <a href="addTask.jsp?<%= GPConstants.NAME %>=<%= tia.get(GPConstants.LSID) %>&view=1"><%= ti.getName() %></a>.<br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}

String contentType = new File(filename).toURL().openConnection().getFileNameMap().getContentTypeFor(filename);
if (contentType == null) {
	final Hashtable htTypes = new Hashtable();
	htTypes.put(".jar", "application/java-archive");
	htTypes.put(".zip", "application/zip");
	htTypes.put("." + GPConstants.TASK_TYPE_PIPELINE, "text/plain");
	htTypes.put(".class", "application/octet-stream");
	htTypes.put(".doc", "application/msword");

	i = filename.lastIndexOf(".");
	String extension = (i > -1 ? filename.substring(i) : "");
	contentType = (String)htTypes.get(extension.toLowerCase());
}
if (contentType == null) contentType = "text/plain";

contentType = contentType + "; name=\"" + filename + "\";";
response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\";");
mySmartUpload.initialize(pageContext);
mySmartUpload.downloadFile(in.getPath(), contentType, filename);
return;
%>
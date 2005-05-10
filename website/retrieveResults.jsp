<%@ page import="java.io.File,
		 java.io.FileInputStream,
		 java.io.IOException,
		 java.util.Hashtable,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 com.jspsmart.upload.*"

	session="false" language="Java" %><jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" /><% 

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String tempDir = request.getParameter("job");
if (tempDir == null) tempDir = request.getParameter("dirName");
String filename = request.getParameter("filename");
boolean bGridded = (request.getParameter("gridded") != null);
boolean errorIfNotFound = (request.getParameter("e") != null);

if (tempDir == null || filename == null) {
	out.println("missing input parameter(s)");
	return;
}

File in = null;
if (request.getParameter("abs") != null) {
	// just strip off the /temp prefix and get the job number
	tempDir = new File(tempDir).getName();
}

filename = new File(filename).getName();
in = new File(GenePatternAnalysisTask.getJobDir(tempDir), filename);
if (!in.exists()) {
if(errorIfNotFound) {
   response.sendError(javax.servlet.http.HttpServletResponse.SC_GONE);
   return;
}
%>
Unable to locate <%= GenePatternAnalysisTask.htmlEncode(filename) %> for job <%= tempDir %>.  It may have been deleted already.
<%
	return;
}

response.setDateHeader("X-lastModified", in.lastModified());
response.setHeader("X-job", tempDir);
response.setHeader("X-filename", filename);

String contentType = in.toURL().openConnection().getFileNameMap().getContentTypeFor(filename);
if (contentType == null) {
	final Hashtable htTypes = new Hashtable();
	htTypes.put(".jar", "application/java-archive");
	htTypes.put(".zip", "application/zip");
	htTypes.put("." + GPConstants.TASK_TYPE_PIPELINE, "text/plain");
	htTypes.put(".class", "application/octet-stream");

	int i = filename.lastIndexOf(".");
	String extension = (i > -1 ? filename.substring(i) : "");
	contentType = (String)htTypes.get(extension.toLowerCase());
	if (contentType == null) contentType = "text/plain";
}
if (bGridded) {
	response.setContentType("text/html");
%>
	<html>
	<head>
	<title><%= GenePatternAnalysisTask.htmlEncode(filename) %>, job <%= tempDir %></title>
	<style>
		TD { font-family: Courier; font-size: 9pt }
	</style>
	<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link href="favicon.ico" rel="shortcut icon">
	</head>
	<body>
	<jsp:include page="navbar.jsp"></jsp:include>
	<form>
		<input type="submit" value="show <%= filename %> ungridded">
		<input type="hidden" name="job" value="<%= tempDir %>">
		<input type="hidden" name="filename" value="<%= filename %>">
	</form>
	<hr>
	<table>
	<tr><td>
<%
        FileInputStream ins = new java.io.FileInputStream(in);
	byte[] buf = new byte[100000];
	int i;
	String s;
	i = ins.read(buf);
	while (i > -1) {
		s = new String(buf, 0, i);
		s = s.replaceAll("\t", "</td><td>");
		s = s.replaceAll("\n", "</td></tr>\n<tr><td>");
		out.print(s); // copy input file to response
		i = ins.read(buf);
	}
	ins.close();
	ins = null;
%>
	</td></tr>
	</table>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
} else {

	mySmartUpload.initialize(pageContext);
	try {
		mySmartUpload.downloadFile(in.getPath(), contentType, in.getName());
	} catch(java.io.IOException e){ // so ClientAbortException does not appear in log
		if(!e.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")) {
			throw e;
		}
	} 
/*
	response.setContentType(contentType);
        FileInputStream ins = new java.io.FileInputStream(in);
	byte[] buf = new byte[100000];
	int i;
	String s;
	boolean isWindows = request.getHeader("user-agent").indexOf("Windows") != -1;
	i = ins.read(buf);
	while (i > -1) {
		s = new String(buf, 0, i);
		if (isWindows) {
//			s = s.replaceAll("\n", "\n\r");
		}
		out.print(s); // copy input file to response
		i = ins.read(buf);
	}
	ins.close();
	ins = null;
	return;
*/
}
return;
%>
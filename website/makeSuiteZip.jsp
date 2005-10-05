<%@ page import="org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.SuiteInfo,
         	 org.genepattern.server.process.ZipSuite,
         	 org.genepattern.server.process.ZipTaskWithDependents,
		 org.genepattern.server.webservice.server.local.LocalAdminClient,
		 java.io.ByteArrayOutputStream,
		 java.io.File,
		 java.io.FilenameFilter,
		 java.io.FileInputStream,
		 java.io.FileOutputStream,
		 java.io.PrintWriter,
		 java.util.Iterator,
		 java.util.HashMap,
		 java.util.Properties,
		 java.util.zip.*,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 com.jspsmart.upload.*"
	session="false" contentType="text/html" language="Java" %><jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" /><% 

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

TaskInfo ti = null;
String name = request.getParameter("name"); 
if (name == null || name.length() == 0) {
	out.println("Must specify task name as name argument to this page");
	return;
}

try {

	String userID= (String)request.getAttribute("userID"); 
	if (userID == null) return; // come back after login

	LocalAdminClient adminClient = new LocalAdminClient(userID);

	SuiteInfo si = adminClient.getSuite(name);

	ZipSuite zs;
	String inclDependents = request.getParameter("includeDependents"); 
	if (inclDependents != null){
		//zs = new ZipTaskWithDependents();
		zs = new ZipSuite();

	} else {
 		zs = new ZipSuite();
	}
	File zipFile = zs.packageSuite(name, userID);

System.out.println("ZF=" + zipFile.getAbsolutePath());

	mySmartUpload.initialize(pageContext);
      mySmartUpload.downloadFile(zipFile.getPath(),"application/x-zip-compressed", si.getName()+".zip");
      zipFile.delete();
      return;
       
} catch (Exception e) {
	e.printStackTrace();
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>zip <%= (ti != null) ? ti.getName() : name %></title>
<style>
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<font color="red">
<pre>
<%= e.getMessage() %>
</pre>
</font>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
<%
}
%>
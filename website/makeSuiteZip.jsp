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
		 org.genepattern.server.genepattern.GenePatternAnalysisTask"
	session="false" contentType="text/html" language="Java" %><% 

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

String contentType = "application/x-zip-compressed" + "; name=\"" + si.getName()+".zip" + "\";";
response.addHeader("Content-Disposition", "attachment; filename=\"" + si.getName()+".zip" + "\";");
response.setContentType(contentType);
      FileInputStream ins = new java.io.FileInputStream(zipFile);
	byte[] buf = new byte[100000];
	int i;
	String s;
	i = ins.read(buf);
	while (i > -1) {
		s = new String(buf, 0, i);
		out.print(s); // copy input file to response
		i = ins.read(buf);
	}
	ins.close();
	ins = null;


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
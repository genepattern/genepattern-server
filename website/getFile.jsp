<% /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ %>
<%@ page import="java.io.File,
		 java.io.*,
		 java.util.Hashtable,
		 org.genepattern.util.LSID,
		 org.genepattern.util.GPConstants,
		org.genepattern.server.webservice.server.DirectoryManager"
	session="false" language="Java" %>
<% 

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String taskName = request.getParameter("task");
if (taskName == null) {
	out.println("no such task: " + taskName);
	return;
}
String filename = request.getParameter("file");
if (filename == null)  {
	out.println("no such file: " + filename);
	return;
}
int i = filename.lastIndexOf(File.separator);
if ((i != -1) && (taskName.trim().length() != 0)) filename = filename.substring(i+1); // disallow absolute paths

String contentType = new File(filename).toURL().openConnection().getFileNameMap().getContentTypeFor(filename);
if (contentType == null) {
	final Hashtable htTypes = new Hashtable();
	htTypes.put(".jar", "application/java-archive");
	htTypes.put(".zip", "application/zip");
	htTypes.put("." + GPConstants.TASK_TYPE_PIPELINE, "text/plain");
	htTypes.put(".class", "application/octet-stream");

	i = filename.lastIndexOf(".");
	String extension = (i > -1 ? filename.substring(i) : "");
	contentType = (String)htTypes.get(extension.toLowerCase());
	if (contentType == null) contentType = "text/plain";
}

File in = null;
try {
	if (taskName.length() > 0) {
		in = new File(DirectoryManager.getTaskLibDir(taskName), filename);
	} else {
		// look in temp for pipelines run without saving
		in = new File(System.getProperty("java.io.tmpdir"), filename);
	}
} catch (Exception e) {
	try {
		in = new File(DirectoryManager.getTaskLibDir(taskName, null, null), filename);
	} catch (Exception e2) {
		out.println("No such task " + taskName);
		return;
	}
}
response.setDateHeader("X-lastModified", in.lastModified());
if (in.exists()) {
	response.setContentType(contentType);
      FileInputStream ins = new java.io.FileInputStream(in);
	int c = 0;
  	while ((c = ins.read()) != -1) {
   		out.write(c);
  	}
	out.flush();


	ins.close();
	ins = null;
} else {
	out.println("no such file: " + in.getPath());
}
return;
%>
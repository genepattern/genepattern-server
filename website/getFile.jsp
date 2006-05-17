<%@ page contentType="application/download" %>
<%@ page import="java.io.File,
		 java.io.*,
		 java.util.Hashtable,
		 org.genepattern.util.LSID,
		 org.genepattern.util.GPConstants,
		org.genepattern.server.webservice.server.DirectoryManager"
	session="false" language="Java" %>
<% 
out.clearBuffer();
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


if (in!=null && in.exists()) {
   // javax.activation.FileTypeMap ftm = new javax.activation.MimetypesFileTypeMap();
   // response.setContentType(ftm.getContentType(filename));
    response.setHeader("Content-Disposition","attachment; filename=" + in.getName() + ";");
    response.setHeader("Content-Type", "application/octet-stream"); 
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);
	response.setDateHeader("X-lastModified", in.lastModified());
    FileInputStream ins = new java.io.FileInputStream(in);
	int c = 0;
	
  	while ((c = ins.read()) >= 0) {
  	  	out.write(c);
  	}
	out.flush();
	ins.close();
	ins = null;
} else {
	out.println("no such file: " + in.getPath());
}
%>
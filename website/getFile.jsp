<%@ page import="java.io.File,
		 java.io.IOException,
		 java.util.Hashtable,
		 org.genepattern.util.LSID,
		 org.genepattern.util.GPConstants,
		org.genepattern.server.webservice.server.DirectoryManager,
		 com.jspsmart.upload.*"
	session="false" language="Java" %><jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" /><% 

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

mySmartUpload.initialize(pageContext);

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
	mySmartUpload.downloadFile(in.getPath(), contentType, in.getName());
} else {
	out.println("no such file: " + in.getPath());
}
return;
%>
<%@ page import="java.util.HashMap,
		 java.util.TreeMap,
		 java.util.Iterator, java.io.File, 
		 org.genepattern.analysis.TaskInfo,
		 org.genepattern.analysis.TaskInfoAttributes,
		 org.genepattern.analysis.OmnigeneException,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask"
	session="false" contentType="text/text" language="Java" %><%

	String userID = GenePatternAnalysisTask.getUserID(request, response); // will force login if necessary
	if (userID == null) return; // come back after login

	String taskName = request.getParameter("name");
	TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, null);
	String libdir = GenePatternAnalysisTask.getTaskLibDir(taskName, (String)taskInfo.getTaskInfoAttributes().get(GPConstants.LSID), userID);
	out.println("libdir="+libdir);
	File[] supportFiles = new File(libdir).listFiles();
	out.print("support.files=");
	for(int i = 0, length = supportFiles.length; i < length; i++ ) {
		if(i > 0) {
			out.print(",");
		}
		out.print(supportFiles[i].getName());
	}
	out.println();
	
	out.print("support.file.dates=");
	for(int i = 0, length = supportFiles.length; i < length; i++ ) {
		if(i > 0) {
			out.print(",");
		}
		out.print(supportFiles[i].lastModified());
	}
	out.println();
%>
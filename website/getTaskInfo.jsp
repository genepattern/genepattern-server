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


<%@ page import="java.util.HashMap,
		 java.util.TreeMap,
		 java.util.Iterator, java.io.File, 
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.util.AccessManager,
org.genepattern.server.webservice.server.DirectoryManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask"
	session="false" contentType="text/text" language="Java" %><%

	String userID= (String)request.getAttribute("userID"); // will force login if necessary
	if (userID == null) return; // come back after login

	String taskName = request.getParameter("name");
	TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, null);
	String libdir = DirectoryManager.getTaskLibDir(taskName, (String)taskInfo.getTaskInfoAttributes().get(GPConstants.LSID), userID);
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
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


<%@ page import="
                 org.apache.commons.fileupload.servlet.ServletRequestContext,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.server.webapp.RunTaskHelper,
                 org.genepattern.util.GPConstants,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.TaskInfo,
                 javax.servlet.RequestDispatcher,
                 java.io.PrintWriter,
                 java.io.File,
                 org.apache.commons.io.FilenameUtils,
              	 java.net.MalformedURLException,
                 java.net.URL,
                 java.util.ArrayList,
                 java.util.HashMap,
                 java.util.Iterator,
                 java.util.List,
                 java.util.Map,
                 java.net.URLDecoder"
         session="false" contentType="text/html" language="Java" %>
<%
    response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
    response.setHeader("Pragma", "no-cache");         // HTTP 1.0 cache control
    response.setDateHeader("Expires", 0);

%>

<html>
<head>
    <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
    <link href="skin/favicon.ico" rel="shortcut icon">
    <title>Running Module</title>
    <jsp:include page="navbarHead.jsp"/>
</head>

<body>

<%
    /**
     * To run a pipeline with prompt when run parameters, we put parameters into the request object.
     * File parameters are stored as FileItems, all other parameters as strings
     */
   	String userID = (String) request.getAttribute(GPConstants.USERID);
	RunTaskHelper runTaskHelper = new RunTaskHelper(userID, request);

	TaskInfo task = runTaskHelper.getTaskInfo();
	if (task == null) {
		out.println("Unable to find module");
		return;
	}

	String lsid = runTaskHelper.getTaskLsid();
	String taskName = runTaskHelper.getTaskName();
	String tmpDirName = runTaskHelper.getTempDirectory().getName();
	Map<String, String> requestParameters = runTaskHelper.getRequestParameters();	
   	
   	ParameterInfo[] parmInfos = task.getParameterInfoArray();
   
    List<ParameterInfo> missingReqParams = runTaskHelper.getMissingParameters();
   	if (missingReqParams.size() > 0) {

%>
<jsp:include page="navbar.jsp"/>
<%
    request.setAttribute("missingReqParams", missingReqParams);
    (request.getRequestDispatcher("runTaskMissingParams.jsp")).include(request, response);
%>
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
		return;
	}
   	request.setAttribute("parameters", parmInfos);
   	request.setAttribute("name", lsid); //used by runPipeline.jsp to get pipeline to run
	RequestDispatcher rd = request.getRequestDispatcher("runPipeline.jsp");
    rd.include(request, response);
%>



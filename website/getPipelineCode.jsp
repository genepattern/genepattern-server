<%@ page import="org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.webapp.*, java.io.*, java.util.zip.*"
	    session="false" language="Java" contentType="text/plain" %><jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" /><%

	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	// given a pipeline name, generate the pipeline R code

	String userID = GenePatternAnalysisTask.getUserID(request, response); // will force login if necessary
//	if (userID == null || userID.length() == 0) return; // come back after login
	String pipelineName = request.getParameter(GPConstants.NAME);
	String language = (request.getParameter("language") != null ? request.getParameter("language") : "R");
	boolean download = (request.getParameter("download") != null); // true to download, false to just get text in browser
	org.genepattern.webservice.TaskInfo taskInfo = null;
	try {
		taskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, userID);
	} catch(Exception e) {
		System.err.println(e.getMessage() + " while getting task from database.");
		e.printStackTrace();
		return;
	}
	java.util.Map tia = taskInfo.getTaskInfoAttributes();
	String serializedModel = (String)tia.get(GPConstants.SERIALIZED_MODEL);
	if(serializedModel==null) {
		response.setContentType("text/html");
	%>
		<html>
		<head>
		<link href="stylesheet.css" rel="stylesheet" type="text/css">
		<link rel="SHORTCUT ICON" href="favicon.ico" >
		<title>GenePattern</title>
		</head>
		<body>	
		<jsp:include page="navbar.jsp"></jsp:include>
		<% 
		out.println("<p>" + taskInfo.getName() + " was created using an older version of GenePattern. Please load the pipeline into the pipeline designer, save it, and then try again.");
		out.println("<p><a href=pipelineDesigner.jsp?name=" + pipelineName + ">Load in pipeline designer</a>");
		return;
	}
	try {
		String code = AbstractPipelineCodeGenerator.getCode(pipelineName, request, language, userID);
		if (download) {
			pipelineName = taskInfo.getName();
			if("Java".equals(language)) {
				pipelineName = pipelineName.substring(0, pipelineName.indexOf(".pipeline")); // remove .pipeline from end of name
				pipelineName = pipelineName.replace('.', '_');
			} 
			if ("MATLAB".equals(language)) {
				response.setContentType("application/octet-stream; name=\"" + pipelineName + ".m\";");
				response.addHeader("Content-Disposition", "attachment; filename=\"" + pipelineName + ".m\";");
			} else {
				response.setContentType("application/octet-stream; name=\"" + pipelineName + "." + language.toLowerCase() + "\";");
				response.addHeader("Content-Disposition", "attachment; filename=\"" + pipelineName + "." + language.toLowerCase() + "\";");
			}
		}
		out.print(code);
	} catch (Exception e) {
		System.err.println(e.getMessage() + " while deserializing pipeline model");
		e.printStackTrace();
	}		
%>
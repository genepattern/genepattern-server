<%@ page import="org.genepattern.server.util.AccessManager,org.genepattern.server.genepattern.GenePatternAnalysisTask, org.genepattern.webservice.TaskInfo,
		 org.genepattern.util.GPConstants,
		 java.util.Map,
		 org.genepattern.server.webapp.*"

	    session="false" language="Java" contentType="text/plain" %><jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" /><%

	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	// given a pipeline name, generate the pipeline R code

	String userID = request.getParameter(GPConstants.USERID); // AccessManager.getUserID(request, response); // will force login if necessary
//	if (userID == null || userID.length() == 0) return; // come back after login
	String pipelineName = request.getParameter(GPConstants.NAME);
	
	try {
		TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(pipelineName, userID);


		Map tia = taskInfo.getTaskInfoAttributes();
		String serializedModel = (String)tia.get(GPConstants.SERIALIZED_MODEL);

		if (request.getParameter("download") != null) {
			response.setContentType("application/octet-stream; name=\"" + pipelineName + ".xml\";");
			response.addHeader("Content-Disposition", "attachment; filename=\"" + pipelineName + ".xml\";");
		}
		out.println(serializedModel );
	} catch (Exception e) {
		System.err.println(e.getMessage() + " while deserializing pipeline model");
		e.printStackTrace();
	}		
	return;
%>
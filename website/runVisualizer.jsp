<%@ page import="org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.analysis.TaskInfo,
		 org.genepattern.analysis.TaskInfoAttributes,
		 org.genepattern.analysis.ParameterInfo,
		 org.genepattern.analysis.ParameterFormatConverter,
		 org.genepattern.visualizer.RunVisualizerConstants,
		 org.genepattern.util.GPConstants,
		 java.io.File" 
   session="false" language="Java" %><%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String userID = GenePatternAnalysisTask.getUserID(request, response); // will force login if necessary
	if (userID == null) return; // come back after login

	String name = request.getParameter(GPConstants.NAME);
	TaskInfo taskInfo = GenePatternAnalysisTask.getTaskInfo(name, userID);
	String message = request.getParameter("message");
   	if (message != null) {
%>
		<html>
		<head>
		<link href="stylesheet.css" rel="stylesheet" type="text/css">
		<link href="favicon.ico" rel="shortcut icon">
		<body>
		<jsp:include page="navbar.jsp"></jsp:include>
		<%= GenePatternAnalysisTask.htmlEncode(message) %><br>
		<br>
<% 		if (taskInfo != null) { %>
			<a href="addTask.jsp?<%= GPConstants.NAME %>=<%= name %>&view=1">view/edit <%= taskInfo.getName() %> task</a><br>
			<a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= name %>">view <%= taskInfo.getName() %> documentation</a><br>
<%		} %>
		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
<%
		return;
	}

	if (name == null) { %>
		must specify task name and input parameters in request
<%
		return;
	}
	if (taskInfo == null) {
%>
		Can't load task info for <%= name %> for user <%= userID %>.
<%
		return;
	}
	name = taskInfo.getName();
	TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
	String lsid = (String)tia.get(GPConstants.LSID);
	String libdir = GenePatternAnalysisTask.getTaskLibDir(lsid);
	ParameterInfo[] parameterInfoArray = new ParameterFormatConverter().getParameterInfoArray(taskInfo.getParameterInfo());
	if (parameterInfoArray == null) parameterInfoArray = new ParameterInfo[0];
	File[] supportFiles = new File(libdir).listFiles();
	int i;
	String appletName = "a" + ("" + Math.random()).substring(2); // unique name so that multiple instances of applet on a single page will not collide
%>
<applet code="<%= org.genepattern.visualizer.RunVisualizerApplet.class.getName() %>" archive="runVisualizer.jar" codebase="downloads" width="1" height="1" alt="Your browser refuses to run applets" name="<%= appletName %>">
<param name="<%= RunVisualizerConstants.NAME %>" value="<%= name %>">

<param name="<%= RunVisualizerConstants.OS %>" value="<%= GenePatternAnalysisTask.htmlEncode(tia.get(GPConstants.OS)) %>">
<param name="<%= RunVisualizerConstants.CPU_TYPE %>" value="<%= GenePatternAnalysisTask.htmlEncode(tia.get(GPConstants.CPU_TYPE)) %>">
<param name="<%= RunVisualizerConstants.LIBDIR %>" value="<%= GenePatternAnalysisTask.htmlEncode(libdir) %>">

<param name="<%= RunVisualizerConstants.PARAM_NAMES %>" value="<%
	for (i = 0; i < parameterInfoArray.length; i++) {
		if (i > 0) out.print(",");
		out.print(GenePatternAnalysisTask.htmlEncode(parameterInfoArray[i].getName()));
	}
%>">
<%	for (i = 0; i < parameterInfoArray.length; i++) {
		String paramName = parameterInfoArray[i].getName();
		if (paramName.equals("className")) { %>
<param name="<%= paramName %>" value="<%= GenePatternAnalysisTask.htmlEncode(parameterInfoArray[i].getDescription()) %>">
<%			continue;
		}
%>
<param name="<%= GenePatternAnalysisTask.htmlEncode(paramName) %>" value="<%= GenePatternAnalysisTask.htmlEncode(request.getParameter(paramName)) %>">
<%	} %>
<param name="<%= RunVisualizerConstants.DOWNLOAD_FILES %>" value="<%
	int numToDownload = 0;
	for (i = 0; i < parameterInfoArray.length; i++) {
		String paramName = parameterInfoArray[i].getName();
		if (parameterInfoArray[i].isInputFile() && 
		    (request.getParameter(paramName).startsWith("http:") ||
		     request.getParameter(paramName).startsWith("https:") ||
		     request.getParameter(paramName).startsWith("ftp:"))) {
			// note that this parameter is a URL that must be downloaded by adding it to the CSV list for the applet
			if (numToDownload > 0) out.print(",");
			out.print(GenePatternAnalysisTask.htmlEncode(parameterInfoArray[i].getName()));
			numToDownload++;
		}
	}
%>">
<param name="<%= RunVisualizerConstants.COMMAND_LINE %>" value="<%= GenePatternAnalysisTask.htmlEncode(tia.get(GPConstants.COMMAND_LINE)) %>">
<param name="<%= RunVisualizerConstants.DEBUG%>" value="1">
<param name="<%= RunVisualizerConstants.SUPPORT_FILE_NAMES %>" value="<%
	for (i = 0; i < supportFiles.length; i++) {
		if (i > 0) out.print(",");
		out.print(GenePatternAnalysisTask.htmlEncode(supportFiles[i].getName()));
	} %>">
<param name="<%= RunVisualizerConstants.SUPPORT_FILE_DATES %>" value="<%
	for (i = 0; i < supportFiles.length; i++) {
		if (i > 0) out.print(",");
		out.print(supportFiles[i].lastModified());
	} %>">
<param name="<%= RunVisualizerConstants.LSID%>" value="<%= lsid %>">
Your browser is ignoring this applet.
</applet>

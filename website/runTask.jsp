<%@ page import="java.io.IOException,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.io.File,
 		 java.net.URLEncoder,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient , 
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.LSID,
		 org.genepattern.util.GPConstants,
		 org.genepattern.webservice.OmnigeneException, 
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String taskName = request.getParameter(GPConstants.NAME);
if (taskName == null || taskName.length() == 0) {
%>
	<html>
	<head>
	<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link rel="SHORTCUT ICON" href="favicon.ico" >
	<title>run GenePattern task</title>
	</head>
	<body>	
	<jsp:include page="navbar.jsp"></jsp:include>
	Must specify task name.<br>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<%
	return;
}
String username = request.getParameter(GPConstants.USERID);
if (username == null || username.length() == 0) {
	username = GenePatternAnalysisTask.getUserID(request, response);
}
boolean bNoEnvelope = (request.getParameter("noEnvelope") != null);

TaskInfo taskInfo = null;
try { taskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, username); } catch (OmnigeneException oe) {}

if (!bNoEnvelope) { %>
	<html>
	<head>
	<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link rel="SHORTCUT ICON" href="favicon.ico" >
	<title>run <%= taskInfo != null ? taskInfo.getName() : "GenePattern task" %></title>
	</head>
	<body>	
	<jsp:include page="navbar.jsp"></jsp:include>
<% } %>

<%
if (taskInfo == null) {
%>
	<script language="javascript">
	alert('No such task <%= taskName %>');
	</script>
	No such task <%= taskName %><br>
	<% if (!bNoEnvelope) { %>
		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
	<% } %>
<%
	return;
}
taskName = taskInfo.getName();
TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
ParameterInfo[] parameterInfoArray = null;
try {
        parameterInfoArray = new ParameterFormatConverter().getParameterInfoArray(taskInfo.getParameterInfo());
	if (parameterInfoArray == null) parameterInfoArray = new ParameterInfo[0];
} catch (OmnigeneException oe) {
}
%>
<table width="100%" cols="2">
<tr><td><b><font size="+1"><%= taskName %></font></b></td>
<%
if (taskName != null) {
	LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(username);

	File[] docFiles = taskIntegratorClient.getDocFiles(taskInfo);
	boolean hasDoc = docFiles != null && docFiles.length > 0;
	if (hasDoc) {
		%><td align="right"><b>Documentation:</b><%
 		for (int i = 0; i < docFiles.length; i++) { %>
			<a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= GenePatternAnalysisTask.htmlEncode(request.getParameter(GPConstants.NAME)) %>&file=<%= URLEncoder.encode(docFiles[i].getName()) %>" target="new"><%= GenePatternAnalysisTask.htmlEncode(docFiles[i].getName()) %></a><% 
 		} 
		%></td><%
 	}
}

String taskType = tia.get("taskType");
boolean isVisualizer = "visualizer".equalsIgnoreCase(taskType);
boolean isPipeline = "pipeline".equalsIgnoreCase(taskType);

String formAction = "runTaskPipeline.jsp";
if (isVisualizer){
	formAction = "preRunVisualizer.jsp";
} else if (isPipeline){
	formAction = "runPromptingPipeline.jsp";
}

%>
</table>

	<form name="pipeline" action="<%=formAction%>" method="post" ENCTYPE="multipart/form-data">
	<input type="hidden" name="taskName" value="<%= taskName %>">
	<input type="hidden" name="taskLSID" value="<%= tia.get(GPConstants.LSID) %>">
	<input type="hidden" name="<%= GPConstants.USERID %>" value="<%= username %>">
	<input type="hidden" name="taskName" value="<%= taskName %>">
	<table cols="2" valign="top" width="100%">
	<col align="right" width="10%"><col align="left" width="*">

<%	
	int numParams = parameterInfoArray.length;

	if (numParams > 0) { %>
		<tr><td align='left' colspan='2'><b>&nbsp;&nbsp;</b></td><td></td></tr>
<%	} else { %>
		<tr><td align='left' colspan='2'><i>has no input parameters</i></td><td></td></tr>
<%	} %>
			<% 	

	String prefix = "";
	for (int param = 0; param < parameterInfoArray.length; param++) {
out.flush();
		ParameterInfo pi = parameterInfoArray[param];
//		if (pi.getName().equals("server") && taskName.endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) continue; // skip server parameter
		HashMap pia = pi.getAttributes();
		String[] choices = null;
		String[] stChoices = pi.getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
		String val = pi.getValue();
		String defaultValue = (request.getParameter(pi.getName()) != null ? request.getParameter(pi.getName()) : (String)pia.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]));
		if (defaultValue != null) defaultValue = defaultValue.trim();
		String description = pi.getDescription();
%>
		<tr>
     		<td align="right" width="10%" valign="top"><nobr><%= pi.getName().replace('.',' ') %>:</nobr></td>
		<td valign="top">
<% 		if (pi.isInputFile()) { %>
			<input	type="file" 
				name="<%= pi.getName() %>" 
				size="60" 
				onchange="this.form.shadow<%= param %>.value=this.value;" 
				onblur="javascript:if (this.value.length > 0) { this.form.shadow<%= param %>.value=this.value; }" 
				ondrop="this.form.shadow<%= param %>.value=this.value;" 
				class="little">
			<br><input name="shadow<%= param %>" 
			 	   type="text" 
				   value="<%= defaultValue == null ? "" : defaultValue %>"
				   readonly 
				   size="130" 
				   tabindex="-1" 
				   class="shadow" 
				   style="{ border-style: none; font-style: italic; font-size=9pt; background-color: transparent }">
<%
			if (description.length() > 0 && !description.equals(pi.getName().replace('.',' '))) {
				out.println("<br>" + GenePatternAnalysisTask.htmlEncode(description));
			}
		} else if (pi.isOutputFile()) {
		} else if (stChoices.length < 2) { %>
			<table align="left"><tr><td valign="top">
			<input name="<%= pi.getName() %>" value="<%=  defaultValue %>">
			</td><%
			if (description.length() > 0) { %>
				<td valign="top"><%= GenePatternAnalysisTask.htmlEncode(description) %></td>
			<% } %>
			</tr></table>
<%		} else { %>
			<table align="left"><tr><td valign="top">
			<select name="<%= pi.getName() %>">
<%
			String display = null;
			String option = null;
			String choice;
			for (int iChoice = 0; iChoice < stChoices.length; iChoice++) {
				choice = stChoices[iChoice];
				int c = choice.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
				if (c == -1) {
					display = choice;
					option = choice;
				} else {
					option = choice.substring(0, c);
					display = choice.substring(c+1);
				}
				display = display.trim();
				option = option.trim();
%>			<option value="<%= option %>"<%= defaultValue.equals(option) || defaultValue.equals(display) ? " selected" : "" %>><%= display %></option>
<%			} %>
			</select>
			</td>
			<td valign="top"><%= GenePatternAnalysisTask.htmlEncode(description) %></td>
			</tr></table>
<%
		}
		out.println("</td></tr>");
	}
%>
	<tr>	
		<td></td>
		<td><input type="submit" name="cmd" value="run"> <input type="button" value="help" onclick="window.open('getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= taskName %>', '_new')"></td>
	</tr>

	</table>
	</form>

<% if (!bNoEnvelope) { %>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>
<% } %>
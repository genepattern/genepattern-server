<%@ page import="java.io.IOException,
		 java.util.StringTokenizer,
		 java.util.Enumeration,
		 java.util.HashMap,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.webservice.OmnigeneException,
		 com.jspsmart.upload.*,
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %>
<%
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);
%>
<jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" />
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link href="favicon.ico" rel="shortcut icon">
	<title>Running Pipeline</title>
</head>
<body>
	<jsp:include page="navbar.jsp"></jsp:include>


<%
//mySmartUpload.initialize(pageContext);
//mySmartUpload.upload();

com.jspsmart.upload.Request requestParameters = null;


//requestParameters = (com.jspsmart.upload.Request)request.getAttribute("smartUpload");

requestParameters = mySmartUpload.getRequest();
String taskName = requestParameters.getParameter(GPConstants.NAME);

if (taskName == null) {
	requestParameters = mySmartUpload.getRequest();
}
taskName = requestParameters.getParameter(GPConstants.NAME);
if (taskName == null) {
	taskName = request.getParameter(GPConstants.NAME);
}


if (taskName == null)
	taskName = requestParameters.getParameter(GPConstants.NAME);


if (taskName == null || taskName.length() == 0) {
	taskName = (String)request.getAttribute("name");
}

if (taskName == null || taskName.length() == 0) {
	out.println("Must specify task name.");
	return;
}
String username = requestParameters.getParameter(GPConstants.USERID);
if (username == null || username.length() == 0) {
	username = GenePatternAnalysisTask.getUserID(request, response);
}
boolean bNoEnvelope = (requestParameters.getParameter("noEnvelope") != null);

int taskNum = 0;

TaskInfo taskInfo = null;
try { taskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, username); } catch (OmnigeneException oe) {}
if (taskInfo == null) {
%>
	<script language="javascript">
	alert('No such task <%= taskName %>');
	</script>
	No such task <%= taskName %><br>
	
		
<%
	return;
}
TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
if (tia.get(GPConstants.TASK_TYPE).equals(GPConstants.TASK_TYPE_VISUALIZER)) {
%>
	Sorry, visualizers cannot be run from the web client.<br>
<%	 if (!bNoEnvelope) { %>
		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
<% 	
	} 
	return;
}

ParameterInfo[] parameterInfoArray = null;
try {
        parameterInfoArray = new ParameterFormatConverter().getParameterInfoArray(taskInfo.getParameterInfo());
	if (parameterInfoArray == null) parameterInfoArray = new ParameterInfo[0];
} catch (OmnigeneException oe) {
} 

%>

<form name="pipeline" action="runPipeline.jsp" method="post" ENCTYPE="multipart/form-data">

	<input type="hidden" name="name" value="<%= taskName %>">
	<input type="hidden" name="t<%= taskNum %>_taskName" value="<%= taskName %>">
	<input type="hidden" name="pipeline_description" value="try running <%= taskName %>">
	<input type="hidden" name="pipeline_author" value="<%= username %>">
	<input type="hidden" name="<%= GPConstants.USERID %>" value="<%= username %>">
	<input type="hidden" name="<%= GPConstants.PRIVACY %>" value="<%= GPConstants.PRIVATE %>">
	<input type="hidden" name="<%= GPConstants.VERSION %>" value="">
	<input type="hidden" name="<%= GPConstants.LANGUAGE %>" value="R">
	<input type="hidden" name="taskName" value="<%= taskName %>">
	<table cols="2" valign="top" width="100%">
	<col align="right" width="10%"><col align="left" width="*">
<% 	
	int numParams = parameterInfoArray.length;
//	if (taskName.endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) numParams--; // skip server parameter	
	if (numParams > 0) { %>
		<tr><td valign="bottom" align="left" colspan="2"><nobr><b><%= taskName %> input parameters</b></td></tr>
<%	} else { 

		request.setAttribute("smartUpload", requestParameters);
		request.setAttribute("name", taskName );
		request.setAttribute("taskInfo", taskInfo );
		request.setAttribute("skipFileUpload", "true");
		request.getRequestDispatcher("runPipeline.jsp").forward(request, response);	
		return;
}

	
	for (int param = 0; param < parameterInfoArray.length; param++) {
		ParameterInfo pi = parameterInfoArray[param];
//		if (pi.getName().equals("server") && taskName.endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) continue; // skip server parameter
		HashMap pia = pi.getAttributes();
		String[] choices = null;
		String[] stChoices = null;
		String val = pi.getValue();
		String defaultValue = (requestParameters.getParameter(pi.getName()) != null ? requestParameters.getParameter(pi.getName()) : (String)pia.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]));
		if (defaultValue != null) defaultValue = defaultValue.trim();
		String description = pi.getDescription();
		stChoices = pi.getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
%>
		<tr><input type="hidden" name="t<%= taskNum %>_prompt_<%= param %>">
		<td align="right" width="10%" valign="top"><nobr><%= pi.getName().replace('.',' ') %>:</nobr></td>
		<td valign="top">
<% 		if (pi.isInputFile()) { %>
			<input	type="file" 
				name="<%= pi.getName() %>" 
				size="60" 
				ondrop="this.form.t<%= taskNum %>_shadow<%= param %>.value=this.value;" 
				class="little">
			<br><input name="t<%= taskNum %>_shadow<%= param %>" 
			 	   type="text" 
				   value="<%= defaultValue %>"
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
			for (int iChoice = 0; iChoice < stChoices.length; iChoice++) {
				String choice = stChoices[iChoice];
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

	</center>
	<jsp:include page="footer.jsp"></jsp:include>
	</body>
	</html>


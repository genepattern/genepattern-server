<%@ page import="java.io.IOException,
		 java.util.Enumeration,
		 java.util.HashMap,
		 org.genepattern.analysis.TaskInfo,
		 org.genepattern.analysis.TaskInfoAttributes,
		 org.genepattern.server.analysis.ParameterFormatConverter,
		 org.genepattern.server.analysis.ParameterInfo,
		 org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.LSID,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.util.OmnigeneException, 
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

int taskNum = 0;
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

	<form name="pipeline" action="makePipeline.jsp" method="post" ENCTYPE="multipart/form-data">
	<input type="hidden" name="pipeline_name" value="try.<%= taskName %>">
	<input type="hidden" name="t<%= taskNum %>_taskName" value="<%= taskName %>">
	<input type="hidden" name="t<%= taskNum %>_taskLSID" value="<%= tia.get(GPConstants.LSID) %>">
	<input type="hidden" name="pipeline_description" value="try running <%= taskName %>">
	<input type="hidden" name="pipeline_author" value="<%= username %>">
	<input type="hidden" name="<%= GPConstants.USERID %>" value="<%= username %>">
	<input type="hidden" name="<%= GPConstants.PRIVACY %>" value="<%= GPConstants.PRIVATE %>">
	<input type="hidden" name="<%= GPConstants.VERSION %>" value="">
	<input type="hidden" name="<%= GPConstants.LANGUAGE %>" value="R">
	<input type="hidden" name="taskName" value="<%= taskName %>">
	<input type="hidden" name="<%= GPConstants.LSID %>" value="">

<%	
	int numParams = parameterInfoArray.length;
//	if (taskName.endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) numParams--; // skip server parameter	
	if (numParams > 0) { %>
		<nobr><b><%= taskName %> input parameters</b>
<%	} else { %>
		<i><%= taskName %> has no input parameters</i>
<%	} %>
	<table cols="2" valign="top" width="100%">
	<col align="right" width="10%"><col align="left" width="*">
<% 	

	String prefix = taskName + (taskNum+1) + ".";
        prefix="t0_";
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
                    <input type="hidden" name="t<%= taskNum %>_AAApromptAAA_<%= param %>">
		<td align="right" width="10%" valign="top"><nobr><%= pi.getName().replace('.',' ') %>:</nobr></td>
		<td valign="top">
<% 		if (pi.isInputFile()) { %>
			<input	type="file" 
				name="t<%= taskNum %>_<%= pi.getName() %>" 
				size="60" 
				onchange="this.form.t<%= taskNum %>_shadow<%= param %>.value=this.value;" 
				onblur="javascript:if (this.value.length > 0) { this.form.t<%= taskNum %>_shadow<%= param %>.value=this.value; }" 
				ondrop="this.form.t<%= taskNum %>_shadow<%= param %>.value=this.value;" 
				class="little">
			<br><input name="t<%= taskNum %>_shadow<%= param %>" 
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
			<input name="t<%= taskNum %>_<%= pi.getName() %>" value="<%=  defaultValue %>">
			</td><%
			if (description.length() > 0) { %>
				<td valign="top"><%= GenePatternAnalysisTask.htmlEncode(description) %></td>
			<% } %>
			</tr></table>
<%		} else { %>
			<table align="left"><tr><td valign="top">
			<select name="t<%= taskNum %>_<%= pi.getName() %>">
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
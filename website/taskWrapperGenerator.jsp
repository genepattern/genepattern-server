<%@ page import="java.io.File,
		 java.io.FilenameFilter,
		 java.util.Collection,
		 java.util.Date,
		 java.util.HashMap,
		 java.util.Iterator,
		 java.util.StringTokenizer,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.ParameterFormatConverter,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.LSID,
		 org.genepattern.server.genepattern.LSIDManager"
	session="false" contentType="text/html" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String userID = request.getParameter(GPConstants.USERID);
if (userID == null) {
	userID = (String)request.getAttribute("userID"); // will force login if necessary
	if (userID == null) return; // come back after login
}
if (request.getParameter("noenvelope") == null) {
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>generated wrapper code for GenePattern</title>
</head>
<body>	
<jsp:include page="navbar.jsp"></jsp:include>
<% } // if noenvelope == null %>

<% 
// check that task name was specified
String name = request.getParameter(GPConstants.NAME);
LSID lsid;
String sLSID;
if (name == null || name.length() == 0) {
	Collection tmTasks = new LocalAdminClient(userID).getTaskCatalog();
%>
	Generate a wrapper for: <br><br><ul>
<%
	String description;
	TaskInfo ti;
	LSIDManager lsidManager = LSIDManager.getInstance();
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		ti = (TaskInfo)itTasks.next();
		description = ti.getDescription();
		sLSID = (String)ti.giveTaskInfoAttributes().get(GPConstants.LSID);
		lsid = new LSID(sLSID);
		out.println("<li><a href=\"taskWrapperGenerator.jsp?" + GPConstants.NAME + "=" + sLSID + "\"><span class=\"tasks-" + lsidManager.getAuthorityType(lsid) + "\">" + ti.getName() + " version " + lsid.getVersion() + " (" + description + ")</span></a></li>");
	}
%>  
	</ul>
<%
	return;
}

TaskInfoAttributes tia = null;
HashMap attributes = null;
String def = null;

try {
	TaskInfo taskInfo = null;
	try {
		taskInfo = GenePatternAnalysisTask.getTaskInfo(name, userID);
		name = taskInfo.getName();
		tia = taskInfo.giveTaskInfoAttributes();
		sLSID = (String)tia.get(GPConstants.LSID);
		lsid = new LSID(sLSID);
	} catch(OmnigeneException e){
            //this is a new task, no taskID exists
            // do nothing
	    out.println("no such task: " + name);
	    return;
        }
        ParameterInfo[] parameterInfo = new ParameterFormatConverter().getParameterInfoArray(taskInfo.getParameterInfo());

%>

Here&apos;s <a href="http://www.r-project.org/" target="R">R</a> wrapper code for the <font color="blue"><b><%= name %></b></font> task.  Copy and paste this into your editor and start
accessing the <font color="blue"><b><%= name %></b></font> service on <%= request.getServerName() %>:<%= request.getServerPort() %> 
right now!<br><br>
<hr>
<h1><font color="blue">R</font></h1>
<pre style="background-color: lightblue">
library(GenePattern)
gpLogin("<%= userID %>");

<%= name %> <-
#
# <%= taskInfo.getDescription() %>
# <%= tia.get(GPConstants.LSID) %>
#
# input parameters:
<% if (parameterInfo != null) {
      String BLANKS = "                                                                             ";
      int longestName = 0;
      for (int i = 0; i < parameterInfo.length; i++) {
      	longestName = Math.max(parameterInfo[i].getName().length(), longestName);
      }
      String blanks = BLANKS.substring(0,Math.min(longestName, BLANKS.length()));
      
      for (int i = 0; i < parameterInfo.length; i++) {
	String valueList = parameterInfo[i].getValue();
	attributes = parameterInfo[i].getAttributes();
	def = null;
%>#	<%= parameterInfo[i].getName().replace('_','.') %>: <%= BLANKS.substring(parameterInfo[i].getName().length(), longestName) %><%= StringUtils.htmlEncode(parameterInfo[i].getDescription()) %>
<%
	if (valueList != null && valueList.length() > 0) {
%>#	  <%= blanks %>choose from the following values: <%= valueList.replace(';',',') %>
<%
        }
	if (attributes != null) {
	    String attributeName = null;
	    for (int attribute = 0; attribute < GPConstants.PARAM_INFO_ATTRIBUTES.length; attribute++) {
	    	attributeName = (String)GPConstants.PARAM_INFO_ATTRIBUTES[attribute][0];
		def = (String)attributes.get(attributeName);
		if (def != null && def.length() > 0) {
		if (attributeName.equals(GPConstants.PARAM_INFO_TYPE[0]) && def.indexOf(".") != -1) def = def.substring(def.lastIndexOf(".")+1);
		if (attributeName.equals(GPConstants.PARAM_INFO_OPTIONAL[0])) def = "true";
%>#	  <%= blanks %><%= attributeName.replace(GPConstants.PARAM_INFO_SPACER, ' ') %>: <%= StringUtils.htmlEncode(def) %>
<%
		}
	    }
	}
//	out.println();
      }
   } else {
%># 	[none] 
<% } %>#
# returns: list of filenames of results file(s)
# created: <%= new Date() %>
# author:  <%= StringUtils.htmlEncode(tia.get(GPConstants.AUTHOR)) %>
#
function(<% 
	if (parameterInfo != null)  {
	      for (int i = 0; i < parameterInfo.length; i++) {
		String valueList = parameterInfo[i].getValue();
		if (i > 0) out.print(", ");
		out.print(parameterInfo[i].getName().replace('_','.'));
		attributes = parameterInfo[i].getAttributes();
// TODO: if not optional and default not set, call askUser()
		if (attributes != null) {
			def = (String)attributes.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
			if (def != null && parameterInfo[i].hasChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER)) {
				String[] stChoices = parameterInfo[i].getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
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
					if (def.equals(display)) {
// System.out.println("taskWrapperGenerator: " + name + " was " + display + " and is now " + option);
						def = option.trim();
						break;
					}
				}
			}

			if (def != null && def.length() > 0) {
				out.print("=\"" + StringUtils.htmlEncode(def) + "\"");
			}
		}
	      }
	}
%>)
{
<% 
	StringBuffer sbOut = new StringBuffer();
	sbOut.append("	return (runAnalysis(taskName=\"" + sLSID + "\"");
	boolean bNeedsQuoting;
	if (parameterInfo != null) {
	      	for (int i = 0; i < parameterInfo.length; i++) {
			sbOut.append(", ");
			bNeedsQuoting = (parameterInfo[i].getName().indexOf("_") != -1);
			if (bNeedsQuoting) sbOut.append("\"");
			sbOut.append(parameterInfo[i].getName());
			if (bNeedsQuoting) sbOut.append("\"");
			sbOut.append("=");
			sbOut.append(parameterInfo[i].getName().replace('_','.'));
			if (sbOut.length() > 70 && i != (parameterInfo.length-1)) {
				out.print(sbOut.toString());
				sbOut.setLength(0);
				out.println("");
				out.print("\t\t\t    ");
			}
	      }
	}
	out.print(sbOut.toString());
%>))
}
</pre>
<br>

or just run it within R without creating a new function by answering the prompts for input values in the script below:<br><br>

<pre style="background-color: lightblue">
library(GenePattern)
gpLogin("<%= userID %>");
runAnalysis(taskName="<%= sLSID %>"<%
	if (parameterInfo != null) {
	      for (int i = 0; i < parameterInfo.length; i++) {
		String valueList = parameterInfo[i].getValue();
		out.print(",\n\t    ");
		bNeedsQuoting = (parameterInfo[i].getName().indexOf("_") != -1);
	        if (parameterInfo[i].getName().indexOf("filename") != -1) {
		    if (bNeedsQuoting) out.print("\"");
		    out.print(parameterInfo[i].getName());
		    if (bNeedsQuoting) out.print("\"");
		    out.print("=askUserFileChoice(\"" + parameterInfo[i].getName() + "\")");
		} else {
		    if (bNeedsQuoting) out.print("\"");
		    out.print(parameterInfo[i].getName());
		    if (bNeedsQuoting) out.print("\"");
		    out.print("=askUser(\"" + parameterInfo[i].getName() + " (" + StringUtils.htmlEncode(parameterInfo[i].getDescription()) + ")\",\"");
		    attributes = parameterInfo[i].getAttributes();
		    if (attributes != null) {
			def = (String)attributes.get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);
			if (def != null && 
			    valueList.indexOf(GPConstants.PARAM_INFO_CHOICE_DELIMITER) != -1 &&
			    valueList.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR) != -1) {
				StringTokenizer stChoices = new StringTokenizer(valueList, GPConstants.PARAM_INFO_CHOICE_DELIMITER);
				String display = null;
				String option = null;
				while (stChoices.hasMoreTokens()) {
					String choice = stChoices.nextToken();
					int c = choice.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
					if (c == -1) {
						display = choice;
						option = choice;
					} else {
						option = choice.substring(0, c);
						display = choice.substring(c+1);
					}
					if (def.equals(display)) {
// System.out.println("taskWrapperGenerator: " + name + " was " + display + " and is now " + option);
						def = option.trim();
						break;
					}
				}
			}
			if (def != null && def.length() > 0) {
				out.print(StringUtils.htmlEncode(def));
			}
		    }
		    out.print("\")");
		}
	      }
	}
%>);
</pre>
<br>
<hr>
Bindings are also available for Java and MATLAB.  To use them, generate a single-stage pipeline that invokes <%= taskInfo.getName() %>, save it, then download the pipeline code from the <a href="index.jsp">index.jsp</a> page.<br>
<br>
You may regenerate this code anytime with the following URL: 
<a href="taskWrapperGenerator.jsp?<%= GPConstants.NAME %>=<%= StringUtils.htmlEncode(request.getParameter(GPConstants.NAME)) %>" target="<%= StringUtils.htmlEncode(name) %>_wrapper">
<nobr><%=request.getScheme()%>://<%= request.getServerName() %>:<%= request.getServerPort() %>/<%=request.getContextPath()%>/taskWrapperGenerator.jsp?<%= GPConstants.NAME %>=<%= StringUtils.htmlEncode(request.getParameter(GPConstants.NAME)) %></nobr></a><br>

<% 
File[] docFiles = new LocalTaskIntegratorClient(userID, out).getDocFiles(taskInfo);
if (docFiles != null && docFiles.length > 0) { %>
<br>Read <%= name %> documentation: 
<% for (int i = 0; i < docFiles.length; i++) { %>
<a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= StringUtils.htmlEncode(request.getParameter(GPConstants.NAME)) %>&file=<%= StringUtils.htmlEncode(docFiles[i].getName()) %>"><%= docFiles[i].getName() %></a> 
<% } %>
<br>
<% } %>

<br>
<a href="addTask.jsp?<%= GPConstants.NAME %>=<%= StringUtils.htmlEncode(request.getParameter(GPConstants.NAME)) %>">edit <%= taskInfo.getName() %> task</a><br>
<% if (tia != null) { %>
<a href="makeZip.jsp?<%= GPConstants.NAME %>=<%= StringUtils.htmlEncode(request.getParameter(GPConstants.NAME)) %>&includeDependents=1">package <%= taskInfo.getName() %> into a zip file</a><br>
<% } %>
<%
} catch (Exception e) {
	out.println("code generation for " + request.getParameter(request.getParameter(GPConstants.NAME)) + " task failed: <br>");
	out.println(e.getMessage());
	out.println("<br><pre>");
	e.printStackTrace();
	out.println("</pre><br>");
}
%>    
<% if (request.getParameter("noenvelope") == null) { %>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
<% } %>

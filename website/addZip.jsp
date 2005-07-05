<%@ page import="org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 java.net.URLEncoder,
		 java.text.DateFormat,
		 java.text.NumberFormat,
		 java.text.SimpleDateFormat,
		 java.util.Date,
		 java.util.Iterator,
		 java.util.Properties,
		 java.util.TreeMap" 
	contentType="text/html" session="false" language="Java" %>
<% boolean ENABLE_SOURCEFORGE = false; %>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>install GenePattern analysis task zip file</title>
<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<h2>Install analysis task from zip file</h2>
Please choose a GenePattern zip file to submit a new or updated analysis task to the GenePattern server.<br><br>

<table cols="4" width="100%">
<tr>
<td valign="top" colspan="4">
<form name="task" action="installZip.jsp" method="post" ENCTYPE="multipart/form-data">
<b>GenePattern zip file:</b>&nbsp;
<input type="file" name="file1" size="50" accept="application/zip">
<input type="checkbox" name="<%= GPConstants.PRIVACY %>" checked><%= GPConstants.PRIVATE %>
<br><br>
<center>
<input type="submit" value="install" name="install">&nbsp;&nbsp;
</center>
</form>
</td>
</tr>

<% String taskDetailItems[][]  = new String[][] {  { "size", "download<br>size (bytes)" },
						{ "uploaded", "upload<br>date" },
					        { GPConstants.TASK_TYPE, "task type" },
					        { GPConstants.CPU_TYPE, "CPU type" },
					        { GPConstants.OS, "OS" }, 
					        { GPConstants.JVM_LEVEL, "Java JVM level" },
					        { GPConstants.LANGUAGE, "language" }
					 };
%>
<% if (ENABLE_SOURCEFORGE) { // turn off SourceForge!!! %>

<tr><td colspan="4"><hr></td></tr>
<%
TreeMap tmTasks = GenePatternAnalysisTask.getSourceForgeTasks("genepattern", ".zip");
int i;
if (tmTasks.size() > 0) {
	DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
	NumberFormat nf = NumberFormat.getInstance();
%>
<tr><td colspan="*">
	<center><a href="http://sourceforge.net/projects/genepattern/files" target="_new">
	<h3>SourceForge GenePattern download catalog</h3>
	</a></center>
</td></tr>
<tr><td colspan="*">
<table cols="<%= 3 + taskDetailItems.length %>" width="100%">
<tr>
	<td valign="bottom"><b>name</b></td>
	<td valign="bottom" width="150"><b>description</b></td>
	<td valign="bottom"><b>action</b></td>
<% for (i = 0; i < taskDetailItems.length; i++) { %>
	<td valign="bottom" align="center"><b><%= taskDetailItems[i][1] %></b></td>
<% } %>
</tr>
<% out.flush(); %>
<tr><td colspan="<%= 3 + taskDetailItems.length %>"><hr height="1"></td></tr>
<%
	Date dFile = null;
	String key = null;
	String href = null;
	String fileName = null;
	String fileSize = null;
	Properties props = null;
	String DATE_FORMAT = "yyyy-MM-dd HH:mm";
	for (Iterator itTasks = tmTasks.keySet().iterator(); itTasks.hasNext(); ) {
		key = (String)itTasks.next();
		href = (String)tmTasks.get(key);
		i = key.indexOf(",");
		fileName = key.substring(0, i);
		dFile = new SimpleDateFormat(DATE_FORMAT).parse(key.substring(i+1, i+1+DATE_FORMAT.length()));
		fileSize = key.substring(i+1+1+DATE_FORMAT.length());
		
		props = GenePatternAnalysisTask.getPropsFromZipURL(href);
		props.setProperty("uploaded", df.format(new Date(Long.parseLong(props.getProperty("created")))).toLowerCase());
		props.setProperty("size", "<p align=\"right\">" + props.getProperty("size") + "</p>");
%>
<tr>
	<td width="30" valign="top"><a href="<%= href %>"><%= props.getProperty(GPConstants.NAME, "-") %></a></td>
	<td width="150" valign="top"><%= props.getProperty(GPConstants.DESCRIPTION, "-") %></td>
	<td width="30" valign="top"><a href="installZip.jsp?url=<%= URLEncoder.encode(href) %>" target="_new">install</a>/<br>
				    <a href="viewZip.jsp?url=<%= URLEncoder.encode(href) %>">details</a></td>
<% for (i = 0; i < taskDetailItems.length; i++) { %>
	<td width="35" valign="top"><%= props.getProperty(taskDetailItems[i][0], "<center>-</center>") %></td>
<% } %>
</tr>
<% out.flush(); %>
<% } %>
	</td></tr><tr><td> </td></tr><tr><td> </td></tr>
<% } %>

<% } // end of SourceForge conditional code %>


<tr><td colspan="<%= 3 + taskDetailItems.length %>"><br><hr><br>
<a href="taskCatalog.jsp">Get the catalog of new and updated tasks from the GenePattern public website</a></td></tr>

<tr><td colspan="<%= 3 + taskDetailItems.length %>"><br><hr><br>
Or download the task from a website:</td></tr>
<tr><td valign="top" colspan="<%= 3 + taskDetailItems.length %>">
<form name="taskURL" action="installZip.jsp" method="post" ENCTYPE="multipart/form-data">
<b>URL of a GenePattern zip file:</b>
<input type="text" name="url" size="50" value="http://">
&nbsp;&nbsp;
<input type="checkbox" name="<%= GPConstants.PRIVACY %>" checked><%= GPConstants.PRIVATE %>
<br><br>
<center>
<input type="submit" value="install" name="save">&nbsp;&nbsp;
<input type="button" value="help" onclick="window.open('help.jsp', 'help')">
</center>
</form>
</td></tr>
</table>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

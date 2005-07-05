<%@ page session="false" contentType="text/html" language="Java" %>
<%@ page import="org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 java.net.URLEncoder,
		 java.util.zip.*,
		 java.io.*,
		 java.net.URL,
		 java.text.NumberFormat,
		 java.util.Date,
		 java.util.Properties" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String url = request.getParameter("url");
	Properties props = null;
	try {
		props = GenePatternAnalysisTask.getPropsFromZipURL(url);
		if (props == null) throw new IOException("Couldn't load task information from " + url +".  Probably not a GenePattern zip file.");
	} catch (IOException ioe) {
		out.println("<html><body><pre>");
		out.println(ioe.getMessage());
		out.println("</pre></body></html>");
		return;
	}
	NumberFormat nf = NumberFormat.getInstance();
	String taskDetailItems[][]  = new String[][] {  { GPConstants.NAME, "name" },
						        { GPConstants.DESCRIPTION, "description" },
						        { GPConstants.TASK_TYPE, "task type" },
						        { GPConstants.CPU_TYPE, "CPU type" },
						        { GPConstants.OS, "OS" }, 
						        { GPConstants.JVM_LEVEL, "Java JVM level" },
						        { GPConstants.LANGUAGE, "language" }
						 };
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title><%= props.getProperty(GPConstants.NAME) %> task details</title>
<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<h2><%= props.getProperty(GPConstants.NAME) %> task details</h2>
<h3>Source: <a href="<%= url %>"><%= url %></a></h3>
download size: <%= nf.format(Long.parseLong(props.getProperty("size"))) %> bytes, uploaded <%= new Date(Long.parseLong(props.getProperty("created"))) %>
<br><h3>Task details:</h3>
<table cols="2">
<% for (int i = 0; i < taskDetailItems.length; i++) { %>
    <tr>
	<td valign="top" align="right" width="50"><b><nobr><%= taskDetailItems[i][1] %>:</nobr></b></td>
	<td valign="top"><%= props.getProperty(taskDetailItems[i][0], "[not specified]") %></td>
    </tr>
<% } %>
<tr><td valign="top" colspan="2">
<br><h3>Input parameters:</h3>
</td></tr>

<%	for (int i = 1; props.getProperty("p" + i + "_name", null) != null; i++) { %>	
	<tr><td valign="top" align="right"><b><%= props.getProperty("p" + i + "_name") %>:</b></td><td valign="top"><%= props.getProperty("p" + i + "_description") %></td></tr>
<%	} %>
</table>
<br>
<form method="get" action="installZip.jsp">
<input type="hidden" name="url" value="<%= url %>">
<input type="submit" value="install <%= props.getProperty("name") %> on GenePattern server <%= request.getServerName() %>:<%= request.getServerPort() %>">
</form>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

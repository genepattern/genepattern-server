<%@ page contentType="text/html" language="Java" import="java.net.URLEncoder" %>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>GenePattern configuration</title>
</head>
<body>
<base target="aStep">
<b>Configure your
<img src="skin/HeaderLogo.jpg" width="115" height="17" border="0" alt="GenePattern logo" />
server:</b>
<ol>
<li><a href="installLog.jsp">View the server installation report</a><br><br></li>
<li><a href="taskCatalog.jsp?initialInstall=1">Install modules</a> from among those at the Broad public website.
	If you want to install all available modules, click the <b>install checked</b> button.<br><br></li>
<li><a href="<%= System.getProperty("JavaGEInstallerURL") %>?version=<%= System.getProperty("GenePatternVersion") %>&server=<%= URLEncoder.encode("http://" + request.getServerName() + ":" + request.getServerPort()) %>">Install the Graphical Client</a><br><br></li>
<li><a href="installDatasets.htm">Download sample datasets</a><br><br></li>
<li><a href="login.jsp?referrer=index.jsp" target="_parent">Login and begin using GenePattern</a></li>
</ol>

Problems? Questions?  Suggestions? <a href="mailto:gp-help@broad.mit.edu">Contact us</a>.
</body>
</html>

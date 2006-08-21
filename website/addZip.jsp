<!-- /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ -->


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
<title>install GenePattern zip file</title>
<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
</head>
<body>
<jsp:include page="navbar.jsp"/>
<h2>Install from zip file</h2>
Please choose a GenePattern zip file to submit to the GenePattern server.<br><br>

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


<tr><td colspan="<%= 3 + taskDetailItems.length %>"><br><hr><br>
<a href="taskCatalog.jsp">Install tasks from module repository</a></td></tr>

<tr><td colspan="<%= 3 + taskDetailItems.length %>"><br><hr><br>
Or download the zip file from a website:</td></tr>
<tr><td valign="top" colspan="<%= 3 + taskDetailItems.length %>">
<form name="taskURL" action="installZip.jsp" method="post" ENCTYPE="multipart/form-data">
<b>URL of a GenePattern zip file:</b>
<input type="text" name="url" size="50" value="http://">
&nbsp;&nbsp;
<input type="checkbox" name="<%= GPConstants.PRIVACY %>" checked><%= GPConstants.PRIVATE %>
<br><br>
<center>
<input type="submit" value="install" name="save">&nbsp;&nbsp;
</center>
</form>
</td></tr>
</table>

<jsp:include page="footer.jsp"/>
</body>
</html>

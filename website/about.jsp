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


<%@ page session="false" contentType="text/html" language="Java" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>

<title>about <%=messages.get("ApplicationName")%></title>
</head>
<body>	
<jsp:include page="navbar.jsp"></jsp:include>

<img src="skin/logoBig.jpg" width="460" height="68" border="0" alt="<%=messages.get("ApplicationName")%> logo" /><br><br>
<%
 	int major    = (int)Long.parseLong(System.getProperty("version.major"));
	String minor = System.getProperty("version.minor");
	int revision = (int)Long.parseLong(System.getProperty("version.revision"));
 	String release  = System.getProperty("release");
	final String rev = ( revision > 0 ) ? "."+revision : "";
	String full     = major+"."+minor+rev+release;

%>
You are using 
<b><%=messages.get("ApplicationNameColored")%> <font color="blue"><%= full %></font></b> 
<br>build: <%= System.getProperty("build.tag") %> 
<br>built: <%= System.getProperty("date") %>
<br>
<%
String appName = messages.getProperty("ApplicationName");
if (!(appName.indexOf("GenePattern") >= 0)) {
	%>
	<br>
		<i>Powered by</i> <font color="firebrick">Gene</font><font color="blue">Pattern</font> &copy; 2003-2005 <a href="http://www.broad.mit.edu">Broad Institute, MIT</a>


	<%
}

%>

<br>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

<%@ page session="false" contentType="text/html" language="Java" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link href="favicon.ico" rel="shortcut icon">
<title>about GenePattern</title>
</head>
<body>	
<jsp:include page="navbar.jsp"></jsp:include>
<img src="GenePatternLogo.jpg" width="460" height="68" border="0" alt="GenePattern logo" /><br><br>
<%
 	int major    = (int)Long.parseLong(System.getProperty("version.major"));
	String minor = System.getProperty("version.minor");
	int revision = (int)Long.parseLong(System.getProperty("version.revision"));
 	String release  = System.getProperty("release");
	final String rev = ( revision > 0 ) ? "."+revision : "";
	String full     = major+"."+minor+rev+release;


%>
You are using 
<b><font color="firebrick">Gene</font><font color="blue">Pattern <%= full %></b></font> 
<br>build: <%= System.getProperty("build.tag") %> 
<br>built: <%= System.getProperty("date") %>
<br><br>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

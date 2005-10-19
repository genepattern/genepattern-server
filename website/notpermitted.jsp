<%@ page session="false" contentType="text/html" language="Java" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String message = request.getParameter("message");
	String action = request.getParameter("action");
	String link = request.getParameter("link");


%>

<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>GenePattern</title>
<style>
.heading { font-family: Arial,Helvetica,sans-serif; background-color: #0E0166; color: white; font-size: 12pt; font-weight: 800; text-align: center; }
.majorCell { border-width: 2; font-size: 10pt; }
.button  { width: 50; }
.wideButton  { width: 80; }
td { padding-left: 5; }
</style>

</head>
<body>

	<table width="100%" class="navbar">
	<tr>
		<td  valign="top">
			<a href="index.jsp" class='logo'><img src="skin/logoSmall.gif" border="0" alt="GenePattern" align="texttop"> &nbsp;<font size=+2 color='white'><b>Gene</b>Pattern</font></a>
		</td>
		<td align="right"  valign="top">
					</td>
	</tr>
<tr><td><pre>   </pre></td></tr>

</table>
<table width="100%" >
	
	<tr><td><p>
<h3>You do not have permission to perform the action you requested on this GenePattern server.</h3>
<br>
<%=link%>
<br>
<h3>Contact the owner of this GenePattern server to request permisison.<h3>

	</td></tr><tr><td><p></td></tr>
	</table>
	<div class="navbar">
<table class="navbar" width="100%">
	<tr>
		<td align="left" valign="top">
			
			<img src="skin/broadLogo.gif" width=16 height=16 border="1"> &copy; 2003-2005 <a href="http://www.broad.mit.edu" class='navbarlink'>Broad Institute, MIT</a>
		</td>
		<td  align="right" valign="top">
			<a href="mailto:gp-help@broad.mit.edu" class='navbarlink'><nobr>report bugs</nobr></a> |
			<a href="mailto:gp-help@broad.mit.edu" class='navbarlink'><nobr>request help</nobr></a>
		</td>
	</tr>
</table>
</div>
</body>
</html>
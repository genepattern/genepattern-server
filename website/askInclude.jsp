<%@ page session="false" contentType="text/html" language="Java" %>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

String title = request.getParameter("title");
%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>include all tasks used by <%= title %></title>
<script language="Javascript">
// submit and close
function doSubmit(btn) {
	window.opener.location = "makeZip.jsp?name=<%= request.getParameter("name") %>" + 
				 (btn.name == "includeDependents" ? "&includeDependents=1" : "") + 
				 "&close=1";
	window.close();
	return false;
}
</script>
</head>
<body>	
<form name="ask">

Press 'Include tasks' to include all tasks used by <%= title %> in the exported zip file.  <br><br>
Press 'Pipeline only' to include only the <%= title %> pipeline definition itself<br>

<br>
<center>
<input type="button" name="includeDependents" value="Include tasks" onclick="doSubmit(this)" class="little">
<input type="button" name="dontIncludeDependents" value="Pipeline only" onclick="doSubmit(this)" class="little">
<br>
<input type="button" name="cancel" value="Cancel export" onclick="javascript:window.close()" class="little">
</center>
<input type="hidden" name="name" value="<%= request.getParameter("name") %>">
</form>
<script language="Javascript">
document.forms['ask'].target = window.opener;
</script>
</body>
</html>
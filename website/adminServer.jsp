<%@ page session="false" contentType="text/html" language="Java" 
		import="java.util.*,
		java.io.*"
%>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String allowedClients = request.getParameter("allowed.clients");
	String any = request.getParameter("any");
	String localhost = request.getParameter("localhost");

System.out.println("A=" + any + "  LH= " + localhost);

	boolean storeSuccess = true;
	if (allowedClients != null){
		System.setProperty("gp.allowed.clients", allowedClients);
	
		String dir = System.getProperty("genepattern.properties");
		File propFile = new File(dir, "genepattern.properties");
		FileInputStream fis = null;
		FileOutputStream fos = null;
		Properties props = new Properties();
		try {
			fis = new FileInputStream(propFile);
			props.load(fis);
			fis.close();
			fis = null;
			props.setProperty("gp.allowed.clients", allowedClients);
			fos = new FileOutputStream(propFile);
			props.store(fos, "#Genepattern server updated allowed clients list");
			fos.close();			
			fos = null;
		} catch (Exception e){
			// XXX cannot load/save gp props
			if (fis != null) fis.close();
			if (fos != null) fos.close();
			storeSuccess = false;

		}

	} 

%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>GenePattern Server Administration</title>
<style>
.heading { font-family: Arial,Helvetica,sans-serif; background-color: #0E0166; color: white; font-size: 12pt; font-weight: 800; text-align: center; }
.majorCell { border-width: 2; font-size: 10pt; }
.button  { width: 50; }
.wideButton  { width: 80; }
td { padding-left: 5; }
</style>

<script>
function afocus(fld, focus) {
    var deflt = "any";
    if (focus) {
	if (fld.value == deflt) { 
		fld.value = "";
	} else { 
		fld.select();
	}
    } else {
	if (fld.value == "") { 
		fld.value = deflt;
	}
    }
}
function toggle(visible) {
		helpObj = document.getElementById('help');
		showObj = document.getElementById('showhelp');
		if(!visible) {
			helpObj .style.display = "none";
			showObj .style.display = "block";
		} else {
			helpObj .style.display = "block";
			showObj .style.display = "none";
		}
	
}


</script>
</head>
<body>	
<jsp:include page="navbar.jsp"></jsp:include>


<table class="majorCell" width="100%" class="navbar">
		<tr>
		<td class="heading" colspan="2">GenePattern Server Administration</td>
		</tr>
		<tr><td height="3"></td></tr>
		

		
		<tr>
			<td valign="middle" align="right" width="30%">
				Logs
			</td>
			<td valign="top" align="left">
<table cellpadding="0" cellspacing="0">
<tr><td halign="left">
				<input type="button" value="GenePattern" class="wideButton" onclick="javascript:window.location='tomcatLog.jsp'"></td></tr>
<% if (System.getProperty("serverInfo").indexOf("Apache Tomcat") != -1) { %>
				<tr><td><input type="button" value="web server" class="wideButton" onclick="javascript:window.location='tomcatLog.jsp?tomcat=1'"></td></tr>
<% } %>
</table>
			</td>
		</tr>
		<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>
		<tr>
			<td valign="top" align="right" width="30%">
				<br>Allow Clients to connect from:
			</td>
			<td valign="top" align="left">
<% if (!storeSuccess ) { %>

	<h3>Warning:</h3>Unable to store allowed clients update.  
	The change will remain in effect only until server restart. 
	Edit the genepattern.properties file directly if this problem repeats.
<% } %>		

<table cellpadding="0" cellspacing="0" border="0">
<tr><td halign="left" width="30%"><form action="adminServer.jsp" name="allowedClientForm" method="POST">
<input type="hidden" name="allowed.clients" value="This computer"/>
<input type="button" name="localhost" value="This computer" class="wideButton" onclick="this.form.submit()">
</form></td><td halign="left">
<form action="adminServer.jsp" name="allowedClientForm" method="POST">
<input type="hidden" name="allowed.clients" value="Any computer"/>
<input type="button" name="any" value="Any computer" class="wideButton" onclick="this.form.submit()">
</form><td>
</tr><tr><td colspan="2">
<form action="adminServer.jsp" name="allowedClientForm" method="POST">


		
<% if (System.getProperty("gp.allowed.clients") != null) { %>
			<input type="text"  name="allowed.clients" size="40" value="<%=System.getProperty("gp.allowed.clients")%>" onfocus="ufocus(this, true, 'Any computer')" onblur="ufocus(this, false, 'Any computer')">
<% } else { %>
			<input type="text"  name="allowed.clients" size="40" value="any" onfocus="ufocus(this, true, 'Any computer')" onblur="ufocus(this, false, 'Any computer')">
<% } %>
<input type="button" value="set" class="button" onclick="this.form.submit()"><br>

<div id="showhelp" style="display">
<input type="button" value="show help" class="wideButton" onclick="toggle(true)">
</div>
<div id="help" style="display:none;">
<input type="button" value="hide help" class="wideButton" onclick="toggle(false)"><br>
<font color="darkred">

<i>List fragments of host names or IP addresses to be allowed to connect to this GenePattern server as a comma delimited string. 
Computers who's names or addresses include any of the fragments will be allowed to connect. Other computers will be redirected to 
<a href="notallowed.html">notallowed.html</a>. 

<p>
GenePattern Clients who are not allowed will see a "403 Forbidden" error when they attempt to connect. 
<p>
e.g. <font color="red">"broad.mit.edu,wi.mit.edu"</font> would allow any computer from Broad or Whitehead to connect to this GenePattern server.</i>
</font>
</div>

</form>
</td></tr>
</table>
			</td>
		</tr>
<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>

<tr>
			<td valign="middle" align="middle" colspan="2">
<table width="60%><tr><td align="left">
				<a href="shutdownServer.jsp">Shutdown server</a>
</td></tr></table>
			</td>
		</tr>


	</table> <!-- end admin cell -->





<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

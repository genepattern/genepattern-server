<%@ page session="false" contentType="text/html" language="Java" 
		import="java.util.*,
		java.io.*"
%>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String allowedClients = null;
	String clientMode= request.getParameter("clientMode");
	String ANY= "Any computer";
	String LOCAL = "This Computer";	

	if (LOCAL.equals(clientMode)){
		allowedClients = LOCAL;
	} else if (ANY.equals(clientMode)){
		allowedClients = ANY;
	} else if ("specified".equals(clientMode)){
		allowedClients  = request.getParameter("allowed.clients");
	}



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
	allowedClients = System.getProperty("gp.allowed.clients" );
	if (allowedClients == null) allowedClients = ANY;
	

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
var oldClientList;

function clearField(obj) {
	oldClientList = obj.form.allowed_clients.value;
	obj.form.submit.disabled=false;
    	obj.form.allowed_clients.value="";
 }

function refillField(obj) {
	obj.form.allowed_clients.value=oldClientList;
	changeFields(obj);
    
 }

function changeFields(obj){
	obj.form.submit.disabled=(obj.form.allowed_clients.value == "<%=allowedClients%>")  
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
<tr><td colspan="2">
<form action="adminServer.jsp" name="allowedClientForm" method="POST">

<input type="radio" name="clientMode" value="<%=LOCAL%>" 
<%
	if (LOCAL.equals(allowedClients)) out.print(" checked='true' ");
%>
onclick="clearField(this);"> Standalone (local connections only)<br>

<input type="radio" name="clientMode" value="<%=ANY%>" 
<%
if (ANY.equals(allowedClients))  out.print(" checked='true' ");
%>
onclick="clearField(this);"> Any Computer <br>
<input type="radio" name="clientMode" value="specified" 
<%
if (!(ANY.equals(allowedClients) || LOCAL.equals(allowedClients)) ) out.print(" checked='true' ");
%>
onclick="refillField(this);"> These Domains <br>
		


<% 
	String displayVal = "";
	if (!(ANY.equals(allowedClients) || LOCAL.equals(allowedClients)) ) displayVal = allowedClients;
%>
<input type="text"  name="allowed_clients" size="40" value="<%=displayVal%>" onkeyup="changeFields(this)" >
<input type="button" name="submit" value="submit" class="button" onclick="this.form.submit()" disabled="true"><br>






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
			<td valign="middle" al colspan="2">
<table width="60%><tr><td align="right">
				<a href="shutdownServer.jsp">Shutdown server</a>
</td></tr></table>
			</td>
		</tr>


	</table> <!-- end admin cell -->





<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

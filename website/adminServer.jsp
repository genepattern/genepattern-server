<%@ page session="false" contentType="text/html" language="Java" 
		import="java.util.*,
		org.genepattern.server.util.PropertiesManager, 
		java.io.*"
%>
<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	String allowedClients = null;
	String clientMode= request.getParameter("clientMode");
	String proxyHost= request.getParameter("proxyHost");
	String proxyPort= request.getParameter("proxyPort");

System.out.println("Here..." + clientMode + "=" + proxyHost + "=" + proxyPort);

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
		storeSuccess  = PropertiesManager.storeChange("gp.allowed.clients", allowedClients);
	} 
	allowedClients = System.getProperty("gp.allowed.clients" );
	if (allowedClients == null) allowedClients = ANY;


	if (proxyHost!= null){
		storeSuccess  = PropertiesManager.storeChange("http.proxyHost", proxyHost);
	} 
	proxyHost= System.getProperty("http.proxyHost" );
	if (proxyHost== null) proxyHost= "";


	if (proxyPort!= null){
		storeSuccess  = PropertiesManager.storeChange("http.proxyPort", proxyPort);
	} 
	proxyPort= System.getProperty("proxyPort" );
	if (proxyPort== null) proxyPort= "";




	

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

function changeProxyFields(obj){
	obj.form.submitProxy.disabled=((obj.form.proxyHost.value == "<%=proxyHost%>")  && (obj.form.proxyPort.value == "<%=proxyPort%>")) 
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
<input type="submit" name="submit" value="submit" class="button"  disabled="true"><br>

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
<td align='right'>
<table>
<tr><td align='right'>HTTP Proxy Host:</td></tr>
<tr><td align='right'>HTTP Proxy Port:</td></tr>
<tr><td>&nbsp;</td></tr>
</table>

<td>
<form action="adminServer.jsp" name="proxySettingsForm" method="POST">
<table>
<tr><td><input type='text' size='40' name="proxyHost" value='<%=proxyHost%>' onkeyup="changeProxyFields(this)"/></td></tr>
<tr><td><input type='text' size='40' name="proxyPort" value='<%=proxyPort%>' onkeyup="changeProxyFields(this)"/></td></tr>
<tr><td><input type="submit" name="submitProxy" value="submit" class="button"  disabled="true"></td></tr>
</table>
</form>
</td>
</tr>
<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>

<tr>
			<td valign="middle" al colspan="2">
				<a href="shutdownServer.jsp">Shutdown server</a>
			</td>
		</tr>


	</table> <!-- end admin cell -->





<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

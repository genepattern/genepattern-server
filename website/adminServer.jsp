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

	String proxyUser= request.getParameter("proxyUser");
	String proxyPass= request.getParameter("proxyPass");

	String moduleRepository = request.getParameter("moduleRepository");
	String reposDefault = request.getParameter("submitReposDefault");
	String clearProxy = request.getParameter("submitClearProxy");
	String defaultModuleRepository = System.getProperty("DefaultModuleRepositoryURL", moduleRepository);
	String recentHistorySize = request.getParameter("historySize");
	
	if (reposDefault != null){
		moduleRepository = defaultModuleRepository ;
	}

	String ANY= "Any computer";
	String LOCAL = "This Computer";	
	if (LOCAL.equals(clientMode)){
		allowedClients = LOCAL;
	} else if (ANY.equals(clientMode)){
		allowedClients = ANY;
	} else if ("specified".equals(clientMode)){
		allowedClients  = request.getParameter("allowed_clients");
	}

	boolean storeSuccess = true;
	if (allowedClients != null){
		storeSuccess  = PropertiesManager.storeChange("gp.allowed.clients", allowedClients);
	} 
	allowedClients = System.getProperty("gp.allowed.clients" );
	if (allowedClients == null) allowedClients = ANY;


	if ((proxyHost!= null) || (proxyPort!= null)){
		Properties p = new Properties();
		if (proxyPort!= null) p.setProperty("http.proxyPort", proxyPort);
		if (proxyHost!= null) p.setProperty("http.proxyHost", proxyHost);

		storeSuccess  = PropertiesManager.storeChanges(p);
	} 

	if (clearProxy != null){
		Vector vec = new Vector();
		vec.add("http.proxyPort");
		vec.add("http.proxyHost");
		vec.add("http.proxyUser");
		vec.add("http.proxyPassword");

		storeSuccess  = PropertiesManager.removeProperties(vec);
	}
	proxyHost= System.getProperty("http.proxyHost" );
	if (proxyHost== null) proxyHost= "";
	proxyPort= System.getProperty("http.proxyPort" );
	if (proxyPort== null) proxyPort= "";


	if (moduleRepository != null){
		storeSuccess  = PropertiesManager.storeChange("ModuleRepositoryURL", moduleRepository );
	} 
	moduleRepository = System.getProperty("ModuleRepositoryURL","" );
	

	if (recentHistorySize != null){
		storeSuccess  = PropertiesManager.storeChange("recentJobsToDisplay", recentHistorySize );
	} 
	recentHistorySize = System.getProperty("recentJobsToDisplay", "3" );
	


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

	obj.form.submitClearProxy.disabled = ((obj.form.proxyHost.value.length + obj.form.proxyPort.value.length ) == 0)  
}

function changeReposFields(obj){
	obj.form.submitRepos.disabled = (obj.form.moduleRepository.value == "<%=moduleRepository%>")
	obj.form.submitReposDefault.disabled = (obj.form.moduleRepository.value == "<%=defaultModuleRepository%>")
}

function changeHistoryField(obj){

	if ((obj.form.historySize.value != parseInt(obj.form.historySize.value)) || (obj.form.historySize.value == <%=recentHistorySize %>)) {
		obj.form.submit.disabled=true;  
	} else{
		obj.form.submit.disabled=false;
	} 
}



</script>
</head>
<body>	
<jsp:include page="navbar.jsp"></jsp:include>


<table class="majorCell" width="100%" class="navbar"  border="1" rules="all" frame="border" cellpadding="10">
		
		
		<tr>
		
		<td width='50%'><table class="majorCell" width='100%'>
		<tr>
		<td class="heading" colspan="2">Access</td>
		</tr>

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


</table>
</td>
<td valign="top"  width='50%'>
		<table class="majorCell" width='100%'>
			<tr><td class="heading">
				Logs
			</td></tr>
			<tr><td valign="top" align="center">
<table class="majorCell">
<tr><td halign="left">
				<input type="button" value="GenePattern" class="wideButton" onclick="javascript:window.location='tomcatLog.jsp'"></td></tr>
<% if (System.getProperty("serverInfo").indexOf("Apache Tomcat") != -1) { %>
				<tr><td><input type="button" value="web server" class="wideButton" onclick="javascript:window.location='tomcatLog.jsp?tomcat=1'"></td></tr>
<% } %>
</table>
			</td>
		</tr>
		</table>
		</td>
</tr>


<tr>
<td width='50%' valign='top' align='center'>
<form action="adminServer.jsp" name="moduleRepositoryForm" method="POST">

<table width='100%'>
<tr><td class="heading" colspan='2'>Module Repository	</td></tr>
<tr><td align='right'>Repository URL:</td><td><input type='text' size='40' name="moduleRepository" value='<%=moduleRepository%>' onkeyup="changeReposFields(this)"/>
</td></tr>
<tr><td colspan='2' ALIGN='CENTER'><input type="submit" name="submitRepos" value="submit" class="button"  disabled="true"> 
<input type="submit" name="submitReposDefault" value="reset to default" 

<%
	if (defaultModuleRepository.equals(moduleRepository)){
%>
	disabled="true"> <%
	}
%>
</td></tr>
<tr><td colspan='2' align='center'><a href="taskCatalog.jsp">Install/Update modules</a></td></tr>
</table>
</form>


</td>


<td colspan=2>
<form action="adminServer.jsp" name="proxySettingsForm" method="POST">

<table width='100%'>
<tr><td class="heading" colspan='2'>HTTP Proxy Settings</td></tr>

<tr><td align='right'>Proxy Host:</td><td><input type='text' size='40' name="proxyHost" value='<%=proxyHost%>' onkeyup="changeProxyFields(this)"/></td></tr>

<tr><td align='right'>Proxy Port:</td><td><input type='text' size='40' name="proxyPort" value='<%=proxyPort%>' onkeyup="changeProxyFields(this)"/></td></tr>

<tr><td align='right'>Proxy Username:</td><td><input type='password' size='40' name="proxyUser" value='<%=proxyUser%>' onkeyup="changeProxyFields(this)"/></td></tr>
<tr><td align='right'>Proxy Password:</td><td><input type='password' size='40' name="proxyPass" value='<%=proxyPass%>' onkeyup="changeProxyFields(this)"/></td></tr>
<tr><td>&nbsp;</td></tr>

<tr><td colspan=2 align='center'><input type="submit" name="submitProxy" value="submit" class="button"  disabled="true">
<input type="submit" name="submitClearProxy" value="clear" class="button"
<%
	if ((proxyPort.length() + proxyHost.length()) == 0){
%>
	disabled="true" 
<%
	}
%>
><input type="button" name="proxyHelp" value="Help" class="button" onclick="alert('HTTP proxy settings are needed only to connect to the Module Repository, and only if your organization has a web proxy between you and the internet. \nUsername and password are required only if your proxy requires authentication. \nThe username and password values will not be saved to the config file for security reasons and need to be re-entered after a server restart.')">

</td></tr>
</table>
</form>
</td>
</tr>
<tr>
<td valign="top"  width='50%'>
		<table class="majorCell" width='100%'>
			<tr><td class="heading" colspan='2'>
				History
			</td></tr>
			<tr><td valign="top" align='right'>
			<form action="adminServer.jsp" name="recentHistoryForm" method="POST">

			Remember this many recent jobs:</td><td><input name="historySize" value="<%=recentHistorySize %>" size='10' onkeyup="changeHistoryField(this)" />
			<input type="submit" name="submit" value="submit" class="button" disabled="true">

			</td>

		</tr>
		</table>
		</td>

</tr>
<tr>
			<td valign="middle" align='center' colspan="2">
				<a href="shutdownServer.jsp">Shutdown server</a>
			</td>
		</tr>


	</table> <!-- end admin cell -->



<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

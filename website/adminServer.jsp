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


<%@ page session="false" contentType="text/html" language="Java" 
		import="java.util.*,
		org.genepattern.server.webapp.StartupServlet,
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

	String purgeJobsAfter = request.getParameter("purgeJobsAfter");
	String purgeTime = request.getParameter("purgeTime");

   String java_flags = request.getParameter("java_flags");



	String moduleRepository = request.getParameter("moduleRepository");
	
	String reposDefault = request.getParameter("submitReposDefault");
	String clearReposSelection = request.getParameter("clearReposSelection");

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
   
  
   if(proxyUser!=null || proxyPass!=null) {
      System.setProperty("http.proxyUser", proxyUser);
      System.setProperty("ftp.proxyUser", proxyUser);
      System.setProperty("http.proxyPassword", proxyPass);
      System.setProperty("ftp.proxyPassword", proxyPass);
   }
   

	if ((proxyHost!= null) || (proxyPort!= null)){
		Properties p = new Properties();
		if (proxyPort!= null) {
         p.setProperty("http.proxyPort", proxyPort);
         p.setProperty("ftp.proxyPort", proxyPort);
      }
		if (proxyHost!= null) {
         p.setProperty("http.proxyHost", proxyHost);
         p.setProperty("ftp.proxyHost", proxyHost);
      }

		storeSuccess  = PropertiesManager.storeChanges(p);
	} 
  
	if (clearProxy != null){
		Vector vec = new Vector();
		vec.add("http.proxyPort");
		vec.add("http.proxyHost");
		vec.add("http.proxyUser");
		vec.add("http.proxyPassword");
      
      vec.add("ftp.proxyPort");
		vec.add("ftp.proxyHost");
      vec.add("ftp.proxyUser");
		vec.add("ftp.proxyPassword");

		storeSuccess  = PropertiesManager.removeProperties(vec);
	}
   
   proxyUser = System.getProperty("http.proxyUser");
   if (proxyUser== null) proxyUser= "";
   proxyPass = System.getProperty("http.proxyPassword");
   if (proxyPass== null) proxyPass= "";
   
	proxyHost= System.getProperty("http.proxyHost" );
	if (proxyHost== null) proxyHost= "";
	proxyPort= System.getProperty("http.proxyPort" );
	if (proxyPort== null) proxyPort= "";


	if (clearReposSelection != null){
 		storeSuccess = PropertiesManager.removeArrayPropertyAndStore("ModuleRepositoryURLs", moduleRepository, ",", false);

	} else if (moduleRepository != null){
		storeSuccess  = PropertiesManager.storeChange("ModuleRepositoryURL", moduleRepository );
 		storeSuccess = storeSuccess && PropertiesManager.appendArrayPropertyAndStore("ModuleRepositoryURLs", moduleRepository, ",", true, false);

	} 
	moduleRepository = System.getProperty("ModuleRepositoryURL","" );
	

	if (recentHistorySize != null){
		storeSuccess  = PropertiesManager.storeChange("recentJobsToDisplay", recentHistorySize );
	} 
	recentHistorySize = System.getProperty("recentJobsToDisplay", "3" );
	
	if ((purgeJobsAfter != null) || (purgeTime != null)){
		if (purgeTime != null){
			storeSuccess  = PropertiesManager.storeChange("purgeTime", purgeTime );
		} 
		if (purgeJobsAfter != null){
			storeSuccess  = PropertiesManager.storeChange("purgeJobsAfter", purgeJobsAfter );
		}
		StartupServlet.startJobPurger();		
	} 
	purgeJobsAfter = System.getProperty("purgeJobsAfter" );
	purgeTime = System.getProperty("purgeTime" );

	if (java_flags != null){
		storeSuccess  = PropertiesManager.storeChange("java_flags", java_flags );
	} 
	java_flags= System.getProperty("java_flags" );



%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>GenePattern Server Administration</title>

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
	var ac = "<%=allowedClients%>";
	if (ac = "undefined") ac = "";
	obj.form.submit.disabled=(obj.form.allowed_clients.value == ac)  
}

function changeProxyFields(obj){
	obj.form.submitProxy.disabled=((obj.form.proxyHost.value == "<%=proxyHost%>")  && (obj.form.proxyPort.value == "<%=proxyPort%>") 
      && (obj.form.proxyUser.value == "<%=proxyUser%>") && (obj.form.proxyPass.value == "<%=proxyPass%>")) 

	obj.form.submitClearProxy.disabled = ((obj.form.proxyHost.value.length + obj.form.proxyPort.value.length ) == 0)  
}
function changePurgeFields(obj){
	obj.form.submitPurge.disabled=((obj.form.purgeJobsAfter.value == "<%=purgeJobsAfter%>")  && (obj.form.purgeTime.value == "<%=purgeTime%>")) 

}

function changeJavaFlagFields(obj){
	obj.form.submitJavaFlags.disabled=(obj.form.java_flags.value == "<%=java_flags%>") 

}

function changeReposFields(obj){
	obj.form.submitRepos.disabled = (obj.form.moduleRepository.value == "<%=moduleRepository%>")
	obj.form.clearReposSelection.disabled = (obj.form.moduleRepository.value == "<%=moduleRepository%>")
	obj.form.submitReposDefault.disabled = (obj.form.moduleRepository.value == "<%=defaultModuleRepository%>")
}

function selectReposFields(obj){
	
	obj.form.moduleRepository.value = obj.form.selectModuleRepository.value;
	changeReposFields(obj);	
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
onclick="refillField(this);"> These Domains (comma delimited list)<br>
		



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
<% /****************************************************** SECOND ROW *************************************************************/ %>

<td width='50%' valign='top' align='center'>
<form action="adminServer.jsp" name="purgeSettingsForm" method="POST">

<table width='100%'>
<tr><td class="heading" colspan='2'>File Purge Settings</td></tr>

<tr><td align='right'>Purge Jobs After:</td><td><input type='text' size='40' name="purgeJobsAfter" value='<%=purgeJobsAfter%>' onkeyup="changePurgeFields(this)"/> days</td></tr>

<tr><td align='right'>Purge Time:</td><td><input type='text' size='40' name="purgeTime" value='<%=purgeTime%>' onkeyup="changePurgeFields(this)"/></td></tr>

<tr><td>&nbsp;</td></tr>

<tr><td colspan=2 align='center'><input type="submit" name="submitPurge" value="submit" class="button"  disabled="true">
<input type="button" name="purgeHelp" value="Help" class="button" onclick="alert('File purge settings define how often and, at what time of day, intermediate result files created on the server are deleted (purged). To never delete files set the purge frequency to -1. Note that this could result in large amounts of hard drive space being used over time.')">

</td></tr>
</table>
</form>

</td>
<td valign="top"  width='50%'>
<form action="adminServer.jsp" name="javaFlagSettingsForm" method="POST">

<table width='100%'>
<tr><td class="heading" colspan='2'>Java Flag Settings</td></tr>

<tr><td align='right'>Java Flags:</td><td><input type='text' size='40' name="java_flags" value='<%=java_flags%>' onkeyup="changeJavaFlagFields(this)"/> </td></tr>

<tr><td>&nbsp;</td></tr>

<tr><td colspan=2 align='center'><input type="submit" name="submitJavaFlags" value="submit" class="button"  disabled="true">
<input type="button" name="javaFlagHelp" value="Help" class="button" onclick="alert('Java flags are passed to the VM of most Java language tasks.  You can use these flags to increase the amount of memory alotted to a Java language module if you are experiencing OutOfMemory errors.\n\nFor details of potential java flags run\n&nbsp;&nbsp;&nbsp;&nbsp;java -X\n   at a command line .')">

</td></tr>
</table>
</form>

</td>

</tr>
<% /****************************************************** THIRD ROW *************************************************************/ %>



<tr>
<td width='50%' valign='top' align='center'>
<form action="adminServer.jsp" name="moduleRepositoryForm" method="POST">

<table width='100%'>
<tr><td class="heading" colspan='2'>Module Repository	</td></tr>

<tr><td align='right'>Previous Repository URLs:</td><td><select size='3' cols="40" name="selectModuleRepository" onmouseup="selectReposFields(this)">
<% 
	
	ArrayList mrs = PropertiesManager.getArrayProperty("ModuleRepositoryURLs", moduleRepository, ",");
	for (Iterator iter = mrs.iterator(); iter.hasNext(); ){
		String repos = (String)iter.next();
		out.println("<option");
		if (repos.equals(moduleRepository)) out.print(" selected='selected' ");
		out.println(">"+repos+"</option>");
	}
%>

</select>
</td></tr>

<tr><td align='right'>Repository URL:</td><td><input type='text' size='50' name="moduleRepository" onkeyup="changeReposFields(this)"/>
</td></tr>
<tr><td colspan='2' ALIGN='CENTER'><input type="submit" name="submitRepos" value="submit" class="button"  disabled="true"> 
<input type="submit" name="submitReposDefault" value="reset to default" 

<%
	if (defaultModuleRepository.equals(moduleRepository)){
%>
	disabled="true"> <%
	}
%>
<input type="submit" name="clearReposSelection" value="remove from list" class="wideButton"  disabled="true">

</td></tr>
<tr><td colspan='2' align='center'><a href="taskCatalog.jsp">Install/Update tasks</a></td></tr>
</table>
</form>


</td>


<td valign="top">
		<table class="majorCell" width='100%'>
			<tr><td class="heading" colspan='2'>
				History
			</td></tr>
			<tr><td valign="top" align='right'>
			<form action="adminServer.jsp" name="recentHistoryForm" method="POST">

			Display this many recent jobs:</td><td><input name="historySize" value="<%=recentHistorySize %>" size='10' onkeyup="changeHistoryField(this)" />
			<input type="submit" name="submit" value="submit" class="button" disabled="true">
			<input type="button" name="historyHelp" value="Help" class="button" onclick="alert('The recent history size sets the maximum number of\n recent jobs that will be displayed in the task and \npipeline dropdowns at the top of the page.')">


			</td>

		</tr>
		</table>
</td>
</tr>

<% /****************************************************** FOURTH ROW *************************************************************/ %>

<tr>
<td valign="top"  width='50%'>

<form action="adminServer.jsp" name="proxySettingsForm" method="POST">

<table width='100%'>
<tr><td class="heading" colspan='2'>Proxy Settings</td></tr>

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
><input type="button" name="proxyHelp" value="Help" class="button" onclick="alert('Proxy settings are needed only to connect to the Module Repository, and only if your organization has a web proxy between you and the internet. \nUsername and password are required only if your proxy requires authentication. \nThe username and password values will not be saved to the config file for security reasons and need to be re-entered after a server restart.')">

</td></tr>
</table>
</form>

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

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


<%@ page import="org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.util.LSID,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
		 org.genepattern.data.pipeline.PipelineModel,
		 org.genepattern.server.util.AuthorizationManager,
		 java.io.File,
		 java.io.FilenameFilter,
		 java.net.MalformedURLException,
		 java.net.URLEncoder,
		 java.util.Arrays,
		 java.util.Collection,
		 java.util.Collections,
		 java.util.Comparator,
		 java.util.HashMap,
		 java.util.HashSet,
		 java.util.Iterator,
		 java.util.Properties,
		 java.util.List,
		 java.util.Set,
		 java.util.TreeSet,
		 java.util.TreeMap,
		 java.util.Vector"
	session="false" contentType="text/html" language="Java" buffer="50kb" %>
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>


<%
try {
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String userID= (String)request.getAttribute("userID"); // will force login if necessary
if (userID == null) return; // come back after login
LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);
LocalAdminClient adminClient = new LocalAdminClient(userID);
// drop-down selection lists
String[] taskTypes = GenePatternAnalysisTask.getTaskTypes();
String[] cpuTypes = GenePatternAnalysisTask.getCPUTypes();
String[] oses = GenePatternAnalysisTask.getOSTypes();
String[] languages = GenePatternAnalysisTask.getLanguages();
String[] qualities = GPConstants.QUALITY_LEVELS;
String[] privacies = GPConstants.PRIVACY_LEVELS;


int NUM_ATTACHMENTS = 5;
int NUM_PARAMETERS = 5;
String DELETE = "Delete";
String CLONE = "Clone";
String RUN = "Run";
String taskName = request.getParameter(GPConstants.NAME);
String attributeName = null;
String attributeValue = null;
String attributeType = null;
TaskInfo taskInfo = null;
ParameterInfo[] parameterInfoArray = null;
TaskInfoAttributes tia = null;

if (taskName != null && taskName.length() == 0) taskName = null;
if (taskName != null) {
	try {
		taskInfo = GenePatternAnalysisTask.getTaskInfo(taskName, userID);
		if (taskInfo != null) {
			taskName = taskInfo.getName();
		      parameterInfoArray = new ParameterFormatConverter().getParameterInfoArray(taskInfo.getParameterInfo());
			tia = taskInfo.giveTaskInfoAttributes();
			LSID lsid = new LSID((String)tia.get(GPConstants.LSID));
		} else {

			response.sendRedirect("addTask.jsp?name=" +request.getParameter(GPConstants.NAME));
			return;
		}
	} catch (OmnigeneException oe) {
	}
}

	TreeMap tmFileFormats = new TreeMap(String.CASE_INSENSITIVE_ORDER);
	TreeMap tmDomains = new TreeMap(String.CASE_INSENSITIVE_ORDER);

/*	int DOMAIN_PARAM_OFFSET = -1;
	for (int j = 0; j < GPConstants.PARAM_INFO_ATTRIBUTES.length; j++) {
		if (GPConstants.PARAM_INFO_ATTRIBUTES[j] == GPConstants.PARAM_INFO_DOMAIN) {
			DOMAIN_PARAM_OFFSET = j;
			break;
		}
	}*/
	int FILE_FORMAT_PARAM_OFFSET = -1;
	for (int j = 0; j < GPConstants.PARAM_INFO_ATTRIBUTES.length; j++) {
		if (GPConstants.PARAM_INFO_ATTRIBUTES[j] == GPConstants.PARAM_INFO_FILE_FORMAT) {
			FILE_FORMAT_PARAM_OFFSET = j;
			break;
		}
	}

Collection tmTasks = adminClient.getTaskCatalog(); 
TreeSet tsTaskTypes = new TreeSet(String.CASE_INSENSITIVE_ORDER);

// well-known task types, regardless of domain
tsTaskTypes.add(""); // blank entry at top of list
tsTaskTypes.add(GPConstants.TASK_TYPE_PIPELINE);
tsTaskTypes.add(GPConstants.TASK_TYPE_VISUALIZER);

for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
	TaskInfo ti = (TaskInfo)itTasks.next();
	TaskInfoAttributes tia2 = ti.giveTaskInfoAttributes();
	if (tia2 == null) continue;
	boolean isPrivate = tia2.get(GPConstants.PRIVACY).equals(GPConstants.PRIVATE);
	boolean isMine = tia2.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) || tia2.get(GPConstants.USERID).equals(userID);
	String owner = tia2.get(GPConstants.USERID);
	if (!tsTaskTypes.contains(tia2.get(GPConstants.TASK_TYPE))) {
		tsTaskTypes.add(tia2.get(GPConstants.TASK_TYPE));
	}
}
taskTypes = (String[])tsTaskTypes.toArray(new String[0]);

%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">

<title><%= taskName == null ? "add GenePattern task" : ( taskName + " version " + new LSID(tia.get(GPConstants.LSID)).getVersion()) %></title>

<style>.hideable { border-style: none; readonly: true; }</style>

<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">

<script language="javascript">

function onPrivacyChange(selector) {
	if (selector.options[selector.selectedIndex].value == '<%= GPConstants.PRIVATE %>') {
		// changing from public to private
		document.forms['task'].<%= GPConstants.USERID %>.value='<%= userID %>';
	} else {
		// changing from private to public
	}
}

function confirmDeleteSupportFiles() {
	var sel = document.forms['task'].deleteFiles;
	var selection = sel.options[sel.selectedIndex].value;
	if (selection == null || selection == "") return;
	if (window.confirm('Really delete ' + selection + ' from ' + document.forms['task']['<%= GPConstants.FORMER_NAME %>'].value + '\'s support files?')) { 
		//window.location='saveTask.jsp?deleteSupportFiles=1&deleteFiles=' + selection + '&<%= GPConstants.NAME %>=' + document.forms['task'].<%= GPConstants.NAME %>.value + '&<%= GPConstants.LSID %>=' + document.forms['task']['<%= GPConstants.LSID %>'].value;
		sel.form.deleteSupportFiles.value = "1";
		sel.form.submit();
	}
}

<%
// create Javascript associative array of all Tasks (mine and others) so that determination of ownership and rights can be ascertained
%>

<% if (taskInfo != null) { %>
function cloneTask() {
	while (true) {
		var cloneName = window.prompt("Name for cloned task", "copyOf<%= taskName %>");
		if (cloneName == null || cloneName.length == 0) {
			return;
		}
		window.location = "saveTask.jsp?clone=1&<%= GPConstants.NAME %>=<%= taskName %>&<%= GPConstants.LSID %>=<%= tia.get(GPConstants.LSID) %>&cloneName=" + cloneName + "&<%= GPConstants.USERID %>=<%= userID %>";
		break;
	}
}

function runTask() {
	window.location = '<%= request.getContextPath() %>/pages/index.jsf?lsid=<%= tia.get(GPConstants.LSID) %>';
}

<% } %>

function addNewTaskType() {
	var newTaskType = window.prompt("new task type", "");
	if (newTaskType == null || newTaskType == "") return;
	var fld = document.forms['task'].<%= GPConstants.TASK_TYPE %>;
	var n = fld.options.length;
	var found = false;
	for (i = 0; i < n; i++) {
		if (fld.options[i].text == newTaskType) {
			found = true;
			fld.options.selectedIndex = i;
			return;
		}
	}
	fld.options[n] = new Option(newTaskType, newTaskType);
	fld.options.selectedIndex = n;
}


function addNewFileType(name, desc){
	if (name == null || name == "") return;
	if (desc == "") desc = name;
	var frm = document.forms['task'];
	var fld = frm.<%= GPConstants.FILE_FORMAT %>;
	var n = fld.options.length;
	var found = false;
	for (i = 0; i < n; i++) {
		if (fld.options[i].text == name) {
			fld.options[i].selected = true;
			found = true;
			break;
		}
	}
	if (!found) {
		fld.options[n] = new Option(name, desc, true, true);
		for (i = 0; i < <%= GPConstants.MAX_PARAMETERS %>; i++) {
			fld = frm["p" + i + "_<%= GPConstants.PARAM_INFO_FILE_FORMAT[GPConstants.PARAM_INFO_NAME_OFFSET] %>"];
			fld.options[fld.options.length] = new Option(name, desc);
		}
	}
}

function addNewDomainType(name, desc){
	if (name == null || name == "") return;
	if (desc == "") desc = name;
	var frm = document.forms['task'];
	var fld = frm.<%= GPConstants.DOMAIN %>;
	var n = fld.options.length;
	var found = false;
	for (i = 0; i < n; i++) {
		if (fld.options[i].text == name) {
			found = true;
			fld.options[i].selected = true;
			break;
		}
	}
	if (!found) {
		fld.options[n] = new Option(name, desc, true, true);
		for (i = 0; i < <%= GPConstants.MAX_PARAMETERS %>; i++) {
			fld = frm["p" + i + "_<%= GPConstants.PARAM_INFO_DOMAIN[GPConstants.PARAM_INFO_NAME_OFFSET] %>"];
			fld.options[fld.options.length] = new Option(name, desc);
		}
	}
}

</script>
<jsp:include page="navbarHead.jsp"/>
</head>
<body>
<jsp:include page="navbar.jsp"/>
<% if (taskName != null && taskInfo == null) { %>
	<script language="javascript">
	alert('no such task <%= taskName %>');
	</script>
<%
	taskName = null;
	}
AuthorizationManager authManager = new AuthorizationManager();


StringBuffer publicTasks = new StringBuffer();
String name;
String description;
String lsid;
StringBuffer otherTasks = new StringBuffer();
String DONT_JUMP = "dontJump";

// used to avoid displaying multiple versions of same basic task
HashMap hmLSIDsWithoutVersions = new HashMap();

// used to track multiple versions of current task
Vector vVersions = new Vector();
LSID l = null;
String thisLSIDNoVersion = "";
if (tia != null) {
	try {
		lsid = tia.get(GPConstants.LSID);
		thisLSIDNoVersion = new LSID(lsid).toStringNoVersion();
	} catch (MalformedURLException mue) {
	}
}

	String authorityType = null;

	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		TaskInfo ti = (TaskInfo)itTasks.next();
		name = ti.getName();
		description = ti.getDescription();
		TaskInfoAttributes tia2 = ti.giveTaskInfoAttributes();
		if (tia2 == null) continue;
		
		String domain = tia2.get(GPConstants.DOMAIN);
		if (domain != null && domain.length() > 0){
			String[] domains = domain.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
			for(int x = 0; x<domains.length; x++){
		 		tmDomains.put(domains[x], domains[x]);
			}
		}
		String fileFormat = tia2.get(GPConstants.FILE_FORMAT);
		if (fileFormat != null && fileFormat.length() > 0){
		 	String [] fileFormats = fileFormat.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
			for (int y = 0; y< fileFormats.length; y++){
				tmFileFormats.put(fileFormats[y], fileFormats[y]);
			}
		}	
	        ParameterInfo[] pia = new ParameterFormatConverter().getParameterInfoArray(ti.getParameterInfo());
		if (pia != null) {
			for (int pNum = 0; pNum < pia.length; pNum++) {
				HashMap pAttributes = pia[pNum].getAttributes();
				domain = (String)pAttributes.get(GPConstants.DOMAIN);
				if (domain != null && domain.length() > 0) {
					String[] domains = domain.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
					for(int x = 0; x<domains.length; x++){
				 		tmDomains.put(domains[x], domains[x]);
					}
				}
				fileFormat = (String)pAttributes.get(GPConstants.FILE_FORMAT);
				if (fileFormat != null && fileFormat.length() > 0) {
				 	String [] fileFormats = fileFormat.split(GPConstants.PARAM_INFO_CHOICE_DELIMITER);
					for (int y = 0; y< fileFormats.length; y++){
						tmFileFormats.put(fileFormats[y], fileFormats[y]);
					}
				}
			}
		}

		lsid = tia2.get(GPConstants.LSID);
		try {
			l = new LSID(lsid);
			String versionlessLSID = l.toStringNoVersion();
			if (versionlessLSID.equals(thisLSIDNoVersion)) {
				vVersions.add(lsid);
			}

			versionlessLSID = l.toStringNoVersion();
			String key = versionlessLSID+"."+name;			
			if (hmLSIDsWithoutVersions.containsKey(key) &&
			    ((TaskInfo)hmLSIDsWithoutVersions.get(key)).getName().equals(name)) {
				continue;
			}
			hmLSIDsWithoutVersions.put(key, ti);
			authorityType = LSIDManager.getInstance().getAuthorityType(l);
		} catch (MalformedURLException mue) {
			l = null;
		}
		boolean bMine = tia2.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) || tia2.get(GPConstants.USERID).equals(userID);
		String owner = tia2.get(GPConstants.USERID);
		if (owner != null && owner.indexOf("@") != -1) owner = " (" + owner.substring(0, owner.indexOf("@")) + ")";
		StringBuffer sb = bMine ? publicTasks : otherTasks;

		String n = (lsid != null ? lsid : name);
		if (n == null) n = "";
		sb.append("<option value=\"" + n + "\"" + (tia != null && n.equals((String)tia.get(GPConstants.LSID)) ? " selected" : "") + 
				 " class=\"tasks-" + authorityType + "\">" + 
				 (lsid != null ? ti.getName()  : name) + (!bMine ? owner : "") + 
				 (authorityType.equals(LSIDUtil.AUTHORITY_FOREIGN) ? (" (" + l.getAuthority() + ")") : "") +
				 "</option>\n");
	}

	Collections.sort(vVersions, new Comparator() {
				public int compare(Object v1, Object v2) {
					try {
						LSID lsid1 = new LSID((String)v1);
						LSID lsid2 = new LSID((String)v2);
						return lsid1.compareTo(lsid2);
					} catch (MalformedURLException mue) {
						// ignore
						return 0;
					}
				}
			});

	String[][] fileFormats = new String[tmFileFormats.size()][2];
	int i = 0;
	for (Iterator itFileFormat = tmFileFormats.keySet().iterator(); itFileFormat.hasNext(); i++) {
		String key = (String)itFileFormat.next();
		fileFormats[i] = new String[] { key, key};
	}
	GPConstants.PARAM_INFO_ATTRIBUTES[FILE_FORMAT_PARAM_OFFSET][GPConstants.PARAM_INFO_CHOICE_TYPES_OFFSET] = fileFormats;

	String[][] domains = new String[tmDomains.size()][2];
	i = 0;
	for (Iterator itDomain = tmDomains.keySet().iterator(); itDomain.hasNext(); i++) {
		String key = (String)itDomain.next();
		domains[i] = new String[] { key, key};
	}
	if (tia != null) {
		lsid = tia.get(GPConstants.LSID);
	} else {
		lsid = "";
	}

%>

<h2><%= taskName == null ? "Create new "+ messages.get("ApplicationName") +" task" : ( taskName  + " version ") %>
<% if (taskName != null) { %>
	<select name="notused" onchange="javascript:window.location='addTask.jsp?<%= GPConstants.NAME %>=' + this.options[this.selectedIndex].value + '<%= "&view=1"  %>'" style="font-weight: bold; font-size: medium; outline-style: none;">
<%
	for (Iterator itVersions = vVersions.iterator(); itVersions.hasNext(); ) {
		String vLSID = (String)itVersions.next();
		l = new LSID(vLSID);
%>
		<option value="<%= l.toString() %>"<%= vLSID.equals(lsid) ? " selected" : "" %>><%= l.getVersion() %></option>
<%
	}
%>
	</select>
<% } %>
</h2>

<form name="task" action="saveTask.jsp" method="post" ENCTYPE="multipart/form-data">
<input type="hidden" name="<%= GPConstants.FORMER_NAME %>" value="<%= taskInfo != null ? taskInfo.getName() : "" %>">


<input type="button" value="Help" onclick="window.open('help.jsp', 'help')" class="button">
<% if (LSIDManager.getInstance().getAuthorityType(new LSID(tia.get(GPConstants.LSID))).equals(LSIDUtil.AUTHORITY_MINE)) { %><input type="button" value="Edit" onclick="window.location='addTask.jsp?name=<%= request.getParameter(GPConstants.NAME) %>'" class="button"><% } %>

<br><br>
  <table cols="2" valign="top">
  <col align="right" width="20%">
  <col align="left" width="*">
  <tr title="Task name without spaces, used as the name by which the task will be invoked.">
  <td align="right"><b>Name:</b></td>
  <td width="*"><%= taskInfo.getName() %>

&nbsp;&nbsp;&nbsp;&nbsp;

<% if (taskInfo != null) { %>
  <input type="button" value="<%= RUN %>" name="<%= RUN %>" class="little" onclick="runTask()">

<% if (authManager.checkPermission("createTask", userID)){ %>
  <input type="button" value="<%= CLONE %>..." name="<%= CLONE %>" class="little" onclick="cloneTask()">
<%}%>

<% } %>

   &nbsp;&nbsp;&nbsp;<select onchange="javascript:if (this.options[this.selectedIndex].value != '<%= DONT_JUMP %>') window.location='addTask.jsp?<%= GPConstants.NAME %>=' + this.options[this.selectedIndex].value + '&view=1'">
  <option value="<%= DONT_JUMP %>">task catalog</option>
	<option value="">new task</option>
	<%= publicTasks.toString() %>  
	<option value="<%= DONT_JUMP %>">-----------------------------------------</option>
	<option value="<%= DONT_JUMP %>">private tasks (not mine)</option>
	<option value="<%= DONT_JUMP %>">-----------------------------------------</option>
	<%= otherTasks.toString() %>
  </select>
  <select name="notused" onchange="javascript:window.location='addTask.jsp?<%= GPConstants.NAME %>=' + this.options[this.selectedIndex].value + '&view=1'">
<%
	for (Iterator itVersions = vVersions.iterator(); itVersions.hasNext(); ) {
		String vLSID = (String)itVersions.next();
		l = new LSID(vLSID);
%>
		<option value="<%= l.toString() %>"<%= vLSID.equals(lsid) ? " selected" : "" %>><%= l.getVersion() %></option>
<%
	}
%>
  </select>

</td>
  </tr>

  <tr title="LSID">
  <td align="right"><b>LSID:</b></td>
  <td width="*">
 <%=  taskInfo != null ? StringUtils.htmlEncode(tia.get(GPConstants.LSID)) : "" %>
  </td>
  </tr>

  <tr title="A verbose description of the purpose of the program, especially useful to someone who hasn't run the program before to determine whether it is suited to their problem.">
  <td align="right"><b>Description:</b></td>
  <td width="*">
  <%= taskInfo != null ? StringUtils.htmlEncode(taskInfo.getDescription()) : "" %>
  </td>
  </tr>

  <tr title="Author's name, affiliation, email address">
  <td align="right"><b>Author:</b></td>
  <td width="*">
  <%= taskInfo != null ? StringUtils.htmlEncode(tia.get(GPConstants.AUTHOR)) : "" %>
  </td>
  </tr>

  <tr title="Your user ID">
  <td align="right"><b>Owner:</b></td>
  <td width="*">
<%= (tia == null ? userID : tia.get(GPConstants.USERID))   %>
  </td>
  </tr>

  <tr title="Make available to others">
  <td align="right"><b>Privacy:</b></td>
  <td width="*"><%= tia.get(GPConstants.PRIVACY) %>	</td>
  </tr>

  <tr title="Readiness for use by others">
  <td align="right"><b>Quality&nbsp;level:</b></td>
  <td width="*"><%= tia.get(GPConstants.QUALITY) %></td>
  </tr>

<% 
if (taskName != null) {
	File[] docFiles = taskIntegratorClient.getDocFiles(taskInfo);
%><tr><td align="right"><%
	boolean isPipeline = tia != null && tia.get(GPConstants.TASK_TYPE).equals(GPConstants.TASK_TYPE_PIPELINE);
	boolean hasDoc = docFiles != null && docFiles.length > 0;
	if (hasDoc || isPipeline) {
%><b>Documentation:</b></td><td width="*"><%
	}
	if (hasDoc) { 
 		for (i = 0; i < docFiles.length; i++) { %>
<a href="getTaskDoc.jsp?<%= GPConstants.NAME %>=<%= StringUtils.htmlEncode(request.getParameter(GPConstants.NAME)) %>&file=<%= URLEncoder.encode(docFiles[i].getName()) %>" target="new"><%= StringUtils.htmlEncode(docFiles[i].getName()) %></a> 
<% 		} 
 	}
	if (isPipeline) {
%>
		<a href="pipelineDesigner.jsp?<%= GPConstants.NAME %>=<%= tia.get(GPConstants.LSID) %>">pipeline designer</a>
		<input name="<%= PipelineModel.PIPELINE_MODEL %>" type="hidden" value="<%= StringUtils.htmlEncode(tia.get(PipelineModel.PIPELINE_MODEL)) %>">
<%
	}
%>
</td></tr>
<% } %>


  <tr title="the command line used to invoke the application, using &lt;tags&gt; for param &amp; environment variable substitutions.">
  <td align="right" valign="top"><b>command&nbsp;line:</b><br>
   </td>
  <td valign="top" width="*"><%= tia != null ? StringUtils.htmlEncode(tia.get(GPConstants.COMMAND_LINE)) : "" %></td>
  </tr>

  <tr>
  <td align="right"><b>task&nbsp;type:</b></td>
  <td width="*"> <%= tia.get(GPConstants.TASK_TYPE) %>       
  </td>
  </tr>

   <tr>
  <td align="right"><b>CPU&nbsp;type:</b></td>
  <td width="*">    <%= tia.get(GPConstants.CPU_TYPE) %> (if compiled for a specific one)
  </td>
  </tr>

   <tr>
  <td align="right"><b>operating&nbsp;system:</b></td>
  <td width="*"><%= tia.get(GPConstants.OS) %> (if operating system-dependent)
  </td>
   </tr>

<%--
   <tr>
  <td align="right"><b>Java&nbsp;JVM&nbsp;level:</b></td>
  <td width="*">   <%= tia.get(GPConstants.JVM_LEVEL) %>(if Java is used)
         </td>
   </tr>
--%>
   <tr>
  <td align="right"><b>Language:</b></td>
  <td width="*">         <%= tia.get(GPConstants.LANGUAGE) %>
  &nbsp;
    <b>min. language version: </b><%= StringUtils.htmlEncode(tia.get(GPConstants.JVM_LEVEL)) %>
         </td>
   </tr>
   
  <td align="right" valign="top"><b>Version&nbsp;comment:</b></td>
  <td width="*">
  	<%= StringUtils.htmlEncode(tia.get(GPConstants.VERSION)) %>
   </td>
   </tr>

   <tr>
   <td align="right" valign="top"><b>output description:</b></td>
   <td>
	<table>
	<tr>
	<td valign="top">
		output file format(s):
	</td>
	<td valign="top">	
<%
		attributeValue = (tia != null ? tia.get(GPConstants.FILE_FORMAT) : "");
		if (attributeValue == null) attributeValue = "";
%>
		<%= attributeValue %>

	</td>
 

<!--	 <td valign="top"></td> 
	<td valign="top"> 

  

   <!--	<td valign="top"> </td> -->
 	</tr>
	</table>



   </td>
   </tr>
   <input type="hidden" name="<%= GPConstants.REQUIRED_PATCH_LSIDS %>" value="<%= tia != null ? tia.get(GPConstants.REQUIRED_PATCH_LSIDS) : "" %>">
   <input type="hidden" name="<%= GPConstants.REQUIRED_PATCH_URLS %>" value="<%= tia != null ? tia.get(GPConstants.REQUIRED_PATCH_URLS) : "" %>">
   
  

  <tr>
  <td align="right" valign="top"><b>Current&nbsp;files:</b></td>
  <td width="*">
<%
  	   File[] allFiles = taskIntegratorClient.getAllFiles(taskInfo);
	   
	   for (i = 0; i < allFiles.length; i++) { %>
		<a href="getFile.jsp?task=<%= (String)taskInfo.giveTaskInfoAttributes().get(GPConstants.LSID) %>&file=<%= URLEncoder.encode(allFiles[i].getName()) %>" target="new"><%= StringUtils.htmlEncode(allFiles[i].getName()) %></a> 
<%	   }  %>

  <br>
  </td>
   </tr>
        
<tr><td valign="top" align="right"><b>Parameters:</b><font size=-1>&nbsp;</font><br>
<table cols="1"><tr><td><font size="-1">&nbsp;</font></td></tr><tr><td><font size="-1">&nbsp;</font></td></tr><tr><td align="right"><br><br><br><i>example:</i></td></tr></table>
</td>
<td>
<font size=-1>
  The names of these parameters will be available for the command line (above) in the form &lt;name&gt;.<br>
 </font><br>
  <table cols="<%= 3+GPConstants.PARAM_INFO_ATTRIBUTES.length %>">
  <tr>
  <td width="20%" valign="bottom"><b>name</b></td>
  <td width="30%" valign="bottom"><b>description (optional)</b></td>
  <td width="20" valign="bottom"><b>choices</b><br><font size="-1">(optional semicolon-separated list of choices.)</font></td>
<% 
  for (int attribute = 0; attribute < GPConstants.PARAM_INFO_ATTRIBUTES.length; attribute++) { 
		attributeName = ((String)GPConstants.PARAM_INFO_ATTRIBUTES[attribute][GPConstants.PARAM_INFO_NAME_OFFSET]);
		if (attributeName != null) attributeName = attributeName.replace(GPConstants.PARAM_INFO_SPACER, ' ');

%>
  <td valign="bottom"><b><%= attributeName %></b></td>
<% } %>
  </tr>
  <tr>
  <td><i>min</i></td>
  <td><i>values below minimum will be set to this value</i></td>
  <td><i>2=green, default;0=red;1=blue</i></td>
  <td><i>2</i></td>
  </tr>

<%= createParameterEntries(0, NUM_PARAMETERS, parameterInfoArray, taskInfo) %>


<!--
<tr><td>
</td></tr>
<div id="parameters" style="display: none">
-->
<%= createParameterEntries(NUM_PARAMETERS, GPConstants.MAX_PARAMETERS, parameterInfoArray, taskInfo) %>

<tr><td></td></tr>
<tr><td colspan="3" align="center">
<% 
	lsid = tia.get(GPConstants.LSID);
	l = new LSID(lsid);
	authorityType = LSIDManager.getInstance().getAuthorityType(l);
	if (authorityType.equals(LSIDUtil.AUTHORITY_MINE)) {
%>
		<input type="button" value="Edit" onclick="window.location='addTask.jsp?name=<%= request.getParameter(GPConstants.NAME) %>'" class="button">
<%	} else { %>
		<input type="button" value="<%= RUN %>" name="<%= RUN %>" class="little" onclick="runTask()">
		<% if (authManager.isAllowed("addTask.jsp", userID)) { %>
  <input type="button" value="<%= CLONE %>..." name="<%= CLONE %>" class="little" onclick="cloneTask()">
<%		}
 	} 
  
%>
</td></tr>

<!-- </div> -->

</table>

 </td></tr></table>
   </p>

 </form>

<% if (tia != null) { %>
<a href="makeZip.jsp?<%= GPConstants.NAME %>=<%= request.getParameter(GPConstants.NAME) %>&includeDependents=1">package this task into a zip file</a><br>
<% } %>
<jsp:include page="footer.jsp"/>
</body>
</html>
<% } catch (Throwable t) {
	t.printStackTrace();
	t.printStackTrace(new java.io.PrintWriter(out));
   }
%>

<%! public String createParameterEntries(int from, int to, ParameterInfo[] parameterInfoArray, TaskInfo taskInfo) throws Exception {

StringBuffer out = new StringBuffer();
ParameterInfo p = null;
HashMap attributes = null;
String attributeName = null;
String attributeValue = null;
String attributeType = null;

for (int i = from; i < to; i++) { 
	p = (parameterInfoArray != null && i < parameterInfoArray.length) ? parameterInfoArray[i] : null;
	if ( p == null) continue;
	attributes = null;
	if (p != null) attributes = p.getAttributes();
	if (attributes == null) attributes = new HashMap();

	out.append("<tr>\n");
	out.append("<td valign=\"top\">" + StringUtils.htmlEncode(p.getName()) + "</td>\n");
	out.append("<td valign=\"top\">" + (( p.getDescription() == null) ? "" : (StringUtils.htmlEncode(p.getDescription()))) + "</td>\n");
	out.append("<td valign=\"top\">" + ((((p.getValue() == null) ? "" : StringUtils.htmlEncode(GenePatternAnalysisTask.replace(p.getValue(), GPConstants.PARAM_INFO_CHOICE_DELIMITER, GPConstants.PARAM_INFO_CHOICE_DELIMITER+" "))))) + "</td>\n");

	if (p.isInputFile()) {
		attributes.put(GPConstants.PARAM_INFO_TYPE[GPConstants.PARAM_INFO_TYPE_NAME_OFFSET], GPConstants.PARAM_INFO_TYPE_INPUT_FILE);
	}

	for (int attributeNum = 0; attributeNum < GPConstants.PARAM_INFO_ATTRIBUTES.length; attributeNum++) {
		attributeName = (String)GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum][GPConstants.PARAM_INFO_NAME_OFFSET];
		attributeType = (String)GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum][GPConstants.PARAM_INFO_TYPE_OFFSET];

		out.append("<td valign=\"top\">");
		if (attributeType.equals(GPConstants.PARAM_INFO_STRING)) {
			attributeValue = (String)attributes.get(attributeName);
			if (attributeValue == null) {
				attributeValue = "";
			}
			
			out.append(StringUtils.htmlEncode(attributeValue));
			

		} else if (attributeType.equals(GPConstants.PARAM_INFO_CHOICE)) {			
			attributeValue = (String)attributes.get(attributeName);
			if (attributeValue == null) {
				attributeValue = "";
			}
			String[][]choices = null;
			choices = (String[][])GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum][GPConstants.PARAM_INFO_CHOICE_TYPES_OFFSET];
			boolean multiple = (GPConstants.PARAM_INFO_ATTRIBUTES[attributeNum].length > GPConstants.PARAM_INFO_CHOICE_TYPES_MULTIPLE_OFFSET);
			
			
			if (!multiple) {
				for (int choice = 0; choice < choices.length; choice++) { 
				    if (choices[choice][1].equals(attributeValue)) {
					out.append(StringUtils.htmlEncode(choices[choice][GPConstants.PARAM_INFO_NAME_OFFSET]));
				    }
				}
			} else {
				out.append(StringUtils.htmlEncode(attributeValue));
			}
			

		} else if (attributeType.equals(GPConstants.PARAM_INFO_CHECKBOX)) {
			attributeValue = (String)attributes.get(attributeName);
			if (attributeValue == null) {
				attributeValue = "";
			}

			out.append("<input name=\"p" + i + "_" + attributeName + "\" type=\"checkbox\"" + (attributeValue.length() > 0 ? " checked" : "") + " disabled >\n");
		} else {
			throw new Exception("Unknown attribute type " + attributeType);
		}

		out.append("</td>\n");

 	} // end for each attribute
	out.append("</tr>\n");
 } // end for each parameter
 return out.toString();
} // end of method
%>


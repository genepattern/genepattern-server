<%@ page import="java.io.IOException,
		 java.net.InetAddress,
		 java.net.MalformedURLException,
		 java.net.URLEncoder,
		 java.util.Collection,
		 java.util.Collections,
		 java.util.Comparator,
		 java.util.Properties,
		 java.util.HashMap,
		 java.util.HashSet,
		 java.util.Iterator,
		 java.util.Set,
		 java.util.TreeMap,
		 java.util.Vector,
		 org.genepattern.server.webapp.AbstractPipelineCodeGenerator,
		 org.genepattern.util.LSID,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.indexer.Indexer"
	session="false" contentType="text/html" language="Java" %>
<%
	// redirect to the fully-qualified host name to make sure that the one cookie that we are allowed to write is useful
	try {
		String fqHostName = System.getProperty("fullyQualifiedHostName");
		if (fqHostName == null) fqHostName = InetAddress.getLocalHost().getCanonicalHostName();
		if (fqHostName.equals("localhost")) fqHostName = "127.0.0.1";
		String serverName = request.getServerName();
		if (!fqHostName.equalsIgnoreCase(serverName)) {
			String queryString = request.getQueryString();
			if (queryString == null) {
				queryString = "";
			} else {
				queryString = "?" + queryString;
			}
			String fqAddress = "http://" + fqHostName + ":" + request.getServerPort() + request.getRequestURI() + queryString;
			response.sendRedirect(fqAddress);
			return;
		}
	} catch (IOException ioe) {
		ioe.printStackTrace();
	}

	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

try {
String userID = GenePatternAnalysisTask.getUserID(request, null); // get userID but don't force login if not defined
boolean userIDKnown = !(userID == null || userID.length() == 0);
Collection tmTasks = new LocalAdminClient(userID).getTaskCatalog();

%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>GenePattern</title>
<style>
.heading { font-family: Arial,Helvetica,sans-serif; background-color: #0E0166; color: white; font-size: 12pt; font-weight: 800; text-align: center; }
.majorCell { border-width: 2; font-size: 10pt; }
.button  { width: 50; }
.wideButton  { width: 100; }
.wideBoldButton  { width: 100; font-weight: bold; color: red }
td { padding-left: 5; }
</style>

<script language="Javascript">

var localAuthority = '<%= LSIDManager.getInstance().getAuthority() %>';

<% if (userIDKnown) { %>
function jmp(button, url, selector, versionSelector) {
	if (selector.selectedIndex != 0) {
		var lsidNoVersion = selector.options[selector.selectedIndex].value;
		var lsidVersion = versionSelector.options[versionSelector.selectedIndex].value;
		window.location= url + lsidNoVersion + '<%= LSID.DELIMITER %>' + lsidVersion;
	} else {
		window.alert('Please select a ' + selector.name + ' to ' + button.value + '.');
	}
}

function exportPipeline(button) {
	var width = 250;
	var height = 200;
	var exportWindow = window.open('askInclude.jsp?name=' + 
		    button.form.pipeline.options[button.form.pipeline.selectedIndex].value + 
		    '<%= LSID.DELIMITER %>' + 
		    button.form.pipelineVersion.options[button.form.pipelineVersion.selectedIndex].value +
		    '&title=' + button.form.pipeline.options[button.form.pipeline.selectedIndex].text, 
		    'export',
		    'alwaysRaised=yes,height=' + height + ',width=' + width + ',menubar=no,location=no,resizable=yes,scrollbars=yes,directories=no,status=no,toolbar=no');
	exportWindow.focus();
}

function getCode(url, pipeline, versionSelector, language, userID) {
	if (pipeline.selectedIndex != 0 && language.selectedIndex != 0) {
		var lsidVersion = versionSelector.options[versionSelector.selectedIndex].value;
		window.location= url + pipeline.options[pipeline.selectedIndex].value +
				 '<%= LSID.DELIMITER %>' + lsidVersion +
				 '&language=' + language.options[language.selectedIndex].value;
	} else {
		var missing_pipeline = (pipeline.selectedIndex == 0);
		var missing_language = (language.selectedIndex == 0);
		window.alert('Please select ' + (missing_pipeline ? "a pipeline" : "") +
			     (missing_pipeline && missing_language ? " and " : "") +
			     (missing_language ? 'a language' : "") + ".");
	}
}

function taskSelect(selector, type) {
	var versionSelector = selector.form[type + 'Version'];
	var lsidNoVersion = selector.options[selector.selectedIndex].value;
	versionSelector.options.length = 0;
	for (i in LSIDs[lsidNoVersion]) {
		versionSelector.options[i] = new Option(LSIDs[lsidNoVersion][i], LSIDs[lsidNoVersion][i]);
	}
	
	// enable/disable the edit button for the task/pipeline depending on ownership
	var enableEdit = true;
	if (new LSID(lsidNoVersion).authorityType == '<%= LSIDUtil.AUTHORITY_MINE %>') {
		enableEdit = true;
	} else {
		enableEdit = false;
	}
	selector.form['edit' + type].disabled = !enableEdit;
	selector.form['view' + type].disabled = (selector.selectedIndex == 0);
	selector.form['run' + type].disabled = (selector.selectedIndex == 0);
	selector.form['export' + type].disabled = (selector.selectedIndex == 0);
}

var LSIDs = new Array();
<%
	TaskInfo taskInfo;
	TaskInfoAttributes tia;
	boolean isPublic;
	boolean isMine;
	String lsid;
	LSID l;
	TreeMap tmNoVersions = new TreeMap();
	Vector v;
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		tia = taskInfo.giveTaskInfoAttributes();
		isPublic = tia.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC);
		isMine = tia.get(GPConstants.USERID).equals(userID);
		if (!isPublic && !isMine) {
			continue;
		}
		lsid = tia.get(GPConstants.LSID);

		try {
			l = new LSID(lsid);
			v = (Vector)tmNoVersions.get(l.toStringNoVersion());
			if (v == null) {
				v = new Vector();
			}
			v.add(l.getVersion());
			tmNoVersions.put(l.toStringNoVersion(), v);
		} catch (MalformedURLException mue) {
			// don't display tasks with bad LSIDs
		}
	}

	for (Iterator itLSIDs = tmNoVersions.keySet().iterator(); itLSIDs.hasNext(); ) {
		lsid = (String)itLSIDs.next();
		v = (Vector)tmNoVersions.get(lsid);
		out.print("LSIDs['" + lsid + "'] = new Array(");
		for (Iterator itVersions = v.iterator(); itVersions.hasNext(); ) {
			out.print("'" + (String)itVersions.next() + "'");
			if (itVersions.hasNext()) {
				out.print(", ");
			}
		}
		out.println(");");
	}
%>

function LSID(lsid) {
	this.lsid = lsid;
	var tokens = lsid.split('<%= LSID.DELIMITER %>');
	this.authority = tokens[2];
	this.namespace = tokens[3];
	this.identifier = tokens[4];
	this.version = tokens[5];
	this.authorityType = (this.authority == '<%= LSIDManager.getInstance().getAuthority() %>') ? 'mine' : (this.authority == '<%= LSIDUtil.BROAD_AUTHORITY %>' ? 'broad' : 'foreign');
}

<% } %> 

<% if (tmTasks.size() == 0) { %>
function blinkInstallModules() {
	document.forms["index"].installButton.setAttribute("class", installModulesState ? "wideButton" : "wideBoldButton");
	installModulesState = !installModulesState;	
	setTimeout("blinkInstallModules()", 1000); // delay 1000 milliseconds
}
installModulesState = true;
setTimeout("blinkInstallModules()", 1000); // delay 1000 milliseconds

<% } %>

</script>

</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<form name="index" method="post">

<% if (tmTasks.size() == 0) { %>
<font size="+1" color="red">
<br>
There are currently no modules installed on this server.  
You may select and install modules from the <a href="taskCatalog.jsp">Broad website</a> or from your own collection.
<br><br>

<% } %>

</font>

<table cellpadding="10" width="100%" border="1" rules="all" frame="border">
<% if (userIDKnown) { %>
<tr>
    <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading" colspan="2">Pipelines</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>

			<td valign="top" align="right">
			<%= taskCatalog(tmTasks, "pipeline", "pipeline catalog", GPConstants.TASK_TYPE_PIPELINE, userID, true) %>
			<nobr>version <select name="pipelineVersion"></select></nobr>
			</td>

		</tr>
		<tr>
			<td valign="top" align="center">
				<input type="button" value="run" name="runpipeline" class="button" onclick="jmp(this, 'runPipeline.jsp?cmd=run&name=', document.forms['index'].pipeline, document.forms['index'].pipelineVersion)">
				<input type="button" value="view" name="viewpipeline" class="button" onclick="jmp(this, 'viewPipeline.jsp?name=', document.forms['index'].pipeline, document.forms['index'].pipelineVersion)">
				<input type="button" value="edit" name="editpipeline" class="button" onclick="jmp(this, 'pipelineDesigner.jsp?name=', document.forms['index'].pipeline, document.forms['index'].pipelineVersion)">
				<input type="button" value="export" name="exportpipeline" class="button" onclick="exportPipeline(this)">
			</td>
		</tr>

		<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>

		<tr>
			<td colspan="2" align="center" valign="top">
				<input type="button" value="create" class="button" onclick="javascript: window.location='pipelineDesigner.jsp'">
				<input type="button" value="import..." class="button" onclick="javascript:window.location='addZip.jsp'">
			</td>
		</tr>
	</table> <!-- end pipeline cell -->
    </td>

    <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading" colspan="2">Tasks</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>

			<td valign="top" align="right">
			<%= taskCatalog(tmTasks, "task", "task catalog", null, userID, false) %>
			<nobr>version <select name="taskVersion"></select></nobr>
			</td>

		</tr>
		<tr>
			<td valign="top" align="center">
				<input type="button" value="run" name="runtask" class="button" onclick="jmp(this, 'runTask.jsp?name=', document.forms['index'].task, document.forms['index'].taskVersion)">
				<input type="button" value="view" name="viewtask" class="button" onclick="jmp(this, 'addTask.jsp?view=1&name=', document.forms['index'].task, document.forms['index'].taskVersion)">
				<input type="button" value="edit" name="edittask" class="button" onclick="jmp(this, 'addTask.jsp?name=', document.forms['index'].task, document.forms['index'].taskVersion)">
				<input type="button" value="export" name="exporttask" class="button" onclick="jmp(this, 'makeZip.jsp?name=', document.forms['index'].task, document.forms['index'].taskVersion)">
			</td>
		</tr>

		<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>

		<tr>
			<td colspan="2" align="center">
				<input type="button" value="create" class="button" onclick="javascript:window.location='addTask.jsp'">
				<input type="button" value="import..." class="button" onclick="javascript:window.location='addZip.jsp'">
			</td>
		</tr>
	</table> <!-- end task cell -->
    </td>

    <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading" colspan="2">Admin</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>

			<td valign="middle" align="right">
				Jobs
			</td>

			<td valign="top" align="left">
<!--				<input type="button" value="status" class="wideButton" onclick="javascript:window.location='getStatus.jsp'"><br> -->
				<input type="button" value="results" class="wideButton" onclick="javascript:window.location='zipJobResults.jsp'"><br>
			</td>
		</tr>

		<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>

		<tr>
			<td valign="middle" align="right">
				Modules
			</td>
			<td valign="top" align="left">
				<input type="button" name="installButton" value="install/update" class="wideButton" onclick="javascript:window.location='taskCatalog.jsp'"><br>
				<input type="button" value="delete" class="wideButton" onclick="javascript:window.location='deleteTask.jsp'"><br>
			</td>
		</tr>

		<tr>
			<td colspan="2">
				<hr noshade size="1">
			</td>
		</tr>
		<tr>
			<td valign="middle" align="middle" colspan="2">
				<a href="adminServer.jsp">Server administration</a>
			</td>
		</tr>
	</table> <!-- end admin cell -->
    </td>

</tr>

<% } %>
<tr>

    <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading" colspan="2">Documentation</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>
			<td valign="top" align="left">
				<a href="http://www.broad.mit.edu/cancer/software/genepattern/tutorial/" target="_new">User's Manual/Tutorial</a><br><br>
				Release notes (<a href="relNotes.html">local</a>/<a href="http://www.broad.mit.edu/cancer/software/genepattern/doc/relnotes/current/">public website</a>)<br><br>
				<a href="http://www.broad.mit.edu/cancer/software/genepattern/faq/">FAQ</a><br><br>
				<a href="http://www.broad.mit.edu/cancer/software/genepattern/datasets/">Public datasets</a><br><br>
				<a href="getTaskDoc.jsp">Task documentation</a>
			</td>

		</tr>
	</table> <!-- end documentation cell -->
    </td>

    <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading">Resources</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>
			<td valign="top" align="left">
				<a href="Web_Installers/install.htm?server=<%= URLEncoder.encode("http://" + request.getServerName() + ":" + request.getServerPort()) %>">Install</a> graphical client<br><br>
				<a href="mailto:gp-users-join@broad.mit.edu?body=Just send this!">Subscribe to gp-users mailing list</a><br><br>
				<a href="mailto:gp-help@broad.mit.edu">Report bugs</a><br><br>
				<a href="http://www.broad.mit.edu/cancer/software/genepattern/forum/">User Forum</a>
<!-- 
		<form action="search.jsp" name="indexSearch">
			search tasks, jobs, documentation: <nobr><input type="text" class="little" size="20" name="search"
			      value="" ><input type="image" src="search.jpeg" alt="search" value="?" onclick="this.form.submit()" align="top" class="little"></nobr>
			<input type="hidden" name="<%= Indexer.TASK %>" value="1">
			<input type="hidden" name="<%= Indexer.TASK_DOC %>" value="1">
			<input type="hidden" name="<%= Indexer.TASK_SCRIPTS %>" value="1">
			<input type="hidden" name="<%= Indexer.JOB_PARAMETERS %>" value="1">
			<input type="hidden" name="<%= Indexer.JOB_OUTPUT %>" value="1">
			<input type="hidden" name="<%= Indexer.MANUAL %>" value="1">
		</form>
-->
			</td>

		</tr>
	</table> <!-- end resources cell -->
    </td>

    <td valign="top">
	<table class="majorCell" width="100%">
		<tr>
		<td class="heading" colspan="2">Programming</td>
		</tr>
		<tr><td height="3"></td></tr>
		<tr>
			<td valign="top" align="left" colspan="2">
				Download GenePattern library:
			</td>
		</tr>

		<tr>
			<td valign="top" align="left">
				Java
			</td>
			<td valign="top" align="left">
				<a href="downloads/GenePattern.zip">.zip</a>
			</td>

		</tr>

		<tr>
			<td valign="top" align="left">
				R
			</td>

			<td valign="top" align="left">
				<a href="downloads/GenePattern_0.1-0.zip">.zip</a> &nbsp;
				<a href="downloads/GenePattern_0.1-0.tar.gz">.tar.gz</a> &nbsp;
				<a href="GenePattern.R">source</a>
			</td>
		</tr>
		<tr>
			<td valign="top" align="left">
				MATLAB
			</td>
			<td valign="top" align="left">
				<a href="downloads/GenePatternMatlab.zip">.zip</a>
			</td>

		</tr>

<% if (userIDKnown) { %>
		<tr>
			<td valign="top" align="left" colspan="2">
					<hr noshade size="1">
					Download pipeline code:<br>
					<%= taskCatalog(tmTasks, "code", "pipeline", GPConstants.TASK_TYPE_PIPELINE, userID, true) %>
					<br>
					version: <select name="codeVersion">
					</select>
					<select name="pipelineLanguage">
					<option value="">language</option>
<%
					// discover (at runtime) all of the code generators that are available
					Collection cLanguages = AbstractPipelineCodeGenerator.getLanguages();
					for (Iterator itLanguages = cLanguages.iterator(); itLanguages.hasNext();  ) {
						String name = (String)itLanguages.next();
%>
						<option value="<%= name %>"><%= name %></option>
<%
					}
%>
					</select>
				<input type="button" value="code" class="button" onclick="getCode('getPipelineCode.jsp?download=1&name=', document.forms['index'].code, document.forms['index'].codeVersion, document.forms['index'].pipelineLanguage, '<%= userID %>')"><br>
			</td>
		</tr>
<% } %>

	</table> <!-- end programming cell -->
    </td>

</tr>

</form>
</table>

<script language="Javascript">
taskSelect(document.forms['index'].pipeline, 'pipeline');
taskSelect(document.forms['index'].task, 'task');
</script>
<jsp:include page="footer.jsp"></jsp:include>

</body>
</html>
<% } catch (Throwable t) {
	t.printStackTrace();
   }
%>
<%! public String taskCatalog(Collection tmTasks, String selectName, String caption, String type, String userID, boolean bIncludePipelines) {
	StringBuffer sbCatalog = new StringBuffer();
	sbCatalog.append("<select name=\"" + selectName + "\" onchange=\"taskSelect(this, '" + selectName + "')\">\n");
	sbCatalog.append("<option value=\"\">" + caption + "</option>\n");
	String name;
	String description;
	String display;
	String lsid;
	TaskInfo taskInfo;
	TaskInfoAttributes tia;
	boolean isPublic;
	boolean isMine;
	String taskType;

	String authorityType = null;

	// used to avoid displaying multiple versions of same basic task
	HashMap hmLSIDsWithoutVersions = new HashMap();
	String versionlessLSID = null;

	LSID l = null;

	// put public and my tasks into list first
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		tia = taskInfo.giveTaskInfoAttributes();
		taskType = tia.get(GPConstants.TASK_TYPE);
		if (type != null && !taskType.equals(type)) continue;
		if (!bIncludePipelines && taskType.equals(GPConstants.TASK_TYPE_PIPELINE)) continue;
		display = taskInfo.getName();
		if (taskType.equals(GPConstants.TASK_TYPE_PIPELINE)) {
			String dotPipeline = "." + GPConstants.TASK_TYPE_PIPELINE;
			if (display.endsWith(dotPipeline)) {
				display = display.substring(0, display.length() - dotPipeline.length());
			}
		}
		description = taskInfo.getDescription();
		isPublic = tia.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC);
		isMine = tia.get(GPConstants.USERID).equals(userID);
		name = taskInfo.getName();
		lsid = tia.get(GPConstants.LSID);

		try {
			l = new LSID(lsid);

			versionlessLSID = l.toStringNoVersion();
			String key = versionlessLSID+"."+name;			
			if (hmLSIDsWithoutVersions.containsKey(key) &&
			    ((TaskInfo)hmLSIDsWithoutVersions.get(key)).getName().equals(name)) {
				continue;
			}
			hmLSIDsWithoutVersions.put(key, taskInfo);
			authorityType = LSIDManager.getInstance().getAuthorityType(l);
		} catch (MalformedURLException mue) {
			System.out.println("index.jsp: skipping " + mue.getMessage() + " in " + lsid);
			continue;
		}

		
		if (isPublic || isMine) {
			sbCatalog.append("<option value=\"" + (lsid != null ? l.toStringNoVersion() : taskInfo.getName()) + 
					 "\" class=\"tasks-" +  authorityType + "\"" +
					 " title=\"" + GenePatternAnalysisTask.htmlEncode(description) + ", " + l.getAuthority() + "\"" + ">" + display + "</option>\n");
		}
	}
	sbCatalog.append("</select>\n");
	return sbCatalog.toString();
    }
%>
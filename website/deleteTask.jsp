<%@ page import="org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.util.LSID,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
		 org.genepattern.data.pipeline.PipelineModel,
		 org.genepattern.webservice.WebServiceException,
		  org.genepattern.util.StringUtils,
		 java.io.PrintWriter,
		 java.net.MalformedURLException,
		 java.util.Collection,
		 java.util.HashMap,
		 java.util.Iterator,
		 java.util.Map,
		 java.util.Vector"
	session="false" contentType="text/html" language="Java" buffer="50kb" %>
<%
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String userID = AccessManager.getUserID(request, response); // will force login if necessary
if (userID == null) return; // come back after login
LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);
String DELETE_LSID = "del";

try {

%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link href="favicon.ico" rel="shortcut icon">
<title>Delete tasks</title>
<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
<script language="javascript">
function checkAll(lastLSIDNoVersion, bChecked) {
	var frm = document.forms['frmDelete'];
	for (i = 0; i < frm.elements.length; i++) {
		var item = frm.elements[i];
		if (item.type != "checkbox") continue;
		if (item.value.indexOf(lastLSIDNoVersion + '<%= LSID.DELIMITER %>') != 0) continue;
		var readonly = item.getAttribute('readonly');
		if (readonly != null) continue;
		item.checked = bChecked;
	}
}

function confirmDelete() {
	var frm = document.forms['frmDelete'];
	var selection = '';
	for (i = 0; i < frm.elements.length; i++) {
		var item = frm.elements[i];
		if (item.type != "checkbox") continue;
		if (item.value.indexOf('<%= LSID.URN %><%= LSID.DELIMITER %><%= LSID.SCHEME %>') != 0) continue;
		var readonly = item.getAttribute('readonly');
		if (readonly != null) continue;
		if (item.checked) {
			if (selection.length > 0) selection = selection + ', ';
			selection = selection + item.getAttribute('displayName');
		}
	}
	if (selection.length > 0 && window.confirm('Really delete ' + selection + '?')) { 
		frm.submit();
	}
}

</script>
</head>
<body>

<jsp:include page="navbar.jsp"></jsp:include>
<h2>Delete tasks</h2>

<%
// process deletions now, then show remaining tasks
if (request.getParameter(DELETE_LSID) != null) {
	String[] deleteLSIDs = request.getParameterValues(DELETE_LSID);
	for (int i = 0; i < deleteLSIDs.length; i++) {
	    try {
		taskIntegratorClient.deleteTask(deleteLSIDs[i]);
%>
		<script language="Javascript">
		document.writeln('Deleted ' + deleteNavbarItem('<%= deleteLSIDs[i] %>') + '<br>');
		</script>
<%
		out.flush();
	    } catch (WebServiceException wse) {
		out.println(wse.toString());
	    }
	}
%>
	<hr>
<%
}

LocalAdminClient adminClient = new LocalAdminClient(userID);
Collection tmTasks = adminClient.getTaskCatalog(); 

HashMap hmLSIDsUsedByPipelines = new HashMap();

TaskInfo ti = null;
TaskInfoAttributes tia = null;
HashMap hmLSIDsWithoutVersions = new HashMap();
LSID l;
String lsid;
Vector vTaskInfo = null;


String BROAD_AUTHORITY = "broad.mit.edu";
String authorityType = null;

// compute which LSIDs are used by pipelines, generating a HashMap of LSIDs of 
// tasks that are in use whose values are Vectors of pipeline TaskInfo objects
for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
	ti = (TaskInfo)itTasks.next();
	if (!ti.getName().endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) continue;
	tia = ti.giveTaskInfoAttributes();
	lsid = tia.get(GPConstants.LSID);
	l = new LSID(lsid);
	PipelineModel model = null;
	String xml = (String)tia.get(GPConstants.SERIALIZED_MODEL);
	try {
		model = PipelineModel.toPipelineModel(xml);
	} catch (Throwable t) {
		System.err.println(t.toString() + " loading pipeline model " + ti.getName() + " - " + lsid);
		System.err.println(xml);
		break;
	}
	Map mDependencies = model.getLsidDependencies(); // LSID/Vector of TaskInfo map
	for (Iterator itSubTasks = mDependencies.keySet().iterator(); itSubTasks.hasNext(); ) {
		String keyLSID = (String)itSubTasks.next();
		String taskName = (String)mDependencies.get(keyLSID);
		vTaskInfo = (Vector)hmLSIDsUsedByPipelines.get(keyLSID);
		if (vTaskInfo == null) {
			vTaskInfo = new Vector();
			hmLSIDsUsedByPipelines.put(keyLSID, vTaskInfo);
		}
		// mark that task keyLSID is used by this pipeline task
		TaskInfo t = adminClient.getTask(lsid);
		if (!vTaskInfo.contains(t)) {
			vTaskInfo.add(t);
		}
	}
}

HashMap hmNumVersions = new HashMap(); // key = LSID without version+taskName, value = Integer(numVersions)
// compute number of versions for each LSID
for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
	ti = (TaskInfo)itTasks.next();
	tia = ti.giveTaskInfoAttributes();
	lsid = tia.get(GPConstants.LSID);
	try {
		l = new LSID(lsid);
	} catch (MalformedURLException mue) {
		continue;
	}
	String versionLess = l.toStringNoVersion();
	Integer i = (Integer)hmNumVersions.get(versionLess+"-"+ti.getName());
	if (i == null) {
		i = new Integer(1);
	} else {
		i = new Integer(i.intValue() + 1);
	}
	hmNumVersions.put(versionLess+"-"+ti.getName(), i);
}

%>

<form method="post" name="frmDelete">

<table cols="3" width="100%">

<!-- color key -->
<tr>
<td align="left" valign="top">
color key: <b><span class="tasks-<%= LSIDUtil.AUTHORITY_MINE %>">your tasks</span> |
<span class="tasks-<%= LSIDUtil.AUTHORITY_BROAD %>">Broad tasks</span> |
<span class="tasks-<%= LSIDUtil.AUTHORITY_FOREIGN %>">other tasks</span></b>
</td>
<td align="left" colspan="2">
	<input type="button" name="delete" value="delete selected tasks" onclick="confirmDelete()">
	<input type="button" value="install tasks from GenePattern repository" onclick="window.location='taskCatalog.jsp'">
</td>
</tr>

<tr style="font-weight: bold; font-size: +1">
<td>
task name
</td>
<td align="left">
version
</td>
<td align="left">
used by
</td>
</tr>
<tr>
<td colspan="3">
<hr>
</td>
</tr>

<%
String lastTaskName = "";
String lastLSIDNoVersion = "";
LSID lastLSID = null;
LSIDManager lsidManager = LSIDManager.getInstance();
String localAuthority = lsidManager.getAuthority();
String authority = null;
int numVersions = 0;
int n = 0;

for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
	ti = (TaskInfo)itTasks.next();
	String name = ti.getName();
	String description = ti.getDescription();
	tia = ti.giveTaskInfoAttributes();
	lsid = tia.get(GPConstants.LSID);
	try {
		l = new LSID(lsid);
	} catch (MalformedURLException mue) {
		continue; // unable to delete task without LSID
	}
	authority = "";
	if (!l.isSimilar(lastLSID)) {
		lastTaskName = ti.getName();
		lastLSIDNoVersion = l.toStringNoVersion();
		lastLSID = l;
		n = 0;

		boolean bMine = tia.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC) || tia.get(GPConstants.USERID).equals(userID);
		String owner = tia.get(GPConstants.USERID);
		if (owner != null && owner.indexOf("@") != -1) owner = " (" + owner.substring(0, owner.indexOf("@")) + ")";
		authority = (l == null ? "" : l.getAuthority());
		authorityType = lsidManager.getAuthorityType(l);
		numVersions = ((Integer)hmNumVersions.get(l.toStringNoVersion()+"-"+ti.getName())).intValue();
%>
		<tr><td valign="top" rowspan="<%= numVersions %>">
		<span class="tasks-<%= authorityType %>"><b><%= ti.getName() %></b></span>
		<%= !bMine ? owner : "" %>
		<%= authorityType.equals(LSIDUtil.AUTHORITY_FOREIGN) ? (" (" + authority + ")") : "" %>
		<br><font size="1"><%= doDescription(ti.getDescription()) %></font>
		</td>
<%
	}
	String reason = tia.get(GPConstants.VERSION);
	if (reason.equals("1.0") && authority.equals(BROAD_AUTHORITY)) {
		reason = "";
	}
	if (!reason.equals("")) {
		reason = " (" + reason + ")";
	}
	boolean inUse = hmLSIDsUsedByPipelines.containsKey(l.toString());
	if (!inUse) n++;
%>
	<td valign="top"><nobr>
	<input type="checkbox" name="<%= DELETE_LSID %>" value="<%= l.toString() %>" displayName="<%=name %> (<%=l.getVersion() %>)"<%= inUse ? " disabled" : "" %>><a href="<%= name.endsWith(GPConstants.TASK_TYPE_PIPELINE) ? "viewPipeline.jsp" : "addTask.jsp" %>?<%= GPConstants.NAME %>=<%= l.toString() %>&view=1" name="<%= l.toString() %>"><%= l.getVersion() %></a></nobr>
	<%= reason %>
	</td>
	<td valign="top" align="left">
<%	if (inUse) {
		//out.print(" used by ");
		boolean multi = false;
		vTaskInfo = (Vector)hmLSIDsUsedByPipelines.get(l.toString());
		if (vTaskInfo == null) {
			System.out.println(l.toString() + " used by " + lsid + " -> " + vTaskInfo);
		} else {
			for (Iterator itPipelines = vTaskInfo.iterator(); itPipelines.hasNext(); ) {
				TaskInfo t = (TaskInfo)itPipelines.next();
				if (t == null) {
					System.err.println("deleteTask: unable to load entry from " + l.toString() + " pipeline");
					continue;
				}
				tia = t.giveTaskInfoAttributes();
				String lsid2 = tia.get(GPConstants.LSID);
				LSID l2 = new LSID(lsid2);
				if (multi) out.print(", ");
				//out.print("<a href=\"" + (name.endsWith(GPConstants.TASK_TYPE_PIPELINE) ? "pipelineDesigner.jsp" : "addTask.jsp") + "?" + GPConstants.NAME + "=" + l2.toString() + "&view=1\">");
				out.print("<a href=\"#" + l2.toString() + "\">");
				out.print(t.getName() + " ver. " + l2.getVersion());
				out.print("</a>");
				multi = true;
			}
		}
	}
	if (n == 1 && numVersions > 1) {
		out.println(doCheckers(lastLSIDNoVersion, n));
	}
%>
	</td>
	</tr>
<%
	out.flush();
} // end of loop for all tasks
%>

<tr><td> </td><td colspan="2" align="left">
	<br>
	<input type="button" name="delete" value="delete selected tasks" onclick="confirmDelete()">
	<input type="button" value="install tasks from GenePattern repository" onclick="window.location='taskCatalog.jsp'">
</td></tr>

</table>
<br>

</form>
<jsp:include page="footer.jsp"></jsp:include>

</body>
</html>
<% } catch (Throwable t) {
	t.printStackTrace(new PrintWriter(out));
   }
%>
<%! public String doCheckers(String lastLSIDNoVersion, int numVersions) {
	StringBuffer sb = new StringBuffer();
	if (numVersions > 0) {
		sb.append("<a href=\"javascript:checkAll('" + lastLSIDNoVersion + "', true)\">check&nbsp;all</a>");
		sb.append("&nbsp;&nbsp;&nbsp;");
		sb.append("<a href=\"javascript:checkAll('" + lastLSIDNoVersion + "', false)\">uncheck&nbsp;all</a>");
	}
	return sb.toString();
    }
%>
<%! public String doDescription(String description) {
	int start = description.indexOf("http://");
	if (start == -1) start = description.indexOf("https://");
	if (start == -1) start = description.indexOf("ftp://");
	if (start != -1) {
		int end = description.indexOf(" ", start);
		if (end == -1) end = description.indexOf(")", start);
		if (end == -1) end = description.length();
		description = StringUtils.htmlEncode(description.substring(0, start)) + 
				"<a href=\"" + description.substring(start, end) + "\" target=\"_blank\">" + 
				description.substring(start, end) + "</a>" + 
				StringUtils.htmlEncode(description.substring(end));
	}
	return description;
    }
%>
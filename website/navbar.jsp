<%@ page contentType="text/html" language="Java" session="false"
	import="java.net.MalformedURLException,
		java.util.Collection,
		java.util.Iterator,
		java.util.HashSet,
		java.util.HashMap,
		java.util.Set,
		java.util.Vector,
		java.util.LinkedHashSet,
		org.genepattern.webservice.TaskInfo,
		org.genepattern.webservice.TaskInfoAttributes,
		org.genepattern.webservice.JobInfo,
		org.genepattern.webservice.WebServiceException,
     		org.genepattern.server.genepattern.GenePatternAnalysisTask,
		org.genepattern.server.genepattern.LSIDManager,
		org.genepattern.server.webservice.server.local.LocalAdminClient,
		org.genepattern.server.webservice.server.local.LocalAnalysisClient,
 	 	org.genepattern.util.GPConstants,
		org.genepattern.util.LSIDUtil,
		org.genepattern.util.LSID,
		org.genepattern.server.indexer.Indexer" %>
<% { %>
<% 
String DIVIDER = "------";

if (request.getAttribute("navbar") == null) { 

String userID = GenePatternAnalysisTask.getUserID(request, null); // get userID but don't force login if not defined
boolean userUnknown = (userID == null || userID.equals(""));
Collection tmTasks = null;
int recentCount = Integer.parseInt(System.getProperty("recentJobsToDisplay", "3"));
Vector recentTasks = new Vector();
Vector recentPipes = new Vector();

try { 
	LocalAdminClient adminClient = new LocalAdminClient(userID);
	tmTasks = adminClient.getTaskCatalog();
	
	LinkedHashSet recentTasksSet = new LinkedHashSet();
	LinkedHashSet recentPipesSet = new LinkedHashSet();
	LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
	JobInfo[] jobs = analysisClient.getJobs(userID, -1, Integer.MAX_VALUE, false);

	for (int i=0; (i < jobs.length) && (recentCount > 0); i++ ){
		JobInfo job = jobs[i];
		String jobName = job.getTaskName();
		if (jobName.endsWith(".pipeline")){
			if (recentPipes.size() >= recentCount) continue;
			if (!recentPipesSet.contains(job.getTaskName())){
				recentPipesSet.add(job.getTaskName());
				recentPipes.add(adminClient.getTask(job.getTaskLSID()));
 			}
		} else {
			if (recentTasks.size() >= recentCount) continue;
			if (!recentTasksSet.contains(job.getTaskName())){
				recentTasksSet.add(job.getTaskName());
				recentTasks.add(adminClient.getTask(job.getTaskLSID()));
 			}
		}
		if ((recentTasks.size() >= recentCount) && (recentPipes.size() >= recentCount)){
			break;
		}
	   }

} catch (Exception e) {
	tmTasks = new HashSet();
}
String EMAIL_ADDRESS = "email address";
String SEARCH = "search";

%>
<!-- begin navbar.jsp -->
<script language="javascript">
var IGNORE = "dontJump";
var DIVIDER = "------";
var CREATE = "create";
var EDIT = "edit";
var VIEW = "view";
var RUN = "run";

// handle focus and blur events for a field
function ufocus(fld, focus, deflt ) {
    if (focus) {
	if ((fld.value == deflt) ) { 
		fld.value = "";
	} else { 
		fld.select();
	}
    } else {
	if ((fld.value == "")) { 
		fld.value = deflt;
	}
    }
}
 
function jumpTo(btn) {
	var task = document.forms['searchForm'].Task;
	var pipeline = document.forms['searchForm'].Pipeline;
	if (task.selectedIndex == 0 && pipeline.selectedIndex == 0) {
		window.alert('Please select either a pipeline or task to edit or run');
		return;
	}
	if (btn.value == EDIT || btn.value == CREATE) {
		if (task.selectedIndex != 0) {
			window.location = "addTask.jsp?<%= GPConstants.NAME %>=" + task.options[task.selectedIndex].value;
		} else if (pipeline.selectedIndex != 0) {
			window.location = "pipelineDesigner.jsp?<%= GPConstants.NAME %>=" + pipeline.options[pipeline.selectedIndex].value;
		}
	} else if (btn.value == VIEW) {
		if (task.selectedIndex != 0) {
			window.location = "addTask.jsp?<%= GPConstants.NAME %>=" + task.options[task.selectedIndex].value + "&view=1";
		} else if (pipeline.selectedIndex != 0) {
			window.location = "viewPipeline.jsp?<%= GPConstants.NAME %>=" + pipeline.options[pipeline.selectedIndex].value;
		}
	} else if (btn.value == RUN) {
		if (task.selectedIndex > 1) {
			window.location = "runTask.jsp?<%= GPConstants.NAME %>=" + task.options[task.selectedIndex].value;
		} else if (pipeline.selectedIndex > 1) {
			window.location = "runTask.jsp?cmd=run&<%= GPConstants.NAME %>=" + pipeline.options[pipeline.selectedIndex].value;
		}
	}
}

function changeTask() {
	document.forms['searchForm'].Pipeline.selectedIndex = 0;

	// enable/disable the edit button for the task/pipeline depending on ownership
	var enableEdit = true;
	var sel = document.forms['searchForm'].Task;
	var taskLSID = sel[sel.selectedIndex].value;
	if (taskLSID == IGNORE){
		document.forms['searchForm'].navbarrun.disabled = true;
		document.forms['searchForm'].navbaredit.disabled = true;
		return;
	}

	var create = (sel.selectedIndex == 1);

	if (new LSID(taskLSID).authorityType == '<%= LSIDUtil.AUTHORITY_MINE %>' || create) {
		enableEdit = true;
	} else {
		enableEdit = false;
	}
	document.forms['searchForm'].navbaredit.value = (enableEdit ? (create ? CREATE : EDIT) : VIEW);
	document.forms['searchForm'].navbarrun.disabled = (sel.selectedIndex == 0 || create);
	document.forms['searchForm'].navbaredit.disabled = (sel.selectedIndex == 0);
}

function changePipeline() {
	document.forms['searchForm'].Task.selectedIndex = 0;

	// enable/disable the edit button for the task/pipeline depending on ownership
	var enableEdit = true;
	var sel = document.forms['searchForm'].Pipeline;
	var taskLSID = sel[sel.selectedIndex].value;
	if (taskLSID == IGNORE){
		document.forms['searchForm'].navbarrun.disabled = true;
		document.forms['searchForm'].navbaredit.disabled = true;
		return;
	}

	var create = (sel.selectedIndex == 1);
	if (new LSID(taskLSID).authorityType == '<%= LSIDUtil.AUTHORITY_MINE %>' || create) {
		enableEdit = true;
	} else {
		enableEdit = false;
	}
	document.forms['searchForm'].navbaredit.value = (enableEdit ? (create ? CREATE : EDIT) : VIEW);
	document.forms['searchForm'].navbarrun.disabled = (sel.selectedIndex == 0);
	document.forms['searchForm'].navbaredit.disabled = (sel.selectedIndex == 0);
}

// add an item to either the task or pipeline dropdown list
function addNavbarItem(name, lsid) {
	if (<%= userUnknown%>) return; // no selections

	var taskType = name.substr(-(".<%= GPConstants.TASK_TYPE_PIPELINE %>".length)) == ".<%= GPConstants.TASK_TYPE_PIPELINE %>" ? "Pipeline" : "Task";
	var selector = document.forms['searchForm'][taskType];
	var l = new LSID(lsid);
	for (i = 0; i < selector.options.length; i++) {
		if (selector.options[i].text == name) return;
	}
	var newOption = new Option(name, lsid);
	// set the class for this option to get the right coloring
	newOption.className = "navbar-tasks-" + l.authorityType;
//	if (l.authorityType == "<%= LSIDUtil.AUTHORITY_FOREIGN %>") {
		newOption.setAttribute("title", l.getAuthority());
//	}
	selector.options[selector.options.length] = newOption;
	selector.options[selector.options.length-1].selected = true; // highlight it
	eval("change" + taskType + "()"); // call either changeTask() or changePipeline()
}

// delete an item from either the task or pipeline dropdown list
function deleteNavbarItem(lsid) {
       if (<%= userUnknown%>) return; // no selections
	var selector = document.forms['searchForm']['Task'];
	var name = "";
	for (option=0; option < selector.length; option++) {
		if (selector.options[option].value == lsid) {
			name = selector.options[option].text;
			selector.options[option] = null; // delete this entry
			selector.options[0].selected = true; // highlight the top entry
			option--;
		}
	}
	selector = document.forms['searchForm']['Pipeline'];
	for (option=0; option < selector.length; option++) {
		if (selector.options[option].value == lsid) {
			name = selector.options[option].text;
			selector.options[option] = null; // delete this entry
			selector.options[0].selected = true; // highlight the top entry
			option--;
		}
	}
	name = name + " version " + new LSID(lsid).version;
	return name;
}


function LSID(lsid) {
	this.lsid = lsid;
	var tokens = lsid.split('<%= LSID.DELIMITER %>');
	this.authority = tokens[2];
	this.namespace = tokens[3];
	this.identifier = tokens[4];
	this.version = tokens[5];
	this.authorityType = (this.authority == '<%= LSIDManager.getInstance().getAuthority() %>'.replace(" ", "+")) ? '<%= LSIDUtil.AUTHORITY_MINE %>' : (this.authority == '<%= LSIDUtil.BROAD_AUTHORITY %>' ? '<%= LSIDUtil.AUTHORITY_BROAD %>' : '<%= LSIDUtil.AUTHORITY_FOREIGN %>');
}

function checkEnableNavbar() {
       if (<%= userUnknown%>) return; // no selections
	var enableEdit = false;
	var frm = document.forms['searchForm'];
	var sel = frm['Task'];
	for (option=0; option < sel.length; option++) {
		if (sel.options[option].selected) {
			enableEdit = true;
			break;
		}
	}
	if (!enableEdit) {
		sel = frm['Pipeline'];
		for (option=0; option < sel.length; option++) {
			if (sel.options[option].selected) {
				enableEdit = true;
				break;
			}
		}
	}
	var create = (sel.selectedIndex == 1);
	document.forms['searchForm'].navbaredit.value = (enableEdit ? (create ? CREATE : EDIT) : VIEW);
	document.forms['searchForm'].navbarrun.disabled = (sel.selectedIndex == 0);
	document.forms['searchForm'].navbaredit.disabled = (sel.selectedIndex == 0);
}

</script>

<div id="navbar" class="navbar">
	<form action="login.jsp" name="login">
	<table width="100%">
	<tr>
		<td class="navbar" valign="top">
			<a href="index.jsp" class="navbar"><img src="GenePatternLogo.png" border="0" alt="home" align="texttop"></a> &nbsp;
		</td>
		<td align="right" class="navbar" valign="top">
			<nobr>
			<% if (request.getAttribute(GPConstants.USER_LOGGED_OFF) == null) { %>
				<% if (userUnknown) { %>
					<input type="text" class="little" size="30" name="<%= GPConstants.USERID %>" value="<%= EMAIL_ADDRESS %>" onfocus="ufocus(this, true, '<%= EMAIL_ADDRESS %>')" onblur="ufocus(this, false, '<%= EMAIL_ADDRESS %>')"> 
					<input type="submit" value="sign in" class="little">
				<% } else { %>
					<a href="login.jsp" class="navbar">sign out</a> <%= GenePatternAnalysisTask.htmlEncode(userID) %>
				<% } %>
			<% } %>
			</nobr>
			<br>
			<a href="about.jsp" class="navbar">about</a>
		</td>
	</tr>
	</form>
	</table>

	<form action="search.jsp" name="searchForm">

	<table width="100%">
	<tr>
	<td class="navbar" valign="top">
		<% if (!userUnknown) { %>
			<%= _taskCatalog(tmTasks, recentPipes, "Pipeline", "changePipeline();", GPConstants.TASK_TYPE_PIPELINE, userID, request.getParameter(GPConstants.NAME)) %>
			<%= _taskCatalog(tmTasks, recentTasks, "Task", "changeTask();", null, userID, request.getParameter(GPConstants.NAME)) %>
			<nobr>
				<input type="button" value="run" name="navbarrun" onclick="jumpTo(this)" disabled> 
				<input type="button" value="edit" name="navbaredit" onclick="jumpTo(this)" disabled>
			</nobr>
			<script language="Javascript">
				checkEnableNavbar();
			</script>
		<% } %>
	</td>
	<td align="right" valign="top" class="navbar">
			<nobr><input type="text" class="little" size="10" name="search" 
			      value="<%= request.getParameter("search") != null ? 
				request.getParameter("search") : SEARCH %>" onfocus="ufocus(this, true, '<%= SEARCH %>')" 
				onblur="ufocus(this, false, '<%= SEARCH %>')"><input type="image" src="search.jpeg" 
				alt="search" value="?" onclick="this.form.submit()" align="top" 
				class="little"></nobr>
			<input type="hidden" name="<%= Indexer.TASK %>" value="1">
			<input type="hidden" name="<%= Indexer.TASK_DOC %>" value="1">
			<input type="hidden" name="<%= Indexer.TASK_SCRIPTS %>" value="1">
			<input type="hidden" name="<%= Indexer.JOB_PARAMETERS %>" value="1">
			<input type="hidden" name="<%= Indexer.JOB_OUTPUT %>" value="1">
			<input type="hidden" name="<%= Indexer.MANUAL %>" value="1">
	</td>
	</tr>
	</table>
	</form>
</div>

<!-- end navbar.jsp -->
<% out.flush(); %>
<% request.setAttribute("navbar", "already set"); %>
<% } %>
<% } %>
<%! private String _taskCatalog(Collection tmTasks, Vector recent, String selectorName, String onSelectURL, String type, String userID, String requestedName) {
	String IGNORE = "dontJump";
	String DIVIDER = "";
	int maxNameWidth = 0; 
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		TaskInfo task = (TaskInfo)itTasks.next();
		maxNameWidth = Math.max(maxNameWidth, task.getName().length());
			}
	StringBuffer divBuff = new StringBuffer(DIVIDER);
	for (int i=0; i < maxNameWidth; i++){
		divBuff.append("-");
	}
	DIVIDER=divBuff.toString();


	StringBuffer sbCatalog = new StringBuffer();
	sbCatalog.append("<select name=\"" + selectorName + "\" onchange=\"");
	sbCatalog.append(onSelectURL);
	sbCatalog.append("\" class=\"navbar\">\n");
	sbCatalog.append("<option value=\"" + IGNORE + "\">" + (type == null ? "task" : type) + "</option>\n");
	sbCatalog.append("<option value=\"\">new " + (type == null ? "task" : type) + "</option>\n");

	sbCatalog.append("<option value=\"" + IGNORE + "\" disabled>" + DIVIDER + "</option>\n");

	String name;
	String shortName;
	String description;
	String lsid;
	TaskInfo taskInfo;
	TaskInfoAttributes tia;
	boolean isPublic;
	boolean isMine;
	// used to avoid displaying multiple versions of same basic task
	HashMap hmLSIDsWithoutVersions = new HashMap();
	LSID l = null;
	String authority;
	String versionlessLSID;
	boolean selected = false;

	String authorityType = null;

	// put recent tasks into list first
	for (Iterator itTasks = recent.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		if (taskInfo == null) continue;
		name = taskInfo.getName();
		tia = taskInfo.giveTaskInfoAttributes();
		
		shortName = name;
		if (name.endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) {
			shortName = name.substring(0, name.length() - GPConstants.TASK_TYPE_PIPELINE.length() -1);
		}
		description = taskInfo.getDescription();

		lsid = tia.get(GPConstants.LSID);
		try {
			l = new LSID(lsid);
			versionlessLSID = l.toStringNoVersion();
			String key = versionlessLSID+"."+name;			
			authorityType = LSIDManager.getInstance().getAuthorityType(l);
		} catch (MalformedURLException mue) {
			continue; // don't list if it doesn't have an LSID
		}
		selected = requestedName != null && (name.equals(requestedName) || requestedName.startsWith(versionlessLSID + ":"));
		sbCatalog.append("<option value=\"" + (lsid != null ? l.toString() : name) +
			 "\" class=\"navbar-tasks-" + authorityType + "\"" + 
			 " title=\"" + GenePatternAnalysisTask.htmlEncode(description) + ", " + l.getAuthority() + "\"" +
			 (selected ? " selected" : "") +
			 "><i>" + name + "</i></option>\n");
		
	}

	if (recent.size() > 0){
		sbCatalog.append("<option value=\"" + IGNORE + "\" disabled>" + DIVIDER  + "</option>\n");
	}
	
	// put public and my tasks into list first
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		taskInfo = (TaskInfo)itTasks.next();
		tia = taskInfo.giveTaskInfoAttributes();
		name = taskInfo.getName();
		shortName = name;
		if (name.endsWith("." + GPConstants.TASK_TYPE_PIPELINE)) {
			shortName = name.substring(0, name.length() - GPConstants.TASK_TYPE_PIPELINE.length() -1);
			// NOT DISPLAYING PIPELINES IN TASK LIST
			if (type == null || !type.equals(GPConstants.TASK_TYPE_PIPELINE)) continue;
		}
		if (type != null && !tia.get(GPConstants.TASK_TYPE).equals(type)) continue;
		description = taskInfo.getDescription();
		isPublic = tia.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC);
		isMine = tia.get(GPConstants.USERID).equals(userID);
		if (isPublic || isMine) {
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
				continue; // don't list if it doesn't have an LSID
			}
			selected = requestedName != null && (name.equals(requestedName) || requestedName.startsWith(versionlessLSID + ":"));
			sbCatalog.append("<option value=\"" + (lsid != null ? l.toString() : name) +
					 "\" class=\"navbar-tasks-" + authorityType + "\"" + 
					 " title=\"" + GenePatternAnalysisTask.htmlEncode(description) + ", " + l.getAuthority() + "\"" +
					 (selected ? " selected" : "") +
					 ">" + taskInfo.getName() + "</option>\n");
		}
	}

	sbCatalog.append("</select>");
	return sbCatalog.toString();
    }
%>

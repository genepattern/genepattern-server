<%@ page contentType="text/html" language="Java" session="false"
	import="java.net.MalformedURLException,
		java.util.Collection,
		java.util.Iterator,
		java.util.HashSet,
		java.util.HashMap,
		java.util.Set,
		java.util.Vector,
		java.util.ArrayList,
		java.util.StringTokenizer,
		java.util.LinkedHashSet,
		org.genepattern.webservice.TaskInfo,
		org.genepattern.webservice.SuiteInfo,
		org.genepattern.webservice.TaskInfoAttributes,
		org.genepattern.webservice.JobInfo,
		org.genepattern.webservice.WebServiceException,
     		org.genepattern.server.util.AccessManager,
		org.genepattern.server.genepattern.LSIDManager,
		org.genepattern.server.webservice.server.local.LocalAdminClient,
		org.genepattern.server.webservice.server.local.LocalAnalysisClient,
 	 	org.genepattern.util.GPConstants,
		org.genepattern.util.LSIDUtil,
 	 	org.genepattern.server.util.IAuthorizationManager,
		org.genepattern.server.util.AuthorizationManagerFactoryImpl,
		org.genepattern.util.StringUtils,
		org.genepattern.util.LSID,
		org.genepattern.server.indexer.Indexer" %>
<% { %>
<% 
String DIVIDER = "------";

if (request.getAttribute("navbar") == null) { 

String userID = (String)request.getAttribute("userID"); // get userID but don't force login if not defined
boolean userUnknown = (userID == null || userID.equals(""));
Collection tmTasks = null;
int recentCount = Integer.parseInt(System.getProperty("recentJobsToDisplay", "3"));
Vector recentTasks = new Vector();
Vector recentPipes = new Vector();


//
// set up suite filtering by saving suite list in the session
//
ArrayList suiteFilterAttr = (ArrayList)request.getSession().getAttribute("suiteSelection");
String suiteFilterParam = request.getParameter("suiteSelection");
if (suiteFilterParam  != null){
	StringTokenizer strtok = new StringTokenizer(suiteFilterParam , ",");
	suiteFilterAttr = new ArrayList();
	while (strtok.hasMoreTokens()){
		String suiteId = strtok.nextToken();
		if (suiteId.endsWith("'")) suiteId = suiteId.substring(0, suiteId.length()-1); 
		//System.out.println("\t Nt =  " + suiteId +"  " + System.currentTimeMillis());
		suiteFilterAttr.add(suiteId);
	}	
	request.getSession().setAttribute("suiteSelection",suiteFilterAttr );
	//System.out.println("\n SS =  " + suiteFilterParam);

}


SuiteInfo[] suites = new SuiteInfo[0];
try { 
	LocalAdminClient adminClient = new LocalAdminClient(userID);
	boolean allTasks = true;
	suites = adminClient.getAllSuites();

	if (suiteFilterAttr != null) {
		if (suiteFilterAttr.contains("all")){
			allTasks = true;	
		//	System.out.println("\tall=true");
		} else {
			allTasks = false;
		}
	} 

	if (allTasks){
		tmTasks = adminClient.getLatestTasks();
	} else {
		tmTasks = new ArrayList();
		for (int i=0; i < suites.length; i++){
			SuiteInfo suite = suites[i];
			if (suiteFilterAttr.contains(suite.getLSID()) ){
				String[] mods = suite.getModuleLSIDs();
				for (int j=0; j < mods.length; j++ ){
					TaskInfo ti = adminClient.getTask(mods[j]);
					if (ti != null) tmTasks.add(ti);
				}
			}

		}	


	}

		

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

IAuthorizationManager authManager = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();


int height = 120 + 20*suites.length;



%>
<!-- begin navbar.jsp -->
<script language="javascript">
var IGNORE = "dontJump";
var DIVIDER = "------";
var CREATE = "create";
var EDIT = "edit";
var VIEW = "view";
var RUN = "run";
var createTaskPermission = <%= authManager.checkPermission("createTask", userID) %>;
var createPipelinePermission = <%= authManager.checkPermission("createPipeline", userID) %>;
var filterSuiteWindow;

function openSuiteFilter(height){
 filterSuiteWindow = window.open('chooseSuite.jsp', 'Suite Filter','toolbar=no, location=no, status=no, menubar=no, resizable=yes,width=320,height=<%=height%>');

 filterSuiteWindow.focus();
}


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

	enableEdit = enableEdit && createTaskPermission;

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
	enableEdit = enableEdit && createPipelinePermission;
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
		newOption.setAttribute("title", l.authority);
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

function getSelectedValues (select) {
  var r = new Array();
  for (var i = 0; i < select.options.length; i++)
    if (select.options[i].selected)
      r[r.length] = select.options[i].value;
  return r;
}

function selectSuiteFilter() {
//	document.getElementById("lowerPart").style.visibility = "hidden"
	document.getElementById("lowerPart").style.display = "none"
//	document.getElementById("topPart").innerText = value

	var selected = getSelectedValues(document.getElementById("selectSuite"))
	var search = window.location.search;
	var here
	if (search.length==0){
		here = window.location + "?suiteSelection=" + selected +"'"
	} else {
		here = window.location + "&suiteSelection='" + selected +"'"
	}
	parent.selectedSuites = selected;
	window.location = here;

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
		<td valign="top">	<a href="index.jsp" class='logo'><img src='skin/logoSmall.gif' border="0" height=25 width=25 />&nbsp;<B>Gene</B>Pattern</a> &nbsp;
		</td>
		<td align="right"  valign="top">
			<nobr>
			<% if (request.getAttribute(GPConstants.USER_LOGGED_OFF) == null) { %>
				<% if (userUnknown) { %>
					<input type="text" class="little" size="30" name="<%= GPConstants.USERID %>" value="<%= EMAIL_ADDRESS %>" onfocus="ufocus(this, true, '<%= EMAIL_ADDRESS %>')" onblur="ufocus(this, false, '<%= EMAIL_ADDRESS %>')"> 
					<input type="submit" value="sign in" class="little">
				<% } else { %>
					<a href="login.jsp" class="navbarlink">sign out</a> <%= StringUtils.htmlEncode(userID) %>
				<% } %>
			<% } %>
			</nobr>
			<br>
			<a href="about.jsp" class="navbarlink">about</a>
		</td>
	</tr>
	</form>
	</table>

	<form action="search.jsp" name="searchForm">

	<table width="100%">
	<tr>
	<td  valign="top">
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


&nbsp;&nbsp;&nbsp;
<a href="#" 
	onclick="openSuiteFilter()" class="navbarlink"> <nobr>Filter by Suite</nobr></a>

	</td>
	<td align="right" valign="top" >
			<nobr><input type="text" class="little" size="10" name="search" 
			      value="<%= request.getParameter("search") != null ? 
				request.getParameter("search") : SEARCH %>" onfocus="ufocus(this, true, '<%= SEARCH %>')" 
				onblur="ufocus(this, false, '<%= SEARCH %>')"><input type="image" src="skin/search.jpeg" 
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
	int halfLength = Integer.parseInt(System.getProperty("gp.name.halflength", "17"));
		
	for (Iterator itTasks = tmTasks.iterator(); itTasks.hasNext(); ) {
		TaskInfo task = (TaskInfo)itTasks.next();
		maxNameWidth = Math.max(maxNameWidth, task.getName().length());
	}
	if (	maxNameWidth > ((2*halfLength)+3)) {
		maxNameWidth = (2*halfLength)+3;
	}

	StringBuffer divBuff = new StringBuffer(DIVIDER);
	for (int i=0; i < maxNameWidth; i++){
		divBuff.append("-");
	}
	DIVIDER=divBuff.toString();

		

	StringBuffer sbCatalog = new StringBuffer();
	sbCatalog.append("<select name=\"" + selectorName + "\" onchange=\"");
	sbCatalog.append(onSelectURL);
	sbCatalog.append("\">\n");
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

		String shortenedName = shortName;
		
		if (shortName.length() > ((2*halfLength)+3)){
			int len = shortName.length();
			int idx = shortName.length() - halfLength;
			shortenedName = shortName.substring(0,halfLength) + "..." + shortName.substring(idx, len);
			shortName = shortenedName;
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
			 "\" class=\"tasks-" + authorityType + "\"" + 
			 " title=\"" + StringUtils.htmlEncode(description) + ", " + l.getAuthority() + "\"" +
			 (selected ? " selected" : "") +
			 "><i>" + shortName + "</i></option>\n");
		
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

		String shortenedName = shortName;
		
		if (shortName.length() > ((2*halfLength)+3)){
			int len = shortName.length();
			int idx = shortName.length() - halfLength;
			shortenedName = shortName.substring(0,halfLength) + "..." + shortName.substring(idx, len);
			shortName = shortenedName;
		}	
			

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
					 "\" class=\"tasks-" + authorityType + "\"" + 
					 " title=\"" + StringUtils.htmlEncode(description) + ", " + l.getAuthority() + "\"" +
					 (selected ? " selected" : "") +
					 ">" + shortName + "</option>\n");
		}
	}

	sbCatalog.append("</select>");
	return sbCatalog.toString();
    }
%>

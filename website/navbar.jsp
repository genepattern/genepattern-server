<%@ page contentType="text/html" language="Java" 
	import="java.net.MalformedURLException,
		java.util.Collection,
		java.util.Iterator,
		java.util.HashSet,
		java.util.HashMap,
		java.util.Set,
		org.genepattern.server.analysis.genepattern.LSIDManager,
		org.genepattern.util.LSIDUtil,
		org.genepattern.util.LSID,
		org.genepattern.analysis.TaskInfo,
		org.genepattern.analysis.TaskInfoAttributes,
		org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask,
		org.genepattern.server.analysis.webservice.server.local.*,
		org.genepattern.util.GPConstants,
		org.genepattern.server.analysis.genepattern.Indexer" %>
<% { %>
<% 
if (request.getAttribute("navbar") == null) { 

String userID = GenePatternAnalysisTask.getUserID(request, null); // get userID but don't force login if not defined
boolean userUnknown = (userID == null || userID.equals(""));
Collection tmTasks = null;
try { 
	tmTasks = new LocalAdminClient(userID).getTaskCatalog();
} catch (Exception e) {
	tmTasks = new HashSet();
}
String EMAIL_ADDRESS = "email address";
String SEARCH = "search";

%>
<!-- begin navbar.jsp -->
<script language="javascript">
var IGNORE = "dontJump";
var CREATE = "create";
var EDIT = "edit";
var VIEW = "view";
var RUN = "run";

// handle focus and blur events for search field
function sfocus(fld, focus) {
    var deflt = "<%= SEARCH %>";
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

// handle focus and blur events for userid field
function ufocus(fld, focus) {
    var deflt = "<%= EMAIL_ADDRESS %>";
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
			window.location = "runPipeline.jsp?cmd=run&<%= GPConstants.NAME %>=" + pipeline.options[pipeline.selectedIndex].value;
		}
	}
}

function changeTask() {
	document.forms['searchForm'].Pipeline.selectedIndex = 0;

	// enable/disable the edit button for the task/pipeline depending on ownership
	var enableEdit = true;
	var sel = document.forms['searchForm'].Task;
	var taskLSID = sel[sel.selectedIndex].value;
	var create = (sel.selectedIndex == 1);
	if (new LSID(taskLSID).authorityType == '<%= LSIDUtil.AUTHORITY_MINE %>' || create) {
		enableEdit = true;
	} else {
		enableEdit = false;
	}
	document.forms['searchForm'].navbaredit.value = (enableEdit ? (create ? CREATE : EDIT) : VIEW);

}

function changePipeline() {
	document.forms['searchForm'].Task.selectedIndex = 0;

	// enable/disable the edit button for the task/pipeline depending on ownership
	var enableEdit = true;
	var sel = document.forms['searchForm'].Pipeline;
	var taskLSID = sel[sel.selectedIndex].value;
	var create = (sel.selectedIndex == 1);
	if (new LSID(taskLSID).authorityType == '<%= LSIDUtil.AUTHORITY_MINE %>' || create) {
		enableEdit = true;
	} else {
		enableEdit = false;
	}
	document.forms['searchForm'].navbaredit.value = (enableEdit ? (create ? CREATE : EDIT) : VIEW);
}

// add an item to either the task or pipeline dropdown list
function addNavbarItem(name, lsid) {
	if (<%= userUnknown%>) return; // no selections

	var taskType = name.substr(-(".<%= GPConstants.TASK_TYPE_PIPELINE %>".length)) == ".<%= GPConstants.TASK_TYPE_PIPELINE %>" ? "Pipeline" : "Task";
	var selector = document.forms['searchForm'][taskType];
	var l = new LSID(lsid);
	if (l.authorityType == "<%= LSIDUtil.AUTHORITY_FOREIGN %>") {
		name = name + " (" + l.getAuthority() + ")";
	}
	for (i = 0; i < selector.options.length; i++) {
		if (selector.options[i].text == name) return;
	}
	var newOption = new Option(name, lsid);
	// set the class for this option to get the right coloring
	newOption.className = "navbar-tasks-" + l.authorityType;
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
	this.authorityType = (this.authority == '<%= LSIDManager.getInstance().getAuthority() %>') ? '<%= LSIDUtil.AUTHORITY_MINE %>' : (this.authority == '<%= LSIDUtil.BROAD_AUTHORITY %>' ? '<%= LSIDUtil.AUTHORITY_BROAD %>' : '<%= LSIDUtil.AUTHORITY_FOREIGN %>');
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
					<input type="text" class="little" size="30" name="<%= GPConstants.USERID %>" value="<%= EMAIL_ADDRESS %>" onfocus="ufocus(this, true)" onblur="ufocus(this, false)"> 
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
			<%= _taskCatalog(tmTasks, "Task", "changeTask();", null, userID) %>
			<%= _taskCatalog(tmTasks, "Pipeline", "changePipeline();", GPConstants.TASK_TYPE_PIPELINE, userID) %>
			<nobr>
				<input type="button" value="run" name="navbarrun" onclick="jumpTo(this)"> 
				<input type="button" value="edit" name="navbaredit" onclick="jumpTo(this)">
			</nobr>
		<% } %>
	</td>
	<td align="right" valign="top" class="navbar">
			<nobr><input type="text" class="little" size="10" name="search" 
			      value="<%= request.getParameter("search") != null ? 
				request.getParameter("search") : SEARCH %>" onfocus="sfocus(this, true)" 
				onblur="sfocus(this, false)"><input type="image" src="search.jpeg" 
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
<%! private String _taskCatalog(Collection tmTasks, String selectorName, String onSelectURL, String type, String userID) {
	String IGNORE = "dontJump";
	StringBuffer sbCatalog = new StringBuffer();
	sbCatalog.append("<select name=\"" + selectorName + "\" onchange=\"");
	sbCatalog.append(onSelectURL);
	sbCatalog.append("\" class=\"navbar\">\n");
	sbCatalog.append("<option value=\"" + IGNORE + "\">" + (type == null ? "task" : type) + "</option>\n");
	sbCatalog.append("<option value=\"\">new " + (type == null ? "task" : type) + "</option>\n");
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

	String authorityType = null;

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
		if (isPublic || isMine) {
			sbCatalog.append("<option value=\"" + (lsid != null ? l.toString() : name) +
					 "\" class=\"navbar-tasks-" + authorityType + "\">" + taskInfo.getName() + 
					 (authorityType.equals(LSIDUtil.AUTHORITY_FOREIGN) ? (" (" + l.getAuthority() + ")") : "") +
					 "</option>\n");
		}
	}

	sbCatalog.append("</select>");
	return sbCatalog.toString();
    }
%>

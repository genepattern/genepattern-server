<% /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ %>
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
		 java.util.ArrayList,
		 java.util.Set,
		 java.util.TreeMap,
		 java.util.Vector,
		 org.genepattern.codegenerator.AbstractPipelineCodeGenerator,
		 org.genepattern.util.LSID,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.SuiteInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.server.util.AccessManager,
		 org.genepattern.server.util.AuthorizationManagerFactoryImpl,
		 org.genepattern.server.util.IAuthorizationManager,
		 org.genepattern.server.webservice.server.local.*,
		 org.genepattern.util.GPConstants,
		 org.genepattern.util.StringUtils,		
		 org.genepattern.server.indexer.Indexer"
	session="false" contentType="text/html" language="Java" %><%

	// redirect to the fully-qualified host name to make sure that the one cookie that we are allowed to write is useful
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

try {

String agent = request.getHeader("USER-AGENT");
String iFrameWidth=" width='100%'; ";
if (agent.indexOf("Safari") >= 0) {
	iFrameWidth = " width='250px'; ";
} 

String userID= (String)request.getAttribute("userID"); // get userID but don't force login if not defined
boolean userIDKnown = !(userID == null || userID.length() == 0);
LocalAdminClient adminClient = new LocalAdminClient(userID);
Collection tmTasks = null;
Collection latestTmTasks = adminClient.getLatestTasks();
ArrayList suiteFilterAttr = (ArrayList)request.getSession().getAttribute("suiteSelection");


boolean allTasks = true;
SuiteInfo[] suites = adminClient.getAllSuites();

if (suiteFilterAttr != null) {
	if (suiteFilterAttr.contains("all")){
		allTasks = true;	
		System.out.println("\tall=true");
	} else {
		allTasks = false;
	}
} 

if (allTasks){
	tmTasks = adminClient.getTaskCatalog();
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


HashMap latestTaskMap = new HashMap();
for (Iterator itTasks = latestTmTasks.iterator(); itTasks.hasNext(); ) {
	TaskInfo taskInfo = (TaskInfo)itTasks.next();
	TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();
	String versionlessLSID = (new LSID(tia.get(GPConstants.LSID))).toStringNoVersion();
	latestTaskMap.put(versionlessLSID, taskInfo); 
}


IAuthorizationManager authManager = (new AuthorizationManagerFactoryImpl()).getAuthorizationManager();


%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>GenePattern</title>

<script language="Javascript">

var localAuthority = '<%= LSIDManager.getInstance().getAuthority() %>';

function jmp(button, url, selector, versionSelector) {
	if (selector.selectedIndex != 0) {
		var lsidNoVersion = selector.options[selector.selectedIndex].value;
		var lsidVersion = versionSelector.options[versionSelector.selectedIndex].value;
		window.location = url + lsidNoVersion + '<%= LSID.DELIMITER %>' + lsidVersion;
	} else {
		window.alert('Please select a ' + selector.name + ' to ' + button.value + '.');
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
				v.add(l.getVersion());

			} else {
				String highestVersion = (String)v.firstElement();
				String curVersion = l.getVersion();

				if ((curVersion.compareTo(highestVersion)) > 0){
					v.add(0, l.getVersion());

				} else {
					v.add(l.getVersion());
				}
			}
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
	this.authorityType = (this.authority == '<%= LSIDManager.getInstance().getAuthority() %>'.replace(" ", "+")) ? '<%= LSIDUtil.AUTHORITY_MINE %>' : (this.authority == '<%= LSIDUtil.BROAD_AUTHORITY %>' ? '<%= LSIDUtil.AUTHORITY_BROAD %>' : '<%= LSIDUtil.AUTHORITY_FOREIGN %>');
}

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
<% if (tmTasks.size() == 0) { %>
<font size="+1" color="red">
<br>
There are currently no tasks installed on this server.  
You may select and install tasks from the <a href="taskCatalog.jsp">Broad website</a> or from your own collection.
<br><br>

<% } %>

</font>

<table cellpadding="0" cellspacing="0" width="100%" border="0" >
<tr><td colspan=3>
<jsp:include page="navbar.jsp"></jsp:include>


</td></tr>

<tr> 

<td  valign='top' align='left' height='100%'>
<form name="index" method="post">

<iframe frameborder="0" scrolling="yes" marginwidth="1" src="getRecentJobs.jsp" style="<%=iFrameWidth%> height: 590px" name="iframe" id="iframeid">
No &lt;iframes&gt; support  :(
</iframe>


</td> 
<td valign=top>
<table  width="100%" height="100%"  cellspacing=0 cellpadding=0 >
	<tr>
		<%@ include file="indexTaskSection.htm" %>
	</tr>

		<tr>
	      <%@ include file="indexPipelineSection.htm" %>
	</tr>
</table>
</td>
<td valign='top' >

<table bgcolor="#EFEFFF" height='100%' width='100%' border="0" margin='1' class='majorcell'>
	<tr height='520'><td valign='top'>
		<%@ include file="indexDocSection.jsp" %>
</td></tr></table>

</td>
</tr>
</table>


</form>


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
<%! public String taskCatalog(Collection tmTasks, HashMap latestTaskMap, String selectName, String caption, String type, String userID, boolean bIncludePipelines) {
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
		lsid = tia.get(GPConstants.LSID);
		try {
			l = new LSID(lsid);	
		versionlessLSID = l.toStringNoVersion();

		TaskInfo latestTaskInfo = (TaskInfo)latestTaskMap.get(versionlessLSID);
		TaskInfoAttributes latestTia = latestTaskInfo.giveTaskInfoAttributes();
		lsid = latestTia.get(GPConstants.LSID);
		
		taskType = latestTia.get(GPConstants.TASK_TYPE);
		if (type != null && !taskType.equals(type)) continue;
		if (!bIncludePipelines && taskType.equals(GPConstants.TASK_TYPE_PIPELINE)) continue;
		display = latestTaskInfo.getName();
		if (taskType.equals(GPConstants.TASK_TYPE_PIPELINE)) {
			String dotPipeline = "." + GPConstants.TASK_TYPE_PIPELINE;
			if (display.endsWith(dotPipeline)) {
				display = display.substring(0, display.length() - dotPipeline.length());
			}
		}

		int halfLength = Integer.parseInt(System.getProperty("gp.name.halflength", "17"));
		String shortenedName = display;
		if (display.length() > ((2*halfLength)+3 )){
			int len = display.length();
			int idx = display.length() - halfLength ;
			shortenedName = display.substring(0,halfLength) + "..." + display.substring(idx, len);
			display= shortenedName;
		}	


		description = latestTaskInfo.getDescription();
		isPublic = latestTia.get(GPConstants.PRIVACY).equals(GPConstants.PUBLIC);
		isMine = latestTia.get(GPConstants.USERID).equals(userID);
		name = latestTaskInfo.getName();
		
					String key = versionlessLSID;			
			if (hmLSIDsWithoutVersions.containsKey(key) ) {
				continue;
			}
			hmLSIDsWithoutVersions.put(key, latestTaskInfo);
			authorityType = LSIDManager.getInstance().getAuthorityType(l);
		} catch (MalformedURLException mue) {
			System.out.println("index.jsp: skipping " + mue.getMessage() + " in " + lsid);
			continue;
		}

		
		if (isPublic || isMine) {
			// get the name of the last version of this LSID

			sbCatalog.append("<option value=\"" + (lsid != null ? l.toStringNoVersion() : taskInfo.getName()) + 
					 "\" class=\"tasks-" +  authorityType + "\"" +
					 " title=\"" + StringUtils.htmlEncode(description) + ", " + l.getAuthority() + "\"" + ">" + display + "</option>\n");
		}
	}
	sbCatalog.append("</select>\n");
	return sbCatalog.toString();
    }
%>

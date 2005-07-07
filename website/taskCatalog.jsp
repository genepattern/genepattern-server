<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.server.process.*,
		 org.genepattern.server.genepattern.TaskInstallationException,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.util.LSID,
		 java.io.File,
		 java.net.MalformedURLException,
		 java.text.DateFormat,
		 java.text.NumberFormat,
		 java.text.ParseException,
		 java.util.Arrays,
		 java.util.Comparator,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.util.Iterator,
		 java.util.Map,
		 java.util.StringTokenizer,
		 java.util.TreeSet,
		 java.util.List,
		 java.util.ArrayList,
		 java.util.Vector"
   session="false" language="Java" %>
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>


<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);

	// names of columns that can be displayed to the user

	String[] HEADINGS = {
		"name (version)",
		InstallTask.STATE,
		InstallTask.columnNameToHRV(GPConstants.TASK_TYPE),
		"details",
	};

	String userID = "GenePattern";
	String INSTALL_LSID_GROUP = "installLSIDGroup";
	String INSTALL_BUTTON = "install";
	String SORT = "sort";

	String sort = request.getParameter(SORT);
	boolean initialInstall = (request.getParameter("initialInstall") != null);
	boolean checkAll = (request.getParameter("checkAll") != null);
	if (sort == null || sort.length() == 0) sort = InstallTask.STATE;

%>
	<html>
	<head>
	<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
<title>Installable tasks</title>
<style>
td { font-size: 8pt }
</style>

<script language="Javascript">
var ie4 = (document.all) ? true : false;
var ns4 = (document.layers) ? true : false;
var ns6 = (document.getElementById && !document.all) ? true : false;

function writeToLayer(lay,txt) {
	if (ns6) {
		over = document.getElementById([lay]);
		range = document.createRange();
		range.setStartBefore(over);
		domfrag = range.createContextualFragment(txt);
		while (over.hasChildNodes()) {
			over.removeChild(over.lastChild);
		}
		over.appendChild(domfrag);
	} else if (ns4) {
		var l = document['id'+lay];
		l.document.write(txt);
		l.document.close();
	} else if (ie4) {
		document.all(lay).innerHTML = txt;
	}

}

function checkAll(frm, bChecked) {
	frm = document.forms['install'];
	for (i = 0; i < frm.elements.length; i++) {
		if (frm.elements[i].type != "checkbox") continue;
		frm.elements[i].checked = bChecked;
	}
}

function changeFilter(fld) {
	//window.alert("changed " + fld.name + " to " + fld.options[fld.selectedIndex].value);
	var frm = fld.form;
	loc = window.location.href.substring(0, window.location.href.length-window.location.search.length) + "?";
	loc = loc + "<%= SORT %>=" + escape(frm.elements['<%= SORT %>'].options[frm.elements['<%= SORT %>'].selectedIndex].value);
	for (col = 0; col < columns.length ; col++) {
		var selector = frm.elements[columns[col]];
		for (sel = 0; sel < selector.length; sel++) {
			if (selector.options[sel].selected && sel != 0) {
			loc = loc + "&" + columns[col] + "=" + escape(selector.options[sel].value);
			}
		}
	}
	<% if (initialInstall) { %>loc = loc + "&initialInstall=1";
	<% } %>
	window.location = loc;
}

</script>
	</head>
	<body>
	<jsp:include page="navbar.jsp"></jsp:include>
	<span id="fetching">
		Fetching task catalog from <a href="<%= System.getProperty("ModuleRepositoryURL") %>" target="_new"><%= System.getProperty("ModuleRepositoryURL") %></a>...
	</span>
<%
	out.flush();
	InstallTasksCollectionUtils collection = null;
	InstallTask[] tasks = null;
	LocalTaskIntegratorClient taskIntegrator = new LocalTaskIntegratorClient(userID, out);

	try {
		collection = new InstallTasksCollectionUtils(userID, initialInstall);

		// test case independent of XML-based support:
		//	collection.setupTestCollection(new String[]{ "KNN", "GeneNeighbors", "NMF", "ClassNeighbors", "TransposeDataset" });

		tasks = collection.getAvailableModules();
	} catch (Exception e) {
%>
		Sorry, the <%=messages.get("ApplicationName")%> <a href="<%= System.getProperty("ModuleRepositoryURL") %>" target="_new">module repository</a> is not currently available.<br>
		<p>Reason: <code><%= e.getMessage() %></code><br>
		<p>
		<b>Try to correct this problem</b> by changing <a href="adminServer.jsp">web proxy settings</a> or <a href="adminServer.jsp">Module Repository URL.</a>

		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
<%
		e.printStackTrace();
		return;
	} finally {
		// erase the Fetching... message
%>
		<script language="Javascript">
			document.getElementById("fetching").innerHTML = "";
		</script>
<%
		out.flush();
	}
	String motd = collection.getMOTD_message();
	if (motd.length() > 0) {
%>
		<%= motd %><br>
		<font size="1">updated <%= DateFormat.getDateInstance().format(collection.getMOTD_timestamp()) %>.  
		<a href="<%= collection.getMOTD_url() %>" target="_blank">More information</a>.</font><br>
		<%= newerServerAvailable(collection.getMOTD_latestServerVersion(), System.getProperty("GenePatternVersion")) ? 
			("<a href=\"http://www.broad.mit.edu/cancer/software/genepattern/download\">Download updated "+messages.get("ApplicationName")+" version " + 
				collection.getMOTD_latestServerVersion() + "</a> (currently running version " + 
				System.getProperty("GenePatternVersion") + ")<br>") :
			 "" %>
		<br>
		<hr>
<%
		for (int ii = 0; ii < 8*1024; ii++) out.print(" ");
		out.println();
		out.flush();
	}
	// build HashMap of associations between LSIDs and InstallTask objects
	HashMap hmLSIDToInstallTask = new HashMap();
	for (int t = 0; t < tasks.length; t++) {
		hmLSIDToInstallTask.put(tasks[t].getLSID(), tasks[t]);
	}

	if (request.getParameter(INSTALL_BUTTON) != null) {
		//String[] installURLs = request.getParameterValues(INSTALL_LSID_GROUP);
		String[] LSIDVersions = request.getParameterValues(InstallTask.LSID_VERSION);
%>

<%
		// build TreeSet of InstallTask to install (sorting on task name)
		TreeSet tsToInstall = new TreeSet(new Comparator() {
			public int compare(Object o1, Object o2) {
				InstallTask t1 = (InstallTask)o1;
				InstallTask t2 = (InstallTask)o2;
				return (t1.getName().compareToIgnoreCase(t2.getName()));
			}
		    });
		InstallTask installTask;
		for (java.util.Enumeration eNames = request.getParameterNames(); eNames.hasMoreElements(); ) {
			String n = (String)eNames.nextElement();
			String[] v = request.getParameterValues(n);
			for (int j = 0; j < v.length; j++) {
				if (n.startsWith(INSTALL_LSID_GROUP + "_")) {
					installTask = (InstallTask)hmLSIDToInstallTask.get(v[j]);
					tsToInstall.add(installTask);
				}
			}
		}

		int i;
		int numSelected = 0;

		// for each requested LSID to install, get the InstallTask out of the HashMap and install it
		for (Iterator eTasks = tsToInstall.iterator(); eTasks.hasNext(); ) {
			installTask = (InstallTask)eTasks.next();
			numSelected++;
			try {
				boolean wasInstalled = installTask.install(userID, GPConstants.ACCESS_PUBLIC, taskIntegrator);
%>
				<%= wasInstalled ? "Overwrote" : "Installed" %> <a href="addTask.jsp?name=<%= installTask.getLSID() %>"><%= installTask.getName() %></a> version <%= new LSID(installTask.getLSID()).getVersion() %><br>
				<script language="Javascript">
				addNavbarItem("<%= installTask.getName() %>", "<%= installTask.getLSID() %>");
				</script>
<%
			} catch (TaskInstallationException tie) {
				Vector vProblems = tie.getErrors();
				for (int j = 0; j < vProblems.size(); j++) {
					out.println(vProblems.elementAt(j) + "<br>");
				}
			} catch (Exception e) {
				out.println(e.getMessage() + "<br>");
			}
			for (int ii = 0; ii < 8*1024; ii++) out.print(" ");
			out.println();
			out.flush();
		}
		if (numSelected == 0) {
			out.println("No tasks selected for installation!<br>");
		}
%>
<br>
<a href="taskCatalog.jsp<%= initialInstall ? "?initialInstall=1" : "" %>">install more tasks</a><br>

		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>
<%
		return;
	} // end if installing


	// HashMap of filters. key=filter name, value=Vector of settings
	HashMap hmFilter = new HashMap();
	
	String[]columns1 = InstallTask.getAttributeNames();
	int col;
	for (col = 0; col < columns1.length; col++) {
		
		String[] filterValues = request.getParameterValues(columns1[col]);
		if (filterValues != null && filterValues.length > 0) {
			Vector vCol = new Vector(filterValues.length);
			for (int i = 0; i < filterValues.length; i++) {
				vCol.add(filterValues[i]);
			}
			hmFilter.put(columns1[col], vCol);
			
		}
	}
	
	Vector osFilter = (Vector) hmFilter.get(GPConstants.OS);
	boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
	boolean isMac = System.getProperty("mrj.version") != null;
	boolean isLinux = System.getProperty("os.name").toLowerCase().startsWith("linux");
	Map osAttributeMap = new HashMap();
	Vector choices = new Vector();
	choices.add("any");
	osAttributeMap.put(GPConstants.OS, choices); 
	
		if(isWindows) {
			choices.add("Windows"); 
		} else if(isMac) {
			choices.add("Mac OS X"); 
		} else if(isLinux) {
			choices.add("Linux"); 
		} 
	
	if(osFilter==null) { // user did not select a field for OS, set the default
		osFilter = new Vector();
		hmFilter.put(GPConstants.OS, osFilter);
		if(isWindows) { // remove all tasks that are not windows or any
			osFilter.add("Windows");
		} else if(isMac) {
			osFilter.add("Mac OS X");
		} else if(isLinux) {
			osFilter.add("Linux");
		} else {
			osFilter.add("any");
		}
	} 
	
	Vector vState = (Vector)hmFilter.get(InstallTask.STATE);
	if (vState == null || vState.size() == 0) {
		vState = new Vector(2);
		vState.add(InstallTask.NEW);
		vState.add(InstallTask.UPDATED);
		hmFilter.put(InstallTask.STATE, vState);
	}
	// if a specific list of LSIDs is requested, display just those
	Vector vLSIDs = new Vector();
	String[] requestedLSIDs = request.getParameterValues(GPConstants.LSID);
	List requestedLSIDList = new ArrayList();
	if (requestedLSIDs != null && requestedLSIDs.length > 0) {
		requestedLSIDList = Arrays.asList(requestedLSIDs);
		hmFilter.clear();
		vLSIDs.addAll(requestedLSIDList);
		hmFilter.put(GPConstants.LSID, vLSIDs);
	}

	tasks = collection.filterTasks(hmFilter);
	tasks = collection.sortTasks(GPConstants.NAME, true);
	tasks = collection.sortTasks(sort, true);

	String columns[] = new String[]{
		InstallTask.STATE,
		GPConstants.TASK_TYPE,
		GPConstants.QUALITY,
		GPConstants.CPU_TYPE,
		GPConstants.OS,
		GPConstants.LANGUAGE,
		GPConstants.JVM_LEVEL
	};
	
%>

	
        <script language="Javascript">
        var columns = new Array( <%
        	for (col = 0; col < columns.length; col++) {
        %><%= col > 0 ? ", " : "" %>"<%= columns[col] %>"<%
        	} %>);
        </script>

Select from the following tasks from the <%=messages.get("ApplicationName")%> public access website to download and install:<br><br>

<form name="filters">
<!-- Filters -->
<table>
<tr>
<td valign="top" align="right" width="15%">
<font size="+1"><b>Filters: </b></font>
</td>
<td valign="top">
<%
	for (col = 0; col < columns.length; col++) {
		Vector vCol = (Vector)hmFilter.get(columns[col]); // user requested choices
		String[]values = collection.getUniqueValues(columns[col]); // choices for select
		if (columns[col].equals(InstallTask.STATE)) {
			values = new String[] { InstallTask.NEW, InstallTask.UPDATED, InstallTask.UPTODATE };
		}

		%><select name="<%= columns[col] %>" onchange="changeFilter(this);" multiple size="<%= values.length+1 %>" style="vertical-align: top">
		<option value="" style="font-weight: bold"><%= InstallTask.columnNameToHRV(columns[col]) %></option>
<%

		for (int val = 0; val < values.length; val++) {
			boolean selected = vCol == null || vCol.size() == 0 || vCol.contains(values[val]);
%>			
			<option value="<%= StringUtils.htmlEncode(values[val]) %>"<%= selected ? " selected" : ""%>><%= StringUtils.htmlEncode(values[val]) %></option>
<%
			}
%>
		</select><%= !(columns[col].equals(GPConstants.LANGUAGE)) ? " " : "&nbsp;" %>
<%
	}
%>
</td>
</tr>
<!-- end filters -->

<!-- sorting -->
<tr>
<td valign="top" align="right">
<font size="+1"><b>Sort by: &nbsp;</b></font>
</td>
<td valign="top">
<select name="<%= SORT %>" onchange="changeFilter(this);">
<%
	String[] fields = collection.getAttributeNames();
	for (int i = 0; i < fields.length; i++) {
		if (!fields[i].equals(GPConstants.DESCRIPTION) &&
			!fields[i].equals(GPConstants.VERSION) &&
			!fields[i].equals(InstallTask.REFRESHABLE)) {
%>
			<option value="<%= fields[i] %>"<%= sort.equals(fields[i]) ? " selected" : "" %>><%= InstallTask.columnNameToHRV(fields[i]) %></option>
<%
		}
	}
%>
	</select>
</td>
</tr>
<!-- end sorting -->

<!-- task selection -->
<tr>
<td valign="top" align="right">
<font size="+1"><b>Scroll to:</b> &nbsp;</font>
</td>
<td valign="top">
<%
	TreeSet tsTasks = new TreeSet(String.CASE_INSENSITIVE_ORDER);
	for (int module = 0; module < tasks.length; module++) {
		tsTasks.add(tasks[module].getName());
	}
	String[] taskNames = (String[])tsTasks.toArray(new String[0]);
	for (int module = 0; module < taskNames.length; module++) {
%>
		<a href="#<%= taskNames[module] %>"><%= taskNames[module] %></a>
<%
	}

%>
</td>
</tr>
<!-- end task selection -->

</table>
</form>
<% out.flush(); %>

<form name="install" method="post">

<% 
  List missingLSIDList = new ArrayList();
  missingLSIDList.addAll(requestedLSIDList);
  for (int module = 0; module < tasks.length; module++) {
	InstallTask task = tasks[module];
	Map attributes = task.getAttributes();
	String lsidStr = (String)attributes.get(GPConstants.LSID);
	missingLSIDList.remove(lsidStr); // to look for LSIDs requested but absent
  }
  if (missingLSIDList.size() > 0){
  	out.println("<hr><font size=\"+1\" color=\"red\"><b>");
  	out.println("The following requested tasks could not be found in the Broad Task Catalog;</b></font><br>");
	for (int module=0; module < missingLSIDList.size(); module++){
		out.println("<tab>"+missingLSIDList.get(module) + "<br>");
	}
	if (tasks.length > 0) out.println("<hr>");
  }

  if (tasks.length == 0) { 
	String selectedStates = "";
	for (int s = 0; s < vState.size(); s++) {
		if (selectedStates.length() > 0) {
			if (s == (vState.size()-1)) {
				selectedStates = selectedStates + " or ";
			} else {
				selectedStates = selectedStates + ", ";
			}
		}
		selectedStates = selectedStates + vState.elementAt(s);
	}	
%>
	<hr>
	<font size="+1">No  <%= selectedStates %> tasks match filter criteria.</font>
<% } else { 
	// sort tasks by name, then by LSID descending order
	Arrays.sort(tasks, new Comparator() {
				public int compare(Object o1, Object o2) {
					InstallTask t1 = (InstallTask)o1;
					InstallTask t2 = (InstallTask)o2;

					if (!t1.getName().equalsIgnoreCase(t2.getName())) {
						return (t1.getName().compareToIgnoreCase(t2.getName()));
					}
					
					LSID l1 = null;
					LSID l2 = null;
					try {
						l1 = new LSID((String)((InstallTask)t1).getAttributes().get(GPConstants.LSID));
						l2 = new LSID((String)((InstallTask)t2).getAttributes().get(GPConstants.LSID));
						if (!l1.isSimilar(l2)) {
				  			return l1.toString().toLowerCase().compareToIgnoreCase(l2.toString().toLowerCase());
						} else {
							// XXX: TODO: should crawl version string?
							return l2.getVersion().compareToIgnoreCase(l1.getVersion());
						}
					} catch (MalformedURLException mue) {
						// ignore
						return 0;
					}
				}
			});

	// count number of unique tasks (ignore LSID versions of otherwise-same tasks) XXX
	int numUniqueTasks = 0;
	int numRefreshable = 0;
	for (int module = 0; module < tasks.length; module++) {
		InstallTask task = tasks[module];
		Map attributes = task.getAttributes();
		String lsidStr = (String)attributes.get(GPConstants.LSID);
		LSID lsid = new LSID(lsidStr);
		numUniqueTasks++;
		if (attributes.get(InstallTask.REFRESHABLE).equals(InstallTask.YES)) numRefreshable++;
		while (true) { // skip lower-versions of same module
			// see if there are more modules of the same name but different LSID version number
			if ((module+1) == tasks.length) break;
			LSID l = new LSID(tasks[module+1].getLSID());
			if (!l.isSimilar(new LSID(task.getLSID()))) break;
			module++;
		}
	}
%>

<br>
<table>
<tr>
<td width="80%">
<font size="+1"><b><%= numRefreshable %> of <%= numUniqueTasks %> tasks are new or updated</b></font>
</td>
<td>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
</td>
<td>
<input type="submit" name="<%= INSTALL_BUTTON %>" value="install checked"> &nbsp;&nbsp;
<nobr><a href="javascript:checkAll(this.form, true)">check all</a> &nbsp;&nbsp;
<a href="javascript:checkAll(this.form, false)">uncheck all</a></nobr>
</td>
</tr>
</table>

<table cellspacing="5">

<tr>
<td colspan="<%= (HEADINGS.length+1) %>"><hr></td>
</tr>


<tr>
<td valign="top"></td>
<%
	for (col = 0; col < HEADINGS.length; col++) {
%>
		<td valign="top">
		<b><u><%= HEADINGS[col] %></u></b>
		</td>
<%
	}
%>
</tr>
<%
	String ver;
	LSIDManager lsidManager = LSIDManager.getInstance();

	// mark tasks that should have the version selection set
	String SELECTED = "selected";
	for (int module = 0; module < tasks.length; module++) {
		InstallTask task = tasks[module];
		Map attributes = task.getAttributes();
		if (vLSIDs.contains((String)attributes.get(GPConstants.LSID))) {
			attributes.put(SELECTED, SELECTED);
		}
	}

	for (int module = 0; module < tasks.length; module++) {
		InstallTask task = tasks[module];
		Map attributes = task.getAttributes();
		LSID lsid = new LSID((String)attributes.get(GPConstants.LSID));
		String authority = lsid.getAuthority();
%>
<tr>

		<td valign="top" rowspan="2">
		<input type="checkbox" name="<%= INSTALL_LSID_GROUP %>_<%= module %>" value="<%= lsid %>"<%= ((attributes.get(InstallTask.REFRESHABLE).equals("yes")) || checkAll) ? " checked" : "" %>>
		</td>

		<td valign="top" height="1">
		<span class="tasks-<%= lsidManager.getAuthorityType(lsid) %>">
		<a name="<%= attributes.get(GPConstants.LSID) %>"></a>
		<b><nobr><a name="<%= attributes.get(GPConstants.NAME) %>"><%= StringUtils.htmlEncode((String)attributes.get(GPConstants.NAME)) %></a></b>
		</span>
		<select name="<%= InstallTask.LSID_VERSION %>" onchange="javascript:document.forms['install'].<%= INSTALL_LSID_GROUP %>_<%= module %>.value=this.options[this.selectedIndex].value" class="tasks-<%= lsidManager.getAuthorityType(lsid) %>">
<%		    boolean selected = (attributes.get(SELECTED) != null);
		    while (true) { 
			ver = (String)tasks[module].getAttributes().get(GPConstants.VERSION);
			if (ver == null) {
				ver = "";
			} else if (ver.equals("1.0")) {
				ver = "";
			}
%>
			<option value="<%= StringUtils.htmlEncode(tasks[module].getLSID()) %>"<%= selected ? " selected" : ""%> title="<%= StringUtils.htmlEncode(ver) + " - " + tasks[module].getLSID() %>"><%= (!tasks[module].getLSIDVersion().equals("") ? (StringUtils.htmlEncode(tasks[module].getLSIDVersion())) : "") %><%= (!ver.equals("") ? (" - " + StringUtils.htmlEncode(ver.substring(0, Math.min(ver.length(), 50)))) : "") %></option>
<%
			// see if there are more modules of the same name but different LSID version number
			if ((module+1) == tasks.length) break;
			LSID l = new LSID(tasks[module+1].getLSID());
			if (!l.isSimilar(new LSID(task.getLSID()))) break;
			module++;
		    }
%>
		</select>
		
		</nobr>
		
		<%
		
		if(!task.matchesAttributes(osAttributeMap)) {
			%><br />
			<font color="red">
				Warning:This task is not compatible with your operating system.
			</font><%
		}
		
		%>
		</td>

		<td valign="top" height="1">
		<nobr><%= StringUtils.htmlEncode((String)attributes.get(GPConstants.TASK_TYPE)) %></nobr>
		</td>

		<td valign="top" colspan="<%= columns.length-HEADINGS.length %>" rowspan="2">
		<table>

		<tr>
		<td valign="top" align="right">documentation:</td>
		<td valign="top">
<%
		String[] docURLs = task.getDocURLs();
		for (int doc = 0; doc < docURLs.length; doc++) {
			String filename = new File(docURLs[doc]).getName();
			if (filename.equals("version.txt")) continue;
%>
			<a href="<%= docURLs[doc] %>" target="_new"><%= filename %></a>
<%
		}
%>
		</td></tr>

		<tr>
		<td valign="top" align="right">author:</td>
		<td valign="top"><%= fixupLinksInText((String)attributes.get(GPConstants.AUTHOR)) %></td>
		</tr>

		<tr>
		<td valign="top" align="right">quality:</td>
		<td valign="top"><%= StringUtils.htmlEncode((String)attributes.get(GPConstants.QUALITY)) %></td>
		</tr>

<%
	String languageLevel = StringUtils.htmlEncode((String)attributes.get(GPConstants.JVM_LEVEL));
	if (languageLevel.equals(GPConstants.ANY)) {
		languageLevel = "";
	} else {
		languageLevel = " " + languageLevel;
	}
%>
		<tr><td valign="top" align="right">requirements:</td>
		<td valign="top">
<nobr><%= StringUtils.htmlEncode((String)attributes.get(GPConstants.LANGUAGE)) %><%= languageLevel %></nobr>,
<nobr><%= StringUtils.htmlEncode((String)attributes.get(GPConstants.OS)) %> operating system</nobr>,
<nobr><%= StringUtils.htmlEncode((String)attributes.get(GPConstants.CPU_TYPE)) %> cpu</nobr>
		</td></tr>


		</table>
		</td>
</tr>

<tr>
<td valign="top" colspan="3">
<span class="tasks-<%= lsidManager.getAuthorityType(lsid) %>">
<b><%= fixupLinksInText((String)attributes.get(GPConstants.DESCRIPTION)) %></b>
<% if (!authority.equals(LSIDUtil.BROAD_AUTHORITY)) { %>
	<br>source: <%= StringUtils.htmlEncode(authority) %>
<% } %>
</span>
</td>
</tr>

<tr>
<td colspan="<%= (HEADINGS.length+1) %>"><hr></td>
</tr>

<%
	}
%>
<tr>
<td colspan="<%= (columns.length+1) %>" align="center">
<input type="submit" name="<%= INSTALL_BUTTON %>" value="install checked"> &nbsp;&nbsp;
<a href="javascript:checkAll(this.form, true)">check all</a> &nbsp;&nbsp;
<a href="javascript:checkAll(this.form, false)">uncheck all</a>
</td>
</tr>

<% } %>

</table>

</form>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
<%! public String fixupLinksInText(String description) {
	int start = description.indexOf("http://");
	if (start == -1) start = description.indexOf("https://");
	if (start == -1) start = description.indexOf("ftp://");
	if (start == -1) start = description.indexOf("mailto:");
	if (start != -1) {
		int end = -1;
		if (end == -1) end = description.indexOf(")", start);
		if (end == -1) end = description.indexOf(",", start);
		if (end == -1) end = description.indexOf(" ", start);
		if (end == -1) end = description.length();
		description = StringUtils.htmlEncode(description.substring(0, start)) + 
				"<a href=\"" + description.substring(start, end) + "\" target=\"_blank\">" + 
				description.substring(start, end) + "</a>" + 
				StringUtils.htmlEncode(description.substring(end));
	}
	return description;
    }
%>
<%! public boolean newerServerAvailable(String versionAtBroad, String localVersion) {
	boolean ret = false;
	StringTokenizer stBroadVersion = new StringTokenizer(versionAtBroad, LSID.VERSION_DELIMITER);
	StringTokenizer stLocalVersion = new StringTokenizer(localVersion, LSID.VERSION_DELIMITER);
	String broadVersionMinor;
	String localVersionMinor;
	int broadMinor;
	int localMinor;
	NumberFormat df = NumberFormat.getIntegerInstance();
	while (stBroadVersion.hasMoreTokens()) {
		broadVersionMinor = stBroadVersion.nextToken();
		if (!stLocalVersion.hasMoreTokens()) {
			// Broad version has more parts than local, but was equal until now
			// That means that it has an extra minor level and is therefore later
			ret = true;
			break;
		}
		localVersionMinor = stLocalVersion.nextToken();
		try {
			broadMinor = df.parse(broadVersionMinor).intValue();
		} catch (ParseException nfe) {
			// what to do?
			continue;
		}
		try {
			localMinor = df.parse(localVersionMinor).intValue();
		} catch (ParseException pe) {
			// what to do?
			continue;
		}
		if (broadMinor > localMinor) {
			ret = true;
			break;
		} else if (broadMinor < localMinor) {
			//System.out.println("You're running a greater version than downloadable");
			break;
		}
	}
	// don't worry about the case where the local version has more levels than the Broad version
	return ret;
    }
%>
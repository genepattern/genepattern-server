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


<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.server.process.*,
		 org.genepattern.server.genepattern.TaskInstallationException,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.util.LSID,
		 org.genepattern.server.webservice.server.TaskIntegrator,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 java.io.File,
 		 org.genepattern.server.util.AuthorizationManager,
		 org.genepattern.server.util.IAuthorizationManager,
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
	String suiteLsid = request.getParameter("suiteLsid");
	boolean initialInstall = (request.getParameter("initialInstall") != null);

	SuiteRepository sr = new SuiteRepository();
	HashMap suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));
	HashMap hm = (HashMap)suites.get(suiteLsid);

	String username = (String)request.getAttribute("userID");
	AuthorizationManager authManager = new AuthorizationManager();

	boolean taskInstallAllowed = authManager.checkPermission("createTask", username);
	boolean suiteInstallAllowed = authManager.checkPermission("createSuite", username);

	if (!suiteInstallAllowed) {
		response.sendRedirect("notpermitted.jsp?link='installSuite.jsp'");
		return;
	}

%>

<html>
	<head>
	<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
	<title>Installing Suite - <%= hm.get("name") %></title>
</head>
<body>

<jsp:include page="navbar.jsp"></jsp:include>

Installing Suite - 	<font size=+1><b><%= hm.get("name") %></b></font>
...done.<br>
Installing Modules:<br>

<%

	String userID= (String)request.getAttribute("userID");
	TaskIntegrator taskIntegrator = new LocalTaskIntegratorClient( userID , out);
	InstallTasksCollectionUtils collection = null;

	collection = new InstallTasksCollectionUtils(userID, false);

	InstallTask[] tasks = collection.getAvailableModules();
	HashMap hmLSIDToInstallTask = new HashMap();
	for (int t = 0; t < tasks.length; t++) {
		hmLSIDToInstallTask.put(tasks[t].getLSID(), tasks[t]);
	}


	// install the suite
	taskIntegrator.install(suiteLsid);

	if (taskInstallAllowed){		
	// now the modules in it

	// build TreeSet of InstallTask to install (sorting on task name)
	TreeSet tsToInstall = new TreeSet(new Comparator() {
		public int compare(Object o1, Object o2) {
			InstallTask t1 = (InstallTask)o1;
			InstallTask t2 = (InstallTask)o2;
			return (t1.getName().compareToIgnoreCase(t2.getName()));
		}
	    });
	InstallTask installTask;
	ArrayList modules = (ArrayList)hm.get("modules");
		
	for (Iterator iter = modules.iterator(); iter.hasNext();  ) {
			Map m = (Map)iter.next();
	
			installTask = (InstallTask)hmLSIDToInstallTask.get((String)m.get("lsid"));
			tsToInstall.add(installTask);
			
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
				&nbsp;&nbsp;&nbsp;&nbsp;<%= wasInstalled ? "Overwrote" : "Installed" %> <a href="addTask.jsp?name=<%= installTask.getLSID() %>"><%= installTask.getName() %></a> version <%= new LSID(installTask.getLSID()).getVersion() %><br>
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
	} else {
%>	
No tasks installed.  You do not have permission to install tasks on this server.
<%
	}
%>
<br>
<a href="suiteCatalog.jsp<%= initialInstall ? "?initialInstall=1" : "" %>">install more suites</a><br>

		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>

	
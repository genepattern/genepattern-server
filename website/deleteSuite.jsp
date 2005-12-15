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
		 org.genepattern.server.webservice.server.local.LocalAdminClient,
		 org.genepattern.webservice.SuiteInfo,
		 java.util.ArrayList,
		 java.util.Vector"
   session="false" language="Java" %>
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>

<%
	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
	String suiteLsid = request.getParameter("suiteLsid");
	String userID= (String)request.getAttribute("userID");
	boolean initialInstall = (request.getParameter("initialInstall") != null);

//	SuiteRepository sr = new SuiteRepository();
//	HashMap suites = sr.getSuites(System.getProperty("SuiteRepositoryURL"));
//	HashMap hm = (HashMap)suites.get(suiteLsid);

	LocalAdminClient adminClient = new LocalAdminClient("GenePattern");
	SuiteInfo si = adminClient.getSuite(suiteLsid);

%>

<html>
	<head>
	<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
	<title>Deleting Suite - <%= si.getName() %></title>
</head>
<body>

<jsp:include page="navbar.jsp"></jsp:include>

Deleting Suite - 	<font size=+1><b><%= si.getName() %></b></font>
<%
try {
	TaskIntegrator taskIntegrator = new LocalTaskIntegratorClient( userID , out);
	taskIntegrator.delete(suiteLsid);
} catch (Throwable t){
	t.printStackTrace();
}


%>
...done.<br>
<br>
<a href="suiteCatalog.jsp<%= initialInstall ? "?initialInstall=1" : "" %>">back to suites</a><br>

		<jsp:include page="footer.jsp"></jsp:include>
		</body>
		</html>

	
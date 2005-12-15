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


<%@ page contentType="text/html" language="Java" session="false" 
	import="java.net.MalformedURLException,
		java.util.Collection,
		java.util.Iterator,
		java.util.HashSet,
		java.util.HashMap,
		java.util.Set,
		java.util.Vector,
		java.util.Enumeration,
		java.util.ArrayList,
		java.util.StringTokenizer,
		java.util.LinkedHashSet,
		org.genepattern.webservice.SuiteInfo,
		org.genepattern.webservice.WebServiceException,
     		org.genepattern.server.util.AccessManager,
		org.genepattern.server.genepattern.LSIDManager,
		org.genepattern.server.webservice.server.local.LocalAdminClient,
	 	org.genepattern.util.GPConstants,
		org.genepattern.util.LSIDUtil,
 	 	org.genepattern.util.StringUtils,
		org.genepattern.util.LSID" %>

<%
String userID = (String)request.getAttribute("userID"); // get userID but don't force login if not defined
ArrayList suiteFilterAttr = (ArrayList)request.getSession().getAttribute("suiteSelection");
String[] suiteFilterParams = request.getParameterValues("suiteSelection");
if (suiteFilterAttr == null) suiteFilterAttr = new ArrayList();

String filter = request.getParameter("filter");


System.out.println("filter " + filter);
boolean filtering = false;

if ("filter".equals(filter)) {

	if (suiteFilterAttr.contains("all")) {
		suiteFilterAttr.remove("all");
	}
	filtering=true;
}  else if (filter == null) {
	if (suiteFilterAttr.contains("all")) {
		filtering = false;
	} else {
		filtering = true;
	}
}


if (filtering) { 
	if (suiteFilterAttr.contains("all")) suiteFilterAttr.remove("all");


	if (suiteFilterParams  != null){
		suiteFilterAttr = new ArrayList();
		for (int i=0; i < suiteFilterParams.length; i++){
			String suiteId = suiteFilterParams[i];
			suiteFilterAttr.add(suiteId);
		}
		request.getSession().setAttribute("suiteSelection",suiteFilterAttr );
	}

} else {
	// ensure all is set but remember other values
	if (!(suiteFilterAttr.contains("all"))){
		suiteFilterAttr.add("all");
		request.getSession().setAttribute("suiteSelection",suiteFilterAttr );
	}
}
	
SuiteInfo[] suites = new SuiteInfo[0];
 
LocalAdminClient adminClient = new LocalAdminClient(userID);
suites = adminClient.getAllSuites();

%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
<title> Filter by suite</title>

</head>
<body class="paleBackground" >
<script language="Javascript">
function okPressed(frm) {
	frm.submit()
//	window.opener.history.go(0);	
	window.opener.location.reload();	
	//window.close();

}

function toggleFilter(showing, form){
	
	var checks = document.getElementsByName("suiteSelection")
	for (i=0; i < checks.length; i++){
		checks[i].disabled=!showing
	}
}

</script>



<%
if (suites.length == 0){
%>
There are no suites loaded. To filter by suites you must first <a href='editSuite.jsp' target='#'>create</a> or <a href='suiteCatalog.jsp' target='#'>install</a>
an existing suite.<p>

<input type='button' name='cancel' value='Close' onClick="window.close()"/>

<% 
} else { // suites.length != 0
%>

<form>
<table border=0 cellspacing=0  width=100%>
<tr class='paleBackground'><td >

<input type='radio' name='filter' value='all' onClick="toggleFilter(false, this.form)"
<%if (!filtering) {%>
checked='true' 
<%}%>
>No Filtering (show all tasks)</input>
</td></tr>
<tr class='paleBackground'><td >

<input type='radio' name='filter' value='filter'  onClick="toggleFilter(true, this.form)"
<%if (filtering) {%>
checked='true' 
<%}%>
>Filter (show only tasks from selected suites)</input>
</td></tr>
<tr class='paleBackground'><td><hr></td></tr>
<%
for (int i=0; i < suites.length; i++ ){
	SuiteInfo suite = suites[i];		
	boolean selectedSuite = false;
	if (suiteFilterAttr.contains(suite.getLSID())) {
		selectedSuite = true;
	} else {
		selectedSuite = false;
	}
%>
	<tr
<%
	if ((i % 2) != 0){
%>
	bgcolor="#EFEFFF" 
<%} else  { %>
	bgcolor="#FFFFFF" 
<%}%>
><td>
	<input type='checkbox' name='suiteSelection'
<%
	if (selectedSuite) { %> checked='true'
<% } %>
<%
	if (!filtering) { %> disabled='true'
<% } %>

 value='<%= suite.getLSID()%>'/><%=suite.getName()%><br>
</td></tr>
<%
	} // end loop over suites
%>


<tr><td>
<input type='button' name='ok' value='OK' onClick="okPressed(this.form)"/>
<input type='button' name='cancel' value='Close' onClick="window.close()"/>
</td></tr>
</table>
</form>
<% 
} // end of we have more than 0 suites
%>

</body>
</html>

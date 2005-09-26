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
if (!("all".equals(filter))) {
	filtering = true;
}

if (filtering) { 
	if (suiteFilterParams  != null){
		suiteFilterAttr = new ArrayList();
		for (int i=0; i < suiteFilterParams.length; i++){
			String suiteId = suiteFilterParams[i];
			suiteFilterAttr.add(suiteId);
		}
		if (suiteFilterAttr.contains("all")) {
			suiteFilterAttr.remove("all");
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
<head><title> Filter by suite</title></head>

<script language="Javascript">
function okPressed(frm) {

	frm.submit()

	window.opener.history.go(0);	
	//window.close();

}

function toggleFilter(showing, form){
	
	var checks = document.getElementsByName("suiteSelection")
	for (i=0; i < checks.length; i++){
		checks[i].disabled=!showing
	}
}

</script>


<body>
<form>
<input type='radio' name='filter' value='all' onClick="toggleFilter(false, this.form)"
<%if (!filtering) {%>
checked='true' 
<%}%>
>No Filtering (show all tasks)</input><br>
<input type='radio' name='filter' value='filter'  onClick="toggleFilter(true, this.form)"
<%if (filtering) {%>
checked='true' 
<%}%>
>Filter (show only tasks from selected suites)</input>
<hr>

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
	<input type='checkbox' name='suiteSelection'
<%
	if (selectedSuite) { %> checked='true'
<% } %>
<%
	if (!filtering) { %> disabled='true'
<% } %>

 value='<%= suite.getLSID()%>'/><%=suite.getName()%><br>



<%
	} // end loop over suites
%>
<input type='button' name='ok' value='OK' onClick="okPressed(this.form)"/>
<input type='button' name='cancel' value='Close' onClick="window.close()"/>
</form>


</body>
</html>

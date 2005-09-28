<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.server.process.*,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.LocalAdminClient,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.SuiteInfo,
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
		 java.util.Properties,
		 java.util.Map,
		 java.util.TreeMap,
		 java.util.TreeSet,
		 java.util.StringTokenizer,
		 java.util.TreeSet,
		 java.util.List,
		 java.util.ArrayList,
		 java.util.Vector"
   session="false" language="Java" %>
<jsp:useBean id="messages" class="org.genepattern.server.util.MessageUtils" scope="page"/>


<%
	int NUM_ATTACHMENTS = 3;

	response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
	response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
	response.setDateHeader("Expires", 0);
	String userID= (String)request.getAttribute("userID");
	LocalAdminClient adminClient = new LocalAdminClient("GenePattern");

	String[] privacies = GPConstants.PRIVACY_LEVELS;
	Map typeTaskMap = adminClient.getLatestTasksByType();
	Map taskVersionMap = adminClient.getLSIDToVersionsMap();
	

try {
%>
	<html>
	<head>
	<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
<title>Edit Suite</title>
</head><body>
<form action="createSuite.jsp">
<jsp:include page="navbar.jsp"></jsp:include>

<table width=100% cellspacing=2>
<tr><td colspan=3><h2>Create new GenePattern Suite</td></tr>
<tr>
<td align="right"><b>Name:</b></td><td><input type='text' name='suiteName' size=60  maxlength="100">(*required, no spaces)</td></tr>
<td align="right"><b>Description:</b></td><td><textArea rows="2" cols="60" name='suiteDescription'></textarea> </td></tr>
<td align="right"><b>Author:</b></td><td><input type='text' name='suiteAuthor' size=80 maxlength="100">(name, affiliation)</td></tr>
<td align="right"><b>Owner:</b></td><td><input type='text' name='suiteOwner' size=60 maxlength="100" value="<%=userID%>">(email address)</td></tr>
<td align="right"><b>Privacy:</b></td>
	<td>
		<select name='privacy'>
			<option value="<%=privacies[0]%>"><%=privacies[0]%></option>
			<option value="<%=privacies[1]%>"><%=privacies[1]%></option>
		</select>
	</td>
</tr>
	

 <tr>
  <td align="right" valign="top"><b>Support&nbsp;files:</b><br>(pdf, doc, <br>data files,<br> etc.)<br>
  </td>
  <td width="*">
<font size=-1>
  Any documentation or data files you wish to bundle in with the suite</font><br>


<% for (int i = 1; i <= NUM_ATTACHMENTS; i++) { %>
  	<input type="file" name="file<%= i %>" size="70" class="little"><br>
<% } %>

</td>
</tr>
</table>
<table width=100% cellspacing=2>
<tr><td colspan=1 align="center"><input type="submit" value="save"/><input type="button" name="clear" value="clear"/></td></tr>

<%
	//
	// get the stats on the sizes of the categories
	//
	int numCat = typeTaskMap.size();
	int[] counts = new int[numCat];
	HashMap sizeMap = new HashMap();
	HashMap catMap = new HashMap();
	int j=0;
	int sum = 0;
	int max = 0;
	for (Iterator iter = typeTaskMap.keySet().iterator(); iter.hasNext(); j++){
		String taskType = (String)iter.next();
		TreeSet tasks = (TreeSet)typeTaskMap.get(taskType);

		counts[j] = tasks.size();
		sum += tasks.size();
		sizeMap.put(new Integer(j), new Integer(tasks.size()));
		catMap.put(new Integer(j), taskType);
		max = Math.max(max, tasks.size());
	}
	int mean = (int)Math.ceil(sum / numCat);
	int transition = (max + mean)/2; 

	//
	// calculte the indices
	//
	ArrayList col1 = new ArrayList();
	ArrayList col2 = new ArrayList();
	int idx = 0;
	
	while (sizeMap.size() > 0){
		int col1Size = 0;
		int col2Size = 0;
		ArrayList row1 = new ArrayList();
		ArrayList row2 = new ArrayList();
		col1.add(row1);
		col2.add(row2);	
	
		Integer index;
		int addNext = counts[idx];
				
		// initialize the row
		if (col1Size == 0) {
			index = new Integer(idx);
			row1.add(index);
			col1Size += addNext;
			sizeMap.remove(index);
		} 

		// look for col 2 > mean
		int idx2 = idx;
		// find the next index which is not in use and who's size is > mean
		boolean foundNext = false;
		do {
			idx2++;
			// see it is not used yet
			if (sizeMap.get(new Integer(idx2))  != null) {
				if (counts[idx2] > mean){
					foundNext = true;	
				}
			} else if (idx2 >= numCat) {
				foundNext = true;
			}
		} while (!foundNext);

		if (idx2 >= numCat) break;

		index = new Integer(idx2);
		row2.add(index);
		col2Size += counts[idx2];
		sizeMap.remove(index);

		// now pad col 1 with next ones until it is as big as col2
		idx2 = idx + 1;
		addNext = counts[idx2];
		while ( ((col1Size + addNext) <= (col2Size+1)) && (idx2 < numCat)  ) {
			if (sizeMap.get(new Integer(idx2))  != null){
				index = new Integer(idx2);
				row1.add(index);
				col1Size += addNext;
				sizeMap.remove(index);
			}
			idx2++;
			while ((sizeMap.get(new Integer(idx2))  == null) && (idx2 < numCat)){
				idx2++;
				System.out.println(" " + idx2);
			}
			addNext = counts[idx2];

		} // end of row next starting index is lowest value left in the keyset of sizeMap
		
		int min = numCat;
		for (Iterator iter = sizeMap.keySet().iterator(); iter.hasNext(); ){
			index = (Integer)iter.next();
			if (index.intValue() < min) min = index.intValue();	
		}
		idx = min;

	}


	int numRows = (int)Math.max(col1.size(), col2.size());
	for (int i=0; i < numRows; i++){
		out.println("<tr>");

		ArrayList r1 = (ArrayList)col1.get(i);
		ArrayList r2 = (ArrayList)col2.get(i);
		
		out.println("<td width='50%'>");
		for (int k=0; k < r1.size(); k++){
			Integer ridx = (Integer)r1.get(k);
			String taskType = (String)catMap.get(ridx);
			TreeSet tasks = (TreeSet)typeTaskMap.get(taskType);
		
%>
<P><b><%=taskType%></b>	
<%

			for (Iterator iter2 = tasks.iterator(); iter2.hasNext(); ){
				TaskInfo task = (TaskInfo)iter2.next();
				String lsidStr = (String)task.getTaskInfoAttributes().get("LSID");
				LSID lsid = new LSID(lsidStr);
				String lsidNoVer = lsid.toStringNoVersion();
				Vector versions = (Vector)taskVersionMap.get(lsidNoVer );
				String link = (task.getName().endsWith(".pipeline") ? "viewPipeline.jsp" : "addTask.jsp");

%>
<br>&nbsp;&nbsp;&nbsp;&nbsp;<input type="checkbox" name="LSID" value="<%=lsidStr%>"/><a href="<%=link%>?view=1&name=<%=lsidStr%>"><%=task.getName()%></a>

<% if (versions.size() > 1) { %>
&nbsp;&nbsp;<select name="<%=lsidNoVer%>">
<%
			for (Iterator iter3 = versions.iterator(); iter3.hasNext(); ){

				String ver = (String )iter3.next();
%>
	<option value="<%=ver%>"><%=ver%></option>
<%}%>		
</select>
<% } else {  // only one version available %>
	<input type='hidden' name='<%=lsidNoVer%>' value='<%=lsid.getVersion()%>'/>&nbsp;(<%=lsid.getVersion()%>)

<% } %>

<% 			} // iterate over tasks in a type  

		} 
		out.println("</td>");
		if (r2 != null){
			out.println("<td width='50%'>");
			for (int k=0; k < r2.size(); k++){
				Integer ridx = (Integer)r2.get(k);
				String taskType = (String)catMap.get(ridx);
				TreeSet tasks = (TreeSet)typeTaskMap.get(taskType);
		
%>
<b><%=taskType%></b>	
<%

			for (Iterator iter2 = tasks.iterator(); iter2.hasNext(); ){
				TaskInfo task = (TaskInfo)iter2.next();
				String lsidStr = (String)task.getTaskInfoAttributes().get("LSID");
				LSID lsid = new LSID(lsidStr);
				String lsidNoVer = lsid.toStringNoVersion();
				Vector versions = (Vector)taskVersionMap.get(lsidNoVer);
				String link = (task.getName().endsWith(".pipeline") ? "viewPipeline.jsp" : "addTask.jsp");

				
%>
<br>&nbsp;&nbsp;&nbsp;&nbsp;<input type="checkbox" name="LSID" value="<%=task.getTaskInfoAttributes().get("LSID")%>"/><a href="<%=link%>?view=1&name=<%=lsidStr%>"><%=task.getName()%></a>

<% if (versions.size() > 1) { %>
&nbsp;&nbsp;<select name="<%=lsidNoVer%>">
<%
			for (Iterator iter3 = versions.iterator(); iter3.hasNext(); ){

				String ver = (String )iter3.next();
%>
	<option value="<%=ver%>"><%=ver%></option>
<%}%>		
</select>
<% } else {  // only one version available %>
	<input type='hidden' name='<%=lsidNoVer%>' value='<%=lsid.getVersion()%>'/>&nbsp;(<%=lsid.getVersion()%>)

<% } %>
<% 			} // iterate over tasks in a type  



			} 
			out.println("</td>");
		}
		out.println("</tr>");
	}
 
%>
<tr><td colspan=2 align="center"><input type="submit" value="save"/><input type="button" name="clear" value="clear"/></td></tr>
</table>
<%
	} catch (Throwable t){
		t.printStackTrace();
	}
%>
</form>

<jsp:include page="footer.jsp"></jsp:include> 
</body>
</html>

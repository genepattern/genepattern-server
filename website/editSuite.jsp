<%@ page import="org.genepattern.server.webapp.*,
		 org.genepattern.server.process.*,
		 org.genepattern.server.genepattern.LSIDManager,
		 org.genepattern.server.webservice.server.local.LocalAdminClient,
		 org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.SuiteInfo,
		 org.genepattern.util.LSIDUtil,
		 org.genepattern.util.GPConstants,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.util.LSID,
		  java.net.URLEncoder,
		 java.io.File,
		 javax.activation.*,
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
	LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(userID, out);

	String[] privacies = GPConstants.PRIVACY_LEVELS;
	Map typeTaskMap = adminClient.getLatestTasksByType();
	Map taskVersionMap = adminClient.getLSIDToVersionsMap();
	String suiteLsid = request.getParameter("suiteLsid");
	SuiteInfo si = new SuiteInfo("", "", "", "", userID, new ArrayList(), 1, new ArrayList());
	if (suiteLsid != null) {
		si = adminClient.getSuite(suiteLsid);
	}



try {



%>
	<html>
	<head>
	<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
<title>Edit Suite</title>


<script language="javascript">

function confirmDeleteSupportFiles() {
	var sel = document.forms['edit'].deleteFiles;
	var selection = sel.options[sel.selectedIndex].value;
	if (selection == null || selection == "") return;
	if (window.confirm('Really delete ' + selection + ' from  <%= si.getName() %>\'s support files?')) { 
		//window.location='saveTask.jsp?deleteSupportFiles=1&deleteFiles=' + selection + '&<%= GPConstants.NAME %>=' + document.forms['task'].<%= GPConstants.NAME %>.value + '&<%= GPConstants.LSID %>=' + document.forms['task']['<%= GPConstants.LSID %>'].value;
		sel.form.deleteSupportFiles.value = "1";
		sel.form.submit();
	}
}
</script>


</head><body>
<jsp:include page="navbar.jsp"></jsp:include>


<form action="createSuite.jsp" name='edit' method="post" ENCTYPE="multipart/form-data" >
<input type='hidden' name='suiteLSID' value="<%=si.getLSID()%>"/>

<%// ENCTYPE="multipart/form-data" %>


<table width=100% cellspacing=2>
<tr><th colspan=2 align='left'><h2>Create new GenePattern Suite</h2></th></tr>
<tr>
<td align="right"><b>Name:</b></td><td><input type='text' name='suiteName' size=60  maxlength="100" value="<%=si.getName()%>">(*required, no spaces)</td></tr>

<tr><td align="right"><b>LSID:</b></td><td><%=si.getLSID()%></td></tr>

<tr><td align="right"><b>Description:</b></td><td><textArea rows="2" cols="60" name='suiteDescription'><%=si.getDescription()%></textarea> </td></tr>
<tr><td align="right"><b>Author:</b></td><td><input type='text' name='suiteAuthor' size=80 maxlength="100" value="<%=si.getAuthor()%>">(name, affiliation)</td></tr>
<tr><td align="right"><b>Owner:</b></td><td><input type='text' name='suiteOwner' size=60 maxlength="100" value="<%=si.getOwner()%>">(email address)</td></tr>
<tr><td align="right"><b>Privacy:</b></td>
    <td><select name='privacy'>
		
<% 
	for (int i=0; i < privacies.length; i++){
		boolean selected = (i == si.getAccessId());

		out.println("<option value='"+ privacies[i]+"'");
		if (selected) out.println(" selected='true' ");
		out.println(" > "+privacies[i]+" </option>");
}
%>
		</select>
	</td>
</tr>

<tr>
<td colspan=2> <hr> </td>	
</tr>

 <tr>
  <td align="right" valign="top"><b>Support&nbsp;files:</b><br>(pdf, doc, <br>data files,<br> etc.)<br>
  </td>
  <td width="*">
<font size=-1>
  Any documentation or data files you wish to bundle in with the suite</font><br>


<% 
// XXX must display existing support files and link to delete them

	for (int i = 1; i <= NUM_ATTACHMENTS; i++) { %>
  	<input type="file" name="file<%= i %>" size="70" class="little"><br>
<% } %>

</td>
</tr>


 <tr>
  <td align="right" valign="top"><b>Current&nbsp;files:</b></td>
  <td width="*">
<%
   	DataHandler[] dh = new DataHandler[0];

	if ((si.getLSID() != null) && (si.getLSID().trim().length() > 0)){
	

String[] docs = si.getDocumentationFiles();
for (int k=0; k < docs.length; k++ ){
		String doc = docs[k];
%>
		<a href='getSuiteDoc.jsp?name=<%=si.getLSID()%>&file=<%=StringUtils.htmlEncode(doc)%>'><%=doc%></a>&nbsp;&nbsp;

<% }%>

<%	   if (docs.length > 0 ) { %>
		   <br>
		   <select name="deleteFiles">
		   <option value="">delete support files...</option>
<%		   for (int i = 0; i < docs.length; i++) { %>
			<option value="<%= StringUtils.htmlEncode(docs[i]) %>"><%= docs[i] %></option> 
<%		   }  %>
		   </select>
		   <input type="hidden" name="deleteSupportFiles" value="">
		   <input type="button" value="delete..." class="little" onclick="confirmDeleteSupportFiles()">
<%	   } 

	}%>


  <br>
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
			Integer anInt = 	new Integer(idx2);		
			Object anIdx = null;
			try {
				anIdx = sizeMap.get(anInt);
			} catch (ArrayIndexOutOfBoundsException e){
				System.out.println("ERROR\n\n" + anInt + "\n--" + anIdx);
			}

			if ( anIdx != null){
				index = new Integer(idx2);
				row1.add(index);
				col1Size += addNext;
				sizeMap.remove(index);
			}
			idx2++;
			while ((sizeMap.get(new Integer(idx2))  == null) && (idx2 < numCat)){
				idx2++;
			}
			
			if (idx2 < counts.length)
				addNext = counts[idx2];
			
		} // end of row next starting index is lowest value left in the keyset of sizeMap
		
		int min = numCat;
		for (Iterator iter = sizeMap.keySet().iterator(); iter.hasNext(); ){
			index = (Integer)iter.next();
			if (index.intValue() < min) min = index.intValue();	
		}
		idx = min;

	}

	String[] lsidStrs = si.getModuleLSIDs();
	List modsLsids = Arrays.asList(lsidStrs);
			

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
				String checkVer = null;
				boolean checked = false;
				if (modsLsids.contains(lsidStr)){
					checked = true;
				} else {
					// look for alt versions
					for (Iterator iter3 = versions.iterator(); iter3.hasNext(); ){
						String ver = (String )iter3.next();
						if (modsLsids.contains(lsidNoVer+":"+ver)){
							checked = true;
							checkVer = ver;	
							break;
						}
					}
				}

%>
<br>&nbsp;&nbsp;&nbsp;&nbsp;<input type="checkbox" name="LSID" value="<%=lsidStr%>"
<%
	if (checked) out.println(" checked='true' ");
%>
/><a href="<%=link%>?view=1&name=<%=lsidStr%>"><%=task.getName()%></a>

<% if (versions.size() > 1) { %>
&nbsp;&nbsp;<select name="<%=lsidNoVer%>">
<%
			for (Iterator iter3 = versions.iterator(); iter3.hasNext(); ){

				String ver = (String )iter3.next();
%>
	<option value='<%=ver%>' 
<%
				if (ver.equals(checkVer)){
					 out.println(" selected='true' ");
				}

%>
	><%=ver%></option>
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
				boolean checked = false;
				String checkVer = null;
				if (modsLsids.contains(lsidStr)){
					checked = true;
				}  else {
					// look for alt versions
					for (Iterator iter3 = versions.iterator(); iter3.hasNext(); ){
						String ver = (String )iter3.next();
						if (modsLsids.contains(lsidNoVer+":"+ver)){
							checked = true;
							checkVer = ver;	
							break;
						}
					}
				}

				
%>
<br>&nbsp;&nbsp;&nbsp;&nbsp;<input type="checkbox" name="LSID" 
<%
	if (checked) out.println(" checked='true' ");
%>
value="<%=task.getTaskInfoAttributes().get("LSID")%>"/><a href="<%=link%>?view=1&name=<%=lsidStr%>"><%=task.getName()%></a>

<% if (versions.size() > 1) { %>
&nbsp;&nbsp;<select name="<%=lsidNoVer%>">
<%
			for (Iterator iter3 = versions.iterator(); iter3.hasNext(); ){

				String ver = (String )iter3.next();
%>
	<option value='<%=ver%>' 
<%
		if (ver.equals(checkVer)) out.println(" selected='true' ");
%>

><%=ver%></option>
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

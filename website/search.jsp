<%@ page import="org.apache.lucene.document.Document,
		 org.apache.lucene.document.Field,
		 org.apache.lucene.search.IndexSearcher,
		 org.apache.lucene.queryParser.MultiFieldQueryParser,
		 org.apache.lucene.search.Query,
		 org.apache.lucene.search.Hits,
		 org.apache.lucene.store.FSDirectory,
		 org.apache.lucene.store.Directory,
		 org.apache.lucene.queryParser.QueryParser,
		 org.apache.lucene.analysis.standard.StandardAnalyzer,
		 java.io.File,
		 java.text.DecimalFormat,
		 java.util.Enumeration,
		 java.util.Vector,
		 org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.server.analysis.genepattern.Indexer"
    session="false" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);


boolean DEBUG = (request.getParameter("DEBUG") != null);
//DEBUG = true;
String[] searchTypes = new String[] { Indexer.TASK, Indexer.TASK_DOC, Indexer.TASK_SCRIPTS, Indexer.JOB_PARAMETERS, Indexer.JOB_OUTPUT /*, Indexer.MANUAL */};
String[] searchTypeNames = new String[] { "tasks", "task documentation", "task support files", "job parameters", "job output files" /*, "manual" */};

%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link href="favicon.ico" rel="shortcut icon">
<title>GenePattern search engine</title>
<style>
TD.small { font-size: 8pt }
</style>

<script language="javascript">
// invoked on search type checkbox changes
function changeTypes(fld) {
	var numChecked = 0;
<%	for (int i = 0; i < searchTypes.length; i++) { %>
	if (fld.form.<%= searchTypes[i] %>.checked) numChecked++;
<%	} %>
	fld.form.submit.disabled = (numChecked == 0);
}

</script>

</head>
<body onload="document.forms['find'].search.focus();">
<jsp:include page="navbar.jsp"></jsp:include>
<form name="find">

<%

String SEARCH = "search";
String GPResources = System.getProperty("index");
if (GPResources == null) throw new Exception("genepattern.properties environment variable not set");
File indexDir = new File(GPResources);
Hits hits = null;
Directory fsDir = null;
IndexSearcher is = null;
String q = request.getParameter(SEARCH);

if (q != null && q.length() > 0) {
    fsDir = FSDirectory.getDirectory(indexDir, false);
    is = new IndexSearcher(fsDir);
    Vector vFieldsToQuery = new Vector();
    vFieldsToQuery.add(Indexer.TASKNAME);
    vFieldsToQuery.add(Indexer.FILENAME);
    for (int i = 0; i < searchTypes.length; i++) {
	if (request.getParameter(searchTypes[i]) != null) {
		vFieldsToQuery.add(searchTypes[i]);
		if (DEBUG) {
			out.println("searching " + searchTypes[i] + "<br>");
		}
	}
    }

    String[] fields = new String[vFieldsToQuery.size()];
    vFieldsToQuery.toArray(fields);

    // seems to help if query is quoted for those with some punctuation in them, such as AFFX-BioB-3_at
    // if (q.indexOf(" ") == -1 && q.indexOf("\"") == -1) q = "\"" + q + "\"";


    int numHits = 0;
    try {
	    Query query = MultiFieldQueryParser.parse(q, fields, Indexer.GPAnalyzer);
	    hits = is.search(query);
	    if (DEBUG) {
		out.println(hits.length() + " hits<br>");
	    }
	    String docType = null;
	    for (int i = 0; i < hits.length(); i++) {
	        docType = hits.doc(i).get(Indexer.TYPE);
		if (DEBUG) {
			System.out.println(hits.doc(i).get(Indexer.TYPE));
		}
		if (docType.equals("task") && request.getParameter(Indexer.TASK) == null) continue;
		if (docType.equals("job") && request.getParameter(Indexer.JOB_PARAMETERS) == null) continue;
		if (docType.equals("doc") && request.getParameter(Indexer.TASK_DOC) == null && request.getParameter(Indexer.TASK_SCRIPTS) == null && request.getParameter(Indexer.MANUAL) == null) continue;
		if (docType.equals("output") && request.getParameter(Indexer.JOB_OUTPUT) == null) continue;
		numHits++;
	    }
    } catch (Throwable e) {
%><b><font color="red"><%= e.getMessage() %> in query term <font color="black"><%= GenePatternAnalysisTask.htmlEncode(q) %></font></font></b><br><br>
<%
    }
%>
<%= numHits %> <%= numHits != 1 ? "matches" : "match" %> for 
<% } else { %>
search for: 
<% } %>
	   <input type="text" name="<%= SEARCH %>" value="<%= request.getParameter(SEARCH) != null ? GenePatternAnalysisTask.htmlEncode(request.getParameter(SEARCH)) : "" %>" size="40" onfocus="javascript:this.select()">
	   <input type="submit" name="submit" value="search" class="little"><br>
<!--<b><%=  GenePatternAnalysisTask.htmlEncode(q) %></b> -->
<font size="-2">
	<br>
	in 
<%	for (int i = 0; i < searchTypes.length; i++) { %>
		<nobr><input type="checkbox" name="<%= searchTypes[i] %>" value="1" <%= request.getParameter(searchTypes[i]) != null ? "checked" : ""%> onchange="changeTypes(this)"><%= searchTypeNames[i] %></nobr>
<%	} %>
</font>

<br><br>
<%
if (q != null && q.length() > 0) {
%>
<table>
<%

    DecimalFormat df = new DecimalFormat("##0%"); // format for display of percent relevance
    String docType = null;

    // TODO: enumerate fields in doc

    for (int i = 0; hits != null && i < hits.length(); i++) {
        Document doc = hits.doc(i);

	if (DEBUG) {
		out.println("<tr><td>");
		for (Enumeration eFields = doc.fields(); eFields.hasMoreElements(); ) {
			Field fld = (Field)eFields.nextElement();
			out.println(fld.name() + "=" + fld.stringValue() + "<br>");
		}
		out.println("</td></tr>");
	}

	docType = doc.get(Indexer.TYPE);
	if (docType.equals("task") && request.getParameter(Indexer.TASK) == null) continue;
	if (docType.equals("job") && request.getParameter(Indexer.JOB_PARAMETERS) == null) continue;
	if (docType.equals("doc") && request.getParameter(Indexer.TASK_DOC) == null && request.getParameter(Indexer.TASK_SCRIPTS) == null && request.getParameter(Indexer.MANUAL) == null) continue;
	if (docType.equals("output") && request.getParameter(Indexer.JOB_OUTPUT) == null) continue;

	String filename = doc.get(Indexer.FILENAME);
	String url = doc.get(Indexer.URL);
	String taskName = doc.get(Indexer.TASKNAME);
	String jobID = doc.get(Indexer.JOBID);
	String title = doc.get(Indexer.TITLE);
	String lsid = doc.get(Indexer.LSID);
	boolean jobHasOutput = (doc.get(Indexer.JOB_HAS_OUTPUT) != null);
	if (filename == null) filename = taskName;
	if (url != null && filename != null) {
%>
		<tr>
		    <td align="right"><%= df.format(hits.score(i)) %></td>
		    <td><%= doc.get(Indexer.TYPE) %>:<a href="<%= url %>" target="<%= filename %>"><%= title == null ? filename : title %></a><% 
			if (jobID != null) {
%> &nbsp;(job:<%= jobHasOutput ? "<a href=\"getJobResults.jsp?jobID=" + jobID + "\">" : "" %><%=jobID %><%= jobHasOutput ? "</a>" : "" %>
				- task:<%= lsid != null ? ("<a href=\"addTask.jsp?" + GPConstants.NAME + "=" + lsid + "&view=1\">") : "" %><%=taskName %><%= lsid != null ? "</a>" : "" %>)
<%			} else if (doc.get(Indexer.FILENAME) != null && taskName != null) { %>
				(task:<%= lsid != null ? ("<a href=\"addTask.jsp?" + GPConstants.NAME + "=" + lsid + "&view=1\">") : "" %><%=taskName %><%= lsid != null ? "</a>" : "" %>)
<%			} else { %>
<%			} %>
		    </td>
		</tr>
<%
	} else {
%>
		<tr>
		    <td align="right"></td>
		    <td>no <%= filename == null ? "filename" : "" %> <%= url == null ? "url" : "" %> for <%= doc.get(Indexer.TASKNAME) %></td>
		</tr>
<%
	}
    }
%>
</table>
<%
    fsDir.close();
    is.close();
}
%>
<br>
<table cols="3">

<!-- help on search syntax -->
<tr><td colspan="3"><hr></td></tr>
<tr>
	<td colspan="3" class="small">
	Search syntax:
	</td>
</tr>
<tr>
	<td align="top" class="small"><samp>te?t</samp></td>
	<td colspan="2" class="small">single character wildcard, matches test, text.  ? may not be used as the first character in the search query.</td>
</tr>
<tr>
	<td align="top" class="small"><samp>test*</samp></td>
	<td colspan="2" class="small">multiple character (0 or more) wildcard, matches test, tests, tester.  * may not be used as the first character in the search query.</td>
</tr>
<tr>
	<td align="top" class="small"><samp>roam~</samp></td>
	<td colspan="2" class="small">fuzzy search, matches foam and roams</td>
</tr>
<tr>
	<td align="top" class="small"><samp>"cyclin neighbors"~10</samp></td>
	<td colspan="2" class="small">proximity search, matches cyclin within 10 words of neighbors</td>
</tr>
<tr>
	<td align="top" class="small">boolean operators</td>
	<td colspan="2" class="small">You may use OR, AND, NOT, +, - to denote combinations of terms.  Boolean operators must be uppercase! </td>
</tr>
<tr>
	<td align="top" class="small"><samp>(foo AND bar) OR zed</samp></td>
	<td colspan="2" class="small">Grouping</td>
</tr>
<tr>
	<td align="top" class="small"><samp>+ - &amp;&amp; || ! ( ) { } [ ] ^ &quot; ~ * ? : \</samp></td>
	<td align="top" colspan="2" class="small">These special characters, when in a search term, must be escaped with a preceding backslash (<b>\</b>)<br>
	Example: search for [3+5]: <samp>\[3\+5\]</samp></td>
</tr>
</table>
</form>
<script language="javascript">
	changeTypes(document.forms['find'].elements['<%= searchTypes[0] %>']);
</script>
<%
// <form action="testIndexer.jsp"><input type="submit" name="reindex" value="re-index" class="little"></form>
%>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>
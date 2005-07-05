<%@ page import="org.genepattern.server.indexer.Indexer, 
		 java.io.File,
		 org.apache.lucene.index.IndexWriter,
		 org.apache.lucene.index.IndexReader,
		 org.apache.lucene.index.Term"
    session="false" language="Java" %>
<%
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String CMD = "cmd";
String TYPE = "type";
String ID = "ID";
String FILENAME = "filename";

// CMD settings
String INDEX = "index";
String DELETE = "delete";
String RESET = "reset";

// TYPE settings
String TASK = "task";
String JOB = "job";
String JOB_OUTPUT_FILE = "job output file";
String ALL = "all";

String cmd = request.getParameter(CMD);
String indexType = request.getParameter(TYPE);
String id = request.getParameter(ID);
String filename = request.getParameter(FILENAME);
%>

<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>test Indexer</title>
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<form>

<table width="90%" cols="2">
<tr>
<td align="right" width="15%">
Command:
</td>
<td>
<input name="<%= CMD %>" type="radio" value="<%= INDEX %>" <%= cmd != null && cmd.equals(INDEX) ? "checked" : ""%>>index
<input name="<%= CMD %>" type="radio" value="<%= DELETE %>" <%= cmd != null && cmd.equals(DELETE) ? "checked" : ""%>>delete
<input name="<%= CMD %>" type="radio" value="<%= RESET %>" <%= cmd != null && cmd.equals(RESET) ? "checked" : ""%>>reset
</td>
</tr>

<tr>
<td align="right" width="15%">
Type: 
</td>
<td>
<nobr><input name="<%= TYPE %>" type="radio" value="<%= TASK %>" <%= indexType != null && indexType.equals(TASK) ? "checked" : ""%>>task</nobr>
<nobr><input name="<%= TYPE %>" type="radio" value="<%= JOB %>" <%= indexType != null && indexType.equals(JOB) ? "checked" : ""%>>job</nobr>
<nobr><input name="<%= TYPE %>" type="radio" value="<%= JOB_OUTPUT_FILE %>" <%= indexType != null && indexType.equals(JOB_OUTPUT_FILE) ? "checked" : ""%>>job output file (delete only)</nobr>
<nobr><input name="<%= TYPE %>" type="radio" value="<%= ALL %>" <%= indexType != null && indexType.equals(ALL) ? "checked" : ""%>>all (index only)</nobr>
</td>
</tr>

<tr>
<td align="right" width="15%">TaskID/JobID: </td>
<td><input name="<%= ID %>" value="<%= id != null ? id : "" %>"></td>
</tr>

<tr>
<td align="right" width="15%">Job filename: </td>
<td><input name="<%= FILENAME %>" value="<%= filename != null ? filename : "" %>">
</td>
</tr>

<tr><td></td></tr>

<tr>
<td width="15%"></td><td><input type="submit" value="submit"></td>
</tr>

</table>
</form>
<pre>
<% 
File indexDir = new File(System.getProperty("genepattern.properties") + "/../index");

if (cmd != null && indexType != null) {
	Indexer indexer = new Indexer(out);

        IndexWriter writer = null;

	// delete [task | job | file] [taskName | jobID | jobID] [filename_for_job]
	try {
		int numDeleted = 0;
		if (cmd.equals(DELETE)) {
			if (indexType.equals(TASK)) {
				numDeleted = indexer.deleteTask(Integer.parseInt(id));
			} else if (indexType.equals(JOB)) {
				numDeleted = indexer.deleteJob(Integer.parseInt(id));
			} else if (indexType.equals(JOB_OUTPUT_FILE)) {
				numDeleted = indexer.deleteJobFile(Integer.parseInt(id), filename);
			}
			out.println("deleted " + numDeleted + " documents from index");
		} else if (cmd.equals(INDEX)) {
			if (indexType.equals(TASK)) {
				writer = new IndexWriter(indexDir, Indexer.GPAnalyzer, false);
				indexer.indexTask(writer, Integer.parseInt(id));
			} else if (indexType.equals(JOB)) {
				writer = new IndexWriter(indexDir, Indexer.GPAnalyzer, false);
				indexer.indexJob(writer, Integer.parseInt(id));
			} else if (indexType.equals(ALL)) {
				indexer.reset(indexDir);
			        indexer.index(indexDir);
			}
		} else if (cmd.equals(RESET)) {
			Indexer.reset(indexDir);
			out.println("Indexes reset");
		} else {
			out.println("Don't understand command " + cmd);
		}
	} catch (Throwable t) {
		out.println(t);
		t.printStackTrace();
	} finally {
		if (writer != null) writer.close();
	}
}
%>
</pre>
<a href="search.jsp">search</a><br>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

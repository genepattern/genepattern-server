<%@ page import="java.io.File,
		 java.io.FileInputStream,
		 java.io.FileOutputStream,
		 java.io.IOException,
		 java.io.PrintWriter,
		 java.io.UnsupportedEncodingException,
		 java.net.URLDecoder,
		 java.net.URLEncoder,
         java.text.DecimalFormat,
		 java.text.SimpleDateFormat,
		 java.util.Calendar,
		 java.util.Date,
		 java.util.Enumeration,
		 java.util.Hashtable,
		 java.util.zip.ZipEntry,
		 java.util.zip.ZipOutputStream,
		 java.sql.*,
		 org.genepattern.server.util.BeanReference,
		 org.genepattern.server.ejb.AnalysisJobDataSource,
		 org.genepattern.webservice.JobStatus,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.server.indexer.Indexer,
		 org.genepattern.util.GPConstants,
		 com.jspsmart.upload.*"
	session="false" contentType="text/html" language="Java" %><jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" /><% 

	/*********************************************************
	 NOTE THAT THIS PAGE CANNOT BE MARKED NON-CACHEABLE.
	 Bug 555 attempted to do this, and bug 600 was the result.
	 *********************************************************/

try {
	int i;
	int j;
	String attachmentName;
	File attachment;
	String jobID = request.getParameter("jobID");
	String[] attachmentNames = request.getParameterValues("dl");

	String name = request.getParameter("name"); 
        boolean DEBUG = (request.getParameter("DEBUG") != null);
	boolean isDelete = (request.getParameter("delete") != null);

	if (isDelete || name == null || name.length() == 0) {
		response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
		response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
		response.setDateHeader("Expires", 0);
%>
		<html>
		<head>
		<link href="stylesheet.css" rel="stylesheet" type="text/css">
		<link href="favicon.ico" rel="shortcut icon">
		<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
<%
	}

	if (isDelete) {
%>
		<title>delete job results</title>
		<script language="Javascript">
			function checkAll(frm, bChecked) {
				frm = document.forms["results"];
				for (i = 0; i < frm.elements.length; i++) {
					if (frm.elements[i].type != "checkbox") continue;
					frm.elements[i].checked = bChecked;
				}
			}
		</script>
		</head>
		<body>
		<jsp:include page="navbar.jsp"></jsp:include>
<%
		String jobDir = System.getProperty("jobs");
		if (jobDir.startsWith("./")) jobDir = jobDir.substring(2);
		if (jobDir.startsWith("/")) jobDir = jobDir.substring(1);


		if (attachmentNames != null) {
			for (j = 0; j < attachmentNames.length; j++) {
				attachmentName = attachmentNames[j];
				i = attachmentName.indexOf("=");
				if (i == -1) continue; // either the submit button or the task name
				String fname = attachmentName.substring(i+1);
				attachment = new File(jobDir, fname);
				try {
 					attachment.delete();
	 				try {
 						int numDeleted = Indexer.deleteJobFile(Integer.parseInt(jobID), fname);
 					} catch (IOException ioe) {
 						// ignore Lucene Lock obtain timed out exceptions
						System.err.println(ioe + " while deleting search indices for job " + jobID);
 					}
					out.println("deleted " + URLDecoder.decode(attachmentName.substring(0,i), "UTF-8") +
						    " (job " + jobID + ")<br>");
				} catch (Exception e) {
					out.println("Not a job file: " + fname + "<br>");
				}
			}
		}
		out.println("<br>");
	}

	if (isDelete || name == null || name.length() == 0) {

SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm:ss");
Calendar midnight = Calendar.getInstance();
midnight.set(Calendar.HOUR_OF_DAY, 0);
midnight.set(Calendar.MINUTE, 0);
midnight.set(Calendar.SECOND, 0);
midnight.set(Calendar.MILLISECOND, 0);

String STOP = "stop";
String SHOW_ALL = "showAll";
boolean showAll = (request.getParameter(SHOW_ALL) != null);
String userID = GenePatternAnalysisTask.getUserID(request, null); // get userID but don't force login if not defined
if (userID == null || userID.length() == 0) {
	return;
}

String stopTaskID = request.getParameter(STOP);
if (stopTaskID != null) {
	Process p = GenePatternAnalysisTask.terminatePipeline(stopTaskID);
	if (p != null) {
	    try {
		GenePatternAnalysisTask.updatePipelineStatus(Integer.parseInt(stopTaskID), JobStatus.JOB_ERROR, null);
	    } catch (Exception e) { /* ignore */ }
	}
}


AnalysisJobDataSource ds = BeanReference.getAnalysisJobDataSourceEJB();

String sql = "select job_no, task_name, analysis_job.user_id, date_submitted, date_completed, status_id, status_name, parameter_info, task_master.lsid from analysis_job, task_master, job_status where analysis_job.task_id=task_master.task_id and analysis_job.status_id=job_status.status_id and analysis_job.status_id != " + JobStatus.JOB_NOT_STARTED;
if (!showAll) sql = sql + " and analysis_job.user_id = '" + userID + "'";
//sql = sql + " order by job_no";
sql = sql + " union select job_no, ifnull(NULL, 'pipeline - ' || user_id) as task_name, analysis_job.user_id, date_submitted, date_completed, status_id, status_name, parameter_info, user_id as lsid from analysis_job, job_status where analysis_job.task_id=-1 and analysis_job.status_id=job_status.status_id and analysis_job.status_id != " + JobStatus.JOB_NOT_STARTED;
if (!showAll) sql = sql + " and analysis_job.user_id = '" + userID + "'";
sql = sql + " order by job_no";

ParameterFormatConverter parameterFormatConverter = new ParameterFormatConverter();
DecimalFormat df = new DecimalFormat();

try {
	ResultSet rs = ds.executeSQL(sql);
	boolean isFirst = true;
	SimpleDateFormat fmt = null;
	Date d = null;
	Hashtable htColors = new Hashtable();
	htColors.put("" + JobStatus.JOB_NOT_STARTED, "red");
	htColors.put("" + JobStatus.JOB_PROCESSING, "blue");
	htColors.put("" + JobStatus.JOB_FINISHED, "green");
	htColors.put("" + JobStatus.JOB_ERROR, "red");
	htColors.put("" + JobStatus.JOB_TIMEOUT, "red");
	Hashtable filenames = new Hashtable();
	StringBuffer buf = new StringBuffer();

	if (!isDelete) {
%>
		<title>job results</title>
		<body>
		<jsp:include page="navbar.jsp"></jsp:include>
<%	} %>

	<form>
	<input type="checkbox" name="<%= SHOW_ALL %>" <%= showAll ? "checked" : "" %> value="<%= SHOW_ALL %>"
	onclick="javascript:window.location='zipJobResults.jsp<%= showAll ? "" : ("?" + SHOW_ALL + "=1") %>'">show everyone's jobs
	</form>

<%
	if (rs.last())
	do {
//	while (rs.next()) {
		if (isFirst) {
			isFirst = false;
%>
		<table cellspacing="4">
		<tr>
			<td align="right"><b><u>job &nabla;</u></b></td>
			<td><b><u>task</u></b></td>
			<td><b><u>submitted</u></b></td>
			<td><b><u>completed</u></b></td>
			<td><b><u>status</u></b></td>
		</tr>
<%
		} // end if isFirst

		boolean firstFile = true;
		boolean anyExists= false;
		filenames.clear();
                ParameterInfo[] params = parameterFormatConverter.getParameterInfoArray(rs.getString("parameter_info"));
		buf.setLength(0);
                if (DEBUG) {
                	buf.append("<tr><td colspan=\"5\">There are " + (params != null ? params.length : 0) + " parameters</td></tr>");
                }
		for (i = 0; params != null && i < params.length; i++) {
			ParameterInfo p = params[i];
			if (p.isOutputFile()) {
				String filename = p.getValue();
                                if (DEBUG) {
                                	if (firstFile) { buf.append("<tr><td colspan=\"5\">"); }
					buf.append("job " + rs.getString("job_no") + " outputfile params[" +  i + "]=" + p.getName() + "=" + filename + "<br>\n");
                                	if (firstFile) { buf.append("</td></tr>\n"); }
                                }
				File f = new File(System.getProperty("jobs"), filename);
				String baseName = f.getName();
				if (filenames.contains(filename)) {
        				if (DEBUG) {
                                           if (firstFile) { buf.append("<tr><td colspan=\"5\">"); } 
					   buf.append("already contains " + baseName + "<br>\n");
                                           if (firstFile) { buf.append("</td></tr>\n"); }
        				}
					continue;
				}
				filenames.put(p.getName(), filename);
				boolean exists = f.exists();
				if (firstFile) {
					firstFile = false;
					buf.append("<form>\n");
					buf.append("<tr><td></td><td colspan=\"4\">\n");
					buf.append("<input type=\"hidden\" name=\"jobID\" value=\"" + rs.getString("job_no") + "\">\n");
					buf.append("<input type=\"hidden\" name=\"name\" value=\"" + rs.getString("task_name") + "\">\n");
					if (showAll) buf.append("<input type=\"hidden\" name=\"" + SHOW_ALL + "\" value=\"1\">\n");
				}
				if (exists) {
				    anyExists = true;
				    String sourceJobID = f.getParent();
				    sourceJobID = sourceJobID.substring(1+sourceJobID.lastIndexOf(File.separator));
				    String sourceTaskName = p.getName().equals(baseName) ? rs.getString("task_name") : p.getName();


				    try {
				        buf.append("<input type=\"checkbox\" value=\"" + sourceTaskName + "/" + baseName + "=" + filename + "\" " +
				    	       "name=\"dl\" checked><a href=\"retrieveResults.jsp?job=" + sourceJobID + "&filename=" + URLEncoder.encode(baseName, "utf-8") + "\" target=\"_blank\">" +
					  ((!p.getName().equals(baseName) && (p.getName().length()>0)) ? (p.getName() + "/") : "") + GenePatternAnalysisTask.htmlEncode(URLDecoder.decode(baseName, "UTF-8")) + "</a>" +
					  (!sourceJobID.equals(rs.getString("job_no")) ? 
					  	(" (job <a href=\"getJobResults.jsp?jobID=" + sourceJobID + "\">" + sourceJobID + "</a>, &nbsp;" + df.format(f.length()).toString() + " bytes)") : 
                                 (", &nbsp;" + df.format(f.length()).toString() + " bytes") +
				 	 (showAll ? (", " + GenePatternAnalysisTask.htmlEncode(rs.getString("user_id"))) : "")));
				    } catch (UnsupportedEncodingException uee) {
						// ignore
				    }
				    buf.append("<br>\n");
				} else {
				    buf.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<strike>"+(!p.getName().equals(baseName) ?
					   (p.getName() + "/") : "") + GenePatternAnalysisTask.htmlEncode(URLDecoder.decode(baseName, "UTF-8"))+"</strike><br>");
				}
			}
		}
		if (anyExists) {
			buf.append("<input type=\"submit\" name=\"delete\" value=\"delete\" class=\"little\">\n");
			buf.append("<input type=\"submit\" name=\"download\" value=\"download\" class=\"little\">\n");
			buf.append("</td>\n</tr>\n</form>\n");
		}		


%>
		<tr>
		<td align="right"><a <% if (anyExists) { %>href="getJobResults.jsp?jobID=<%= rs.getString("job_no") %>"<% } %> name="<%= rs.getString("job_no") %>"><%= rs.getString("job_no") %></a></td>
		<td><a href="addTask.jsp?<%= GPConstants.NAME %>=<%= rs.getString("lsid") %>&view=1"><%= rs.getString("task_name") %></a></td>
<%
		d = rs.getTimestamp("date_submitted");
		fmt = d.after(midnight.getTime()) ? shortDateFormat : dateFormat;
%>
		<td><%= fmt.format(d) %></td>
<%
		d = rs.getTimestamp("date_completed");
		if (d != null && rs.getInt("status_id") != JobStatus.JOB_PROCESSING) {
			fmt = d.after(midnight.getTime()) ? shortDateFormat : dateFormat;
%>
		<td><%= fmt.format(d) %></td>
<%
		} else {
%>
		<td></td>
<%
		}
%>
		<td><font color="<%= htColors.get(rs.getString("status_id")) %>"><%= rs.getString("status_name") %></font>
		<%= (rs.getInt("status_id") == JobStatus.JOB_PROCESSING) ? " - <a href=\"zipJobResults.jsp?" + STOP + "=" + rs.getString("job_no") + (showAll ? ("&" + SHOW_ALL + "=1") : "") + "\"><font color=\"red\">stop</font></a>" : "" %>
		</td>
	</tr>
<%
		out.print(buf.toString());
	} while (rs.previous());
	rs.close();
	if (isDelete) { %>
	   	<script language="javascript">
   		window.location.hash = "<%= jobID %>";
	   	</script>
<% 	} 
	if (!isFirst) {
%>
</table>
<%
	} else {
%>
		No jobs to show! <br>
<%
	}
%>
		<jsp:include page="footer.jsp"></jsp:include>
<%
} catch (SQLException se) {
	out.println(se + " for SQL: " + sql + "<br>");
}
%>
</form>
<%
		return;
	} // end if no name given

	// create zip file
	FileInputStream is = null;
	File zipFile = null;
	ZipOutputStream zos = null;
	String zipEntryFilename;
	ZipEntry zipEntry;
	boolean isFirst = true;
	
	byte[] buf = new byte[100000];
	// request parameters are of the format taskname/filename-without-path=filename-on-server
	for (j = 0; attachmentNames != null && j < attachmentNames.length; j++) {
		attachmentName = attachmentNames[j];
		i = attachmentName.indexOf("=");
		if (i == -1) continue; // either the submit button or the task name
		attachment = new File(System.getProperty("jobs"), attachmentName.substring(i+1));
		if (!attachment.exists()) {
			// file was deleted by user, let it go
			continue;
		}
		if (isFirst) {
			zipFile = new File(System.getProperty("java.io.tmpdir"), name.trim() + ".zip");
			zipFile.delete();
			zos = new ZipOutputStream(new FileOutputStream(zipFile));
			isFirst = false;
		}
		zipEntryFilename = attachmentName.substring(0, i);
		try {
			zipEntryFilename = URLDecoder.decode(zipEntryFilename, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			// ignore
		}
//		System.out.println("adding " + zipEntryFilename + " to " + zipFile + "<br>");
		zipEntry = new ZipEntry(zipEntryFilename);
		zipEntry.setTime(attachment.lastModified());
		zipEntry.setSize(attachment.length());
		zos.putNextEntry(zipEntry);
		long fileLength = attachment.length();
		long numRead = 0;
		is = new FileInputStream(attachment);
		int n;
		while ((n = is.read(buf, 0, buf.length)) > 0) {
			zos.write(buf, 0, n);
			numRead += n;
		}
		is.close();
		if (numRead != fileLength) throw new Exception("only read " + numRead + " of " + fileLength + " bytes in " + attachment.getPath());
		zos.closeEntry();
	}
	if (zos != null) {
		zos.finish();
		zos.close();

		mySmartUpload.initialize(pageContext);
		mySmartUpload.downloadFile(zipFile.getPath(),"application/x-zip-compressed", zipFile.getName());
		zipFile.delete();
	} else {
%>
		<html>
		<head>
		<link href="stylesheet.css" rel="stylesheet" type="text/css">
		<link href="favicon.ico" rel="shortcut icon">
		<meta http-equiv="content-type" content="text/html; charset=ISO-8859-1">
		<title>job results</title>
		<body>
		<jsp:include page="navbar.jsp"></jsp:include>
		No files to zip!<br>
		<br>
		<a href="zipJobResults.jsp">choose again</a>
		<br>
		<jsp:include page="footer.jsp"></jsp:include>
<%
	}
	return;
} catch (Exception e) {
	out.println("<pre>" + e.getMessage() + " in zipJobResults.jsp</pre>");
	e.printStackTrace(new PrintWriter(out));
%>
<jsp:include page="footer.jsp"></jsp:include>
<% } %>

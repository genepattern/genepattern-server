<%@ page import="java.sql.*,
		 java.net.URLEncoder,
		 java.util.Calendar,
		 java.util.Date,
		 java.util.Hashtable,
		 java.lang.StringBuffer,
		 java.text.SimpleDateFormat,
		 org.genepattern.server.util.BeanReference,
		 org.genepattern.server.ejb.AnalysisJobDataSource,
		 org.genepattern.webservice.JobStatus,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask"
	session="false" contentType="text/html" language="Java" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String INTERVAL = "interval";
String STOP = "stop";
String SHOW_ALL = "showAll";

String userID = GenePatternAnalysisTask.getUserID(request, null);

String refreshIntervalSeconds = request.getParameter(INTERVAL);
if (refreshIntervalSeconds == null) refreshIntervalSeconds = "15";
SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");
SimpleDateFormat shortDateFormat = new SimpleDateFormat("HH:mm:ss");
Calendar midnight = Calendar.getInstance();
midnight.set(Calendar.HOUR_OF_DAY, 0);
midnight.set(Calendar.MINUTE, 0);
midnight.set(Calendar.SECOND, 0);
midnight.set(Calendar.MILLISECOND, 0);

boolean showAll = (request.getParameter(SHOW_ALL) != null);

String stopTaskID = request.getParameter(STOP);
if (stopTaskID != null) {
	Process p = GenePatternAnalysisTask.terminatePipeline(stopTaskID);
	if (p != null) {
	    try {
		GenePatternAnalysisTask.updatePipelineStatus(Integer.parseInt(stopTaskID), JobStatus.JOB_ERROR, null);
	    } catch (Exception e) { /* ignore */ }
	}
}
%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
<link rel="SHORTCUT ICON" href="favicon.ico" >
<title>GenePattern job status on <%= request.getServerName() %>:<%= request.getServerPort() %></title>
<% if (refreshIntervalSeconds.length() > 0) { %>
<meta http-equiv="refresh" content="<%= refreshIntervalSeconds %>;URL=http://<%= request.getServerName() %>:<%= request.getServerPort() %>/gp/getStatus.jsp?<%= INTERVAL %>=<%= refreshIntervalSeconds %><%= showAll ? "&" + SHOW_ALL + "=1" : "" %>">
<% } %>
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<form>
<table cols="3" width="100%">
	<tr>
		<td colspan="2">Status of <%= showAll ? "everyone's" : (userID.length() > 0 ? (GenePatternAnalysisTask.htmlEncode(userID) + "'s") : "") %> jobs on <%= request.getServerName() %>:<%= request.getServerPort() %></td>
		<td align="right"><%= dateFormat.format(new Date()) %></td>
	</tr> 
	<tr>
		<td><input type="checkbox" name="<%= SHOW_ALL %>" <%= showAll ? "checked" : "" %>
		     onclick="javascript:window.location='getStatus.jsp<%= showAll ? "" : ("?" + SHOW_ALL + "=1") %>'">show everyone's jobs</td>
		<td align="right" colspan="2"><input type="submit" value="refresh"> every <input name="<%= INTERVAL %>" value="<%= refreshIntervalSeconds %>" size="3"> seconds</td>
	</tr>
</table>
<%
AnalysisJobDataSource ds = BeanReference.getAnalysisJobDataSourceEJB();

StringBuffer sql = new StringBuffer();
sql.append("select job_no, task_name, analysis_job.user_id, date_submitted, date_completed, status_id, status_name from analysis_job, task_master, job_status where analysis_job.task_id=task_master.task_id and analysis_job.status_id=job_status.status_id");
if (!showAll) sql.append(" and analysis_job.user_id='" + userID + "'");
sql.append(" union select job_no, ifnull(input_filename, 'pipeline - ' || user_id) as task_name, analysis_job.user_id, date_submitted, date_completed, status_id, status_name from analysis_job, job_status where analysis_job.task_id=-1 and analysis_job.status_id=job_status.status_id");
if (!showAll) sql.append(" and analysis_job.user_id='" + userID + "'");
sql.append(" order by job_no");

try {
	ResultSet rs = ds.executeSQL(sql.toString());
	boolean isFirst = true;
	SimpleDateFormat fmt = null;
	Date d = null;
	Hashtable htColors = new Hashtable();
	htColors.put("" + JobStatus.JOB_NOT_STARTED, "red");
	htColors.put("" + JobStatus.JOB_PROCESSING, "blue");
	htColors.put("" + JobStatus.JOB_FINISHED, "green");
	htColors.put("" + JobStatus.JOB_ERROR, "red");
	htColors.put("" + JobStatus.JOB_TIMEOUT, "red");

	if (rs.last())
	do {
//	while (rs.next()) {
		if (isFirst) {
			isFirst = false;
%>
<table cellspacing="4">
	<tr>
		<td align="right" valign="bottom"><b><u>job &nabla;</u></b></td>
		<td valign="bottom"><b><u>task</u></b></td>
		<td valign="bottom"><b><u>submitted</u></b></td>
		<td valign="bottom"><b><u>completed</u></b></td>
		<td valign="bottom"><b><u>status</u></b></td>
	</tr>
<%
		}
%>
	<tr>
		<td align="right"><a href="getJobResults.jsp?jobID=<%= rs.getString("job_no") %>"><%= rs.getString("job_no") %></a></td>
		<td><%= rs.getString("task_name") %><% if (showAll) { %> (<%= GenePatternAnalysisTask.htmlEncode(rs.getString("user_id")) %>)<% } %></td>
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
		<%= (rs.getInt("status_id") == JobStatus.JOB_PROCESSING) ? " - <a href=\"getStatus.jsp?" + STOP + "=" + rs.getString("job_no") + "&" + INTERVAL + "=" + refreshIntervalSeconds + (showAll ? ("&" + SHOW_ALL + "=1") : "") + "\"><font color=\"red\">stop</font></a>" : "" %>
		</td>
	</tr>
<%
		
	} while (rs.previous());
	rs.close();
	if (!isFirst) {
%>
</table>
<%
	} else {
%>
	<br><br>No jobs to show! <br>
<%
	}
} catch (SQLException se) {
	out.println(se + " for SQL: " + sql + "<br>");
}
%>
</form>

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

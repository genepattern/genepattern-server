<!-- /*
  The Broad Institute
  SOFTWARE COPYRIGHT NOTICE AGREEMENT
  This software and its documentation are copyright (2003-2006) by the
  Broad Institute/Massachusetts Institute of Technology. All rights are
  reserved.

  This software is supplied without any warranty or guaranteed support
  whatsoever. Neither the Broad Institute nor MIT can be responsible for its
  use, misuse, or functionality.
*/ --><%@ page import="java.io.File,
		 java.net.URLEncoder,
		 java.util.Date,
		 org.genepattern.server.webservice.server.dao.AnalysisDAO,
		 org.genepattern.server.webservice.server.dao.AdminDAO,
		 org.genepattern.webservice.JobInfo,
		 org.genepattern.webservice.JobStatus,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.server.util.AccessManager"
	session="false" contentType="text/html" language="Java" buffer="10kb" %><%

response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);

String userID = (String)request.getAttribute("userID"); // get userID but don't force login if not defined
if (userID == null || userID.length() == 0) {
	return;
}

String JOBID = "jobID";
String jobID = request.getParameter(JOBID);
String DOWNLOAD_URL = "zipJobResults.jsp?download=&?name=" + jobID;
AnalysisDAO ds = new AnalysisDAO();
JobInfo jobInfo = null;
ParameterInfo[] params = null;
TaskInfo taskInfo = null;

if (jobID == null) {
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>download job results</title>
<jsp:include page="navbarHead.jsp"/>
</head>

<body>
<jsp:include page="navbar.jsp"/>
<h2>Get job results</h2>
jobs: <%
	JobInfo[] jobs = ds.getJobInfo(new Date());
	for (int i = 0; i < jobs.length; i++) {
		jobInfo = jobs[i];
		// very slow to get return to user when it ask for task name!
		int taskID = -1; // jobInfo.getTaskID();
		taskInfo = (taskID == -1 ? null : (new AdminDAO()).getTask(taskID));
		params = jobInfo.getParameterInfoArray();
		boolean hasOutputFiles = false;
		if (params != null) {
			for (int p = 0; p < params.length; p++) {
				if (params[p].isOutputFile() && new File(params[p].getValue()).exists()) {
					hasOutputFiles = true;
					break;
				}
			}
		}
		String jobStatus = jobInfo.getStatus();
		if (!hasOutputFiles && !jobStatus.equals(JobStatus.NOT_STARTED) && !new File(System.getProperty("jobs") + File.separator + jobInfo.getJobNumber()).exists()) {
			continue;
		}
		String prefix = "";
		String suffix = "";
		if (hasOutputFiles) {
			prefix = "<a href=\"getJobResults.jsp?" + JOBID +"=" + jobInfo.getJobNumber() +"\">";
			suffix = "</a>";
		} else if (!jobStatus.equals(JobStatus.PROCESSING)) {
			prefix = "<s>";
			suffix = "</s>";
		}
%>
		<%= taskInfo != null ? (taskInfo.getName()  + ": ") : ""%><%= prefix %><%= jobInfo.getJobNumber() %><%= suffix %>&nbsp; 
<%	} %>

<br><br>
<form>
job #: <input name="<%= JOBID %>" size="8"> <input type="submit" name="submit" value="download results" class="little">
</form>
<font size="-1">
<br>Job numbers without links indicate no output is available yet.<br>
<s>Jobs which are done but have no output</s>.<br>
</font>
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
	} else {
		try {
			jobInfo = ds.getJobInfo(Integer.parseInt(jobID));
		} catch (Exception e) {
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>download job results</title>
</head>
<body>
<jsp:include page="navbar.jsp"></jsp:include>
<h2>Get job results</h2>
			<font color="red"><%= jobID %> is not a valid job number</font><br><br>
			<a href="getJobResults.jsp">list of jobs</a><br><br>
			<form>
			job #: <input name="<%= JOBID %>" size="8"> <input type="submit" name="submit" value="download results" class="little">
			</form>
			<jsp:include page="footer.jsp"/>
			</body>
			</html>
<%
			return;
		}
		params = jobInfo.getParameterInfoArray();
		boolean gotFile = false;
		if (params != null) {
			StringBuffer url = new StringBuffer(DOWNLOAD_URL);
			url.append("&jobID=" + jobInfo.getJobNumber());
			for (int i = 0; i < params.length; i++) {
				if (params[i].isOutputFile()) {
					url.append("&dl=");
					String f = new File(params[i].getValue()).getName();
					if (params[i].getName().equals(f)) {
						url.append(URLEncoder.encode(f));
					} else {
						url.append(URLEncoder.encode(params[i].getName() + File.separator + new File(params[i].getValue()).getName()));
					}
					url.append(URLEncoder.encode("="));
					url.append(URLEncoder.encode(params[i].getValue()));
				}
			}
			response.sendRedirect(url.toString());
			return;
		} else {
%>
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>download job results</title>
</head>
<body>
<jsp:include page="navbar.jsp"/>
<h2>Get job results</h2>
			no output from job <%= jobID %><br>
			<br>
			<a href="getJobResults.jsp">list of jobs</a><br><br>
			<form>
			job #: <input name="<%= JOBID %>" size="8"> <input type="submit" name="submit" value="download results" class="little">
			</form>
			<jsp:include page="footer.jsp"/>
			</body>
			</html>
<%			return;
		}
	}
%>
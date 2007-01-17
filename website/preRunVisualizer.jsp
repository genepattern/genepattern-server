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


<%@ page
	import="java.io.IOException,
		 java.util.StringTokenizer,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.io.File,
		 java.io.FileInputStream,
		 java.io.FileOutputStream,
		 java.util.Date,
		 java.io.UnsupportedEncodingException,
		 java.net.InetAddress,
 		 java.net.URLEncoder, 
		 java.net.URLDecoder,
 		 java.text.SimpleDateFormat,
		 java.util.Date,
		 java.util.ArrayList,
		 java.util.Enumeration, 
		 java.util.GregorianCalendar,
		 java.text.ParseException,
		 java.text.DateFormat,
		 java.util.Properties,
		 java.nio.channels.FileChannel,
		 java.util.List,
		 java.util.Map,
		 java.util.Iterator,		
		 java.util.Enumeration,
		 org.apache.commons.fileupload.DiskFileUpload,
         org.apache.commons.fileupload.FileItem,
         org.apache.commons.fileupload.FileUpload,
 		 org.genepattern.util.StringUtils,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.util.LSID,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.webservice.AnalysisWebServiceProxy,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.JobInfo,
		 org.genepattern.server.webapp.RunTaskHelper,
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java"%>
<%
response.setHeader("Cache-Control", "no-store"); // HTTP 1.1 cache control
response.setHeader("Pragma", "no-cache");		 // HTTP 1.0 cache control
response.setDateHeader("Expires", 0);
%>

<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
<link href="skin/favicon.ico" rel="shortcut icon">
<title>Running Task</title>
<jsp:include page="navbarHead.jsp"/>
</head>
<body>

<jsp:include page="navbar.jsp"/>


<%

/**
 * To run a visualizer, we first upload the files here to get them a URL and then call
 * runVisualizer.jsp to actually launch it after we move the params out of the file upload into
 * a normal request
 */

String userID = (String) request.getAttribute(GPConstants.USERID);
try {
	String RUN = "run";
	String CLONE = "clone";
	
	
	// set up the call to the runVisualizer.jsp by putting the params into the request
	// and then forwarding through a requestDispatcher
	
	RunTaskHelper runTaskHelper = new RunTaskHelper(userID, request);
	TaskInfo task = runTaskHelper.getTaskInfo();
	ParameterInfo[] parmInfos = task.getParameterInfoArray();
	
	Map<String, String> requestParameters = runTaskHelper.getRequestParameters();
	String tmpDirName = runTaskHelper.getTempDirectory().getName();
	
	String lsid = runTaskHelper.getTaskLsid();
	String taskName = runTaskHelper.getTaskName();
	
	request.setAttribute("name", lsid);
	String server = request.getScheme() + "://"+ InetAddress.getLocalHost().getCanonicalHostName() + ":"
					+ System.getProperty("GENEPATTERN_PORT");

	List<ParameterInfo> missingReqParams = runTaskHelper.getMissingParameters();
	if (missingReqParams.size() > 0){
		System.out.println(""+missingReqParams);

		request.setAttribute("missingReqParams", missingReqParams);
		(request.getRequestDispatcher("runTaskMissingParams.jsp")).include(request, response);
%>
<jsp:include page="footer.jsp"/>
</body>
</html>
<%
		return;
	}

	for (int i=0; i < parmInfos.length; i++){
		ParameterInfo pinfo = parmInfos[i];
		String value = pinfo.getValue();	
		request.setAttribute(pinfo.getName(), value);
	}
	
	RequestDispatcher rd = request.getRequestDispatcher("runVisualizer.jsp");
	rd.include(request, response);
	GenePatternAnalysisTask.createVisualizerJob(userID, ParameterFormatConverter.getJaxbString(parmInfos) , taskName, lsid);
%>

<table width='100%' cellpadding='10'>
	<tr>
		<td>Running <a href="addTask.jsp?view=1&name=<%= lsid %>"><%=requestParameters.get("taskName")%></a>
		version <%= new LSID(lsid).getVersion() %> on <%=new Date()%></td>
	</tr>
	<tr>
		<td><%=requestParameters.get("taskName")%> ( <%
	for (int i=0; i < parmInfos.length; i++){
		ParameterInfo pinfo = parmInfos[i];
		String value = pinfo.getValue();	
		out.println(pinfo.getName());
		out.println("=");
		if (pinfo.isInputFile()) {
			String htmlValue = StringUtils.htmlEncode(pinfo.getValue());		
			if (value.startsWith("http:") || value.startsWith("https:") || value.startsWith("ftp:") || value.startsWith("file:")) {
				out.println("<a href='"+ htmlValue + "'>"+htmlValue +"</a>");
			} else {
				out.println("<a href='getFile.jsp?task=&file="+ URLEncoder.encode(tmpDirName +"/" + value)+"'>"+htmlValue +"</a>");
	
			}
		} else {
			out.println(StringUtils.htmlEncode(pinfo.getValue()));
		}
		if (i != (parmInfos.length -1))out.println(", ");
	}

} catch (Exception e){
	e.printStackTrace();
}

%> )<br>
		</td>
	</tr>
</table>




<jsp:include page="footer.jsp"/>

<%@ page import="java.io.IOException,
		 java.util.StringTokenizer,
		 java.util.Enumeration,
		 java.util.HashMap,
		 java.io.File,
		 java.util.Date,
		 java.io.UnsupportedEncodingException,
		 java.net.InetAddress,
 		 java.net.URLEncoder, 
		 java.net.URLDecoder,
 		 java.text.SimpleDateFormat,
		 java.util.Iterator,
		 java.util.Enumeration, 
		 java.util.ArrayList, 
		 java.util.GregorianCalendar,
		 java.text.ParseException,
		 java.text.DateFormat,
		
 		 org.genepattern.util.StringUtils,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.util.GPConstants,
		 org.genepattern.webservice.OmnigeneException,
		 org.genepattern.webservice.AnalysisWebServiceProxy,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.JobInfo,
		 com.jspsmart.upload.*,
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %>

<font size='+1' color='red'><b> Warning </b></font><br>
The task could not be run. The following required parameters need to have values provided;
<p>

<%
	ArrayList missingParams = (ArrayList)request.getAttribute("missingReqParams");
	
	for (int i=0; i < missingParams.size(); i++){
		ParameterInfo pinfo = (ParameterInfo)missingParams.get(i);
		out.println("&nbsp;&nbsp;&nbsp;&nbsp;<b>" + StringUtils.replaceAll(pinfo.getName(),"."," ") );
		out.println("</b><br>");
	
	}
%>
<p>
Hit the back button to fill them in and resubmit the job.


<%@ page import="java.io.IOException,
		 java.util.StringTokenizer,
		 java.util.Enumeration,
		 java.util.HashMap,
		 org.genepattern.webservice.TaskInfo,
		 org.genepattern.webservice.TaskInfoAttributes,
		 org.genepattern.webservice.ParameterFormatConverter,
		 org.genepattern.webservice.ParameterInfo,
		 org.genepattern.server.genepattern.GenePatternAnalysisTask,
		 org.genepattern.webservice.OmnigeneException,  com.jspsmart.upload.*,
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %>
<jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" />
<html>
<head>
<link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
	<link href="skin/favicon.ico" rel="shortcut icon">
	<title>Cannot Run Pipeline</title>
</head>
<body>
	<jsp:include page="navbar.jsp"></jsp:include>


<%
String taskName = (String)request.getAttribute("name");
%>
<p>
The <%=taskName%> cannot be run as the pipeline environment cannot prompt for runtime parameters.<p>	

<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>


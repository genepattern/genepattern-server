<%@ page import="java.io.IOException,
		 java.util.StringTokenizer,
		 java.util.Enumeration,
		 java.util.HashMap,
		 org.genepattern.analysis.TaskInfo,
		 org.genepattern.analysis.TaskInfoAttributes,
		 org.genepattern.analysis.ParameterFormatConverter,
		 org.genepattern.analysis.ParameterInfo,
		 org.genepattern.server.analysis.genepattern.GenePatternAnalysisTask,
		 org.genepattern.analysis.OmnigeneException,  com.jspsmart.upload.*,
		 org.genepattern.data.pipeline.PipelineModel"
	session="false" contentType="text/html" language="Java" %>
<jsp:useBean id="mySmartUpload" scope="page" class="com.jspsmart.upload.SmartUpload" />
<% String pipelineName = (String)request.getAttribute("name");%>
<html>
<head>
<link href="stylesheet.css" rel="stylesheet" type="text/css">
	<link href="favicon.ico" rel="shortcut icon">
	<title>Can't find <%= pipelineName %></title>
</head>
<body>
	<jsp:include page="navbar.jsp"></jsp:include>

 

<p>
Unknown pipeline: <%= pipelineName %>
<p>
<jsp:include page="footer.jsp"></jsp:include>
</body>
</html>

			
			
<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>


<%@ page import="java.io.IOException,
                 java.util.StringTokenizer,
                 java.util.Enumeration,
                 java.util.HashMap,
                 org.genepattern.webservice.TaskInfo,
                 org.genepattern.webservice.TaskInfoAttributes,
                 org.genepattern.webservice.ParameterFormatConverter,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.server.genepattern.GenePatternAnalysisTask,
                 org.genepattern.webservice.OmnigeneException,
                 org.genepattern.data.pipeline.PipelineModel"
         session="false" contentType="text/html" language="Java" %>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <title>Cannot Run Pipeline</title>
        <jsp:include page="navbarHead.jsp" />
    </head>

    <body>
        <jsp:include page="navbar.jsp" />
        <%
            String taskName = (String) request.getAttribute("name");
        %>
        <p>
            The <%=taskName%> cannot be run as pipelines that prompt for runtime parameters must be saved first.

        <p>

            <jsp:include page="footer.jsp" />
    </body>
</html>


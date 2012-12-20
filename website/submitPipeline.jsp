<%@ page import="
                 java.util.ArrayList,
                 java.util.List,
                 java.util.Map,
                 org.genepattern.server.webapp.RunTaskHelper,
                 org.genepattern.server.webservice.server.local.LocalAnalysisClient,
                 org.genepattern.util.GPConstants,
                 org.genepattern.webservice.JobInfo,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.TaskInfo,
                 org.apache.commons.fileupload.FileUploadException"
         session="false" contentType="text/html" language="Java" %>
<%--
  ~ Copyright 2012 The Broad Institute, Inc.
  ~ SOFTWARE COPYRIGHT NOTICE
  ~ This software and its documentation are the copyright of the Broad Institute, Inc. All rights are reserved.
  ~
  ~ This software is supplied without any warranty or guaranteed support whatsoever. The Broad Institute is not responsible for its use, misuse, or functionality.
  --%>

<%
    String userID = (String) request.getAttribute(GPConstants.USERID);
    RunTaskHelper runTaskHelper = null;
    TaskInfo task = null;
    List<ParameterInfo> missingReqParams = new ArrayList<ParameterInfo>();
    FileUploadException fileUploadException = null;
    try {
        runTaskHelper = new RunTaskHelper(userID, request);
        task = runTaskHelper.getTaskInfo();
        if (task == null) {
            out.println("Unable to find module");
            return;
        }
        missingReqParams = runTaskHelper.getMissingParameters();
    } catch (FileUploadException e) {
        fileUploadException = e;
    }
    if (fileUploadException != null || missingReqParams.size() > 0) {
        request.setAttribute("missingReqParams", missingReqParams);
%>
<html>
    <head>
        <link href="skin/stylesheet.css" rel="stylesheet" type="text/css">
        <link href="skin/favicon.ico" rel="shortcut icon">
        <link href="css/style.css" rel="stylesheet" type="text/css">
        <link href="css/style-jobresults.css" rel="stylesheet" type="text/css">
        <title>GenePattern - Run Module Results</title>
        <script language="Javascript" src="js/prototype.js"></script>
        <script language="Javascript" src="js/commons-validator-1.3.0.js"></script>
        <script language="Javascript" src="js/genepattern.js"></script>
        <jsp:include page="navbarHead.jsp" />
    </head>
    <body>

        <jsp:include page="navbar.jsp" />
        <% if (missingReqParams.size() > 0) { %>
        <jsp:include page="runTaskMissingParams.jsp" />
        <% } else { %>
        <font size='+1' color='red'><b> Warning </b></font><br>
        <% out.println(fileUploadException.getLocalizedMessage()); %>
        <p>

        <p>
            Hit the back button to resubmit the job.
                <% } %>
            <jsp:include page="footer.jsp" />
    </body>
</html>
<%
        return;
    }

    ParameterInfo[] paramInfos = task.getParameterInfoArray();
    paramInfos = paramInfos == null ? paramInfos = new ParameterInfo[0] : paramInfos;
    LocalAnalysisClient analysisClient = new LocalAnalysisClient(userID);
    JobInfo job = analysisClient.submitJob(task.getID(), paramInfos);
    String jobId = "" + job.getJobNumber();
    response.sendRedirect("/gp/pages/jobStatus.jsf?jobNumber=" + jobId);
%>

<%@ page import="
                 java.util.List,
                 java.util.Map,
                 org.genepattern.server.webapp.RunTaskHelper,
                 org.genepattern.server.webservice.server.local.LocalAnalysisClient,
                 org.genepattern.util.GPConstants,
                 org.genepattern.webservice.JobInfo,
                 org.genepattern.webservice.ParameterInfo,
                 org.genepattern.webservice.TaskInfo"
         session="false" contentType="text/html" language="Java" %>
<%
try {
	String userID = (String) request.getAttribute(GPConstants.USERID);
	RunTaskHelper runTaskHelper = new RunTaskHelper(userID, request);		
	TaskInfo task = runTaskHelper.getTaskInfo();
	if (task == null) {
		out.println("Unable to find module");
   		return;
	}

   	List<ParameterInfo> missingReqParams = runTaskHelper.getMissingParameters();
    if (missingReqParams.size() > 0) {
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
    <jsp:include page="navbarHead.jsp"/>
</head>
<body>

<jsp:include page="navbar.jsp"/>
<jsp:include page="runTaskMissingParams.jsp"/>
<jsp:include page="footer.jsp"/>
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
    response.sendRedirect("/gp/pages/jobResult.jsf?jobNumber="+jobId);
} 
catch (Throwable e) {
    org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(getClass());
    log.error(e);
}
%>
